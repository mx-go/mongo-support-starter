# mongo-support-starter

基于spring-boot和自己封装的nacos-config-core实现通过nacos可以动态更改mongo配置

# 解决了什么问题

1. mongodb快速使用，语法简单，傻瓜式操作；
2. 支持配置中心配置，动态更改配置信息，无需重启应用；

# 使用方式

## 引入Maven坐标

```properties
<dependency>
   <groupId>com.github.mx-go</groupId>
    <artifactId>mongo-support-starter</artifactId>
   <version>{latest-version}</version>
</dependency>
```

> 目前最新版本为 1.0.0

## 使用方法(例子)

### 配置中心配置

```properties
# mongo服务地址
mongo.servers=mongodb://userName:passWord@localhost:27017/database
trust.dbName==admin
# 需要扫描pojo的包
mongo.mapPackage=com.mx.entity
```

### Nacos读取的dataId

```java
@Configuration
public class MongoConfig {

    @Bean
    public MongoDataStoreFactoryBean mongoDatasource() {
        // 非必填：配置在nacos中的groupId
        // 必填：配置在nacos中的dataId
        return new MongoDataStoreFactoryBean("service-provider", "mongo.properties");
    }
}
```

### 实体类

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Entity(value = "student", noClassnameStored = true)
public class Student implements Serializable {
  
    private static final long serialVersionUID = 8727190668826580623L;

    @Id
    private String objectId;

    private Integer age;

    private String name;

    public Student(int age, String name) {
        this.age = age;
        this.name = name;
    }
}
```

### DAO查询接口

> 需要继承BaseDao

```java
@Repository
public interface StudentDao extends BaseDao<Student> {

    /**
     * 更新age
     */
    int updateAge(String id, Integer age);
}
```

### DAO实现类

> 继承BaseDaoImpl

```java
@Component
public class StudentDaoImpl extends BaseDaoImpl<Student> implements StudentDao {

    @Autowired
    public StudentDaoImpl(DatastoreExt mongoDatasource) {
        super(mongoDatasource, Student.class);
    }

    @Override
    public int updateAge(String id, Integer age) {

        Query<Student> query = createQuery();
        query.field("_id").equal(new ObjectId(id));

        UpdateOperations<Student> update = createUpdateOperations();
        update.set("age", age);

        return getDatastore().update(query, update).getUpdatedCount();
    }
}
```

### 使用(注入DAO)

```java
@Resource
private StudentDao studentDao;
```

# 配置中心可配置的参数

|             名称             |                 描述                 | 是否必填 |  默认值  |
| :--------------------------: | :----------------------------------: | :------: | :------: |
|        mongo.servers         |           MongDB集群连接串           |  **是**  |          |
|         mongo.dbName         |          mongodb连接库名称           |    否    |          |
|       mongo.mapPackage       |             扫描的包路径             |  **是**  |          |
|  mongo.ignoreInvalidClasses  |     指定是否忽略包中无法映射的类     |    否    |  false   |
|      mongo.storeEmpties      | 是否允许lists/map/set/arrays存储空值 |    否    |  false   |
|       mongo.storeNulls       |            是否存储null值            |    否    |  false   |
|           username           |            mongodb用户名             |    否    |          |
|           password           |             mongodb密码              |    否    |          |
|     mongo.readPreference     |            设置读取首选项            |    否    | primary  |
| mongo.serverSelectionTimeout |    设置服务器选择超时以毫秒为间隔    |    否    |  10000   |
|      mongo.maxWaitTime       |   设置的最长时间，线程阻塞等待连接   |    否    |  120000  |
| mongo.maxConnectionLifeTime  |       设置池连接的最大生命时间       |    否    | 86400000 |
| mongo.maxConnectionIdleTime  |       设置池连接的最大空闲时间       |    否    |  30000   |
| mongo.maxConnectionsPerHost  |       设置每个主机的最大连接数       |    否    |   100    |
|     mongo.connectTimeout     |             设置连接超时             |    否    |   5000   |
|     mongo.socketTimeout      |            设置套接字超时            |    否    |  60000   |