package com.telushealth.thcp.pipeline.common.artifactory

import java.io.Serializable

import org.apache.commons.lang3.StringUtils
import org.jfrog.build.api.Artifact
import org.jfrog.build.api.Module
// import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo

import com.telushealth.thcp.pipeline.common.THCPConstants
import com.telushealth.thcp.pipeline.common.util.BuildUtil

import groovy.lang.Grab
import groovy.lang.Script

@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.6')
/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * Artifactory shared common lib
 *
 */
class Artifactory implements Serializable {

    private Script script

    /**
     * The constructor
     * @param script
     */
    Artifactory(Script script) {
        this.script = script
    }

    /**
     * Publish artifact to server
     * @param snapshotRepo - the snapshot repo
     * @param releaseRepo - the release repp
     * @param targetNamespace - the name space within the repo
     * @param filePattern - the file pattern to select the source artifacts
     * @return
     */
    void publish(String snapshotRepo, String releaseRepo, String targetNamespace, String filePattern, boolean
            allowSnapshot = true, boolean isRpm = false) {

        script.echo 'Project Version: ' + BuildUtil.getProjectProperties(script).get(BuildUtil.PROJECT_VERSION_PROPERTY)

        String targetRepo = releaseRepo

        if (BuildUtil.isSnapshot(script)) {
            if (!allowSnapshot) {
                throw new IllegalArgumentException('Publishing of snapshot artifact is not allow for this build ' +
                        'artifact')
            }
            targetRepo = snapshotRepo
        }

        if (StringUtils.isEmpty(targetNamespace)) {
            if(isRpm) {
                targetNamespace = BuildUtil.getRpmArtifactNamespace(script)
            } else {
                targetNamespace = BuildUtil.getArtifactNamespace(script)
            }
        }

        def server = script.Artifactory.server THCPConstants.ARTIFACTORY_SERVER_ID
        def buildInfo = server.upload(getUploadSpec(targetRepo, targetNamespace, filePattern))
        server.publishBuildInfo buildInfo
    }

    /**
     * Publish artifact to server
     * @param repo - the repo
     * @param targetNamespace - the name space within the repo
     * @param filePattern - the file pattern to select the source artifacts
     * @param localPath - the download local path
     * @return
     */
    void download(String repo, String targetNamespace, String filePattern, String localPath, Boolean flat, Boolean explode) {

        def server = script.Artifactory.server THCPConstants.ARTIFACTORY_SERVER_ID
        server.download(getDownloadSpec(repo, targetNamespace, filePattern, localPath, flat, explode), true)
    }
    
    /**
     * Get the name of the first artifact from the build info
     * 
     * @param buildInfo - the gradle build info
     * @return the artifact name
     */
    // String getBuildArtifactName (BuildInfo buildInfo) {
    //     List<Module> modules = buildInfo.getModules()
    //     List<Artifact> artifacts = modules.getArtifacts()
    //     for (Artifact artifact: artifacts) {
    //         return artifact.getName()
    //     }
    // }

    /**
     * Check if the the artifact is a snapshot
     * 
     * @param artifactName - the artifact name
     * @return is this a snapshot
     */
    boolean isSnapshot (String artifactName) {

        return StringUtils.contains(artifactName, "SNAPSHOT")
    }

    /**
     * Helper method to constructs the upload specs
     * @param targetRepo
     * @param targetNamespace
     * @param artifactFilePattern
     * @return the upload spec definition
     */
    private String getUploadSpec (String targetRepo, String targetNamespace, String artifactFilePattern) {
        return """{
          "files": [
            {
              "pattern": "${artifactFilePattern}",
              "target": "${targetRepo}${targetNamespace}"
            }
          ]
        }"""
    }
    
    /**
     * Helper method to constructs the download specs
     * @param repo - the download repo
     * @param namespace - the name space
     * @param filePattern - the search pattern
     * @param localPath - the path to store the artifact
     * @return the upload spec definition
     */
    private String getDownloadSpec (String repo, String namespace, String filePattern, String localPath, boolean flat, boolean explode) {
        return """{
          "files": [
            {
              "pattern": "${repo}${namespace}${filePattern}",
              "target": "${localPath}",
              "flat": "${flat}",
              "explode": "${explode}"
            }
          ]
        }"""
    }
}

