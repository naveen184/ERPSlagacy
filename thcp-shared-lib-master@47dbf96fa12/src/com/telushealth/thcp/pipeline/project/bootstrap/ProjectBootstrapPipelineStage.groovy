package com.telushealth.thcp.pipeline.project.bootstrap

import java.io.Serializable

import groovy.lang.Closure
import groovy.lang.Script

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The project bootstrap pipeline stage implementation class
 */
class ProjectBootstrapPipelineStage implements Serializable {

    private Script script

    ProjectBootstrapPipelineStage(Script script) {

        this.script = script
    }

    /**
     * The stage builder function
     *
     * @param label - the stage label
     * @param body - the script body
     *
     * <pre>
     *  Usage:
     *  stage ('Bootstrap') {
     *  }
     * </pre>
     */
    void stage (String name, Closure body) {

        script.echo 'Calling stage:' + name
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = new ProjectBootstrapPipelineSteps(script)

        script.stage(name) { body() }
    }
}
