package com.myh5.server.audience;

/**
 * 评论条目（VO）—— 给前端的形状。
 */
public record AudienceCommentVo(
        long id,
        String nickname,
        String avatar,
        String content,
        String createdAt
) {
}