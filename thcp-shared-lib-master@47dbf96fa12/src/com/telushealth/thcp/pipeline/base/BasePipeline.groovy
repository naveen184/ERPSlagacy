package com.telushealth.thcp.pipeline.base

import java.io.Serializable

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The base pipeline interface
 */
interface BasePipeline extends Serializable {
	
	void call(String label, Closure body)
}
