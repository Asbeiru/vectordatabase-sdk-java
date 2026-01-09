# API测试指南

## 准备工作

### 1. 配置数据库连接

编辑 `src/main/resources/application.yml`:

```yaml
tencent:
  vectordb:
    url: http://your-actual-vectordb-url:port
    api-key: your-actual-api-key
```

### 2. 启动应用

```bash
cd tcvectordb-spring-boot-starter
mvn spring-boot:run
```

查看日志确认启动成功:
```
向量数据库客户端初始化成功
数据库健康检查通过
Collection excel_docs 已创建
```

## 测试步骤

### 步骤1: 健康检查

```bash
curl http://localhost:8080/api/vectordb/health
```

**预期响应**:
```json
{
  "status": "UP",
  "service": "Tencent Cloud VectorDB Spring Boot Starter",
  "version": "1.0.0"
}
```

### 步骤2: 插入测试数据

#### 方式1: 单文档插入

```bash
curl -X POST http://localhost:8080/api/vectordb/document \
  -H "Content-Type: application/json" \
  -d '{
    "content": "腾讯云向量数据库（Tencent Cloud VectorDB）是一款全托管的自研企业级分布式数据库服务，专用于存储、索引、检索、管理由深度神经网络或其他机器学习模型生成的大量多维嵌入向量。",
    "category": "产品介绍"
  }'
```

```bash
curl -X POST http://localhost:8080/api/vectordb/document \
  -H "Content-Type: application/json" \
  -d '{
    "content": "作为专门为处理输入向量查询而设计的数据库，它支持多种索引类型和相似度计算方法，单索引支持10亿级向量规模，高达百万级 QPS 及毫秒级查询延迟。",
    "category": "技术特性"
  }'
```

```bash
curl -X POST http://localhost:8080/api/vectordb/document \
  -H "Content-Type: application/json" \
  -d '{
    "content": "不仅能为大模型提供外部知识库，提高大模型回答的准确性，还可广泛应用于推荐系统、NLP 服务、计算机视觉、智能客服等 AI 领域。",
    "category": "应用场景"
  }'
```

**预期响应**:
```json
{
  "success": true,
  "message": "文档插入成功"
}
```

**等待索引构建**: 插入后等待5秒,让索引构建完成

```bash
sleep 5
```

#### 方式2: Excel批量上传

**创建测试Excel文件** (`test-data.xlsx`):

| content | category |
|---------|----------|
| 腾讯云向量数据库是一款企业级分布式数据库服务 | 产品介绍 |
| 支持多种索引类型和相似度计算方法 | 技术特性 |
| 可应用于推荐系统和智能客服 | 应用场景 |
| 提供高性能的向量检索能力 | 技术特性 |
| 为大模型提供外部知识库 | 应用场景 |

**上传Excel**:

```bash
curl -X POST http://localhost:8080/api/vectordb/upload \
  -F "file=@test-data.xlsx"
```

**预期响应**:
```json
{
  "success": true,
  "message": "文件上传并处理成功",
  "insertedCount": 5,
  "failedCount": 0
}
```

### 步骤3: 文本检索测试

#### 测试1: 基础检索

```bash
curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是腾讯云向量数据库",
    "limit": 3
  }'
```

**预期响应**:
```json
{
  "success": true,
  "query": "什么是腾讯云向量数据库",
  "totalCount": 3,
  "documents": [
    {
      "id": "xxx-xxx-xxx",
      "content": "腾讯云向量数据库（Tencent Cloud VectorDB）是...",
      "category": "产品介绍",
      "timestamp": 1234567890,
      "score": 0.95
    }
  ]
}
```

#### 测试2: 带分类过滤的检索

```bash
curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "向量数据库有哪些应用",
    "limit": 5,
    "category": "应用场景"
  }'
```

**预期结果**: 只返回category为"应用场景"的文档

#### 测试3: 不同查询文本的语义检索

```bash
# 查询1: 性能相关
curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "向量数据库的性能如何",
    "limit": 3
  }'
```

```bash
# 查询2: 应用场景相关
curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "可以用在哪些AI领域",
    "limit": 3
  }'
```

### 步骤4: 混合检索测试

```bash
curl -X POST http://localhost:8080/api/vectordb/hybrid-search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "向量数据库的技术特点",
    "limit": 5,
    "vectorWeight": 1.0,
    "fullTextWeight": 1.0
  }'
```

**注意**: 当前版本使用纯向量检索,响应中会包含说明信息

