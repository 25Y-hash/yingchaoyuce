package com.myh5.server.audience;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myh5.server.common.BizException;
import com.myh5.server.common.ErrorCodes;
import com.myh5.server.user.UserEntity;
import com.myh5.server.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class AudienceService {

    private final AudienceMapper audienceMapper;
    private final AudienceCommentMapper audienceCommentMapper;
    private final UserMapper userMapper;

    public AudienceService(AudienceMapper audienceMapper,
                           AudienceCommentMapper audienceCommentMapper,
                           UserMapper userMapper) {
        this.audienceMapper = audienceMapper;
        this.audienceCommentMapper = audienceCommentMapper;
        this.userMapper = userMapper;
    }

    /** 排行榜：按票数倒序，rank 从 1 开始 */
    public List<AudienceVoteVo> ranking() {
        List<AudienceVoteEntity> entities = audienceMapper.selectList(
                new QueryWrapper<AudienceVoteEntity>().orderByDesc("votes")
        );
        return IntStream.range(0, entities.size())
                .mapToObj(i -> toVoteVo(entities.get(i), i + 1))
                .toList();
    }

    /** 某队评论，分页 */
    public AudienceCommentPageVo comments(String teamName, int page, int size) {
        Page<AudienceCommentEntity> p = new Page<>(page, size);
        QueryWrapper<AudienceCommentEntity> qw = new QueryWrapper<>();
        if (teamName != null && !teamName.isBlank()) {
            qw.eq("team_name", teamName);
        }
        qw.orderByDesc("created_at");

        Page<AudienceCommentEntity> result = audienceCommentMapper.selectPage(p, qw);

        List<AudienceCommentVo> list = result.getRecords().stream()
                .map(this::toCommentVo)
                .toList();

        return new AudienceCommentPageVo(list, result.getTotal());
    }

    /** 发表评论（需登录） */
    @Transactional
    public void addComment(Long userId, String teamName, String content) {
        if (teamName == null || teamName.isBlank()) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请选择队伍");
        }
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "评论内容不能为空");
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "账号不存在，请重新登录");
        }

        AudienceCommentEntity comment = new AudienceCommentEntity();
        comment.setTeamName(teamName);
        comment.setNickname(user.getNickname());
        comment.setAvatar(
                user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()
                        ? user.getAvatarUrl()
                        : "/images/avatars/avatar_001.png"
        );
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());

        audienceCommentMapper.insert(comment);
    }

    private AudienceVoteVo toVoteVo(AudienceVoteEntity e, int rank) {
        return new AudienceVoteVo(
                rank, e.getTeamName(), e.getTeamLogo(),
                e.getVotes(), e.getPercentage()
        );
    }

    private AudienceCommentVo toCommentVo(AudienceCommentEntity e) {
        String time = e.getCreatedAt() == null ? ""
                : e.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return new AudienceCommentVo(
                e.getId(), e.getNickname(), e.getAvatar(), e.getContent(), time
        );
    }
}