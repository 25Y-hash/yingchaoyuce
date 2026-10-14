package com.myh5.server.audience;

import java.util.List;

/**
 * 评论分页返回：当前页数据 + 总数。
 */
public record AudienceCommentPageVo(
        List<AudienceCommentVo> list,
        long total
) {
}