## 验证要点

### 1. Embedding自动向量化验证

✅ **插入时**:
- 只需提供content和category字段
- 不需要手动计算或提供vector字段
- SDK自动调用Embedding API生成向量

✅ **查询时**:
- 只需提供查询文本(query)
- SDK自动将文本转换为向量
- 返回语义相似的文档

### 2. 语义检索效果验证

测试以下查询,观察返回结果的语义相关性:

| 查询文本 | 预期匹配内容 |
|---------|-------------|
| 什么是向量数据库 | 产品介绍相关文档 |
| 性能怎么样 | 技术特性相关文档 |
| 能用在哪里 | 应用场景相关文档 |
| 大模型知识库 | 包含"外部知识库"的文档 |

### 3. 过滤功能验证

```bash
# 应该只返回"技术特性"分类的文档
curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "向量数据库",
    "category": "技术特性",
    "limit": 10
  }'
```

验证响应中所有文档的category字段都是"技术特性"

### 4. 相似度得分验证

- 检查返回文档的score字段
- 得分越高表示相似度越高
- 得分范围通常在0-1之间(余弦相似度)

## 性能测试

### 1. 批量插入性能

```bash
# 创建包含100行数据的Excel文件
# 上传并记录耗时
time curl -X POST http://localhost:8080/api/vectordb/upload \
  -F "file=@large-test-data.xlsx"
```

### 2. 检索性能

```bash
# 测试检索延迟
time curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "测试查询",
    "limit": 10
  }'
```

**预期性能指标**:
- 单次检索延迟: < 100ms
- 批量插入(100条): < 5秒
- 索引构建时间: 2-5秒

## 错误场景测试

### 1. 无效的Excel文件

```bash
curl -X POST http://localhost:8080/api/vectordb/upload \
  -F "file=@invalid.txt"
```

**预期**: 返回错误提示"仅支持.xlsx格式"

### 2. 空查询文本

```bash
curl -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "",
    "limit": 5
  }'
```

**预期**: 返回参数验证错误

### 3. 过大的文件

```bash
# 上传超过10MB的文件
curl -X POST http://localhost:8080/api/vectordb/upload \
  -F "file=@large-file.xlsx"
```

**预期**: 返回"文件大小不能超过10MB"

## 日志检查

查看应用日志,确认以下关键信息:

```
✅ 向量数据库客户端初始化成功
✅ 数据库健康检查通过
✅ Collection excel_docs 创建成功
✅ 成功创建Embedding Collection
✅ 向量维度: 768
✅ Embedding模型: bge-base-zh
✅ 批量插入结果: AffectRes(affectedCount=5)
✅ 检索到 3 个相似文档
```

## 故障排查

### 问题1: 连接失败

**症状**: 启动时报"无法连接到腾讯云向量数据库"

**排查**:
1. 检查URL和API Key配置
2. 测试网络连通性: `curl http://your-vectordb-url:port`
3. 查看防火墙设置

### 问题2: 检索结果为空

**症状**: 查询返回totalCount=0

**排查**:
1. 确认数据已成功插入
2. 等待索引构建(5秒)
3. 检查过滤条件是否过于严格
4. 尝试更通用的查询文本

### 问题3: Embedding截断警告

**症状**: 日志中出现"Embedding警告"

**说明**: 输入文本过长被截断,不影响功能,但可能影响精度

**建议**: 将长文本分段处理

## 完整测试脚本

```bash
#!/bin/bash

echo "=== 腾讯云向量数据库 Spring Boot Starter 测试 ==="

echo "1. 健康检查..."
curl -s http://localhost:8080/api/vectordb/health | jq

echo -e "\n2. 插入测试数据..."
curl -s -X POST http://localhost:8080/api/vectordb/document \
  -H "Content-Type: application/json" \
  -d '{"content": "腾讯云向量数据库是企业级数据库服务", "category": "产品介绍"}' | jq

echo -e "\n等待索引构建..."
sleep 5

echo -e "\n3. 文本检索测试..."
curl -s -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是向量数据库", "limit": 3}' | jq

echo -e "\n4. 带过滤的检索测试..."
curl -s -X POST http://localhost:8080/api/vectordb/search \
  -H "Content-Type: application/json" \
  -d '{"query": "向量数据库", "category": "产品介绍", "limit": 5}' | jq

echo -e "\n=== 测试完成 ==="
```

保存为 `test.sh`,执行:

```bash
chmod +x test.sh
./test.sh
```
