package com.telushealth.thcp.pipeline.deploy.delegate


import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StrBuilder

/**
 * Copyright (c) 2019 - TELUS
 *    All rights reserved
 *
 * The delegation class for the aws dms step
 */
class AwsDMSDelegate implements Serializable {

    private static final SPACE = ' '

    private static final SINGLE_QUOTE = '\''

    private static final EQUALS = '='

    private static final MINUS = '-'

    private String taskArnCmd = '--taskArn'

    private String startTaskTypeCmd = '--startTaskType'

    String startTaskType
    
    String taskArn
    
    /**
     * Helper method to build run dms replication task command line parameters
     *
     * @return the cmd line to execute
     */
    
    private String buildRunTaskCmdOpts() {
        
        assert !StringUtils.isEmpty(taskArn) : 'taskArn parameter is required'
        
        assert !StringUtils.isEmpty(startTaskType) : 'startTaskType parameter is required'
           
        StrBuilder strBuilder = new StrBuilder()
        strBuilder.
            append(taskArnCmd).
            append(SPACE).
            append(taskArn).
            append(SPACE).
            append(startTaskTypeCmd).
            append(SPACE).
            append(startTaskType).
            append(SPACE)
        
        return strBuilder.toString() 
    }

    /**
     * Helper method to build stop dms replication task command line parameters
     *
     * @return the cmd line to execute
     */
    
    private String buildStopTaskCmdOpts() {
        
        assert !StringUtils.isEmpty(taskArn) : 'taskArn parameter is required'
        
           
        StrBuilder strBuilder = new StrBuilder()
        strBuilder.
            append(taskArnCmd).
            append(SPACE).
            append(taskArn).
            append(SPACE)
        return strBuilder.toString()
    }
}
