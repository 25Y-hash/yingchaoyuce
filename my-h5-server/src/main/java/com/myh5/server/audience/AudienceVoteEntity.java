package com.myh5.server.audience;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 观众投票实体 —— 对应 audience_vote 表。
 */
@Data
@TableName("audience_vote")
public class AudienceVoteEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;
    private String teamLogo;
    private Integer votes;
    private Double percentage;
}