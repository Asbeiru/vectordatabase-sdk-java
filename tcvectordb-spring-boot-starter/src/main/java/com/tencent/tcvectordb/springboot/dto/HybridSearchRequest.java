package com.tencent.tcvectordb.springboot.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 混合检索请求DTO
 * 结合向量相似度和关键词匹配
 *
 * @author Tencent Cloud VectorDB Team
 */
@Data
public class HybridSearchRequest {

    /**
     * 查询文本
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 返回Top-K结果的K值
     */
    @Min(value = 1, message = "limit必须大于0")
    private Integer limit = 5;

    /**
     * 向量检索权重
     * 默认为1.0
     */
    private Double vectorWeight = 1.0;

    /**
     * 全文检索权重
     * 默认为1.0
     */
    private Double fullTextWeight = 1.0;

    /**
     * 可选的过滤条件 - 分类
     */
    private String category;
}
