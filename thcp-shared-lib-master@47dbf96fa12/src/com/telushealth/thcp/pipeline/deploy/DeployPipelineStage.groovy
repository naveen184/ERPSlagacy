package com.telushealth.thcp.pipeline.deploy

import com.telushealth.thcp.pipeline.config.PipelineConfig

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The deploy pipeline stage implementation class
 */
class DeployPipelineStage implements Serializable {

    private String label
    private Script script

    DeployPipelineStage(String label, Script script) {

        this.label = label
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
     *  stage ('Deploy') {
     *  }
     * </pre>
     */
    void stage (String name, Closure body) {

        script.echo 'Calling stage:' + name
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = new DeployPipelineSteps(label, script)

        script.stage(name) { body() }
    }
}
