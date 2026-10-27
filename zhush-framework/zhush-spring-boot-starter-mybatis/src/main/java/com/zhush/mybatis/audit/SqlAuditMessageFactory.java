package com.zhush.mybatis.audit;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.audit.AuditMessage;
import com.mybatisflex.core.audit.DefaultMessageFactory;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

/**
 * SQL 审计消息工厂
 * <p>
 * 继承 {@link DefaultMessageFactory}，在创建 {@link AuditMessage} 时补全业务上下文：
 * <ul>
 *     <li>平台（platform）：按请求路径段 {@code /admin/**}、{@code /member/**} 判定，
 *     无请求上下文（定时任务、异步线程）取 {@link #PLATFORM_SYSTEM}</li>
 *     <li>操作人（user）：当前登录用户ID（管理端优先，其次会员端）</li>
 *     <li>请求URL / 用户IP / 所属模块：取自当前请求</li>
 *     <li>链路追踪ID：写入 metas['traceId']（{@link AuditMessage} 无独立 traceId 字段）</li>
 * </ul>
 * MyBatis-Flex 只预置 {@code platform=库标识常量} 与 hostIp，其余业务字段一律不赋值，必须由本工厂补齐；
 * 又因 {@code AuditManager.startAudit} 在业务线程调用 {@link #create()}、异步落库线程已无请求上下文，
 * 这些字段只能在采集时读取。
 *
 * @author zhush
 * @since 2026/9/5
 */
public class SqlAuditMessageFactory extends DefaultMessageFactory {

    /**
     * 平台取值：定时任务、异步事件线程等无请求上下文的执行来源
     */
    public static final String PLATFORM_SYSTEM = "system";

    /**
     * 无 SkyWalking agent 挂载时 {@link TraceContext#traceId()} 的返回值，不具备追踪价值
     */
    private static final String NO_TRACE_ID = "N/A";

    /**
     * 平台判定用的路径段（{@code LoginTypeConstants} 与控制器路径前缀的约定一致）
     */
    private static final String[] PLATFORM_SEGMENTS = {LoginTypeConstants.ADMIN, LoginTypeConstants.MEMBER};

    /**
     * 代理场景下透传客户端 IP 的请求头（按优先级排列）
     */
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private static final String UNKNOWN = "unknown";

    @Override
    public AuditMessage create() {
        AuditMessage message = super.create();
        // Flex 预置的 platform 是库标识而非业务平台，先归一为后台线程取值，再由请求上下文覆盖
        message.setPlatform(PLATFORM_SYSTEM);
        fillRequestContext(message);
        fillOperator(message);
        message.addMeta("traceId", currentTraceId());
        return message;
    }

    /**
     * 补全请求维度字段：平台、请求URL、用户IP、所属模块
     * <p>
     * 平台按路径段而非登录态判定：未登录的管理端接口（如登录本身）仍属管理端，
     * 而路径不含约定段的请求（如开放接口）留空，表示未识别到端。
     */
    private void fillRequestContext(AuditMessage message) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }
        String url = StrUtil.nullToEmpty(request.getRequestURI());
        message.setUrl(url);
        message.setUserIp(clientIp(request));

        String[] segments = url.split("/");
        String platform = "";
        for (int i = 0; i < segments.length - 1; i++) {
            if (isPlatformSegment(segments[i])) {
                platform = segments[i];
                message.setModule(segments[i + 1]);
                break;
            }
        }
        message.setPlatform(platform);
    }

    /**
     * 补全操作人：管理端优先，其次会员端
     */
    private void fillOperator(AuditMessage message) {
        Long adminId = loginIdDefaultNull(StpAdminUtil::getLoginIdDefaultNull);
        if (adminId != null) {
            message.setUser(String.valueOf(adminId));
            return;
        }
        Long memberId = loginIdDefaultNull(StpMemberUtil::getLoginIdDefaultNull);
        if (memberId != null) {
            message.setUser(String.valueOf(memberId));
        }
    }

    /**
     * 读取登录用户ID，未登录或无 Web 上下文（Sa-Token 取值会抛异常）时返回 null
     * <p>
     * 两端各自捕获：管理端取值失败不应连带跳过会员端判定。
     */
    private static Long loginIdDefaultNull(Supplier<Long> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isPlatformSegment(String segment) {
        for (String platform : PLATFORM_SEGMENTS) {
            if (platform.equals(segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前线程绑定的请求对象，非 Web 线程返回 null
     */
    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 客户端真实 IP：优先代理头，多级代理取第一个非 unknown 的地址
     */
    private static String clientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                int index = ip.indexOf(',');
                return index > 0 ? ip.substring(0, index).trim() : ip.trim();
            }
        }
        return StrUtil.nullToEmpty(request.getRemoteAddr());
    }

    /**
     * 链路追踪ID：未挂载 agent 时的 "N/A" 按空处理，避免落库无价值占位值
     */
    private static String currentTraceId() {
        String traceId = StrUtil.nullToEmpty(TraceContext.traceId());
        return NO_TRACE_ID.equals(traceId) ? "" : traceId;
    }
}
