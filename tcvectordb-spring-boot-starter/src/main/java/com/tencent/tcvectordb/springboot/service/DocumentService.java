package com.tencent.tcvectordb.springboot.service;

import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.DocField;
import com.tencent.tcvectordb.model.Document;
import com.tencent.tcvectordb.model.param.dml.InsertParam;
import com.tencent.tcvectordb.model.param.entity.AffectRes;
import com.tencent.tcvectordb.springboot.config.VectorDBProperties;
import com.tencent.tcvectordb.springboot.dto.DocumentDTO;
import com.tencent.tcvectordb.springboot.dto.UploadResponse;
import com.tencent.tcvectordb.springboot.exception.VectorDBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档操作服务
 *
 * 核心职责:
 * 1. 解析Excel文件
 * 2. 将数据转换为Document对象
 * 3. 批量插入到向量数据库
 * 4. 利用Embedding自动向量化能力
 *
 * @author Tencent Cloud VectorDB Team
 */
@Slf4j
@Service
public class DocumentService {

    @Autowired
    private VectorDBClient vectorDBClient;

    @Autowired
    private VectorDBProperties properties;

    /**
     * 处理Excel文件上传并插入向量数据库
     *
     * Excel格式要求:
     * - 第一行为表头: content, category
     * - content列: 文本内容,会自动向量化
     * - category列: 分类信息
     *
     * 【关键点】自动向量化机制:
     * 1. 创建Document时,只需添加content字段的文本内容
     * 2. 不需要手动计算或提供vector字段
     * 3. SDK在插入时会自动调用腾讯云Embedding API
     * 4. content文本会被自动转换为768维向量并存储
     *
     * @param file Excel文件
     * @return 上传结果
     */
    public UploadResponse uploadExcelAndInsert(MultipartFile file) {
        log.info("开始处理Excel文件: {}", file.getOriginalFilename());

        try {
            // 1. 解析Excel文件
            List<DocumentDTO> documentDTOs = parseExcelFile(file);
            log.info("成功解析Excel,共 {} 行数据", documentDTOs.size());

            if (documentDTOs.isEmpty()) {
                return UploadResponse.builder()
                        .success(false)
                        .message("Excel文件中没有有效数据")
                        .insertedCount(0)
                        .failedCount(0)
                        .build();
            }

            // 2. 转换为Document对象并批量插入
            int insertedCount = batchInsertDocuments(documentDTOs);

            log.info("成功插入 {} 条文档到向量数据库", insertedCount);

            return UploadResponse.builder()
                    .success(true)
                    .message("文件上传并处理成功")
                    .insertedCount(insertedCount)
                    .failedCount(documentDTOs.size() - insertedCount)
                    .build();

        } catch (Exception e) {
            log.error("处理Excel文件失败", e);
            throw new VectorDBException("UPLOAD_FAILED", "Excel文件处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Excel文件
     *
     * 假设Excel格式:
     * | content        | category |
     * |----------------|----------|
     * | 文本内容1      | 类别A    |
     * | 文本内容2      | 类别B    |
     *
     * @param file Excel文件
     * @return 解析后的文档列表
     */
    private List<DocumentDTO> parseExcelFile(MultipartFile file) throws Exception {
        List<DocumentDTO> documents = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);  // 读取第一个sheet

            // 跳过表头,从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                try {
                    // 读取content列(第1列,索引0)
                    Cell contentCell = row.getCell(0);
                    String content = contentCell != null ? contentCell.getStringCellValue() : null;

                    // 读取category列(第2列,索引1)
                    Cell categoryCell = row.getCell(1);
                    String category = categoryCell != null ? categoryCell.getStringCellValue() : "未分类";

                    // 验证必填字段
                    if (content == null || content.trim().isEmpty()) {
                        log.warn("第 {} 行的content为空,跳过", i + 1);
                        continue;
                    }

                    DocumentDTO doc = DocumentDTO.builder()
                            .content(content.trim())
                            .category(category.trim())
                            .timestamp(System.currentTimeMillis())
                            .build();

                    documents.add(doc);

                } catch (Exception e) {
                    log.error("解析第 {} 行数据失败: {}", i + 1, e.getMessage());
                    // 继续处理其他行
                }
            }
        }

