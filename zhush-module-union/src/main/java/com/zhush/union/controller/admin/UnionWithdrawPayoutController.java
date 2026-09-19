package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.Result;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.union.service.UnionWithdrawPayoutService;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-提现出款控制器
 *
 * <p>出款编排入口：发起打款、撤销出款、主动查单。真实资金动作全部委托给
 * {@link UnionWithdrawPayoutService}，本控制器只做权限与操作日志。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-提现出款", description = "提现打款、撤单与查单")
@RequestMapping("/admin/union/withdraw-payout")
public class UnionWithdrawPayoutController {


}
