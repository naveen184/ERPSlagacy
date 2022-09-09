package com.telushealth.thcp.pipeline.deploy.delegate


import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StrBuilder

/**
 * Copyright (c) 2019 - TELUS
 *    All rights reserved
 *
 * The delegation class for the aws ssm document step
 */
class AwsCloudfrontDelegate implements Serializable {

    private static final SPACE = ' '

    private static final SINGLE_QUOTE = '\''

    private static final EQUALS = '='

    private static final MINUS = '-'

    private String distributionCmd = '--distributionIdKey'

    private String itemsCmd = '--items'

    String distributionIdKey
    
    List<String> items;

    /**
     * Helper method to build ECS Command line parameters
     *
     * @return the cmd line to execute
     */
    
    private String buildCmdOptions() {
        
        assert !StringUtils.isEmpty(distributionIdKey) : 'distributionIdKey parameter is required'
        
        if(items == null || items.size() == 0) {
            throw new InterruptedException('items parameter is required')
        }

        StrBuilder strBuilder = new StrBuilder()
        
        items.each {
            if(!StringUtils.isEmpty(it)) {
              strBuilder.
                  append(itemsCmd).
                  append(SPACE).
                  append(SINGLE_QUOTE).
                  append(it).
                  append(SINGLE_QUOTE).
                  append(SPACE)
            }
        }
        
        strBuilder.
            append(distributionCmd).
            append(SPACE).
            append(distributionIdKey)
        
        return strBuilder.toString() 
    }

}
