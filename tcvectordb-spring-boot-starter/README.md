# 腾讯云向量数据库 Spring Boot Starter

基于腾讯云向量数据库官方Java SDK的Spring Boot集成项目,支持Embedding自动向量化功能。

## 功能特性

- ✅ **自动初始化**: 应用启动时自动创建Database和Collection
- ✅ **Embedding自动向量化**: 插入和查询时自动调用腾讯云Embedding服务
- ✅ **Excel文件上传**: 批量导入Excel数据到向量数据库
- ✅ **文本检索**: 输入文本自动转换为向量进行相似度检索
- ✅ **混合检索**: 结合向量检索和全文检索(需配置稀疏向量索引)
- ✅ **完整异常处理**: 统一的错误响应格式
- ✅ **RESTful API**: 标准的HTTP接口

## 技术架构

- **Spring Boot**: 2.7.18
- **腾讯云向量数据库SDK**: 2.6.0
- **Apache POI**: 5.2.3 (Excel处理)
- **Java**: 1.8+

## 快速开始

### 1. 配置数据库连接

编辑 `src/main/resources/application.yml`:

```yaml
tencent:
  vectordb:
    url: http://your-vectordb-url:port
    api-key: your-api-key
    database-name: excel_docs_db
    collection-name: excel_docs
    embedding:
      model-name: bge-base-zh
      dimension: 768
```

### 2. 启动应用

```bash
mvn spring-boot:run
```

应用启动后会自动:
- 连接到腾讯云向量数据库
- 创建Database(如果不存在)
- 创建带Embedding自动向量化的Collection

### 3. 验证健康状态

```bash
curl http://localhost:8080/api/vectordb/health
```

## API文档

### 1. Excel文件上传

**接口**: `POST /api/vectordb/upload`

**功能**: 上传Excel文件,自动解析并插入向量数据库

**Excel格式要求**:

| content        | category |
|----------------|----------|
| 文本内容1      | 类别A    |
| 文本内容2      | 类别B    |

**请求示例**:

```bash
curl -X POST http://localhost:8080/api/vectordb/upload \
  -F "file=@example.xlsx"
```

**响应示例**:

```json
{
  "success": true,
  "message": "文件上传并处理成功",
  "insertedCount": 100,
  "failedCount": 0
}
```

### 2. 单文档插入

**接口**: `POST /api/vectordb/document`

**功能**: 插入单个文档,content字段会自动向量化

**请求示例**:

```bash
curl -X POST http://localhost:8080/api/vectordb/document \
  -H "Content-Type: application/json" \
  -d '{
    "content": "腾讯云向量数据库是一款企业级分布式数据库服务",
    "category": "产品介绍"
  }'
```

**响应示例**:

```json
{
  "success": true,
  "message": "文档插入成功"
}
```

### 3. 文本检索

**接口**: `POST /api/vectordb/search`

**功能**: 输入文本自动转换为向量,检索最相似的文档

**请求示例**:

```bash
curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是向量数据库",
    "limit": 5,
    "category": "产品介绍"
  }'
```

**响应示例**:

```json
{
  "success": true,
  "query": "什么是向量数据库",
  "totalCount": 5,
  "documents": [
    {
      "id": "xxx-xxx-xxx",
      "content": "腾讯云向量数据库是...",
      "category": "产品介绍",
      "timestamp": 1234567890,
      "score": 0.95
    }
  ]
}
```

### 4. 混合检索

**接口**: `POST /api/vectordb/hybrid-search`

**功能**: 结合向量检索和全文检索

**请求示例**:

```bash
curl -X POST http://localhost:8080/api/vectordb/hybrid-search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "向量数据库的应用场景",
    "limit": 5,
    "vectorWeight": 1.0,
    "fullTextWeight": 1.0
  }'
```

**注意**: 完整混合检索需要Collection配置稀疏向量索引,当前版本使用纯向量检索。

## Embedding自动向量化机制

### 创建Collection时配置

```java
.withEmbedding(
    Embedding.newBuilder()
        .withModelName("bge-base-zh")      // 指定Embedding模型
        .withField("content")               // 指定文本字段
        .withVectorField("vector")          // 指定向量字段
        .build()
)
```

### 插入文档时自动向量化

```java
Document document = Document.newBuilder()
    .withId("doc-001")
    .addDocField(new DocField("content", "这是文本内容"))  // 只需提供文本
    // 不需要提供vector字段,SDK会自动调用Embedding API生成向量
    .build();
```

