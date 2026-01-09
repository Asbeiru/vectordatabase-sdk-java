package com.tencent.tcvectordb.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档数据传输对象
 *
 * @author Tencent Cloud VectorDB Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {

    /**
     * 文档ID
     */
    private String id;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 分类
     */
    private String category;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 相似度得分(仅在检索结果中返回)
     */
    private Double score;
}
