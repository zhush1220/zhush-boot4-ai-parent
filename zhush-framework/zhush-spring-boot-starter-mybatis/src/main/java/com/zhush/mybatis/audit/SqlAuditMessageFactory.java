package com.zhush.mybatis.audit;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.audit.AuditMessage;
import com.mybatisflex.core.audit.DefaultMessageFactory;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

/**
 * SQL 审计消息工厂
 * <p>
 * 继承 {@link DefaultMessageFactory}，在创建 {@link AuditMessage} 时补全业务上下文：
 * <ul>
 *     <li>操作人（user）：当前登录用户ID（管理端优先，其次会员端）</li>
 *     <li>平台（platform）：admin / member</li>
 *     <li>链路追踪ID：写入 metas['traceId']（{@link AuditMessage} 无独立 traceId 字段）</li>
 * </ul>
 * 注意：{@code AuditManager.startAudit} 在业务线程调用本工厂的 {@link #create()}，此时 Sa-Token
 * 上下文可用；后续异步落库线程则不再保留上下文，因此业务字段必须在此处补全。
 *
 * @author zhush
 * @since 2026/9/5
 */
public class SqlAuditMessageFactory extends DefaultMessageFactory {

    @Override
    public AuditMessage create() {
        AuditMessage message = super.create();
        fillBusinessContext(message);
        return message;
    }

    /**
     * 补全操作人 / 平台 / 链路追踪ID
     */
    private void fillBusinessContext(AuditMessage message) {
        try {
            Long adminId = StpAdminUtil.getLoginIdDefaultNull();
            if (adminId != null) {
                message.setUser(String.valueOf(adminId));
                message.setPlatform(LoginTypeConstants.ADMIN);
            } else {
                Long memberId = StpMemberUtil.getLoginIdDefaultNull();
                if (memberId != null) {
                    message.setUser(String.valueOf(memberId));
                    message.setPlatform(LoginTypeConstants.MEMBER);
                }
            }
        } catch (Exception e) {
            // 无登录上下文（如后台定时任务）时忽略，交由默认值兜底
        }
        message.addMeta("traceId", StrUtil.blankToDefault(TraceContext.traceId(), ""));
    }
}
