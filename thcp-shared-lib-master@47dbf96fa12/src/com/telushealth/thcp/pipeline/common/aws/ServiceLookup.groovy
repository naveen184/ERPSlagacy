package com.telushealth.thcp.pipeline.common.aws

@Grapes([
    @GrabResolver(name='jcenter-gradle-releases', root='https://artifactory.tools.thcoe.ca/artifactory/gradle-jcenter-remote/'),
    @Grab(group = 'com.amazonaws', module = 'aws-java-sdk-ec2', version = '1.11.211'),
    @GrabResolver(name='jcenter-gradle-releases', root='https://artifactory.tools.thcoe.ca/artifactory/gradle-jcenter-remote/'),
    @Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.6')
])

import org.apache.commons.lang3.StringUtils

import com.amazonaws.regions.Region
import com.amazonaws.services.ec2.model.DescribeRegionsResult
import com.amazonaws.services.ec2.model.DescribeVpcsResult
import com.amazonaws.services.ec2.model.Vpc
import com.telushealth.thcp.pipeline.common.util.AWSUtil



/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 * 
 * AWS service lookup functions    
 */
class ServiceLookup implements Serializable {

	String getRegions() {

		DescribeRegionsResult regionsResult = AWSUtil.getEC2Client().describeRegions()
		List<String> regionNames = new ArrayList<String>()
		for (Region region : regionsResult.getRegions()) {
			regionNames.add(region.getRegionName())
		}

		return StringUtils.toStringArray(regionNames).join("\n")
	}

	String getVpcs() {
		DescribeVpcsResult vpcsResult = AWSUtil.getEC2Client().describeVpcs()
		List<String> vpcIds = new ArrayList<String>()
		for (Vpc vpc: vpcsResult.getVpcs()) {
			vpcIds.add(vpc.getVpcId())
		}

		return StringUtils.toStringArray(vpcIds).join("\n")
	}
}
