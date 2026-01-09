package com.tencent.tcvectordb.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云向量数据库配置属性类
 * 从 application.yml 读取配置参数
 *
 * @author Tencent Cloud VectorDB Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "tencent.vectordb")
public class VectorDBProperties {

    /**
     * 向量数据库连接URL
     * 例如: http://lb-xxxxxxxx-xxxxx.clb.ap-guangzhou.tencentclb.com:xxxx
     */
    private String url;

    /**
     * API Key用于身份认证
     */
    private String apiKey;

    /**
     * 用户名,默认为root
     */
    private String username = "root";

    /**
     * 连接超时时间(秒)
     */
    private Integer timeout = 30;

    /**
     * 最大空闲连接数
     */
    private Integer maxIdleConnections = 10;

    /**
     * 数据库名称
     */
    private String databaseName = "default_db";

    /**
     * Collection名称
     */
    private String collectionName = "excel_docs";

    /**
     * Embedding配置
     */
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /**
     * Embedding配置内部类
     */
    @Data
    public static class EmbeddingConfig {
        /**
         * Embedding模型名称
         * 支持的模型: bge-base-zh, m3e-base, text2vec-large-chinese, e5-large-v2, multilingual-e5-base
         */
        private String modelName = "bge-base-zh";

        /**
         * 向量维度
         * bge-base-zh: 768
         * m3e-base: 768
         * text2vec-large-chinese: 1024
         * e5-large-v2: 1024
         * multilingual-e5-base: 768
         */
        private Integer dimension = 768;

        /**
         * 需要自动向量化的文本字段名
         */
        private String textField = "content";

        /**
         * 向量字段名
         */
        private String vectorField = "vector";

        /**
         * HNSW索引参数 - M值
         */
        private Integer hnswM = 16;

        /**
         * HNSW索引参数 - efConstruction值
         */
        private Integer hnswEfConstruction = 200;
    }
}
