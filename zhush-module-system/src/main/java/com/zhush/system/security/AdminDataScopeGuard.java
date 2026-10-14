package com.zhush.system.security;

import com.zhush.common.exception.IErrorCode;
import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeType;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * byId 越权护栏
 * <p>
 * {@code selectOneById} 系列不经方言 {@code prepareAuth}，行级过滤对其无效；详情 / 修改 / 删除 /
 * 分配 / 重置等按主键取数的入口需显式校验目标行的归属是否落在当前用户的数据范围内，
 * 越权时按调用方给定的错误码拒绝（对外统一为"不存在"以避免资源存在性泄露，审计 R-2）。
 *
 * @author zhush
 */
@Component
public class AdminDataScopeGuard {

    private final AdminDataScopeProvider dataScopeProvider;

    public AdminDataScopeGuard(AdminDataScopeProvider dataScopeProvider) {
        this.dataScopeProvider = dataScopeProvider;
    }

    /**
     * 校验目标行对当前用户可见，不可见抛 {@code denyCode}
     *
     * @param deptId    目标行归属部门ID（无部门语义可传 null）
     * @param ownerId   目标行归属用户ID（对 sys_admin_user 而言即主键 id）
     * @param denyCode  越权时抛出的错误码
     * @param denyArgs  错误码参数
     */
    public void assertVisible(Long deptId, Long ownerId, IErrorCode denyCode, Object... denyArgs) {
        DataScope scope = dataScopeProvider.getDataScope();
        if (isVisible(scope, deptId, ownerId)) {
            return;
        }
        throw exception(denyCode, denyArgs);
    }

    /**
     * 纯判定：目标行是否落在给定数据范围内（可单测，无副作用）
     */
    public static boolean isVisible(DataScope scope, Long deptId, Long ownerId) {
        if (scope == null || scope.getType() == null || scope.getType() == DataScopeType.ALL) {
            // 未登录 / 超级管理员 / 全部数据：不限制
            return true;
        }
        if (scope.getType() == DataScopeType.SELF) {
            return ownerId != null && ownerId.equals(scope.getUserId());
        }
        // 部门类范围：有部门集合按部门匹配，部门集合为空则退化为仅本人（与方言一致）
        Set<Long> deptIds = scope.getDeptIds();
        if (deptIds == null || deptIds.isEmpty()) {
            return ownerId != null && ownerId.equals(scope.getUserId());
        }
        return deptId != null && deptIds.contains(deptId);
    }
}
