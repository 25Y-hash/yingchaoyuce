package com.myh5.server.audience;

/**
 * 投票条目（VO）—— 给前端的形状。
 */
public record AudienceVoteVo(
        int rank,
        String teamName,
        String teamLogo,
        int votes,
        double percentage
) {
}