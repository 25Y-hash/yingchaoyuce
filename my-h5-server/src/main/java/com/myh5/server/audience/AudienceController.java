package com.myh5.server.audience;

import com.myh5.server.auth.AuthContext;
import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "观众预测")
@RestController
public class AudienceController {

    private final AudienceService audienceService;

    public AudienceController(AudienceService audienceService) {
        this.audienceService = audienceService;
    }

    @Operation(summary = "观众投票排行榜", description = "按票数倒序，rank 由后端生成")
    @GetMapping("/audience/ranking")
    public Result<List<AudienceVoteVo>> ranking() {
        return Result.ok(audienceService.ranking());
    }

    @Operation(summary = "某队评论（分页）", description = "teamName 不传则返回全部评论")
    @GetMapping("/audience/comments")
    public Result<AudienceCommentPageVo> comments(
            @RequestParam(required = false) String teamName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        return Result.ok(audienceService.comments(teamName, page, size));
    }

    @Operation(summary = "发表评论（需登录）")
    @PostMapping("/audience/comment")
    public Result<String> addComment(@RequestBody AddCommentRequest req) {
        Long userId = AuthContext.requireUserId();
        audienceService.addComment(userId, req.teamName(), req.content());
        return Result.ok("评论成功");
    }
}