package com.telushealth.thcp.pipeline.common.util

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import jenkins.model.Jenkins
import com.amazonaws.services.ecs.model.*;
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.MountPointEntry
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.EnvironmentEntry
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.LogDriverOption
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.PortMappingEntry
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.PlacementStrategyEntry
import com.cloudbees.jenkins.plugins.amazonecs.ECSTaskTemplate.ExtraHostEntry
import java.util.logging.Logger

@Builder(builderStrategy = SimpleStrategy, prefix = '')
public class ECSSlaveBuilder {
    private static final Logger log = Logger.getLogger(ECSSlaveBuilder.class.name)

    String templateName
    String label
    String taskDefinitionOverride
    String dynamicTaskDefinitionOverride
    String image
    String launchType = 'FARGATE'
    String networkMode = 'awsvpc'
    String remoteFSRoot = "/home/jenkins"
    Integer memory = 0
    Integer memoryReservation = 2048
    Integer sharedMemorySize = 0
    Integer cpu = 1024
    String subnets
    String repositoryCredentials
    String securityGroups
    Boolean assignPublicIp = false
    Boolean privileged = false
    Boolean uniqueRemoteFSRoot = false
    String containerUser
    String platformVersion = 'LATEST'
    String executionRole
    String taskRole
    String logDriver = 'awslogs'
    String logGroup = '/telus/default-ecs-cloud'
    String logStreamName
    String inheritFrom
    List<LogDriverOption> logDriverOpts
    List<EnvironmentEntry> environments
    List<ExtraHostEntry> extraHosts
    List<MountPointEntry> mountPoints
    List<PortMappingEntry> portMappings
    List<PlacementStrategyEntry> placementStrategies

    ECSTaskTemplate build () {
        if(logDriverOpts == null) {
          log.info(' --> creating aws cloudwatch log driver opts')
          logDriverOpts = Arrays.asList(
            new LogDriverOption("awslogs-group", logGroup),
            new LogDriverOption("awslogs-region", 'ca-central-1'),
            new LogDriverOption("awslogs-stream-prefix", logStreamName == null ? templateName : logStreamName)
          )
        }
        ECSTaskTemplate ecsTaskTemplate = new ECSTaskTemplate(templateName,
            label,
            taskDefinitionOverride,
            dynamicTaskDefinitionOverride,
            image,
            repositoryCredentials,
            launchType,
            networkMode,
            remoteFSRoot,
            uniqueRemoteFSRoot,
            platformVersion,
            memory,
            memoryReservation,
            cpu,
            subnets,
            securityGroups,
            assignPublicIp,
            privileged,
            containerUser,
            logDriverOpts,
            environments,
            extraHosts,
            mountPoints,
            portMappings,
            executionRole,
            placementStrategies,
            taskRole,
            inheritFrom,
            sharedMemorySize) 
        ecsTaskTemplate.setLogDriver(logDriver)

        return ecsTaskTemplate
    }
}