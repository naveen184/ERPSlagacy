package com.telushealth.thcp.pipeline.build

import org.apache.commons.lang3.StringUtils

import com.telushealth.thcp.pipeline.base.BasePipeline

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
 * The build pipeline implementation class
 */
class BuildPipeline implements BasePipeline {

	private Script script

	BuildPipeline (Script script) {

		this.script = script
	}

	/**
	 * The build pipeline builder
	 *
	 * @param label - the node agent label
	 * @param body - the script body
	 *
	 * <pre>
	 *  Usage:
	 *  @Library('thcp-shared-lib')
	 *  import com.telushealth.thcp.pipeline.build.BuildPipeline
	 *  BuildPipeline buildPipeline = new BuildPipeline(this)
	 *
	 *  buildPipeline('linux && gradle') {
	 *  	stage ('Stage 1') {
	 *  	}
	 *  	stage ('Stage 2') {
	 *  	}
	 *  }
	 * </pre>
	 */
	void call(String label, Closure body) {

		script.echo 'Calling build pipeline'

		assert !StringUtils.isEmpty(label) : 'Param label is required!'

		body.resolveStrategy = Closure.DELEGATE_FIRST
		body.delegate = new BuildPipelineStage(script)

		script.node(label) { body()  }
	}
}
