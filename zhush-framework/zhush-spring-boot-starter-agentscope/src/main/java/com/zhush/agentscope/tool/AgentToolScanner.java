package com.zhush.agentscope.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具扫描器：容器单例全部就绪后扫描被 {@link AgentTool} 标注的方法，注册进 {@link ToolkitRegistry}。
 *
 * <p>业务模块只需在 Bean 方法上标注 {@link AgentTool} 即可暴露为智能体工具，无需额外登记。
 *
 * <p>回调直接实现在本 Bean 上：此前由一个 {@code @Bean @ConditionalOnMissingBean} 的
 * {@link SmartInitializingSingleton} 触发，而该条件按返回类型匹配到容器的基础设施 Bean
 * （{@code internalEventListenerProcessor}）后恒不成立，导致扫描从未执行。
 *
 * @author zhush
 */
public class AgentToolScanner implements ApplicationContextAware, SmartInitializingSingleton {

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

    @Override
    public void afterSingletonsInstantiated() {
        scan();
    }

    public void scan() {
        if (applicationContext == null) {
            return;
        }
        List<String> registered = new ArrayList<>();
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
            // 取被代理类的目标类型：CGLIB 子类覆写的方法不携带注解，直接遍历代理类会漏注册
            for (Method method : AopUtils.getTargetClass(bean).getMethods()) {
                AgentTool anno = AnnotationUtils.findAnnotation(method, AgentTool.class);
                if (anno != null) {
                    toolkitRegistry.register(bean, method, anno);
                    registered.add(anno.name().isEmpty() ? method.getName() : anno.name());
                }
            }
        }
        log.info("[AgentTool] 工具扫描完成，注册 {} 个：{}", registered.size(), registered);
    }

}
