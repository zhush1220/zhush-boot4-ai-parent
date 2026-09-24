package com.zhush.ai.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.mybatisflex.core.paginate.Page;
import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AiGenerationTaskEntity;
import com.zhush.ai.mapper.AiGenerationTaskMapper;
import com.zhush.ai.query.AiGenerationTaskQuery;
import com.zhush.ai.vo.AiGenerationTaskVO;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.mybatis.core.query.QueryWrapperX;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.zhush.ai.enums.AiErrorCode.GENERATION_TASK_FORBIDDEN;
import static com.zhush.ai.enums.AiErrorCode.GENERATION_TASK_NOT_FOUND;

/**
 * 管理端-生成任务接口：图像/视频异步任务的<b>轮询查询</b>与只读列表。
 *
 * <p>轮询接口（{@code /task/{taskId}}）是「前端轮询新接口」这一异步回推方案的落地：对话提交生成后，
 * 前端持 {@code taskId} 周期拉取本接口，命中 {@code status=2} 渲染产物。归属强校验：非管理员仅可
 * 查询本人任务（{@code user_id + user_type} 双维匹配），杜绝越权轮询他人产物。
 *
 * <p>列表接口（{@code /task/page}）供管理端运维查看全站生成任务，权限 {@code agent:task:list}；
 * 无该权限的普通助手用户亦可通过（与 {@code ai:assistant} 取 OR），但非管理员被强制过滤为本人任务。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-生成任务", description = "图像/视频异步生成任务轮询与列表")
@RequestMapping("/admin/ai/agent/task")
public class AgentTaskController {

    private static final String USER_TYPE_ADMIN = "admin";

    private final AiGenerationTaskMapper taskMapper;
    private final AiSessionContextResolver contextResolver;

    public AgentTaskController(AiGenerationTaskMapper taskMapper, AiSessionContextResolver contextResolver) {
        this.taskMapper = taskMapper;
        this.contextResolver = contextResolver;
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询生成任务（轮询）")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "ai:assistant")
    public Result<AiGenerationTaskVO> get(@Parameter(description = "对外任务号") @PathVariable String taskId) {
        AiGenerationTaskEntity entity = taskMapper.selectByTaskId(taskId);
        if (entity == null) {
            throw ServiceExceptionUtil.exception(GENERATION_TASK_NOT_FOUND, taskId);
        }
        assertTaskOwner(entity);
        return Result.success(toVO(entity));
    }

    @GetMapping("/page")
    @Operation(summary = "生成任务分页")
    // 管理端运维查看（agent:task:list）与助手用户自查本人任务（ai:assistant）共用，取 OR
    @SaCheckPermission(type = CommonConstant.TYPE, mode = SaMode.OR, value = {"agent:task:list", "ai:assistant"})
    public Result<PageResult<AiGenerationTaskVO>> page(@Valid AiGenerationTaskQuery query) {
        AgentContext ctx = contextResolver.resolve();
        // 归属强制：非管理员只能看本人任务（服务端覆盖，防越权列举）
        if (ctx == null || !USER_TYPE_ADMIN.equals(ctx.getUserType())) {
            query.setUserId(ctx == null ? 0L : ctx.getUserId());
            query.setUserType(ctx == null ? "member" : ctx.getUserType());
        }
        QueryWrapperX wrapper = QueryWrapperX.create()
                .eq(AiGenerationTaskEntity::getTaskType, query.getTaskType())
                .eq(AiGenerationTaskEntity::getStatus, query.getStatus())
                .eq(AiGenerationTaskEntity::getUserId, query.getUserId())
                .eq(AiGenerationTaskEntity::getUserType, query.getUserType());
        wrapper.orderBy("submit_time", false);
        Page<AiGenerationTaskEntity> page = taskMapper.selectPageX(query, wrapper);
        List<AiGenerationTaskVO> list = new ArrayList<>(page.getRecords().size());
        for (AiGenerationTaskEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return Result.success(PageResult.of(list, page.getTotalRow()));
    }

    // ================ 内部 ================

    private void assertTaskOwner(AiGenerationTaskEntity entity) {
        AgentContext ctx = contextResolver.resolve();
        if (ctx != null && USER_TYPE_ADMIN.equals(ctx.getUserType())) {
            return;
        }
        Long currentUserId = ctx == null ? null : ctx.getUserId();
        String currentUserType = ctx == null ? null : ctx.getUserType();
        boolean owner = currentUserId != null
                && currentUserId.equals(entity.getUserId())
                && currentUserType != null && currentUserType.equals(entity.getUserType());
        if (!owner) {
            throw ServiceExceptionUtil.exception(GENERATION_TASK_FORBIDDEN);
        }
    }

    private AiGenerationTaskVO toVO(AiGenerationTaskEntity entity) {
        AiGenerationTaskVO vo = new AiGenerationTaskVO();
        vo.setTaskId(entity.getTaskId());
        vo.setTaskType(entity.getTaskType());
        vo.setStatus(entity.getStatus());
        vo.setModel(entity.getModel());
        vo.setPrompt(entity.getPrompt());
        vo.setOptions(entity.getOptions());
        vo.setResultUrl(entity.getResultUrl());
        vo.setFileId(entity.getFileId());
        vo.setFileSize(entity.getFileSize());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setSessionId(entity.getSessionId());
        vo.setSubmitTime(entity.getSubmitTime());
        vo.setFinishTime(entity.getFinishTime());
        return vo;
    }
}
