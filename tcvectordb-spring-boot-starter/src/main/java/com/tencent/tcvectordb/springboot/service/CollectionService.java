package com.tencent.tcvectordb.springboot.service;

import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.Collection;
import com.tencent.tcvectordb.model.Database;
import com.tencent.tcvectordb.model.param.collection.*;
import com.tencent.tcvectordb.springboot.config.VectorDBProperties;
import com.tencent.tcvectordb.springboot.exception.VectorDBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Collection管理服务
 *
 * 核心职责:
 * 1. 创建带Embedding自动向量化功能的Collection
 * 2. 管理Collection的生命周期
 * 3. 确保应用启动时Collection已就绪
 *
 * @author Tencent Cloud VectorDB Team
 */
@Slf4j
@Service
public class CollectionService {

    @Autowired
    private VectorDBClient vectorDBClient;

    @Autowired
    private VectorDBProperties properties;

    /**
     * 应用启动时自动初始化Database和Collection
     */
    @PostConstruct
    public void initializeCollectionOnStartup() {
        log.info("开始初始化向量数据库和Collection...");
        try {
            ensureDatabaseExists();
            ensureCollectionExists();
            log.info("向量数据库和Collection初始化完成");
        } catch (Exception e) {
            log.error("初始化向量数据库失败", e);
            throw new VectorDBException("INIT_FAILED", "向量数据库初始化失败", e);
        }
    }

    /**
     * 确保数据库存在,如果不存在则创建
     */
    private void ensureDatabaseExists() {
        String dbName = properties.getDatabaseName();
        try {
            // 检查数据库是否已存在
            // 使用官方SDK的listDatabase()方法获取所有数据库列表
            if (!vectorDBClient.listDatabase().contains(dbName)) {
                log.info("数据库 {} 不存在,正在创建...", dbName);

                // 使用官方SDK的createDatabase()方法创建数据库
                Database database = vectorDBClient.createDatabase(dbName);

                log.info("数据库 {} 创建成功", dbName);
            } else {
                log.info("数据库 {} 已存在", dbName);
            }
        } catch (Exception e) {
            throw new VectorDBException("CREATE_DB_FAILED", "创建数据库失败: " + dbName, e);
        }
    }

    /**
     * 确保Collection存在,如果不存在则创建
     *
     * 关键点: 配置Embedding自动向量化
     */
    private void ensureCollectionExists() {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        try {
            Database database = vectorDBClient.database(dbName);

            // 检查Collection是否已存在
            // 使用官方SDK的IsExistsCollection()方法
            if (!database.IsExistsCollection(collName)) {
                log.info("Collection {} 不存在,正在创建...", collName);

                // 创建带Embedding自动向量化的Collection
                createEmbeddingCollection(dbName, collName);

                log.info("Collection {} 创建成功", collName);
            } else {
                log.info("Collection {} 已存在", collName);

                // 验证Collection配置
                Collection collection = vectorDBClient.describeCollection(dbName, collName);
                log.info("Collection详情: {}", collection.toString());
            }
        } catch (Exception e) {
            throw new VectorDBException("CREATE_COLLECTION_FAILED", "创建Collection失败: " + collName, e);
        }
    }

