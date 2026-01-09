package com.tencent.tcvectordb.springboot.controller;

import com.tencent.tcvectordb.springboot.dto.*;
import com.tencent.tcvectordb.springboot.service.DocumentService;
import com.tencent.tcvectordb.springboot.service.HybridSearchService;
import com.tencent.tcvectordb.springboot.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 腾讯云向量数据库REST API控制器
 *
 * 提供的接口:
 * 1. Excel文件上传
 * 2. 单文档插入
 * 3. 文本检索
 * 4. 混合检索
 *
 * @author Tencent Cloud VectorDB Team
 */
@Slf4j
@RestController
@RequestMapping("/api/vectordb")
@Validated
public class VectorDBController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private HybridSearchService hybridSearchService;

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Tencent Cloud VectorDB Spring Boot Starter");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * Excel文件上传接口
     *
     * 功能说明:
     * 1. 接收前端上传的.xlsx文件
     * 2. 解析Excel内容(content, category列)
     * 3. 自动向量化content字段
     * 4. 批量插入到向量数据库
     *
     * Excel格式要求:
     * | content        | category |
     * |----------------|----------|
     * | 文本内容1      | 类别A    |
     * | 文本内容2      | 类别B    |
     *
     * @param file Excel文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadExcel(
            @RequestParam("file") MultipartFile file) {

        log.info("收到Excel上传请求: 文件名={}, 大小={}KB",
                file.getOriginalFilename(), file.getSize() / 1024);

        // 验证文件类型
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(
                    UploadResponse.builder()
                            .success(false)
                            .message("仅支持.xlsx格式的Excel文件")
                            .build()
            );
        }

        // 验证文件大小(限制10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(
                    UploadResponse.builder()
                            .success(false)
                            .message("文件大小不能超过10MB")
                            .build()
            );
        }

        UploadResponse response = documentService.uploadExcelAndInsert(file);
        return ResponseEntity.ok(response);
    }

    /**
     * 单文档插入接口
     *
     * 功能说明:
     * 1. 接收单个文档的内容和分类
     * 2. 自动向量化content字段
     * 3. 插入到向量数据库
     *
     * 请求示例:
     * {
     *   "content": "腾讯云向量数据库是一款企业级分布式数据库服务",
     *   "category": "产品介绍"
     * }
     *
     * @param documentDTO 文档DTO
     * @return 插入结果
     */
    @PostMapping("/document")
    public ResponseEntity<Map<String, Object>> insertDocument(
            @Valid @RequestBody DocumentDTO documentDTO) {

        log.info("收到文档插入请求: category={}", documentDTO.getCategory());

        // 自动设置时间戳
        if (documentDTO.getTimestamp() == null) {
            documentDTO.setTimestamp(System.currentTimeMillis());
        }

        boolean success = documentService.insertDocument(documentDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "文档插入成功" : "文档插入失败");

        return ResponseEntity.ok(response);
    }

    /**
     * 文本检索接口
     *
     * 功能说明:
     * 1. 接收用户的查询文本
     * 2. 自动将文本转换为向量
     * 3. 执行向量相似度检索
     * 4. 返回Top-K最相似的文档
     *
     * 请求示例:
     * {
     *   "query": "什么是向量数据库",
     *   "limit": 5,
     *   "category": "产品介绍"  // 可选
     * }
     *
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @Valid @RequestBody SearchRequest request) {

        log.info("收到文本检索请求: query={}", request.getQuery());

        List<DocumentDTO> results = searchService.searchByText(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("query", request.getQuery());
        response.put("totalCount", results.size());
        response.put("documents", results);

        return ResponseEntity.ok(response);
    }

    /**
     * 混合检索接口
     *
     * 功能说明:
     * 1. 结合向量检索和全文检索
     * 2. 使用RRF重排序算法
     * 3. 支持权重配置
     *
     * 注意: 当前版本使用纯向量检索替代
     * 完整混合检索需要Collection配置稀疏向量索引
     *
     * 请求示例:
     * {
     *   "query": "向量数据库的应用场景",
     *   "limit": 5,
     *   "vectorWeight": 1.0,
     *   "fullTextWeight": 1.0,
     *   "category": "技术文档"  // 可选
     * }
     *
     * @param request 混合检索请求
     * @return 检索结果
     */
    @PostMapping("/hybrid-search")
    public ResponseEntity<Map<String, Object>> hybridSearch(
            @Valid @RequestBody HybridSearchRequest request) {

        log.info("收到混合检索请求: query={}", request.getQuery());

        List<DocumentDTO> results = hybridSearchService.hybridSearch(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("query", request.getQuery());
        response.put("totalCount", results.size());
        response.put("documents", results);
        response.put("note", "当前使用纯向量检索,如需启用完整混合检索请参考文档配置稀疏向量索引");

        return ResponseEntity.ok(response);
    }
}
