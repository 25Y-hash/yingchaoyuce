-- ============================================================
-- V14 观众预测：投票 + 评论
-- ------------------------------------------------------------
-- 新增两张表，和现有业务（票务、积分榜）互不干扰：
--   audience_vote    —— 20 队投票数、占比
--   audience_comment —— 观众留言，按队分组
-- ============================================================

-- 观众投票：每队一行，votes 是票数，percentage 是占比（如 14.71）
CREATE TABLE IF NOT EXISTS audience_vote (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_name  VARCHAR(50)  NOT NULL COMMENT '队伍名',
    team_logo  VARCHAR(200) NOT NULL COMMENT '队徽路径',
    votes      INT          NOT NULL DEFAULT 0 COMMENT '票数',
    percentage DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '占比，如 14.71',
    CONSTRAINT uk_audience_vote_team UNIQUE (team_name)
);

-- 观众评论：每条一行，按队分组
CREATE TABLE IF NOT EXISTS audience_comment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_name  VARCHAR(50)  NOT NULL COMMENT '所属队伍',
    nickname   VARCHAR(50)  NOT NULL COMMENT '昵称',
    avatar     VARCHAR(200) NOT NULL COMMENT '头像路径',
    content    VARCHAR(500) NOT NULL COMMENT '评论内容',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 评论查询永远带 team_name 条件，建索引
CREATE INDEX idx_audience_comment_team ON audience_comment (team_name);