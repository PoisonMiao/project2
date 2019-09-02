package com.ifchange.tob.common.gearman;

import com.ifchange.tob.common.gearman.lib.Gearman;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.inject.Inject;

public abstract class GearConfigSupport {
    protected abstract String gearmanUri(Environment env);

    protected boolean enableGearmanClient() {
        return false;
    }

    @Bean
    @Inject
    public Gearman gearman(Environment env) {
        Gearman gearman = GearmanFactory.gearman(gearmanUri(env));
        if (enableGearmanClient()) {
            new GearmanClient(gearman);
        }
        return gearman;
    }
}
