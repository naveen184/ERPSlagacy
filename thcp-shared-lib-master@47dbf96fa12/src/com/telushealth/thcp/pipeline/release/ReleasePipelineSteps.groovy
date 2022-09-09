package com.telushealth.thcp.pipeline.release
import org.apache.commons.lang3.StringUtils

import com.telushealth.thcp.pipeline.base.BaseStep
import com.telushealth.thcp.pipeline.common.util.BitbucketUtil
import com.telushealth.thcp.pipeline.common.util.BuildUtil
import com.telushealth.thcp.pipeline.common.util.TemplateUtil

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The release pipeline steps implementation class
 */
class ReleasePipelineSteps extends BaseStep {

    private Script script

    ReleasePipelineSteps (Script script) {

        this.script = script
    }

    /**
     * The checkoutProject step
     * This checks out the project branch for local updates for the provided scmUrl
     *
     * @param scmUrl - the scm url to clone
     * @param branch - the branch to checkout
     *
     * <pre>
     *  Usage:
     *  checkoutProject('https://someurl.com/project.git', 'master')
     * </pre>
     */
    void checkoutProject (String scmUrl, String branch) {
        
        assert !StringUtils.isEmpty(scmUrl) : 'scmUrl param is required'
        
        assert !StringUtils.isEmpty(branch) : 'branch param is required'
        
        String credentialsId='bitbucket-user'
        
        if(StringUtils.startsWith(scmUrl, "ssh")) {
            credentialsId = 'git-ssh-user'
        }
        
        script.checkout(
            [
                $class: 'GitSCM',
                branches: [[name: "*/${master}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                    [$class: 'CleanCheckout'],
                    [$class: 'LocalBranch', localBranch: '**']
                ],
                submoduleCfg: [],
                userRemoteConfigs: [
                    [credentialsId: credentialsId, url: scmUrl ]
                ]
            ]
        )
    }

    /**
     * The gradleRelease step
     * This runs the pipeline-gradle-plugin task
     *
     * @param releaseTask - the gradle task to run. See pipeline-gradle-plugin for valid tasks
     *
     * <pre>
     *  Usage:
     *  gradleRelease('gradleTaskName')
     * </pre>
     */
    void gradleRelease (String releaseTask) {

        //copy the build.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/release/build.gradle'
        script.writeFile file: 'build.gradle', text: gradleFile

        script.withCredentials(
            [
                usernamePassword(
                        credentialsId: 'naveen184',
                        passwordVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_PASSWORD',
                        usernameVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_USERNAME'
                )
            ]
        ) {
            gradleBuild () {
                buildFile = 'build.gradle'
                buildTasks = "${releaseTask}"
            }
        }
    }
    
    
    void seedCIReleaseJob ( ) {
        
        def jobName = script.env.JOB_NAME
        String[] arr = jobName.split("/")
        def productName = arr[arr.length-2]
        
        script.echo "Project Name: ${productName}"
        
        def config = script.readYaml file: 'ci_release.yml'
        def types = config.releaseTypes

        types.each { entry ->

            def buildType = entry.key 
            script.echo buildType

            for(def projectConfig : entry.value) {
                
                String projectKey = projectConfig.projectKey
                String branchToBuild = projectConfig.containsKey('branchToBuild')?projectConfig.branchToBuild:'master'
                
                TemplateUtil.generateProductCIReleasesFolder(productName, projectKey, script)
                
                List repoList = BitbucketUtil.getProjectRepos(projectKey, script)
                repoList.each { repo ->
                    repo.sshCredsId = 'git-ssh-user'
                    repo.httpCredsId = 'bitbucket-user'
                    
                    script.echo "Repo Name: ${repo.name}   HTTP Clone Url: ${repo.httpUrl}   SSH Clone Url: ${repo.sshUrl}"
                    
                    TemplateUtil.generateCIReleasePipelineJob(productName, projectKey, buildType, repo, branchToBuild, script)
                }
                TemplateUtil.generateCIParentReleasePipelineJob(productName, projectKey, buildType, repoList, branchToBuild, script)
            }
        }
    }
    
    void artifactCIRelease(String releaseType, String repoName, String branchName, String releaseVersion) {
        
        assert !StringUtils.isEmpty(releaseType) : 'releaseType param is required'
        
        assert !StringUtils.isEmpty(repoName) : 'repo param is required'
        
        assert !StringUtils.contains(releaseVersion, "SNAPSHOT") : 'Snapshot version is not allowed for a release'
        
        script.sh '''
            git config user.name "Pipeline User"
            git config user.email "thcp-prod-pipeline@telus.com"
        '''
        def methodName = "${releaseType}CIRelease"
        String projectVersion = this."$methodName"(releaseType, releaseVersion, repoName, branchName)
        
        pushRelease(releaseType, branchName)
        
        triggerCIBuild(releaseType, repoName, projectVersion)
        
    }
    
    private String mvnCIRelease( String releaseType, String releaseVersion, String repoName, String branchName ) {
        
        def versionParam = ''
        if(!StringUtils.isEmpty(releaseVersion)) {
            versionParam = "-DreleaseVersion=${releaseVersion}"
        }
        mavenBuild {
            goals = "-B release:prepare -DpushChanges=false -DtagNameFormat=release/@{project.version} ${versionParam}"
        }
        
        def releaseTag = BuildUtil.getMvnReleaseProperties(script).get(BuildUtil.MVN_RELEASE_TAG_PROPERTY)
        return releaseTag.replaceAll('/','%2F')
    }
    
    private String gradleCIRelease( String releaseType, String releaseVersion, String repoName, String branchName ) {
        
        def versionParam = ''
        if(!StringUtils.isEmpty(releaseVersion)) {
            versionParam = "-Prelease.forceVersion=${releaseVersion}"
        }
        gradleBuild {
            buildTasks = "createRelease -Prelease.disableUncommittedCheck -Prelease.localOnly ${versionParam}"
        }
        
        return branchName
    }
    
    private void triggerCIBuild(String releaseType, String repoName, String projectVersion) {
        
        def jobName = script.env.JOB_NAME
        String[] arr = jobName.split("/")
        def productName = arr[0].toUpperCase()
        def projectName = arr[2].toUpperCase()
        script.sleep 5
        script.echo "Running ${releaseType} ci build for ${productName} - ${projectName} - ${repoName}"
        
        script.echo 'Project Version: ' + projectVersion
        
        script.build job: "${productName}/${projectName}/${repoName}/${projectVersion}", propagate: true
    }
    
    private void pushRelease (String releaseType, String branchName) {
        //copy the shell script
        String gitCommitFile = script.libraryResource "com/telushealth/thcp/pipeline/gradle/git/push-${releaseType}-release.sh"
        script.writeFile file: "push-${releaseType}-release.sh", text: gitCommitFile

        //commit the release files to repo
        script.sh "chmod 755 ./push-${releaseType}-release.sh"
        sshagent(['git-ssh-user']) {
            script.sh "./push-${releaseType}-release.sh ${branchName}"
        }
    }
}
