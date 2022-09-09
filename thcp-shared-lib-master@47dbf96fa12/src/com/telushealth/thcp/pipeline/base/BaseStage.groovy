package com.telushealth.thcp.pipeline.base

import java.io.Serializable

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The base stage interface
 */
interface BaseStage extends Serializable {

	void call (String name , Closure body)		
}
