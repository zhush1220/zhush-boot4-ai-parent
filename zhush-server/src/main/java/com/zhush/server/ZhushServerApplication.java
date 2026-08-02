package com.zhush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 启动类
 *
 * <p>启动类位于 {@code com.zhush.server} 包，而各业务模块（system/member 等）
 * 位于 {@code com.zhush.xxx} 包，故必须显式指定：
 * <ul>
 *     <li>{@code scanBasePackages = "com.zhush"}：扫描全部业务模块的
 *     Controller/Service/Component（starter 走自动装配，不依赖包扫描）</li>
 *     <li>{@code @MapperScan}：扫描 {@code com.zhush} 下标注 {@code @Mapper}
 *     的接口（限定 annotationClass，避免误注册普通接口；
 *     注意 basePackages 不支持 ** 通配符）</li>
 * </ul>
 *
 * @author zhush
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.zhush")
@MapperScan(basePackages = "com.zhush", annotationClass = Mapper.class)
public class ZhushServerApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(ZhushServerApplication.class, args).getEnvironment();
        logStartupInfo(env);
    }

    /**
     * 打印启动成功后的访问地址
     */
    private static void logStartupInfo(Environment env) {
        String appName = env.getProperty("spring.application.name", "zhush-server");
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
            // 获取失败时降级为 localhost，不影响启动
        }
        String base = "http://" + hostAddress + ":" + port + contextPath;
        log.info("""
                        
                        ----------------------------------------------------------
                        \t应用 [{}] 启动成功！
                        \t接口文档: \t{}/doc.html
                        \tDruid监控: \t{}/druid
                        \tSBA监控: \t{}/monitor（需 zhush.monitor.enabled=true）
                        \t激活环境: \t{}
                        ----------------------------------------------------------""",
                appName, base, base, base, String.join(",", env.getActiveProfiles()));
    }

}
