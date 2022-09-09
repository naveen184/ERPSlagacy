package com.telushealth.thcp.pipeline.base.delegate

import org.apache.commons.text.StrBuilder
import com.telushealth.thcp.pipeline.common.THCPConstants

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The delegation class for the gradle build step
 */
class NpmBuildStepDelegate implements Serializable {
    String srcPath
    String repo
    String args // Note that NodeJs plugin doesn't support args parameter for npm publish
    String tool = THCPConstants.NPM_TOOL_NAME
}