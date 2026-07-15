package com.zhush.system.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.zhush.system.entity.SysDatasource;
import com.zhush.system.mapper.SysDatasourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 *
 * @ClassName DatasourceLoaderConfig
 * @Description 启动时从数据库加载动态数据源配置，注册到 FlexDataSource
 * @Author zhush
 * @Date 2026/7/12 星期日 21:52
 *
 **/
@Configuration
public class DatasourceLoaderConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatasourceLoaderConfig.class);

    private final SysDatasourceMapper mapper;
    private final FlexDataSource flexDataSource;

    public DatasourceLoaderConfig(SysDatasourceMapper mapper, FlexDataSource flexDataSource) {
        this.mapper = mapper;
        this.flexDataSource = flexDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SysDatasource> list = mapper.selectEnabledList();
        for (SysDatasource ds : list) {
            try {
                DruidDataSource dataSource = new DruidDataSource();
                dataSource.setUrl(ds.getUrl());
                dataSource.setUsername(ds.getUsername());
                dataSource.setPassword(ds.getPassword());
                // 设置驱动类（非MySQL数据库时需要显式指定）
                if (ds.getDriverClass() != null && !ds.getDriverClass().isBlank()) {
                    dataSource.setDriverClassName(ds.getDriverClass());
                }
                // 设置连接池参数
                dataSource.setInitialSize(ds.getInitialSize());
                dataSource.setMaxActive(ds.getMaxActive());
                dataSource.setMinIdle(ds.getMinIdle());
                dataSource.setMaxWait(ds.getMaxWait());

                flexDataSource.addDataSource(ds.getName(), dataSource);
                log.info("动态数据源 [{}] 加载成功", ds.getName());
            } catch (Exception e) {
                log.error("动态数据源 [{}] 加载失败，跳过该数据源", ds.getName(), e);
            }
        }
    }
}
