package com.telushealth.thcp.pipeline.deploy.delegate


import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StrBuilder

/**
 * Copyright (c) 2019 - TELUS
 *    All rights reserved
 *
 * The delegation class for the aws ssm document step
 */
class AwsECSDelegate implements Serializable {

    private static final SPACE = ' '

    private static final SINGLE_QUOTE = '\''

    private static final EQUALS = '='

    private static final MINUS = '-'

    private String subnetsCmd = '--subnets'

    private String securityGroupsCmd = '--securityGroups'

    private String countCmd = '--count'
    
    private String desiredCountCmd = '--desiredCount'
    
    private String clusterCmd = '--clusterId'
    
    private String serviceArnCmd = '--serviceArn'
    
    private String taskDefCmd = '--taskDefinition'
    
    private String logGroupCmd = '--logGroupName'
    
    private String logStreamCmd = '--logStreamPrefix'
    
    private String taskTimeoutCmd = '--taskTimeout'
    
    private String policyTargetValueCmd = '--policyTargetValue'

    String subnets
    
    String securityGroups
    
    String clusterId
    
    String taskDefinition

    String count = "1"
    
    String desiredCount
    
    String serviceArn
    
    String logGroupName
    
    String logStreamPrefix
    
    String timeout = "900"
    
    String policyTargetValue

    /**
     * Helper method to build run fargate task command line parameters
     *
     * @return the cmd line to execute
     */
    
    private String buildRunTaskCmdOpts() {
        
        assert !StringUtils.isEmpty(clusterId) : 'clusterId parameter is required'
        
        assert !StringUtils.isEmpty(taskDefinition) : 'taskDefinition parameter is required'
        
        StrBuilder strBuilder = new StrBuilder()
        strBuilder.
            append(clusterCmd).
            append(SPACE).
            append(clusterId).
            append(SPACE).
            append(countCmd).
            append(SPACE).
            append(count).
            append(SPACE).
            append(taskDefCmd).
            append(SPACE).
            append(taskDefinition).
            append(SPACE).
            append(securityGroupsCmd).
            append(SPACE).
            append(securityGroups).
            append(SPACE).
            append(subnetsCmd).
            append(SPACE).
            append(subnets).
            append(SPACE).
            append(taskTimeoutCmd).
            append(SPACE).
            append(timeout).
            append(SPACE)
        
        if(!StringUtils.isEmpty(logGroupName)) {
            strBuilder.
                append(logGroupCmd).
                append(SPACE).
                append(SINGLE_QUOTE).
                append(logGroupName).
                append(SINGLE_QUOTE).
                append(SPACE)
        }
        
        if(!StringUtils.isEmpty(logStreamPrefix)) {
            strBuilder.
                append(logStreamCmd).
                append(SPACE).
                append(SINGLE_QUOTE).
                append(logStreamPrefix).
                append(SINGLE_QUOTE).
                append(SPACE)
        }
            
        return strBuilder.toString() 
    }
    
    /**
     * Helper method to build update ecs service command line parameters
     *
     * @return the cmd line to execute
     */
    
    private String buildUpdateServiceCmdOpts() {
        
        assert !StringUtils.isEmpty(clusterId) : 'clusterId parameter is required'
        
        assert !StringUtils.isEmpty(desiredCount) : 'desiredCount parameter is required'
        
        StrBuilder strBuilder = new StrBuilder()
        strBuilder.
            append(clusterCmd).
            append(SPACE).
            append(clusterId).
            append(SPACE).
            append(desiredCountCmd).
            append(SPACE).
            append(desiredCount).
            append(SPACE)

       if(!StringUtils.isEmpty(policyTargetValue)) {
           strBuilder.
           append(policyTargetValueCmd).
           append(SPACE).
           append(policyTargetValue).
           append(SPACE)
       }
       
       if(!StringUtils.isEmpty(serviceArn)) {
           strBuilder.
                append(serviceArnCmd).
                append(SPACE).
                append(serviceArn).
                append(SPACE)
       }
        return strBuilder.toString()
    }

}
