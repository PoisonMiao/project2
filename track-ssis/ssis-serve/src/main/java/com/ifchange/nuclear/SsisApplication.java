package com.ifchange.nuclear;

import com.ifchange.tob.common.helper.SpringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.ifchange.nuclear"})
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class SsisApplication implements CommandLineRunner {
    private static final Class<?> APP_CLAZZ = SsisApplication.class;
    private static final Logger LOG = LoggerFactory.getLogger(APP_CLAZZ);
    private static final String APP = APP_CLAZZ.getSimpleName();

    public static void main(String[] args) {
        try {
            SpringApplication.run(APP_CLAZZ, args);
        } catch (Exception e) {
            LOG.info("{} startup failure then terminated ", APP);
            System.exit(Integer.MAX_VALUE);
        }
    }

    @Override
    public void run(String... args) {
        // Here can add something initialize or task

        LOG.info("{} on at {} environment", APP, SpringHelper.applicationEnv());
    }
}
