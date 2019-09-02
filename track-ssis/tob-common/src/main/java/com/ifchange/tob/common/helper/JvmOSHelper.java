package com.ifchange.tob.common.helper;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Supplier;

/** JVM信息工具类 **/
public final class JvmOSHelper {

    private JvmOSHelper() {
    }

    @SuppressWarnings("unused")
    private static String OS_NAME, OS_ARCH, FILE_SEPARATOR, PROJECT_DIR;

    /** 是否Window操作系统 **/
    public static boolean isWindows() {
        return ofValue(OS_NAME, ()-> System.getProperty("os.name")).toUpperCase().startsWith("WIN");
    }

    /** 获取操作系统版本 **/
    public static boolean isV64Bit() {
        return ofValue(OS_ARCH, ()-> System.getProperty("os.arch")).endsWith("64");
    }

    /** 不同系统的文件分隔符 **/
    public static String fileSeparator() {
        return ofValue(FILE_SEPARATOR, () -> System.getProperty("file.separator"));
    }

    /** 获取工程所在目录 **/
    public static String projectDir() {
        return ofValue(PROJECT_DIR, ()-> System.getProperty("user.dir"));
    }

    /** 获取指定包下指定子类型的CLASS列表 **/
    public static <T> Set<Class<? extends T>> classesSubOf(String basePackage, final Class<T> typed) {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getSubTypesOf(typed);
    }

    /** 获取指定包下指定注解的CLASS列表 **/
    public static Set<Class<?>> classesAnnotatedWith(String basePackage, final Class<? extends Annotation> annotated) {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getTypesAnnotatedWith(annotated);
    }

    private static String ofValue(String rs, Supplier<String> supplier) {
        if(StringHelper.isBlank(rs)) {
            rs = supplier.get();
        }
        return rs;
    }
}
