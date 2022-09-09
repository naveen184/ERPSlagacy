package com.telushealth.thcp.pipeline.build

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The build pipeline stage implementation class
 */
class BuildPipelineStage implements Serializable {

	private Script script

	BuildPipelineStage (Script script) {

		this.script = script
	}

	def methodMissing ( String name, args) {
		script.echo ('Missing method: ' + name + ' args:' + args)
	}

	/**
	 * The stage builder function
	 *
	 * @param label - the stage label
	 * @param body - the script body
	 *
	 * <pre>
	 *  Usage:
	 *  stage ('Build') {
	 *  }
	 * </pre>
	 */
	void stage (String name, Closure body) {

		script.echo 'Calling stage:' + name
		body.resolveStrategy = Closure.DELEGATE_FIRST
		body.delegate = new BuildPipelineSteps(script)

		script.stage(name) { body() }
	}
}
