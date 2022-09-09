package com.telushealth.thcp.pipeline.build

import org.apache.commons.lang3.StringUtils

import com.telushealth.thcp.pipeline.base.BaseStep
import com.telushealth.thcp.pipeline.base.delegate.ArtifactoryStepDelegate
import com.telushealth.thcp.pipeline.base.delegate.GradleBuildStepDelegate
import com.telushealth.thcp.pipeline.base.delegate.MavenBuildStepDelegate
import com.telushealth.thcp.pipeline.base.delegate.NpmBuildStepDelegate
import com.telushealth.thcp.pipeline.common.THCPConstants
import com.telushealth.thcp.pipeline.common.artifactory.Artifactory
import com.telushealth.thcp.pipeline.common.util.BuildUtil
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil
import hudson.model.Result

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The build pipeline steps implementation class
 */
class BuildPipelineSteps extends BaseStep {

    private Script script

    BuildPipelineSteps (Script script) {

        this.script = script
    }

    /**
     * Step to publish artifacts to the gradle local snapshot/releases repos
     *
     * @param body - the script body
     */
    void publish (Closure body) {

        script.echo 'Calling publish step'

        ArtifactoryStepDelegate delegate = new ArtifactoryStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.fileFilter) : 'Param fileFilter is required'
        
        doPublish(THCPConstants.GRADLE_SNAPSHOT_LOCAL_REPO, THCPConstants.GRADLE_RELEASE_LOCAL_REPO,
                delegate.namespace, delegate.fileFilter, true)
    }

    /**
     * The gradle publish step
     * 
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * gradlePublish {
     *     buildTasks = 'publish'
     * }
     * </pre>
     */
    void gradlePublish (Closure body) {

        script.echo 'Calling gradle publish step'

        GradleBuildStepDelegate delegate = new GradleBuildStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.buildTasks) : 'Param buildTasks is required'

        def targetRepo = THCPConstants.GRADLE_RELEASE_LOCAL_REPO
        if (BuildUtil.isSnapshot(script)) {
            targetRepo = THCPConstants.GRADLE_SNAPSHOT_LOCAL_REPO
        }
        def server = script.Artifactory.server THCPConstants.ARTIFACTORY_SERVER_ID
        def rtGradle = script.Artifactory.newGradleBuild()
        rtGradle.usesPlugin = true
        rtGradle.tool = THCPConstants.GRADLE_TOOL_NAME
        rtGradle.deployer server: server, repo: targetRepo
        def buildInfo = rtGradle.run rootDir: ".", buildFile: delegate.buildFile, tasks: delegate.buildTasks
        server.publishBuildInfo buildInfo
    }
    
    /**
     * The maven publish step
     *
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * mavenPublish {
     *     goals = 'publish'
     * }
     * </pre>
     */
    void mavenPublish (Closure body) {

        script.echo 'Calling mvn publish step'

        MavenBuildStepDelegate delegate = new MavenBuildStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.goals) : 'Param goals is required'

        def server = script.Artifactory.server THCPConstants.ARTIFACTORY_SERVER_ID
        def rtMaven = script.Artifactory.newMavenBuild()
        rtMaven.tool = THCPConstants.MAVEN_TOOL_NAME
        rtMaven.deployer server: server, releaseRepo:THCPConstants.MAVEN_RELEASE_LOCAL_REPO, snapshotRepo:THCPConstants.MAVEN_SNAPSHOT_LOCAL_REPO
        def buildInfo = rtMaven.run pom: delegate.pomFile, goals: delegate.goals
        rtMaven.deployer.deployArtifacts buildInfo
        server.publishBuildInfo buildInfo
    }

    /**
     * Step to publish puppet artifacts to the puppet-local repo
     *
     * @param body - the script body
     */
    void publishToPuppetRepo (Closure body) {

        script.echo 'Calling publishPuppetModule step'

        ArtifactoryStepDelegate delegate = new ArtifactoryStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.fileFilter) : 'Param fileFilter is required'

        doPublish(null, THCPConstants.PUPPET_LOCAL_REPO,
                delegate.namespace, delegate.fileFilter, false)
    }
    
    /**
     * Step to publish python artifacts to the python-local repo
     *
     * @param body - the script body
     */
    void publishToPythonRepo (Closure body) {

        script.echo 'Calling publishToPythonRepo step'

        ArtifactoryStepDelegate delegate = new ArtifactoryStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.fileFilter) : 'Param fileFilter is required'

        doPublish(null, THCPConstants.PYTHON_LOCAL_REPO,
                delegate.namespace, delegate.fileFilter, false)
    }
  
    /**
    * Step to publish rpm artifacts to the rpm artifactory repo
    *
    * @param body - the script body
    */
    void publishToRpmRepo (Closure body) {

        script.echo 'Calling publishToRpmRepo step'

        ArtifactoryStepDelegate delegate = new ArtifactoryStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.fileFilter) : 'Param fileFilter is required'

        doPublish(THCPConstants.RPM_SNAPSHOT_REPO, THCPConstants.RPM_RELEASE_REPO,
              delegate.namespace, delegate.fileFilter, true, true)
    }

    /**
     * The npm publish step
     *
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * npmPublish {
     *      srcPath
     *      repo
     * }
     * </pre>
     */
    void npmPublish (Closure body) {

        script.echo 'Calling npm publish step'

        NpmBuildStepDelegate delegate = new NpmBuildStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.srcPath) : 'Param srcPath is required'
        assert !StringUtils.isEmpty(delegate.repo) : 'Param repo is required'

        def rtNpm = script.Artifactory.newNpmBuild()
        def server = script.Artifactory.server THCPConstants.ARTIFACTORY_SERVER_ID
        rtNpm.deployer server: server, repo: delegate.repo
        rtNpm.tool = delegate.tool

        script.echo 'Publishing from project directory: ' + delegate.srcPath + ' and publishing to artifactory repo: ' + delegate.repo

        def buildInfo = rtNpm.publish path: delegate.srcPath //, args: delegate.args
        server.publishBuildInfo buildInfo
    }

    /**
     * Step to publish nuget artifacts to the nuget-local repo
     *
     * @param body - the script body
     */
    void publishToNugetRepo (Closure body) {

        script.echo 'Calling Nuget step'

        ArtifactoryStepDelegate delegate = new ArtifactoryStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.fileFilter) : 'Param fileFilter is required'

        doPublish(THCPConstants.NUGET_SNAPSHOT_REPO, THCPConstants.NUGET_RELEASE_REPO, 
              delegate.namespace, delegate.fileFilter, true)
    }
    
    /**
     * Step to publish Integration tests reports to sonarqube server
     *
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * sonarqubePublish {
     *      failOnError = true/false (not mandatory)
     * }
     * </pre>
     */
    void sonarqubePublish (Closure body) {
        script.echo 'Calling sonarqube step'
        
        Map vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()
        
        Boolean throwException = vars.failOnError == null ? false : vars.failOnError
        
        def projectName = JenkinsUtil.getProjectName(script)
        
        assert !StringUtils.isEmpty(projectName) : 'Unable to fetch the project name'
        
        try {
        
            script.withCredentials(
                [
                    usernamePassword(
                            credentialsId: "${projectName}-sonarqube-server",
                            passwordVariable: 'SONARQUBE_SERVER_TOKEN',
                            usernameVariable: 'SONARQUBE_SERVER_URL'
                    )
                ]
            ) {
                mavenBuild {
                  goals = "sonar:sonar -Dsonar.host.url=${script.env.SONARQUBE_SERVER_URL} -Dsonar.login=${script.env.SONARQUBE_SERVER_TOKEN} -Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-ut/jacoco.xml"
                }
            }
        } catch(Exception e) {
            if(throwException) {
                throw new InterruptedException("Problem in publishing to ${projectName} sonarqube", ex)
            } else {
                script.echo "NOT FAILING - ${script.currentBuild.result}. Problem in publishing to ${projectName} sonarqube: ${e.message}"
                script.currentBuild.rawBuild.@result = hudson.model.Result.SUCCESS
            }
        }
        script.echo "Build Status - ${script.currentBuild.result}."
    }
    
    /**
     * Helper method to do the artifact publishing
     *
     * @param snapshotRepo
     * @param releaseRepo
     * @param targetNamespace
     * @param filePattern
     */
    private void doPublish (String snapshotRepo, String releaseRepo, String namespace, String fileFilter,
                            boolean allowSnapshot, boolean isRpm = false) {

        script.echo 'Publishing to Artifactory namespace:' + namespace + ', filefilter:' + fileFilter

        Artifactory artifactory = new Artifactory(script)
        artifactory.publish(snapshotRepo, releaseRepo, namespace, fileFilter, allowSnapshot, isRpm)
    }
}
