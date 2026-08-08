package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionNotificationParam;
import com.zhush.union.query.UnionNotificationQuery;
import com.zhush.union.service.UnionNotificationService;
import com.zhush.union.vo.UnionNotificationVO;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName UnionNotificationController
 * @Description 消息通知表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-消息通知", description = "消息通知的增删改查")
@RequestMapping("/admin/union/notification")
public class UnionNotificationController {

    private final UnionNotificationService notificationService;

    public UnionNotificationController(UnionNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 新增消息通知
     */
    @PostMapping
    @OperateLog(module = "消息通知", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增消息通知")
    public Result<Long> create(@RequestBody @Valid UnionNotificationParam param) {
        return Result.success(notificationService.createNotification(param));
    }

    /**
     * 修改消息通知
     */
    @PutMapping
    @OperateLog(module = "消息通知", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改消息通知")
    public Result<Void> update(@RequestBody @Valid UnionNotificationParam param) {
        notificationService.updateNotification(param);
        return Result.success();
    }

    /**
     * 删除消息通知
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "消息通知", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除消息通知")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        notificationService.deleteNotification(id);
        return Result.success();
    }

    /**
     * 消息通知详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "消息通知详情")
    public Result<UnionNotificationVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(notificationService.getNotification(id));
    }

    /**
     * 消息通知分页
     */
    @GetMapping("/page")
    @Operation(summary = "消息通知分页")
    public Result<PageResult<UnionNotificationVO>> page(@Valid UnionNotificationQuery query) {
        return Result.success(notificationService.getNotificationPage(query));
    }

}
