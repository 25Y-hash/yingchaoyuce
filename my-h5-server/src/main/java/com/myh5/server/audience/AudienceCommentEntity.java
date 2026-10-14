package com.myh5.server.audience;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 观众评论实体 —— 对应 audience_comment 表。
 */
@Data
@TableName("audience_comment")
public class AudienceCommentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;
    private String nickname;
    private String avatar;
    private String content;
    private LocalDateTime createdAt;
}
