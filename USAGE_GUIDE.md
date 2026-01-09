# Vector Database SDK Java - 使用和验证指南

## 📋 目录
1. [环境准备](#环境准备)
2. [编译项目](#编译项目)
3. [配置连接参数](#配置连接参数)
4. [运行示例代码](#运行示例代码)
5. [验证方法](#验证方法)

---

## 🔧 环境准备

### 必需条件
- **Java**: JDK 8 或更高版本
- **Maven**: 3.6 或更高版本
- **网络**: 需要访问 Maven 中央仓库和腾讯云向量数据库

### 验证环境
```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version
```

---

## 🏗️ 编译项目

### 方法一：编译整个项目
```bash
cd vectordatabase-sdk-java/tcvectordb
mvn clean compile
```

### 方法二：编译并打包
```bash
cd vectordatabase-sdk-java/tcvectordb
mvn clean package -DskipTests
```

编译成功后，会在 `target/` 目录生成 JAR 文件：
```
target/vectordatabase-sdk-java-2.6.0.jar
```

---

## ⚙️ 配置连接参数

### 步骤 1: 获取腾讯云向量数据库凭证

需要先在腾讯云创建向量数据库实例，获取：
- **数据库 URL** (`vdbURL`)
- **API Key** (`vdbKey`)

参考文档：https://cloud.tencent.com/document/product/1709/94951

### 步骤 2: 修改配置文件

编辑 `tcvectordb/src/main/java/com/tencent/tcvectordb/examples/CommonService.java`:

```java
private static ConnectParam initConnectParam() {
    // 替换为你的数据库 URL
    String vdbURL = "http://your-vdb-instance.tencentcloudapi.com";

    // 替换为你的 API Key
    String vdbKey = "your-api-key-here";

    return ConnectParam.newBuilder()
            .withUrl(vdbURL)
            .withUsername("root")
            .withKey(vdbKey)
            .withTimeout(30)
            .build();
}
```

---

## 🚀 运行示例代码

### 可用示例列表

项目提供了多个示例，位于 `tcvectordb/src/main/java/com/tencent/tcvectordb/examples/`:

1. **VectorDBExample.java** - 基础功能演示
   - 创建数据库和集合
   - 插入向量数据
   - 查询和过滤
   - 索引管理

2. **VectorDBExampleWithEmbedding.java** - Embedding 功能
   - 文本向量化
   - 向量检索

3. **VectorDBWithFullTextSearchExample.java** - 全文搜索
   - 文本索引
   - 关键词搜索

4. **VectorDBWithHybridSearchEmbeddingExample.java** - 混合搜索
   - 向量检索 + 全文搜索

5. **VectorDBExampleWithSparseVector.java** - 稀疏向量
   - 稀疏向量存储和检索

### 运行方式

#### 方法 1: 使用 Maven 运行
```bash
cd vectordatabase-sdk-java/tcvectordb

# 运行基础示例
mvn exec:java -Dexec.mainClass="com.tencent.tcvectordb.examples.VectorDBExample"

# 运行 Embedding 示例
mvn exec:java -Dexec.mainClass="com.tencent.tcvectordb.examples.VectorDBExampleWithEmbedding"

# 运行全文搜索示例
mvn exec:java -Dexec.mainClass="com.tencent.tcvectordb.examples.VectorDBWithFullTextSearchExample"
```

#### 方法 2: 编译后运行
```bash
# 先编译
mvn clean package -DskipTests

# 运行
java -cp target/vectordatabase-sdk-java-2.6.0.jar:target/lib/* \
  com.tencent.tcvectordb.examples.VectorDBExample
```

#### 方法 3: 使用 IDE
1. 在 IntelliJ IDEA 或 Eclipse 中导入项目
2. 等待 Maven 依赖下载完成
3. 找到示例文件，右键点击 `Run`

---

## ✅ 验证方法

### 1. 编译验证
```bash
cd tcvectordb
mvn clean compile
```
成功标志：显示 `BUILD SUCCESS`

### 2. 依赖验证
```bash
mvn dependency:tree
```
检查所有依赖是否正确下载

### 3. 运行验证

运行基础示例后，应该看到类似输出：

```
--------create database-------
    database: {"dbName":"java_sdk_book","info":"..."}

--------create collection-------
    collection: {"database":"java_sdk_book","collectionName":"java_sdk_book_segments"...}

--------upsert document-------
    upsert result: {"code":0,"msg":"success","affectedCount":5}

--------query document-------
    query result: {"count":3,"documents":[...]}
```

### 4. 功能验证清单

- [ ] 编译成功，无错误
- [ ] 可以连接到向量数据库
- [ ] 可以创建数据库和集合
- [ ] 可以插入向量数据
- [ ] 可以查询数据
- [ ] 可以执行向量检索
- [ ] 可以删除数据

---

## 🔍 代码示例解析

### 基础使用流程

```java
// 1. 初始化客户端
VectorDBClient client = CommonService.initClient();

// 2. 创建数据库
CreateDatabaseParam param = CreateDatabaseParam.newBuilder()
    .withDbName("my_database")
    .build();
Database db = client.createDatabase(param);

// 3. 创建集合
CreateCollectionParam collParam = CreateCollectionParam.newBuilder()
    .withName("my_collection")
    .addVectorField(VectorField.newBuilder()
        .withName("vector")
        .withDimension(768)
        .withIndexType(IndexType.HNSW)
        .build())
    .addScalarField(ScalarField.newBuilder()
        .withName("text")
        .withFieldType(FieldType.String)
        .build())
    .build();
Collection coll = db.createCollection(collParam);

// 4. 插入数据
Document doc = Document.newBuilder()
    .withId("doc1")
    .withVector(Arrays.asList(0.1, 0.2, 0.3, ...))
    .addDocField(new DocField("text", "示例文本"))
    .build();
coll.upsert(InsertParam.newBuilder()
    .addAllDocument(Collections.singletonList(doc))
    .build());

// 5. 查询数据
SearchResult result = coll.search(SearchParam.newBuilder()
    .withVectors(Arrays.asList(Arrays.asList(0.1, 0.2, 0.3, ...)))
    .withLimit(10)
    .build());

// 6. 关闭客户端
client.close();
```

---

## 🐛 常见问题

### 问题 1: 编译失败 - 依赖下载失败
**解决方法**:
```bash
# 清理本地仓库缓存
rm -rf ~/.m2/repository/com/tencent/tcvectordb

# 重新编译
mvn clean compile
```

### 问题 2: 连接数据库失败
**检查项**:
- 确认 `vdbURL` 和 `vdbKey` 配置正确
- 检查网络连接
- 验证数据库实例是否已启动

### 问题 3: 运行示例时找不到主类
**解决方法**:
```bash
# 确保先编译
mvn clean compile

# 或者使用完整的包名
mvn exec:java -Dexec.mainClass="com.tencent.tcvectordb.examples.VectorDBExample"
```

---

## 📚 相关资源

- [腾讯云向量数据库官方文档](https://cloud.tencent.com/document/product/1709)
- [API 参考文档](https://cloud.tencent.com/document/product/1709/97768)
- [GitHub 仓库](https://github.com/Tencent/vectordatabase-sdk-java)

---

## 💡 下一步

1. 阅读示例代码，了解各个功能的使用方法
2. 根据自己的需求修改示例代码
3. 参考 API 文档实现自己的向量检索应用
4. 进行性能测试和优化

---

最后更新：2026-01-09
