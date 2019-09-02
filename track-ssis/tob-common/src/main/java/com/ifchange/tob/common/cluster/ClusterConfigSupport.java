package com.ifchange.tob.common.cluster;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.inject.Inject;

public abstract class ClusterConfigSupport {
    /** JVM集群名 **/
    protected abstract String clusterName(Environment env);

    /** you can implements your own MessageHandler **/
    protected IMessageHandler injectMessageHandler(ApplicationContext context) {
        return new IMessageHandler() {};
    }
    /** you can implements your own IClusterSrvListener **/
    protected IClusterSrvListener injectSrvChangeListener(ApplicationContext context) {
        return new IClusterSrvListener(){};
    }

    /** JGroups 集群支持 **/
    @Bean
    @Inject
    public ClusterManager jGroupsCluster(ApplicationContext context) throws Exception {
        return new ClusterManager(clusterName(context.getEnvironment()), injectMessageHandler(context), injectSrvChangeListener(context));
    }
}
