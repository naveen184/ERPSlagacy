package com.telushealth.thcp.pipeline.project.bootstrap.delegate


import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StrBuilder

/**
 * Copyright (c) 2019 - TELUS
 *    All rights reserved
 *
 * The delegation class for the aws ssm document step
 */
class ProjectBootstrapDelegate implements Serializable {

    private static final SPACE = ' '

    private static final SINGLE_QUOTE = '\''

    private static final EQUALS = '='

    private static final MINUS = '-'

    private String typesCmd = '--type'

    private String accountsCmd = '--accounts'

    private String slavesCmd = '--slaves'
    
    private String externalIdsCmd = '--externalIds'

    private String envName
    
    private String serverName
    
    private String vpcName
    
    private Script script
    
    private def defaultSlaves
    
    private def environments
    
    private def server
    
    private def environment
    
    private boolean isCI = false;
    
    String key
    
    String name
    
    String amiBakingKeyId
    
    String assumeRole = 'bootstrap'
    
    Map accounts = [:]

    Map externalIds = [:]

    Map slaves = [:]
    
    boolean dryRun = false
    
    boolean isECS = false
    
    boolean isProjectExists = false
    
    ProjectBootstrapDelegate(Script script, def bootstrapConfig) {

        this.script = script
        
        this.envName = bootstrapConfig.envName
        assert !StringUtils.isEmpty(envName) : 'Environment Name is null'
        
        this.serverName = bootstrapConfig.serverName
        assert !StringUtils.isEmpty(serverName) : 'Server Name is null'
        
        isCI = "ci".equals(serverName)
        
        vpcName         = bootstrapConfig.vpc
        dryRun          = bootstrapConfig.dryRun
        amiBakingKeyId  = bootstrapConfig.bakingKeyId
        defaultSlaves   = bootstrapConfig.slaves
        environments    = bootstrapConfig.environments
        
        environments.each { e -> if(envName.equals(e.name)) { environment = e } }
        assert environment!=null : 'Environment not found in the Bootstrap config'
        
        environment.servers.each { s -> if(serverName.equals(s.name)) { server = s  } }
        assert server!=null : 'Server not found in the Bootstrap config'
    }
    
    /**
     * Helper method to get the Project Bootstrap base task params
     *
     * @return the cmd line to execute
     */
    private String getBaseTaskParams () {
        
        assert !StringUtils.isEmpty(key) : 'Project key param is required'
        
        StrBuilder builder = new StrBuilder()
        
        builder
            .append('--key').append(SPACE).append(key).append(SPACE)
        
        builder
            .append('--jenkinsEnv').append(SPACE).append(envName).append(SPACE)
        
        builder
            .append('--jenkinsServer').append(SPACE).append(serverName).append(SPACE)
        
        if(dryRun) {
            builder.append('--dryRun').append(SPACE)
        }
        
        accounts.each {
            if(!StringUtils.isEmpty(it.value)) {
              builder.
                  append(accountsCmd).
                  append(SPACE).
                  append(it.key).
                  append(EQUALS).
                  append(SINGLE_QUOTE).
                  append(it.value).
                  append(SINGLE_QUOTE).
                  append(SPACE)
            }
        }
        
        externalIds.each {
            if(!StringUtils.isEmpty(it.value)) {
              builder.
                  append(externalIdsCmd).
                  append(SPACE).
                  append(it.key).
                  append(EQUALS).
                  append(SINGLE_QUOTE).
                  append(it.value).
                  append(SINGLE_QUOTE).
                  append(SPACE)
            }
        }
        
        return builder.toString()
    }
    
    /**
     * Helper method to get the BitBucket task params
     * 
     * @return the cmd line to execute
     */
    private String getBitBucketTaskParams () {
      
        List projectTypes = server.projectTypes
        
        if(isECS) {
            projectTypes.add("ECS")
        }
        
        assert !projectTypes.isEmpty() : 'Project types param is required'
        
        assert !StringUtils.isEmpty(name) : 'Project name param is required'
        
        StrBuilder builder = new StrBuilder()
        
        String baseParams = getBaseTaskParams()
        
        builder.append(baseParams);
        
        builder
            .append('--name')
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(name)
            .append(SINGLE_QUOTE)
            .append(SPACE)
        
        projectTypes.each {
            builder.
                append(typesCmd).
                append(SPACE).
                append(it).
                append(SPACE)
        }
        
        return builder.toString()
    }
    
