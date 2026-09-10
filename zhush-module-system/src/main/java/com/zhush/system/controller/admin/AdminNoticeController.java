package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminNoticeParam;
import com.zhush.system.query.AdminNoticeQuery;
import com.zhush.system.query.NoticeRecordQuery;
import com.zhush.system.service.AdminNoticeService;
import com.zhush.system.vo.AdminNoticeVO;
import com.zhush.system.vo.NoticeRecordVO;
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

import java.util.List;

/**
 * 通知公告接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-通知公告", description = "通知公告的增删改查、查看记录")
@RequestMapping("/admin/system/notice")
public class AdminNoticeController {

    private final AdminNoticeService noticeService;

    public AdminNoticeController(AdminNoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /**
     * 新增公告
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:notice:create")
    @Idempotent
    @OperateLog(module = "通知公告", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增公告")
    public Result<Long> create(@RequestBody @Valid AdminNoticeParam param) {
        return Result.success(noticeService.createNotice(param));
    }

    /**
     * 修改公告
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:notice:update")
    @Idempotent
    @OperateLog(module = "通知公告", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改公告")
    public Result<Void> update(@RequestBody @Valid AdminNoticeParam param) {
        noticeService.updateNotice(param);
        return Result.success();
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:notice:delete")
    @Idempotent
    @OperateLog(module = "通知公告", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除公告")
    public Result<Void> delete(@Parameter(description = "公告ID") @PathVariable Long id) {
        noticeService.deleteNotice(id);
        return Result.success();
    }

    /**
     * 批量删除公告
     */
    @DeleteMapping("/batch")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:notice:delete")
    @Idempotent
    @OperateLog(module = "通知公告", type = OperateTypeEnum.DELETE)
    @Operation(summary = "批量删除公告")
    public Result<Void> deleteBatch(@Parameter(description = "公告ID列表") @RequestBody List<Long> ids) {
        noticeService.deleteNoticeBatch(ids);
        return Result.success();
    }

    /**
     * 公告详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:notice:query")
    @Operation(summary = "公告详情")
    public Result<AdminNoticeVO> get(@Parameter(description = "公告ID") @PathVariable Long id) {
        return Result.success(noticeService.getNotice(id));
    }

    /**
     * 公告分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:notice:query")
    @Operation(summary = "公告分页")
    public Result<PageResult<AdminNoticeVO>> page(@Valid AdminNoticeQuery query) {
        return Result.success(noticeService.getNoticePage(query));
    }

    /**
     * 记录当前登录用户查看公告
     */
    @PostMapping("/{id}/view")
    @Idempotent(timeout = 3, message = "正在记录查看，请勿重复提交")
    @Operation(summary = "记录当前登录用户查看公告")
    public Result<Void> view(@Parameter(description = "公告ID") @PathVariable Long id) {
        noticeService.viewNotice(id);
        return Result.success();
    }

    /**
     * 公告查看记录分页
     */
    @GetMapping("/{id}/record")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:notice:query")
    @Operation(summary = "公告查看记录分页")
    public Result<PageResult<NoticeRecordVO>> record(@Parameter(description = "公告ID") @PathVariable Long id,
                                                     @Valid NoticeRecordQuery query) {
        return Result.success(noticeService.getNoticeRecordPage(id, query));
    }
}
