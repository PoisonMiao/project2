package com.ifchange.tob.common.initializ;

import com.alibaba.fastjson.serializer.DoubleSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.SpringHelper;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Locale;

public class BootContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        // Set ApplicationContext
        new SpringHelper(){}.setApplicationContext(context);
        // Logback Reset Initializer
        new LogbackResetInitializer().configure();
        // TODO: config move others
    }

    static {
        // setting user country language
        System.setProperty("user.country", Locale.CHINESE.getCountry());
        System.setProperty("user.language", Locale.CHINESE.getLanguage());

        // jgroups cluster bind address ipv4 then JVM start must with -Djava.net.preferIPv4Stack=true
        System.setProperty("jgroups.bind_addr", NetworkHelper.machineIP());
        System.setProperty("java.net.preferIPv4Stack", String.valueOf(true));

        SerializeConfig.getGlobalInstance().put(Long.TYPE , ToStringSerializer.instance);
        SerializeConfig.getGlobalInstance().put(long.class , ToStringSerializer.instance);
        SerializeConfig.getGlobalInstance().put(Long.class , ToStringSerializer.instance);
        SerializeConfig.getGlobalInstance().put(Double.class, new DoubleSerializer("#.######"));
    }
}
