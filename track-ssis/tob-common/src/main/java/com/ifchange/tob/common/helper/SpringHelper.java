package com.ifchange.tob.common.helper;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.function.Supplier;

/** SPRING容器工具类 **/
public abstract class SpringHelper {
    private static ApplicationContext ctx;

    public void setApplicationContext(ApplicationContext context) {
        SpringHelper.ctx = context;
    }

    public static Environment getEnvironment() {
        return null == ctx ? null : ctx.getEnvironment();
    }

    public static <T> T getBean(Class<T> clazz) {
        return ctx.getBean(clazz);
    }

    public static <T> T getBean(String beanName, Class<T> clazz) {
        return ctx.getBean(beanName, clazz);
    }


    @SuppressWarnings("unused")
    private static String CONTEXT_PATH, APP_NAME, APP_PORT;

    /** context path of the application. **/
    public static String contextPath() {
        return ofValue(CONTEXT_PATH, ()-> confValue("server.servlet.context-path"));
    }

    /** 获取应用名 **/
    public static String applicationName() {
        return ofValue(APP_NAME, ()-> confValue("spring.application.name"));
    }

    /** 获取应用端口 **/
    public static String applicationPort() {
        return StringHelper.defaultIfBlank(ofValue(APP_PORT, ()-> confValue("server.port")), "0");
    }

    /** 获取应用运行的环境 **/
    public static String applicationEnv() {
        if(null == ctx || null == ctx.getEnvironment()) {
            return StringHelper.EMPTY;
        }
        String[] envList = ctx.getEnvironment().getActiveProfiles();
        if (!CollectsHelper.isNullOrEmpty(envList)) {
            return envList[0];
        }
        return StringHelper.EMPTY;
    }

    /** 获取应用环境KEY值 **/
    public static String confValue(String key) {
        if(null == ctx || null == ctx.getEnvironment()) {
            return StringHelper.EMPTY;
        }
        return StringHelper.defaultString(ctx.getEnvironment().getProperty(key));
    }

    private static String ofValue(String rs, Supplier<String> supplier) {
        if(StringHelper.isBlank(rs)) {
            rs = supplier.get();
        }
        return rs;
    }
}