package com.telushealth.thcp.pipeline.bake

import java.io.Serializable

import groovy.lang.Closure
import groovy.lang.Script

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The bake pipeline stage implementation class
 */
class BakePipelineStage implements Serializable {

	private Script script

	BakePipelineStage (Script script) {

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
	 *  stage ('Bake') {
	 *  }
	 * </pre>
	 */
	void stage (String name, Closure body) {

		script.echo 'Calling stage:' + name
		body.resolveStrategy = Closure.DELEGATE_FIRST
		body.delegate = new BakePipelineSteps(script)

		script.stage(name) { body() }
	}
}
