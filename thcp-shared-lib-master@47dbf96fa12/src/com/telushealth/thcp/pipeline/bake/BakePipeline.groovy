package com.telushealth.thcp.pipeline.bake

import org.apache.commons.lang3.StringUtils

import com.telushealth.thcp.pipeline.base.BasePipeline
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

import groovy.lang.Grab
import groovy.lang.Grapes

@Grapes([
    @Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.6'),
    @Grab(group = 'org.apache.commons', module = 'commons-text', version = '1.1'),
])
/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The bake pipeline implementation class
 */
class BakePipeline implements BasePipeline {

    private Script script

    BakePipeline (Script script) {

        this.script = script
    }

    /**
     * The stage builder function
     *
     * @param label - the node agent label
     * @param body - the script body
     *
     * <pre>
     *  Usage:
     *  @Library('thcp-shared-lib')
     *  import com.telushealth.thcp.pipeline.bake.BakePipeline
     *  BakePipeline bakePipeline = new BakePipeline(this)
     *  
     *  bakePipeline('linux && packer') {
     *  	stage ('Stage 1') {
     *  	}
     *  	stage ('Stage 2') {
     *  	}
     *  }
     * </pre>
     */
    void call(String label, Closure body) {

        script.echo 'Calling bake pipeline'

        assert !StringUtils.isEmpty(label) : 'Param label is required!'

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = new BakePipelineStage(script)

        if (JenkinsUtil.isSlaveAllowed(script, label)) {
            script.node(label) { body()  }
        }
        else {
            script.error 'This project is not authorized to run slave with label:' + label
        }
    }
}
