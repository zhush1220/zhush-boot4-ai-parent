package com.zhush.system.job;

import com.zhush.system.service.AdminJobService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 定时任务启动同步器
 *
 * <p>应用启动完成后，为数据库中「启用」状态的定时任务补漏注册调度
 * （Quartz JDBC 持久化下重启自动恢复；已注册的跳过，多实例安全）。
 *
 * @author zhush
 */
@Component
@Order(100)
public class JobStartupRunner implements ApplicationRunner {

    private final AdminJobService adminJobService;

    public JobStartupRunner(AdminJobService adminJobService) {
        this.adminJobService = adminJobService;
    }

    @Override
    public void run(ApplicationArguments args) {
        adminJobService.syncEnabledJobsOnStartup();
    }
}
