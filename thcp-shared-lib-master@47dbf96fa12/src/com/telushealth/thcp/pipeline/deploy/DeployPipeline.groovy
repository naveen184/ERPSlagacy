package com.telushealth.thcp.pipeline.deploy

import org.apache.commons.lang3.StringUtils

import com.telushealth.thcp.pipeline.base.BasePipeline
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

import groovy.lang.Grab
import groovy.lang.Grapes

@Grapes([
    @Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.12.0'),
    @Grab(group = 'org.apache.commons', module = 'commons-text', version = '1.1')
])
/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The deploy pipeline implementation class
 */
class DeployPipeline implements BasePipeline {

    private Script script
    
    DeployPipeline (Script script) {

        this.script = script
    }

    /**
     * The deploy pipeline builder
     *
     * @param label - the node agent label
     * @param body - the script body
     *
     * <pre>
     *  Usage:
     *  deployPipeline('linux && gradle') {
     *  	stage ('Stage 1') {
     *  	}
     *  	stage ('Stage 2') {
     *  	}
     *  }
     * </pre>
     */
    void call(String label, Closure body) {

        script.echo 'Calling deploy pipeline'

        assert !StringUtils.isEmpty(label) : 'Param label is required!'

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate =  new DeployPipelineStage(label, script)
        
        if (JenkinsUtil.isSlaveAllowed(script, label)) {
            script.node(label) { body()  }
        }
        else {
            script.error 'This project is not authorized to run slave with label:' + label
        }
    }
}