    /**
     * Helper method to get the AssumeRole Cfn task params
     *
     * @return the cmd line to execute
     */
    private String getAssumeRoleCfnTaskParams () {
      
        assert !StringUtils.isEmpty(vpcName) : 'vpcName param is required'
        
        StrBuilder builder = new StrBuilder()
        
        String baseParams = getBaseTaskParams()
        
        builder.append(baseParams);
        
        builder
            .append('--vpcName')
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(vpcName)
            .append(SINGLE_QUOTE)
            .append(SPACE)
            
        builder
            .append('--assumeRole')
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(assumeRole)
            .append(SINGLE_QUOTE)
            .append(SPACE)
        
        return builder.toString()
    }
    
    /**
     * Helper method to get the CrossAccount Cfn task params
     *
     * @return the cmd line to execute
     */
    private String getCrossAccountRoleCfnTaskParams () {
      
        assert !StringUtils.isEmpty(amiBakingKeyId) : 'amiBakingKeyId param is required'
        
        StrBuilder builder = new StrBuilder()
        
        String baseParams = getBaseTaskParams()
        
        builder.append(baseParams);
        
        builder
            .append('--amiBakingKeyId')
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(amiBakingKeyId)
            .append(SINGLE_QUOTE)
            .append(SPACE)

        return builder.toString()
    }

     /**
     * Helper method to get the ssm parameters
     * 
     * @return the cmd line to execute
     */
    private String getThcpSeebJobsParams () {

        String securityGroup = server.securityGroup
        assert !StringUtils.isEmpty(securityGroup) : 'securityGroup is required'

        String subnetId = environment.subnet
        assert !StringUtils.isEmpty(subnetId) : 'subnetId is required'
        
        StrBuilder builder = new StrBuilder()
        
        String baseParams = getBaseTaskParams()
        
        builder.append(baseParams);
        
        builder
            .append('--securityGroup')
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(securityGroup)
            .append(SINGLE_QUOTE)
            .append(SPACE)
        
        builder
            .append('--subnetId')
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(subnetId)
            .append(SINGLE_QUOTE)
            .append(SPACE)
            
        if(isECS) {
            builder.append('--ecs').append(SPACE)
        }
        if(isProjectExists) {
            builder.append('--projectExists').append(SPACE)
        }
        if(!isCI) {
            defaultSlaves.each { sl ->
                String amiType = sl.amiType
                if(server.slaves != null) {
                    server.slaves.each { serverSl ->
                        if(serverSl.type != null && serverSl.type.equals(sl.type)) {
                            amiType = serverSl.amiType
                        }
                    }
                }
                
                String amiDetails = sl.amiName + "#" + amiType
                
                if(sl.numExecutors != null) {
                    amiDetails += "#" + sl.numExecutors
                }
                if(sl.instanceCapStr != null) {
                    amiDetails += "#" + sl.instanceCapStr
                }
                if(sl.initScript != null) {
                    amiDetails += "#" + sl.initScript
                }
                
                if(!isECS && "docker".equals(sl.type)) {
                    amiDetails = null
                }
                if(slaves.get(sl.type) == null && amiDetails != null) {
                    slaves.put(sl.type, amiDetails)
                }
            }
        }
        
        slaves.each {
            if(!StringUtils.isEmpty(it.value)) {
                builder.
                  append(slavesCmd).
                  append(SPACE).
                  append(it.key).
                  append(EQUALS).
                  append(SINGLE_QUOTE).
                  append(it.value).
                  append(SINGLE_QUOTE).
                  append(SPACE)
            }
        }
        return builder.toString()
    }
}