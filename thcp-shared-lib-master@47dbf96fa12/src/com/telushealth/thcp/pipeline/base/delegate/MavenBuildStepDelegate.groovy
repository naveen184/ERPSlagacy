package com.telushealth.thcp.pipeline.base.delegate

import org.apache.commons.text.StrBuilder

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The delegation class for the gradle build step
 */
class MavenBuildStepDelegate implements Serializable {
    
    private static final SPACE = ' '
    
    private static final SINGLE_QUOTE = '\''
    
    private static final EQUALS = '='
    
    private static final SYSTEM_PROPERTY_SWITCH = '-D'
    
    String pomFile = 'pom.xml'
    
    String goals

    Map vars = [:]
    
    String buildSystemPropertySwitch () {
        StrBuilder builder = new StrBuilder()
        vars.each {
            builder.
                append(SYSTEM_PROPERTY_SWITCH).
                append(it.key).
                append(EQUALS).
                append(SINGLE_QUOTE).
                append(it.value).
                append(SINGLE_QUOTE).
                append(SPACE)
        }
        
        return builder.toString()
    }
}