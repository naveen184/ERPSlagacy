package com.telushealth.thcp.pipeline.common.aws

import com.amazonaws.AmazonClientException
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest
import com.telushealth.thcp.pipeline.common.util.AWSUtil

@Grab(group = 'com.amazonaws', module = 'aws-java-sdk-cloudformation', version = '1.11.211')

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * This class provides common Cloudformation functions
 */
class CloudFormation implements Serializable {

	private Script script
	private String region

	/**
	 * The CloudFormation constructor
	 * @param script - the script reference
	 * @param region - the AWS region, defaults to ca-central_1
	 */
	CloudFormation(Script script, String region = Regions.CA_CENTRAL_1.name) {
		this.script = script
		this.region = region
	}

	/**
	 * Validates the CFN templates selected by the file filter pattern recursively
	 *
	 * @param filterPattern - the wild card Ant style pattern
	 * @return
	 */
	def validateTemplate(String filterPattern) {
		ValidateTemplateRequest request

		try {
			def files = script.steps.findFiles(glob: filterPattern)
			files.each {
				script.echo('Validating cfn template:' + it.path)
				String content = script.steps.readFile(it.path)

				AmazonCloudFormation cloudFormation = AmazonCloudFormationClientBuilder.standard().
						withCredentials(AWSUtil.getInstanceProfileProvider()).withRegion(region).
						build()

				request = new ValidateTemplateRequest()
				request.setTemplateBody(content)

				cloudFormation.validateTemplate(request)
			}
		}
		catch (AmazonCloudFormationException ex) {
			script.echo('CloudFormation exception occurred: ' + ex.message)
			throw ex
		}
		catch (AmazonClientException ex) {
			script.echo('Amazon client exception occurred: ' + ex.message)
			throw ex
		}
	}
}