### 查询时自动向量化

```java
SearchByEmbeddingItemsParam searchParam = SearchByEmbeddingItemsParam.newBuilder()
    .withEmbeddingItems(Arrays.asList("查询文本"))  // 只需提供查询文本
    // SDK会自动将文本转换为向量进行检索
    .withLimit(5)
    .build();
```

## 支持的Embedding模型

| 模型名称                    | 维度  | 适用场景       |
|----------------------------|------|---------------|
| bge-base-zh                | 768  | 中文通用(推荐) |
| m3e-base                   | 768  | 中文通用       |
| text2vec-large-chinese     | 1024 | 中文高精度     |
| e5-large-v2                | 1024 | 英文高精度     |
| multilingual-e5-base       | 768  | 多语言        |

## 项目结构

```
tcvectordb-spring-boot-starter/
├── src/main/java/com/tencent/tcvectordb/springboot/
│   ├── config/              # 配置类
│   │   ├── VectorDBConfig.java           # VectorDBClient Bean配置
│   │   └── VectorDBProperties.java       # 配置属性类
│   ├── controller/          # REST API控制器
│   │   └── VectorDBController.java
│   ├── service/             # 业务逻辑层
│   │   ├── CollectionService.java        # Collection管理
│   │   ├── DocumentService.java          # 文档操作
│   │   ├── SearchService.java            # 文本检索
│   │   └── HybridSearchService.java      # 混合检索
│   ├── dto/                 # 数据传输对象
│   │   ├── SearchRequest.java
│   │   ├── HybridSearchRequest.java
│   │   ├── DocumentDTO.java
│   │   └── UploadResponse.java
│   ├── exception/           # 异常处理
│   │   ├── VectorDBException.java
│   │   └── GlobalExceptionHandler.java
│   └── VectorDBApplication.java          # 启动类
└── src/main/resources/
    └── application.yml      # 配置文件
```

## 生产环境建议

### 1. 连接池配置

```yaml
tencent:
  vectordb:
    max-idle-connections: 20  # 根据并发量调整
    timeout: 60               # 适当增加超时时间
```

### 2. 批量插入优化

- 单次批量插入建议不超过1000条
- 插入后等待索引构建完成(2-5秒)
- 使用异步任务处理大批量数据

### 3. 检索性能优化

- 合理设置HNSW参数(ef值):100-500
- 只返回必要的字段,避免传输大量数据
- 添加合适的过滤条件减少搜索范围

### 4. 监控埋点

```java
// 在关键操作前后添加日志和监控
log.info("开始检索,query={}", query);
long startTime = System.currentTimeMillis();
// ... 执行检索
log.info("检索完成,耗时={}ms", System.currentTimeMillis() - startTime);
```

## 常见问题

### 1. 连接失败

- 检查URL和API Key是否正确
- 确认网络可达性
- 查看防火墙设置

### 2. 向量维度不匹配

- 确保配置的dimension与模型一致
- bge-base-zh必须配置为768维

### 3. 检索结果为空

- 确认数据已成功插入
- 等待索引构建完成(2-5秒)
- 检查过滤条件是否过于严格

### 4. Excel解析失败

- 确认文件格式为.xlsx
- 检查表头是否为: content, category
- 验证单元格数据类型

## 扩展功能

### 启用完整混合检索

1. 在`CollectionService.createEmbeddingCollection()`中添加稀疏向量索引:

```java
.addField(new SparseVectorIndex("sparse_vector", IndexType.INVERTED, MetricType.IP))
```

2. 在插入文档时添加稀疏向量:

```java
SparseVectorBm25Encoder encoder = SparseVectorBm25Encoder.getBm25Encoder("zh");
document.withSparseVector(encoder.encodeTexts(Arrays.asList(content)));
```

3. 启用`HybridSearchService`中注释的完整混合检索代码

## 参考资料

- [腾讯云向量数据库官方文档](https://cloud.tencent.com/document/product/1709)
- [Java SDK官方示例](https://github.com/Tencent/vectordatabase-sdk-java/tree/main/tcvectordb/src/main/java/com/tencent/tcvectordb/examples)
- [API文档](https://cloud.tencent.com/document/product/1709/97768)

## License

MIT License

## 作者

Tencent Cloud VectorDB Team
