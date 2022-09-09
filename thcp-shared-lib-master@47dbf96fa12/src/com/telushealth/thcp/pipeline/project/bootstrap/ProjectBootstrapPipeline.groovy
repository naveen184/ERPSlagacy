package com.telushealth.thcp.pipeline.project.bootstrap

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
 * The project bootstrap pipeline implementation class
 */
class ProjectBootstrapPipeline implements BasePipeline {

    private Script script

    ProjectBootstrapPipeline(Script script) {

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
     *  projectBootstrapPipeline('linux && deploy') {
     *      stage ('Stage 1') {
     *      }
     *      stage ('Stage 2') {
     *      }
     *  }
     * </pre>
     */
    void call(String label, Closure body) {

        script.echo 'Calling project bootstrap pipeline'

        assert !StringUtils.isEmpty(label) : 'Param label is required!'

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = new ProjectBootstrapPipelineStage(script)

        script.node(label) { body()  }

    }
}
