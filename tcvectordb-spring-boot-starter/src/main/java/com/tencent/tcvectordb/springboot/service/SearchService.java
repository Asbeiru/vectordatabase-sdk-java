package com.tencent.tcvectordb.springboot.service;

import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.Document;
import com.tencent.tcvectordb.model.param.collection.HNSWSearchParams;
import com.tencent.tcvectordb.model.param.dml.Filter;
import com.tencent.tcvectordb.model.param.dml.SearchByEmbeddingItemsParam;
import com.tencent.tcvectordb.model.param.entity.SearchRes;
import com.tencent.tcvectordb.springboot.config.VectorDBProperties;
import com.tencent.tcvectordb.springboot.dto.DocumentDTO;
import com.tencent.tcvectordb.springboot.dto.SearchRequest;
import com.tencent.tcvectordb.springboot.exception.VectorDBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文本检索服务
 *
 * 核心职责:
 * 1. 提供基于文本的向量检索功能
 * 2. 利用Embedding自动向量化能力
 * 3. 支持过滤条件查询
 *
 * @author Tencent Cloud VectorDB Team
 */
@Slf4j
@Service
public class SearchService {

    @Autowired
    private VectorDBClient vectorDBClient;

    @Autowired
    private VectorDBProperties properties;

    /**
     * 文本检索 - 自动向量化查询
     *
     * 【核心功能】自动向量化检索:
     * 1. 用户只需提供查询文本(query)
     * 2. SDK会自动调用腾讯云Embedding API
     * 3. 将查询文本转换为768维向量
     * 4. 使用向量进行相似度检索
     * 5. 返回Top-K最相似的文档
     *
     * 关键API: searchByEmbeddingItems
     * - withEmbeddingItems(): 提供查询文本列表
     * - SDK自动处理文本到向量的转换
     * - 不需要手动调用Embedding API
     *
     * 参考官方示例: VectorDBExampleWithEmbedding.java的searchByEmbeddingItems方法(第274-288行)
     *
     * @param request 检索请求
     * @return 检索结果列表
     */
    public List<DocumentDTO> searchByText(SearchRequest request) {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        log.info("执行文本检索: query={}, limit={}, category={}",
                request.getQuery(), request.getLimit(), request.getCategory());

        try {
            // 构建检索参数
            SearchByEmbeddingItemsParam.Builder paramBuilder = SearchByEmbeddingItemsParam.newBuilder()
                    // 【关键】提供查询文本列表
                    // SDK会自动将这些文本转换为向量进行检索
                    .withEmbeddingItems(Arrays.asList(request.getQuery()))

                    // HNSW检索参数
                    // ef: 动态候选列表大小,越大召回率越高,但检索速度越慢
                    // 建议范围: 100-500
                    .withParams(new HNSWSearchParams(100))

                    // Top-K的K值
                    .withLimit(request.getLimit());

            // 如果指定了category过滤条件
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                // 使用Filter进行过滤
                // 语法: 字段名="值"
                Filter filter = new Filter("category=\"" + request.getCategory() + "\"");
                paramBuilder.withFilter(filter);
                log.info("应用过滤条件: category={}", request.getCategory());
            }

            // 是否返回向量数据(默认不返回,减少数据传输)
            paramBuilder.withRetrieveVector(request.getRetrieveVector());

            SearchByEmbeddingItemsParam searchParam = paramBuilder.build();

            // 执行检索
            // 返回类型为SearchRes,包含检索结果和可能的警告信息
            SearchRes searchRes = vectorDBClient.searchByEmbeddingItems(dbName, collName, searchParam);

            // 检查是否有Embedding截断警告
            if (searchRes.getWarning() != null && !searchRes.getWarning().isEmpty()) {
                log.warn("Embedding警告: {}", searchRes.getWarning());
            }

            // 获取检索结果
            // getDocuments()返回二维列表,每个查询文本对应一组结果
            // 由于我们只查询一个文本,取第一组结果
            List<List<Document>> documentsList = searchRes.getDocuments();
            if (documentsList.isEmpty() || documentsList.get(0).isEmpty()) {
                log.info("未找到匹配的文档");
                return new ArrayList<>();
            }

            List<Document> documents = documentsList.get(0);
            log.info("检索到 {} 个相似文档", documents.size());

            // 转换为DTO
            return convertToDocumentDTOs(documents);

        } catch (Exception e) {
            log.error("文本检索失败", e);
            throw new VectorDBException("SEARCH_FAILED", "文本检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将SDK的Document对象转换为DTO
     *
     * @param documents SDK Document列表
     * @return DTO列表
     */
    private List<DocumentDTO> convertToDocumentDTOs(List<Document> documents) {
        List<DocumentDTO> dtos = new ArrayList<>();

        for (Document doc : documents) {
            try {
                // 获取文档字段
                Map<String, Object> fields = doc.getDocFields();

                DocumentDTO dto = DocumentDTO.builder()
                        .id(doc.getId())
                        .content(getFieldValue(fields, properties.getEmbedding().getTextField(), String.class))
                        .category(getFieldValue(fields, "category", String.class))
                        .timestamp(getFieldValue(fields, "timestamp", Long.class))
                        .score(doc.getScore())  // 相似度得分
                        .build();

                dtos.add(dto);

            } catch (Exception e) {
                log.error("转换文档失败: {}", doc.getId(), e);
                // 继续处理其他文档
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
            log.warn("字段 {} 类型转换失败: {} -> {}", fieldName, value.getClass(), clazz);
            return null;
        }
    }
}
