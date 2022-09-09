package com.telushealth.thcp.pipeline.config
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The class for the Pipeline configurations
 */
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class PipelineConfig {

    Boolean slackEnabled = true

    Boolean slackAtStart = true

    Boolean slackAtSuccess = true

    Boolean slackAtFailure = true
    
    Boolean useEcsSlave = true
    
}