package com.telushealth.thcp.pipeline.bake.delegate


import org.apache.commons.text.StrBuilder

import com.telushealth.thcp.pipeline.common.THCPConstants
import com.telushealth.thcp.pipeline.common.util.BuildUtil

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The delegation class for the packer step
 */
class PackerStepDelegate implements Serializable {

    private static final SPACE = ' '

    private static final SINGLE_QUOTE = '\''

    private static final EQUALS = '='

    private static final MINUS = '-'

    private static final VAR_PARAM = '-var'

    private String cmd = 'build'

    private String template

    private boolean isDefaultBranch = true

    private String puppet_key_id

    private Map options = [:]

    private Map vars = [:]
    
    private static final VAR_RELEASE_NAME = THCPConstants.PACKER_VAR_RELEASE_NAME

    /**
     * Helper method to build the cmd line options
     * 
     * @param packerCmd - the absolute path for the packer cmd
     * 
     * @return the cmd line to execute
     */
    String buildCmdOptions (String packerCmd) {

        StrBuilder builder = new StrBuilder()
        builder.
            append(packerCmd).
            append(SPACE).
            append(cmd).
            append(SPACE)

        options.each {
            builder.
                append(MINUS).
                append(it.key).
                append(EQUALS).
                append(it.value).
                append(SPACE)
        }

        vars.each {
            if(!VAR_RELEASE_NAME.equalsIgnoreCase("${it.key}")) {
                builder.
                    append(VAR_PARAM).
                    append(SPACE).
                    append(SINGLE_QUOTE).
                    append(it.key).
                    append(EQUALS).
                    append(it.value).
                    append(SINGLE_QUOTE).
                    append(SPACE)
            }
        }

        builder.append(template)

        return builder.toString()
    }
    
    /**
     * Helper method to get the release name from the delegate vars
     *
     * @return the release name value
     */
    String getReleaseName () {
        if(vars.containsKey(VAR_RELEASE_NAME)) {
            return vars.get(VAR_RELEASE_NAME)
        }
        return null;
    }
    
    /**
     * Helper method to check if rapid7 scan is enabled or not
     */
    boolean isRapid7ScanEnabled () {
        boolean rapid7Scan = false
        
        if(vars.containsKey("rapid7_domain") && vars.containsKey("rapid7_site_name")) {
            rapid7Scan = true
        }
        return rapid7Scan
    }
}