package com.telushealth.thcp.pipeline.base.delegate

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The delegation class for the artifactory step step
 */
class ArtifactoryStepDelegate implements Serializable {
	
	String repo
	String namespace
	String fileFilter
	String localPath
	Boolean isUntar
  Boolean flat = Boolean.FALSE
  Boolean explode  = Boolean.FALSE
}