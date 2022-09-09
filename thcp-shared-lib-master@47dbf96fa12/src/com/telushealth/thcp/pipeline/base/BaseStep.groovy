package com.telushealth.thcp.pipeline.base

import org.apache.commons.lang3.StringUtils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.telushealth.thcp.pipeline.base.delegate.ArtifactoryStepDelegate
import com.telushealth.thcp.pipeline.base.delegate.GradleBuildStepDelegate
import com.telushealth.thcp.pipeline.base.delegate.MavenBuildStepDelegate
import com.telushealth.thcp.pipeline.base.delegate.NpmBuildStepDelegate
import com.telushealth.thcp.pipeline.common.THCPConstants
import com.telushealth.thcp.pipeline.common.artifactory.Artifactory
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil
import com.telushealth.thcp.pipeline.common.util.SecretsUtil

import jenkins.model.Jenkins

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The base step implementation class
 */
class BaseStep implements  Serializable {

    static ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    
    /**
     * Delegate native pipeline methods to the script object
     *
     * @param name - the method name
     * @param args - method args
     * @return the method output
     */
    def methodMissing ( String name, args) {
        def result = script.invokeMethod(name, args)
        result
    }


    /**
     * The SCM checkout step
     */
    void checkout () {

        script.echo 'Calling checkout step'
        script.checkout script.scm
    }

    /**
     * The SCM checkoutLocalBranch step
     */
    void checkoutLocalBranch () {

        script.echo 'Calling checkoutLocalBranch step'
        script.checkout([
            $class: 'GitSCM',
            branches: script.scm.branches,
            extensions: script.scm.extensions + [
                [$class: 'CleanCheckout'],
                [$class: 'LocalBranch', localBranch: '**']
            ],
            userRemoteConfigs: script.scm.userRemoteConfigs
        ])
    }

    /**
     * The gradle build step
     * 
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * gradleBuild {
     *     buildTasks = 'clean distTar'  
     * }
     * </pre>
     */
    void gradleBuild (Closure body) {

        script.echo 'Calling gradle build step'

        GradleBuildStepDelegate delegate = new GradleBuildStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.buildTasks) : 'Param buildTasks is required'
        
