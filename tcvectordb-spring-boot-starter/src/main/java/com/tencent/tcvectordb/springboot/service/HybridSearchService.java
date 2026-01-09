package com.tencent.tcvectordb.springboot.service;

import com.tencent.tcvdbtext.encoder.SparseVectorBm25Encoder;
import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.Document;
import com.tencent.tcvectordb.model.param.dml.*;
import com.tencent.tcvectordb.model.param.entity.HybridSearchRes;
import com.tencent.tcvectordb.springboot.config.VectorDBProperties;
import com.tencent.tcvectordb.springboot.dto.DocumentDTO;
import com.tencent.tcvectordb.springboot.dto.HybridSearchRequest;
import com.tencent.tcvectordb.springboot.exception.VectorDBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 混合检索服务
 *
 * 核心职责:
 * 1. 结合向量检索和全文检索
 * 2. 使用RRF(Reciprocal Rank Fusion)重排序
 * 3. 支持权重配置
 *
 * 混合检索优势:
 * - 向量检索: 捕捉语义相似性
 * - 全文检索: 精确关键词匹配
 * - 结合两者: 更准确的检索结果
 *
 * 参考官方示例: VectorDBWithHybridSearchEmbeddingExample.java
 *
 * 注意: 混合检索需要Collection同时支持向量索引和稀疏向量索引
 * 由于当前Collection配置只有向量索引,这里提供简化版混合检索
 * 如需完整混合检索,需要在Collection创建时添加SparseVectorIndex
 *
 * @author Tencent Cloud VectorDB Team
 */
@Slf4j
@Service
public class HybridSearchService {

    @Autowired
    private VectorDBClient vectorDBClient;

    @Autowired
    private VectorDBProperties properties;

