package com.tencent.tcvectordb.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应DTO
 *
 * @author Tencent Cloud VectorDB Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 插入成功的文档数量
     */
    private Integer insertedCount;

    /**
     * 处理失败的行数
     */
    private Integer failedCount;
}
