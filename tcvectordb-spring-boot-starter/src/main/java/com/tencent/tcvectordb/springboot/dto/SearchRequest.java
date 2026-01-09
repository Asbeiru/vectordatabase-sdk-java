package com.tencent.tcvectordb.springboot.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 文本检索请求DTO
 *
 * @author Tencent Cloud VectorDB Team
 */
@Data
public class SearchRequest {

    /**
     * 查询文本
     * 该文本会自动通过腾讯云Embedding模型转换为向量进行检索
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 返回Top-K结果的K值
     * 默认为5
     */
    @Min(value = 1, message = "limit必须大于0")
    private Integer limit = 5;

    /**
     * 可选的过滤条件 - 分类
     * 如果指定,则只返回该分类下的文档
     */
    private String category;

    /**
     * 是否返回向量数据
     * 默认不返回,以减少传输数据量
     */
    private Boolean retrieveVector = false;
}
