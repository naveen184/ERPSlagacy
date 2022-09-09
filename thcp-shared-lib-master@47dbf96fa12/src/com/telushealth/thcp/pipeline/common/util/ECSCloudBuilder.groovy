package com.telushealth.thcp.pipeline.common.util

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import jenkins.model.Jenkins
import com.amazonaws.services.ecs.model.*;
import com.cloudbees.jenkins.plugins.amazonecs.ECSCloud
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate
import com.cloudbees.jenkins.plugins.amazonecs.pipeline.TaskTemplateMap
import java.util.logging.Logger

@Builder(builderStrategy = SimpleStrategy, prefix = '')
public class ECSCloudBuilder {
    private static final Logger log = Logger.getLogger(ECSCloudBuilder.class.name)

    String cloudName
    String credentialsId = null
    String region = 'ca-central-1'
    String clusterArn
    String jenkinsUrl
    String tunnel
    String allowedOverrides = 'all'
    Boolean retainAgents = false
    Integer slaveTimeoutInSeconds = 360
    List<ECSTaskTemplate> templates

    void build () {
        def instance = Jenkins.getInstance()
        
         def cloud = instance.getCloud(cloudName)
         if (cloud != null) {
            instance.clouds.remove(cloud)
        }
        
        ECSCloud ecsCloud = new ECSCloud(cloudName, credentialsId, clusterArn)
        ecsCloud.setTemplates(templates)
        ecsCloud.setRegionName(region)
        ecsCloud.setJenkinsUrl(jenkinsUrl)
        ecsCloud.setSlaveTimeoutInSeconds(slaveTimeoutInSeconds)
        ecsCloud.setTunnel(tunnel)
        ecsCloud.setAllowedOverrides(allowedOverrides)
        ecsCloud.setRetainAgents(retainAgents)
        
        instance.clouds.add(ecsCloud)
        instance.save()
        instance.reload()
    }
}