    /**
     * 混合检索 - 向量检索 + 全文检索
     *
     * 【说明】完整的混合检索实现:
     * 1. 向量检索(ANN): 使用Embedding自动向量化进行语义检索
     * 2. 全文检索(Match): 使用稀疏向量进行关键词匹配
     * 3. 重排序(Rerank): 使用RRF算法融合两种检索结果
     *
     * 【当前实现】简化版本:
     * 由于示例Collection只配置了向量索引,这里先实现向量检索部分
     * 完整混合检索需要Collection同时支持:
     * - VectorIndex: 密集向量索引
     * - SparseVectorIndex: 稀疏向量索引(用于全文检索)
     *
     * 【扩展方法】如需启用完整混合检索:
     * 1. 在CollectionService的createEmbeddingCollection方法中添加:
     *    .addField(new SparseVectorIndex("sparse_vector", IndexType.INVERTED, MetricType.IP))
     * 2. 在插入文档时添加稀疏向量:
     *    SparseVectorBm25Encoder encoder = SparseVectorBm25Encoder.getBm25Encoder("zh");
     *    document.withSparseVector(encoder.encodeTexts(Arrays.asList(content)))
     * 3. 使用本方法中注释掉的完整混合检索代码
     *
     * 参考: VectorDBWithHybridSearchEmbeddingExample.java (第137-165行)
     *
     * @param request 混合检索请求
     * @return 检索结果
     */
    public List<DocumentDTO> hybridSearch(HybridSearchRequest request) {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        log.info("执行混合检索: query={}, limit={}, vectorWeight={}, fullTextWeight={}",
                request.getQuery(), request.getLimit(),
                request.getVectorWeight(), request.getFullTextWeight());

        try {
            /*
             * 【完整混合检索实现】
             * 需要Collection配置SparseVectorIndex才能使用
             *
            // 1. 获取BM25编码器用于全文检索
            SparseVectorBm25Encoder encoder = SparseVectorBm25Encoder.getBm25Encoder("zh");

            // 2. 构建混合检索参数
            HybridSearchParam.Builder hybridParamBuilder = HybridSearchParam.newBuilder()
                    // 向量检索部分(ANN)
                    .withAnn(AnnOption.newBuilder()
                            .withFieldName(properties.getEmbedding().getTextField())  // "content"
                            .withTextData(request.getQuery())  // 自动向量化查询文本
                            .withLimit(request.getLimit())
                            .build())

                    // 全文检索部分(Match)
                    .withMatch(MatchOption.newBuilder()
                            .withFieldName("sparse_vector")
                            .withData(encoder.encodeQueries(Arrays.asList(request.getQuery())))
                            .withCutoffFrequency(0.1)
                            .withTerminateAfter(4000)
                            .withLimit(request.getLimit())
                            .build())

                    // 重排序 - 使用RRF融合两种检索结果
                    .withRerank(new WeightRerankParam(
                            Arrays.asList("vector", "sparse_vector"),
                            Arrays.asList(request.getVectorWeight(), request.getFullTextWeight())
                    ))

                    .withLimit(request.getLimit())
                    .withRetrieveVector(false)
                    .build();

            // 可选: 添加过滤条件
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                Filter filter = new Filter("category=\"" + request.getCategory() + "\"");
                hybridParamBuilder.withFilter(filter);
            }

            HybridSearchParam hybridParam = hybridParamBuilder.build();

            // 执行混合检索
            HybridSearchRes hybridSearchRes = vectorDBClient.hybridSearch(dbName, collName, hybridParam);

            List<Document> documents = hybridSearchRes.getDocuments();
            log.info("混合检索到 {} 个文档", documents.size());

            return convertToDocumentDTOs(documents);
            */

            // 【简化实现】当前仅使用向量检索
            // 由于Collection未配置SparseVectorIndex,暂时使用向量检索
            log.warn("当前Collection未配置稀疏向量索引,使用纯向量检索代替混合检索");
            log.warn("如需启用完整混合检索,请参考HybridSearchService中的注释说明");

            // 使用向量检索替代
            return performVectorOnlySearch(request);

        } catch (Exception e) {
            log.error("混合检索失败", e);
            throw new VectorDBException("HYBRID_SEARCH_FAILED", "混合检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 纯向量检索(混合检索的简化版本)
     * 在Collection未配置稀疏向量索引时使用
     */
    private List<DocumentDTO> performVectorOnlySearch(HybridSearchRequest request) {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        try {
            // 构建向量检索参数
            SearchByEmbeddingItemsParam.Builder paramBuilder = SearchByEmbeddingItemsParam.newBuilder()
                    .withEmbeddingItems(Arrays.asList(request.getQuery()))
                    .withParams(new com.tencent.tcvectordb.model.param.collection.HNSWSearchParams(100))
                    .withLimit(request.getLimit());

            // 添加过滤条件
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                Filter filter = new Filter("category=\"" + request.getCategory() + "\"");
                paramBuilder.withFilter(filter);
            }

            SearchByEmbeddingItemsParam searchParam = paramBuilder.build();

            // 执行检索
            com.tencent.tcvectordb.model.param.entity.SearchRes searchRes =
                    vectorDBClient.searchByEmbeddingItems(dbName, collName, searchParam);

            List<List<Document>> documentsList = searchRes.getDocuments();
            if (documentsList.isEmpty() || documentsList.get(0).isEmpty()) {
                return new ArrayList<>();
            }

            List<Document> documents = documentsList.get(0);
            return convertToDocumentDTOs(documents);

        } catch (Exception e) {
            throw new VectorDBException("VECTOR_SEARCH_FAILED", "向量检索失败", e);
        }
    }

    /**
     * 将SDK的Document对象转换为DTO
     */
    private List<DocumentDTO> convertToDocumentDTOs(List<Document> documents) {
        List<DocumentDTO> dtos = new ArrayList<>();

        for (Document doc : documents) {
            try {
                Map<String, Object> fields = doc.getDocFields();

                DocumentDTO dto = DocumentDTO.builder()
                        .id(doc.getId())
                        .content(getFieldValue(fields, properties.getEmbedding().getTextField(), String.class))
                        .category(getFieldValue(fields, "category", String.class))
                        .timestamp(getFieldValue(fields, "timestamp", Long.class))
                        .score(doc.getScore())
                        .build();

                dtos.add(dto);

            } catch (Exception e) {
                log.error("转换文档失败: {}", doc.getId(), e);
            }
        }

        return dtos;
    }

    /**
     * 安全地获取字段值
     */
    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Map<String, Object> fields, String fieldName, Class<T> clazz) {
        Object value = fields.get(fieldName);
        if (value == null) {
            return null;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.warn("字段 {} 类型转换失败", fieldName);
            return null;
        }
    }
}