        def server = script.Artifactory.server THCPConstants.ARTIFACTORY_SERVER_ID
        def rtGradle = script.Artifactory.newGradleBuild()
        rtGradle.usesPlugin = true
        rtGradle.tool = THCPConstants.GRADLE_TOOL_NAME
        rtGradle.deployer server: server, releaseRepo: THCPConstants.GRADLE_RELEASE_LOCAL_REPO, snapshotRepo: THCPConstants.GRADLE_SNAPSHOT_LOCAL_REPO
        rtGradle.run rootDir: ".", buildFile: delegate.buildFile, tasks: delegate.buildTasks, switches: delegate.buildSystemPropertySwitch()
    }

    /**
     * The maven build step
     *
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * mavenBuild {
     *     buildTasks = 'clean distTar'
     * }
     * </pre>
     */
    void mavenBuild (Closure body) {

        script.echo 'Calling maven build step'

        MavenBuildStepDelegate delegate = new MavenBuildStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.goals) : 'Param goals is required'

        def rtMaven = script.Artifactory.newMavenBuild()        
        rtMaven.tool = THCPConstants.MAVEN_TOOL_NAME
        rtMaven.run pom: delegate.pomFile, goals: delegate.goals, switches: delegate.buildSystemPropertySwitch()
    }

    /**
     * The npm build step
     *
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * npmBuild {
     *     srcPath = 'backend'
     *     repo = 'npm'
     * }
     * </pre>
     */
    void npmBuild (Closure body) {

        script.echo 'Calling npm build step'

        NpmBuildStepDelegate delegate = new NpmBuildStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.srcPath) : 'Param srcPath is required'
        assert !StringUtils.isEmpty(delegate.repo) : 'Param repo is required'

        def rtNpm = script.Artifactory.newNpmBuild()
        def server = script.Artifactory.server THCPConstants.ARTIFACTORY_SERVER_ID
        rtNpm.resolver server: server, repo: delegate.repo

        if (delegate.tool == null || delegate.tool == '') { 
            rtNpm.tool = THCPConstants.NPM_TOOL_NAME
        } else {
            rtNpm.tool = delegate.tool
        }

        script.echo 'Building from project directory: ' + delegate.srcPath + ' and getting dependencies from artifactory repo: ' + delegate.repo
        rtNpm.install path: delegate.srcPath, args: delegate.args
    }
    
    /**
     * The download artifact step
     * 
     * @param body - the script body
     * 
     *  <pre>
     *  Usage:
     *  downloadArtifact {
     *      repo = 'gradle-snapshots-local'
     *      namespace = '/com/telushealth/thcp/pipeline/packer/'
     *      fileFilter = '&#42;&#42;/thcp-aws-ebs-linux-0.1.0-SNAPSHOT.tar.gz'
     *      localPath = 'download/'
     *      isUntar = true
     *  }
     *  </pre>
     */
    void downloadArtifact (Closure body) {

        script.echo 'Calling downloadArtifact step'

        ArtifactoryStepDelegate delegate = new ArtifactoryStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.repo) : 'Param repo is required'
        assert !StringUtils.isEmpty(delegate.namespace) : 'Param namespace is required'
        assert !StringUtils.isEmpty(delegate.fileFilter) : 'Param fileFilter is required'
        assert !StringUtils.isEmpty(delegate.localPath) : 'Param localPath is required'

        script.echo 'Downloading artifact from repo: ' + delegate.repo + ', namespace:' + delegate.namespace + ', filefilter:' +
                delegate.fileFilter + ', localPath:' + delegate.localPath

        Artifactory artifactory = new Artifactory(script)
        artifactory.download(delegate.repo, delegate.namespace, delegate.fileFilter, delegate.localPath, delegate.flat, delegate.explode)

        if (delegate.isUntar && !delegate.explode) {
            untar(body)
        }
    }

    /**
     * The untar step. Untars all files which matches the fileFilter.
     *
     * @param body - the script body
     *
     *  <pre>
     *  Usage:
     *  untar {
     *      fileFilter = '&#42;&#42;/thcp-aws-ebs-linux-0.1.0-SNAPSHOT.tar.gz'
     *  }
     *  </pre>
     */
    void untar (Closure body) {

        script.echo 'Calling untar step'

        ArtifactoryStepDelegate delegate = new ArtifactoryStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        assert !StringUtils.isEmpty(delegate.fileFilter) : 'Param fileFilter is required'

        def files = script.findFiles(glob: delegate.fileFilter)

        files.each( {
            script.echo 'Untaring file: ' + it.path
            if(script.isUnix()) {
                script.sh 'tar -zxvf ' + it.path
            } else {
                def fileName = it.name.replace('.gz','')
                script.powershell """
                  7z x ${it.path}
                  7z x ${fileName}
                """
            }
        } )
    }
    
    /**
     * The delete ami step with target roles to assume.
     * This step will delete the ami in the management or target account.
     *
     * @param body - the script body
     * <pre>
     * Usage:
     * deleteAmi {
     *  releaseName = product release name. Optional
     *  deleteOlderThanDays = 60
     *  amiName = Name of the ami to be deleted --> Manadatory parameter
     * }
     *
     * deleteAmi {
     *   releaseName = product release name. Optional
     *  amiToKeep = 5
     *  amiName = Name of the ami to be deleted --> Manadatory parameter
     * }
     * </pre>
     */
    void deleteAmi (String releaseName='', Closure body) {
        def gradleOpts = JenkinsUtil.getGradleCmdOpts(body)
        
        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ami/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        def buildTask = 'deleteAmi'
        
        def bakingManifestFileName = THCPConstants.BAKING_MANIFEST_FILE_NAME
        if(!StringUtils.isEmpty(releaseName)) {
            bakingManifestFileName = releaseName + "-" + bakingManifestFileName
        }
        def sourceBackingManifestFileName = bakingManifestFileName

        gradleBuild {
            buildFile = 'deploy.gradle'
            buildTasks = "--stacktrace ${buildTask} ${gradleOpts} -PsourcePackerManifest=${sourceBackingManifestFileName}"
        }
    }
    
    void deleteSlaves(Closure body) {
        
        Map vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()
        
        Boolean dryRun = vars.dryRun == null ? false : vars.dryRun
        
        List<String> exclusionLabels = ["master"]
        
        if(vars.excludeLabels != null) {
             for(String str : vars.excludeLabels) {
                 exclusionLabels.add(str)
             }
        }
        for (computer in Jenkins.instance.computers) {
            script.echo '===================='
            String computerName = computer.displayName.toLowerCase()
            script.echo 'Displayname: ' + computerName
    
            if (!dryRun) {
                if(isComputerExcluded(computerName, exclusionLabels)) {
                    script.echo 'Skiping deleting : ' + computerName
                    continue
                }
                script.echo 'Deleting ' + computerName + ' Slave'
                computer.doDoDelete()
            }
        }
    }

    Boolean isComputerExcluded(String computerName, List exclusionLabels) {
        boolean slaveExcluded = false
        for(String excludeLabel : exclusionLabels) {
            if(computerName.contains(excludeLabel)) {
                slaveExcluded = true
                break;
            }
        }
        return slaveExcluded
    }
    
    void decryptFile(Closure body) {
        
        Map vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()
        
        String fileName = vars.secretFileName;
        assert !StringUtils.isEmpty(fileName) : 'Param secretFileName is required'

        String projectName = JenkinsUtil.getProjectName(script)
        script.sh "echo Project name: ${projectName}"
        String keyContents = JenkinsUtil.getBasicSSHUserPrivateKey( projectName + '-puppet-private-key')
        def files = script.findFiles glob: "**/${fileName}"
        
        for (def i=0; i<files.length; i++) {
            script.sh "echo File Path: ${files[i].path}"
            String contents = script.readFile(files[i].path)
/*            def secretsObj = script.readYaml text:contents;
            int idx=0;
            def decryptObj = secretsObj
            secretsObj.secrets.each { secret -> 
                byte[] bytes = SecretsUtil.decryptString(keyContents, secret.value);
                decryptObj.secrets[idx].value = new String(bytes);
                idx++;
            }*/
            byte[] bytes = SecretsUtil.decryptString(keyContents, contents);
            script.writeFile encoding: 'UTF-8', file: "decrypt/${files[i].name}", text: new String(bytes)
        }
    }
    
}