package com.zhush.agentscope.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;

/**
 * 工具扫描器：启动时扫描容器内被 {@link AgentTool} 标注的方法，注册进 {@link DefaultToolkitRegistry}。
 *
 * <p>业务模块只需在 Bean 方法上标注 {@link AgentTool} 即可暴露为智能体工具，无需额外登记。
 *
 * @author zhush
 */
public class AgentToolScanner implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(AgentToolScanner.class);

    private final ToolkitRegistry toolkitRegistry;
    private ApplicationContext applicationContext;

    public AgentToolScanner(ToolkitRegistry toolkitRegistry) {
        this.toolkitRegistry = toolkitRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void scan() {
        if (applicationContext == null) {
            return;
        }
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (BeansException ignored) {
                continue;
            }
            if (bean == null) {
                continue;
            }
            for (Method method : bean.getClass().getMethods()) {
                AgentTool anno = method.getAnnotation(AgentTool.class);
                if (anno != null) {
                    toolkitRegistry.register(bean, method, anno);
                }
            }
        }
    }

}
