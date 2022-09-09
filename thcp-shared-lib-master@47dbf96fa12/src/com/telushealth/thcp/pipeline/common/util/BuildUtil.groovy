package com.telushealth.thcp.pipeline.common.util

import com.telushealth.thcp.pipeline.common.THCPConstants
import org.apache.commons.lang3.StringUtils

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *     
 * Helper class for build related functions
 */
class BuildUtil implements Serializable {

    static final SNAPSHOT = "SNAPSHOT"

    static final PROJECT_PROPERTY_FILE = "build/distributions/project.properties"
    
    static final MVN_RELEASE_PROPERTY_FILE = "release.properties"
    
    static final PROJECT_NAME_PROPERTY = "project.name" 
    
    static final PROJECT_GROUP_PROPERTY = "project.group"
    
    static final PROJECT_VERSION_PROPERTY = "project.version"
    
    static final MVN_RELEASE_TAG_PROPERTY = "scm.tag"
    
    static String getPackerCmd (Script script) {
        if(script.isUnix()) {
            return 	script.tool(name: THCPConstants.PACKER_TOOL_NAME, type: 'biz.neustar.jenkins.plugins.packer.PackerInstallation') + '/packer'
        } else {
            return 	script.tool(name: THCPConstants.PACKER_WINDOWS_TOOL_NAME, type: 'biz.neustar.jenkins.plugins.packer.PackerInstallation') + '/packer.exe'
        }
    }
    
    static Properties getProjectProperties (Script script) {
        
        return script.readProperties(file: PROJECT_PROPERTY_FILE) 		
    }
    
    static Properties getMvnReleaseProperties (Script script) {
        return script.readProperties(file: MVN_RELEASE_PROPERTY_FILE)
    }
    
    /**
     * Convert a java package name to file path
     * 
     * @param name - the package name
     * @return the path
     */
    static String convertPackageToPath (String... names) {

        String packageName = StringUtils.join(names, ".")
        return "/" + StringUtils.replace(packageName, ".", "/") + "/"
    }

    /**
     * Check if the given artifact is a snapshot version
     * 
     * @param name - the artifact name
     * @return is a snapshot
     */
    static boolean isSnapshot (String name) {
        return StringUtils.contains(name, SNAPSHOT)
    }
    
    static boolean isSnapshot (Script script) {
        return isSnapshot(getProjectProperties(script).get(PROJECT_VERSION_PROPERTY))
    }
    
    /**
     * Get the artifact repo namespace
     * 
     * @return the artifact repo namespace
     */
    static String getArtifactNamespace (Script script) {
        Properties prop = getProjectProperties(script)
        String projectGroup = prop.get(PROJECT_GROUP_PROPERTY)
        String projectName = prop.get(PROJECT_NAME_PROPERTY)
        String projectVersion = prop.get(PROJECT_VERSION_PROPERTY)
        String path = convertPackageToPath(projectGroup, projectName) + "/" + projectVersion + "/"
        return path
    }
    
    /**
     * Get the artifact repo namespace for rpm
     *
     * @return the artifact repo namespace for rpm
     */
    static String getRpmArtifactNamespace (Script script) {
        Properties prop = getProjectProperties(script)
        String projectGroup = prop.get(PROJECT_GROUP_PROPERTY)
        String projectName = prop.get(PROJECT_NAME_PROPERTY)
        String repoName = isSnapshot(script) ? "snapshots" : "releases"
        String path = convertPackageToPath(projectGroup, repoName, projectName); 
        return path
    }
}