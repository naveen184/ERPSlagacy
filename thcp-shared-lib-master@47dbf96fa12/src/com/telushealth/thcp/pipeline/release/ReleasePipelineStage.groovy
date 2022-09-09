package com.telushealth.thcp.pipeline.release

import java.io.Serializable

import groovy.lang.Closure
import groovy.lang.Script

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The release pipeline stage implementation class
 */
class ReleasePipelineStage implements Serializable {

    private Script script

    ReleasePipelineStage (Script script) {

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
     *  stage ('Release') {
     *  }
     * </pre>
     */
    void stage (String name, Closure body) {

        script.echo 'Calling stage:' + name
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = new ReleasePipelineSteps(script)

        script.stage(name) { body() }
    }
}
