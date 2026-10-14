package com.myh5.server.audience;

/**
 * 发表评论的请求体。
 */
public record AddCommentRequest(
        String teamName,
        String content
) {
}