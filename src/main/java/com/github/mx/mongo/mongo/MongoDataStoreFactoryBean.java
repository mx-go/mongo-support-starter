package com.github.mx.mongo.mongo;

import com.github.mx.nacos.config.core.ConfigFactory;
import com.github.mx.nacos.config.core.RemoteConfig;
import com.github.mx.nacos.config.core.api.IConfig;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.Uninterruptibles;
import com.mongodb.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FactoryBean生成代理对象
 * <p>
 * Create by max on 2020/01/16
 */
@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class MongoDataStoreFactoryBean implements InitializingBean, DisposableBean, FactoryBean<DatastoreExt> {

    private static final Pattern MONGO_URI = Pattern.compile("mongodb://((.+):(.*)@)");
    /**
     * nacos中的groupId(不填默认取spring.application.name)
     */
    @Setter
    private String groupId;
    /**
     * nacos中的dataId
     */
    @Setter
    private String dataId;

    private IConfig config;
    private Map<String, DatastoreExt> stores = Maps.newConcurrentMap();
    private DatastoreExt first;

    @Override
    public void afterPropertiesSet() throws Exception {
        groupId = StringUtils.defaultString(groupId, ConfigFactory.getApplicationName());
        ConfigFactory.getInstance().registerListener(dataId, groupId, c -> {
            this.config = RemoteConfig.convert(c);
            loadConfig(config);
        });
    }

    @Override
    public void destroy() throws Exception {
        stores.values().forEach(it -> ((DatastoreHandler) Proxy.getInvocationHandler(it)).getDelegate().getMongo().close());
    }

    @Override
    public Class<?> getObjectType() {
        return DatastoreExt.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public DatastoreExt getObject() {
        return first;
    }

    DatastoreExt getOrCreate(String dbName, String format) {
        String key = Strings.isNullOrEmpty(format) ? dbName : (dbName + ':' + format);
        return stores.computeIfAbsent(key, it -> {
            List<String> values = Splitter.on(':').limit(2).splitToList(it);
            String db = values.get(0);
            String fmt = values.size() > 1 ? values.get(1) : null;
            ConnectionString connection = new ConnectionString(decodePassword(getUri(db)));
            Datastore datastore = doCreate(connection, fmt);
            DatastoreHandler handler = new DatastoreHandler(this, connection.getDatabase(), datastore);
            return Reflection.newProxy(DatastoreExt.class, handler);
        });
    }

    private void loadConfig(IConfig config) {
        if (stores.isEmpty()) {
            initFirst(config);
        } else {
            List<Mongo> oldClients = Lists.newArrayList();
            for (Iterator<String> it = stores.keySet().iterator(); it.hasNext(); ) {
                String key = it.next();
                DatastoreHandler handler = (DatastoreHandler) Proxy.getInvocationHandler(stores.get(key));
                oldClients.add(handler.getDelegate().getMongo());
                if (key.startsWith("mongodb://")) {
                    it.remove();
                } else {
                    List<String> values = Splitter.on(':').limit(2).splitToList(key);
                    String db = values.get(0);
                    String fmt = values.size() > 1 ? values.get(1) : null;
                    handler.setDelegate(doCreate(new ConnectionString(decodePassword(getUri(db))), fmt));
                }
            }
            // 延迟关闭正在使用的mongoClient
            new Thread(() -> {
                Uninterruptibles.sleepUninterruptibly(30, TimeUnit.SECONDS);
                log.warn("close {} old clients", oldClients.size());
                oldClients.forEach(Mongo::close);
            }).start();
        }
    }

    private void initFirst(IConfig config) {
        String dbName = config.get("mongo.dbName");
        String uriDbName = new ConnectionString(config.get("mongo.servers")).getDatabase();
        String name = firstNotEmpty(dbName, uriDbName, "admin");
        first = getOrCreate(name, null);
    }

    private String firstNotEmpty(String... names) {
        for (String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                return name;
            }
        }
        return null;
    }

    private String getUri(String dbName) {
        String servers = config.get("mongo.servers");
        // 如果配置的dbName和要使用的不一致,这里要做切换
        if (!servers.endsWith('/' + dbName)) {
            int pos = servers.lastIndexOf('/');
            // mongo.servers=mongodb://127.0.0.1:27017/test
            if (servers.charAt(pos - 1) != '/') {
                servers = servers.substring(0, pos + 1) + dbName;
            } else {
                servers = servers + '/' + dbName;
            }
        }
        String username = config.get("username");
        String password = config.get("password");
        String auth;
        if (!Strings.isNullOrEmpty(username)) {
            auth = Strings.isNullOrEmpty(password) ? username : (username + ':' + password);
            if (servers.startsWith("mongodb://")) {
                return "mongodb://" + auth + '@' + servers.substring(10);
            } else {
                return "mongodb://" + auth + '@' + servers;
            }
        }
        return servers;
    }

    private String decodePassword(final String servers) {
        String uri = servers;
        if (config.getBool("encrypt.pwd") || config.getBool("encrypt")) {
            Matcher m = MONGO_URI.matcher(servers);
            if (m.find()) {
                try {
                    /**
                     * 解密的具体逻辑
                     * String pwd = decode();
                     * uri = servers.substring(0, m.end(2) + 1) + pwd + servers.substring(m.end(1) - 1);
                     */
                } catch (Exception e) {
                    log.error("cannot decode " + m.group(3), e);
                }
            }
        }
        return uri;
    }

    private Datastore doCreate(ConnectionString connection, String format) {
        MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
        builder.readPreference(ReadPreference.valueOf(config.get("mongo.readPreference", "primary")))
                .serverSelectionTimeout(config.getInt("mongo.serverSelectionTimeout", 10000))
                .maxWaitTime(config.getInt("mongo.maxWaitTime", 120000))
                .maxConnectionLifeTime(config.getInt("mongo.maxConnectionLifeTime", 86400000))
                .maxConnectionIdleTime(config.getInt("mongo.maxConnectionIdleTime", 30000))
                .connectionsPerHost(config.getInt("mongo.maxConnectionsPerHost", 100))
                .connectTimeout(config.getInt("mongo.connectTimeout", 5000))
                .socketTimeout(config.getInt("mongo.socketTimeout", 60000));
        try {
            // 低版本的driver没有这个方法，可以忽略
            builder.applicationName(ConfigFactory.getApplicationName());
        } catch (NoSuchMethodError ignore) {
        }

        Mapper mapper = new Mapper();
        if (!Strings.isNullOrEmpty(format)) {
            mapper = new MapperExt(format);
        }
        MapperOptions options = new MapperOptions();
        options.setStoreEmpties(config.getBool("mongo.storeEmpties"));
        options.setStoreNulls(config.getBool("mongo.storeNulls"));
        mapper.setOptions(options);
        Morphia morphia = new Morphia(mapper);
        morphia.mapPackage(config.get("mongo.mapPackage"), config.getBool("mongo.ignoreInvalidClasses"));
        MongoClient mongo = new MongoClient(new MongoClientURI(getAuthorizedURI(connection), builder));
        return morphia.createDatastore(mongo, connection.getDatabase());
    }

    private String getAuthorizedURI(ConnectionString connection) {
        String uri = connection.getConnectionString();
        String trustDb = config.get("trust.dbName", config.get("authSource"));
        if ("admin".equals(trustDb)) {
            int pos = uri.indexOf('?');
            // mongo底层authSource参数只支持admin
            return uri + (pos > 0 ? "&authSource=admin" : "?authSource=admin");
        }
        if (connection.getDatabase() != null) {
            return uri;
        }
        String dbName = config.get("mongo.dbName");
        if (dbName != null) {
            return uri + '/' + dbName;
        }
        return uri;
    }
}