    /**
     * 创建支持Embedding自动向量化的Collection
     *
     * 【核心功能】配置自动向量化的关键步骤:
     * 1. 使用VectorIndex定义向量字段,维度必须与Embedding模型匹配
     * 2. 使用withEmbedding()配置Embedding自动向量化
     *    - withField(): 指定源文本字段(content)
     *    - withVectorField(): 指定目标向量字段(vector)
     *    - withModelName(): 指定Embedding模型(bge-base-zh)
     *
     * 【重要】: 配置后,插入文档时只需提供content字段,向量数据库会自动:
     * - 调用腾讯云Embedding服务
     * - 将content文本转换为768维向量
     * - 存储到vector字段
     *
     * Collection字段设计:
     * - id: 主键,String类型,必须全局唯一
     * - vector: 向量字段,768维(对应bge-base-zh模型),使用HNSW索引
     * - content: 文本内容字段,需要建立FilterIndex以支持过滤(但不是用于自动向量化的字段)
     * - category: 分类字段,建立FilterIndex以支持按分类过滤
     * - timestamp: 时间戳字段,建立FilterIndex以支持时间范围查询
     *
     * @param dbName 数据库名
     * @param collName Collection名
     */
    private void createEmbeddingCollection(String dbName, String collName) {
        VectorDBProperties.EmbeddingConfig embeddingConfig = properties.getEmbedding();

        // 构建CreateCollectionParam
        // 参考官方示例: VectorDBExampleWithEmbedding.java的initCreateEmbeddingCollectionParam方法
        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withName(collName)
                .withShardNum(1)  // 分片数
                .withReplicaNum(1)  // 副本数
                .withDescription("Excel文档向量数据库,支持Embedding自动向量化")

                // 1. 【必需】主键索引
                .addField(new FilterIndex("id", FieldType.String, IndexType.PRIMARY_KEY))

                // 2. 【必需】向量索引 - 使用HNSW算法
                // 维度必须与Embedding模型一致: bge-base-zh = 768维
                // MetricType.COSINE: 使用余弦相似度
                // HNSWParams(M, efConstruction): HNSW算法参数
                //   - M: 每个节点的最大连接数,影响召回率和索引大小
                //   - efConstruction: 构建索引时的动态候选列表大小
                .addField(new VectorIndex(
                        embeddingConfig.getVectorField(),  // "vector"
                        embeddingConfig.getDimension(),     // 768
                        IndexType.HNSW,
                        MetricType.COSINE,
                        new HNSWParams(embeddingConfig.getHnswM(), embeddingConfig.getHnswEfConstruction())
                ))

                // 3. 【业务字段】建立过滤索引,支持条件查询
                // 注意: 这些是业务字段索引,不是用于自动向量化的字段
                .addField(new FilterIndex("category", FieldType.String, IndexType.FILTER))
                .addField(new FilterIndex("timestamp", FieldType.Uint64, IndexType.FILTER))

                // 4. 【核心配置】Embedding自动向量化
                // withField("content"): 指定需要向量化的文本字段
                // withVectorField("vector"): 指定存储向量的字段
                // withModelName("bge-base-zh"): 指定使用的Embedding模型
                //
                // 配置后的效果:
                // - 插入文档时,只需提供content字段的文本内容
                // - SDK会自动调用腾讯云Embedding API将content转为向量
                // - 生成的向量自动存储到vector字段
                // - 查询时也支持自动将查询文本转为向量
                .withEmbedding(
                        Embedding.newBuilder()
                                .withModelName(embeddingConfig.getModelName())  // "bge-base-zh"
                                .withField(embeddingConfig.getTextField())      // "content"
                                .withVectorField(embeddingConfig.getVectorField())  // "vector"
                                .build()
                )
                .build();

        // 使用官方SDK的createCollection()方法创建Collection
        vectorDBClient.createCollection(dbName, param);

        log.info("成功创建Embedding Collection: {}", collName);
        log.info("- 向量维度: {}", embeddingConfig.getDimension());
        log.info("- Embedding模型: {}", embeddingConfig.getModelName());
        log.info("- 文本字段: {}", embeddingConfig.getTextField());
        log.info("- 向量字段: {}", embeddingConfig.getVectorField());
    }

    /**
     * 获取Collection信息
     */
    public Collection getCollection() {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        try {
            return vectorDBClient.describeCollection(dbName, collName);
        } catch (Exception e) {
            throw new VectorDBException("GET_COLLECTION_FAILED", "获取Collection信息失败", e);
        }
    }

    /**
     * 删除Collection(谨慎使用)
     */
    public void dropCollection() {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        try {
            log.warn("正在删除Collection: {}", collName);
            vectorDBClient.dropCollection(dbName, collName);
            log.info("Collection {} 已删除", collName);
        } catch (Exception e) {
            throw new VectorDBException("DROP_COLLECTION_FAILED", "删除Collection失败", e);
        }
    }
}