        return documents;
    }

    /**
     * 批量插入文档到向量数据库
     *
     * 【核心功能】利用Embedding自动向量化:
     * 1. 创建Document时,只添加业务字段(id, content, category, timestamp)
     * 2. 不需要添加vector字段 - SDK会自动处理
     * 3. SDK在插入时会自动:
     *    - 提取content字段的文本
     *    - 调用腾讯云Embedding API(bge-base-zh模型)
     *    - 将文本转换为768维向量
     *    - 存储到vector字段
     *
     * 参考官方示例: VectorDBExampleWithEmbedding.java的upsertData方法
     *
     * @param documentDTOs 文档DTO列表
     * @return 成功插入的数量
     */
    private int batchInsertDocuments(List<DocumentDTO> documentDTOs) {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        try {
            List<Document> documents = new ArrayList<>();

            for (DocumentDTO dto : documentDTOs) {
                // 构建Document对象
                // 注意: 只需提供业务字段,不需要提供vector字段
                // content字段的文本会被自动向量化
                Document document = Document.newBuilder()
                        .withId(UUID.randomUUID().toString())  // 生成唯一ID
                        // 【关键】添加content字段 - 这是配置了自动Embedding的字段
                        // SDK会自动将这个文本转换为向量
                        .addDocField(new DocField(properties.getEmbedding().getTextField(), dto.getContent()))
                        // 添加其他业务字段
                        .addDocField(new DocField("category", dto.getCategory()))
                        .addDocField(new DocField("timestamp", dto.getTimestamp()))
                        .build();

                documents.add(document);
            }

            // 使用官方SDK的upsert方法批量插入
            // withBuildIndex(true): 插入后立即构建索引
            InsertParam insertParam = InsertParam.newBuilder()
                    .addAllDocument(documents)
                    .withBuildIndex(true)  // 自动构建索引
                    .build();

            // 执行插入操作
            AffectRes affectRes = vectorDBClient.upsert(dbName, collName, insertParam);

            log.info("批量插入结果: {}", affectRes.toString());

            // 【重要】upsert操作可能有延迟,建议等待索引构建完成
            // 生产环境中可以通过异步任务或定时任务检查索引状态
            Thread.sleep(2000);

            return affectRes.getAffectedCount();

        } catch (Exception e) {
            log.error("批量插入文档失败", e);
            throw new VectorDBException("INSERT_FAILED", "批量插入文档失败", e);
        }
    }

    /**
     * 单个文档插入
     *
     * @param documentDTO 文档DTO
     * @return 是否成功
     */
    public boolean insertDocument(DocumentDTO documentDTO) {
        String dbName = properties.getDatabaseName();
        String collName = properties.getCollectionName();

        try {
            Document document = Document.newBuilder()
                    .withId(documentDTO.getId() != null ? documentDTO.getId() : UUID.randomUUID().toString())
                    .addDocField(new DocField(properties.getEmbedding().getTextField(), documentDTO.getContent()))
                    .addDocField(new DocField("category", documentDTO.getCategory()))
                    .addDocField(new DocField("timestamp", documentDTO.getTimestamp()))
                    .build();

            InsertParam insertParam = InsertParam.newBuilder()
                    .addDocument(document)
                    .withBuildIndex(true)
                    .build();

            AffectRes affectRes = vectorDBClient.upsert(dbName, collName, insertParam);

            return affectRes.getAffectedCount() > 0;

        } catch (Exception e) {
            log.error("插入文档失败", e);
            throw new VectorDBException("INSERT_FAILED", "插入文档失败", e);
        }
    }
}
