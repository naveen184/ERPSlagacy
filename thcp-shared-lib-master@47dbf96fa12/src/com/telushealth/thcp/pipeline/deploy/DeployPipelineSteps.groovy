package com.telushealth.thcp.pipeline.deploy

import com.telushealth.thcp.pipeline.base.BaseStep
import com.telushealth.thcp.pipeline.common.util.AWSUtil
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil
import com.telushealth.thcp.pipeline.common.THCPConstants
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StrBuilder

import com.telushealth.thcp.pipeline.deploy.delegate.AwsCloudfrontDelegate
import com.telushealth.thcp.pipeline.deploy.delegate.AwsDMSDelegate
import com.telushealth.thcp.pipeline.deploy.delegate.AwsECSDelegate
import com.telushealth.thcp.pipeline.deploy.delegate.AwsSSMDocumentValidator

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The deploy pipeline steps implementation class
 */
class DeployPipelineSteps extends BaseStep {

    private String label
    private Script script

    DeployPipelineSteps(String label, Script script) {
        this.label = label
        this.script = script
    }

    /**
     * The gradle deploy step with default slave instance profile role
     * 
     * @param sourceRoleCredentialId - the source role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * gradleDeploy {
     *     buildTasks = 'clean distTar'
     * }
     * </pre>
     */
    void gradleDeploy (Closure body) {

        gradleBuild(body)
    }

    /**
     * The gradle deploy step with source role to assume
     * 
     * @param sourceRoleCredentialId - the source role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * gradleDeploy ('source-role-credential-id', 'use-sts-credentials') {
     *     buildTasks = 'clean distTar'
     * }
     * </pre>
     */
    void gradleDeploy (String sourceRoleCredentialId, Boolean useSts=false, Closure body) {

        List<String> envCredentials = new ArrayList<String>()

        assert !StringUtils.isEmpty(sourceRoleCredentialId) : 'sourceRoleCredentialId param is required'

        //assume role if not set to instance-profile
        if (!StringUtils.equalsIgnoreCase(sourceRoleCredentialId, THCPConstants.ASSUME_INSTANCE_PROFILE)) {

            if(useSts) {
              envCredentials.addAll(AWSUtil.getStSRoleDetails(sourceRoleCredentialId, script, AWSUtil.TARGET_PREFIX))
            } else {
              envCredentials.addAll(AWSUtil.getAssumedRoleCredentials(sourceRoleCredentialId, script, AWSUtil.NO_PREFIX))
            }
            script.withEnv(envCredentials) {
                gradleBuild(body)
            }
        }
        else {
            //use the instance profile instead
            gradleBuild(body)
        }
    }

    /**
     * The gradle deploy step with source and target roles to assume
     * 
     * @param sourceRoleCredentialId - the source role credential id
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * gradleDeploy ('source-role-credential-id', 'source-role-credential-id') {
     *     buildTasks = 'clean distTar'
     * }
     * </pre>
     */
    void gradleDeploy (String sourceRoleCredentialId, String targetRoleCredentialId, Closure body) {

        List<String> envCredentials = new ArrayList<String>()

        assert !StringUtils.isEmpty(sourceRoleCredentialId) : 'sourceRoleCredentialId param is required'
        assert !StringUtils.isEmpty(targetRoleCredentialId) : 'targetRoleCredentialId param is required'

        //assume role if not set to instance profile
        if (!StringUtils.equalsIgnoreCase(sourceRoleCredentialId, THCPConstants.ASSUME_INSTANCE_PROFILE)) {
            envCredentials.addAll(AWSUtil.getStSRoleDetails(sourceRoleCredentialId, script, AWSUtil.SOURCE_PREFIX))
        }
        envCredentials.addAll(AWSUtil.getStSRoleDetails(targetRoleCredentialId, script, AWSUtil.TARGET_PREFIX))

        script.withEnv(envCredentials) {
            gradleBuild(body)
        }
    }

    /**
     * The copy ami step with target roles to assume. This step first trys to read the manifest file generated by packer, if not present will read the manifest
     * file stored in the repo
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param isCreate - Whether createAmi task to be invoked or not
     * @param releaseName - release name optional parameter
     * @param body - the script body
     * <pre>
     * Usage:
     * copyAmi ('target-role-credential-id', 'invoke-gradle-create-ami-task', releaseName, closure)
     * </pre>
     */
    void copyAmi (String targetRoleCredentialId, Boolean isCreate, String releaseName, Closure body={}) {
        def gradleOpts = JenkinsUtil.getGradleCmdOpts(body)
        
        assert !StringUtils.isEmpty(targetRoleCredentialId) : 'targetRoleCredentialId param is required'

        //checkout the current project
        checkoutLocalBranch()

        def bakingManifestFileName = THCPConstants.BAKING_MANIFEST_FILE_NAME
        def packerManifestStashName = THCPConstants.PACKER_MANIFEST_STASH_NAME
        if(!StringUtils.isEmpty(releaseName)) {
            bakingManifestFileName = releaseName + "-" + bakingManifestFileName
            packerManifestStashName = releaseName + "-" + packerManifestStashName
        }
        def sourceBackingManifestFileName = bakingManifestFileName
        def branchName = script.env.BRANCH_NAME
        //unstash packer manifest file if ami id not specified
        try {
            script.unstash name: packerManifestStashName
            script.echo "Using ${bakingManifestFileName} from packer build to copy AMI"
        }
        catch (hudson.AbortException ex) {
            //Ignore this exception. This step can be called without packer build step for non default branches
            script.echo "Packer manifest not found in stash: ${ex.message}"
            
            bakingManifestFileName = "${branchName}-${THCPConstants.BAKING_MANIFEST_FILE_NAME}"
            if(!StringUtils.isEmpty(releaseName)) {
                bakingManifestFileName = releaseName + "-" + bakingManifestFileName
            }
            script.echo "Using checked in ${bakingManifestFileName} version to copy AMI"
        }

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ami/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        //this task does the cross account ami copy and generates the baking manifest.json file
        def buildTask = 'copyAmi'
        if(isCreate) {
            buildTask = 'createAmi'
        }
        
        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "--stacktrace ${buildTask} --packerManifest=${bakingManifestFileName} ${gradleOpts} -PsourcePackerManifest=${sourceBackingManifestFileName}"
        }

        String gitCommitFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/git/commit-and-push-file.sh'
        script.writeFile file: 'commit-and-push-file.sh', text: gitCommitFile

        //commit the manifest file to repo
        script.sh 'chmod 755 ./commit-and-push-file.sh'
        script.withCredentials([sshUserPrivateKey(credentialsId: 'git-ssh-user', keyFileVariable: 'SSH_KEY')]) {
            script.sh "./commit-and-push-file.sh ${bakingManifestFileName}"
        }
    }

    /**
     * The copy ami step with target roles to assume. This step first trys to read the manifest file generated by packer, if not present will read the manifest
     * file stored in the repo
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * <pre>
     * Usage:
     * copyAmi ('target-role-credential-id', closure)
     * </pre>
     */
    void copyAmi (String targetRoleCredentialId, Closure body={}) {
        copyAmi(targetRoleCredentialId, false, null, body)
    }
    
    /**
     * The copy ami step with target roles to assume. This step first trys to read the manifest file generated by packer, if not present will read the manifest
     * file stored in the repo
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param isCreate - Whether createAmi task to be invoked or not
     * @param body - the script body
     * <pre>
     * Usage:
     * copyAmi ('target-role-credential-id', isCreate, closure)
     * </pre>
     */
    void copyAmi (String targetRoleCredentialId, Boolean isCreate, Closure body={}) {
        copyAmi(targetRoleCredentialId, isCreate, null, body)
    }
    
    /**
     * The copy ami step with target roles to assume. This step first trys to read the manifest file generated by packer, if not present will read the manifest
     * file stored in the repo
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * <pre>
     * Usage:
     * copyAmi ('target-role-credential-id', releaseName, closure)
     * </pre>
     */
    void copyAmi (String targetRoleCredentialId, String releaseName, Closure body={}) {
        copyAmi(targetRoleCredentialId, false, releaseName, body)
    }

    /**
     * The terminate instances step with target roles to assume.
     * This step will delete the ami in the management or target account.
     *
     * @param body - the script body
     * <pre>
     * Usage:
     * terminateInstances(targetRoleCredentialId) {
     *  deleteOlderThanDays = 3
     *  nameTags = Comma separated name tag for ec2 instance to be terminated
     * }
     * </pre>
     */
    void terminateInstances (String targetRoleCredentialId, Closure body) {
        assert !StringUtils.isEmpty(targetRoleCredentialId) : 'targetRoleCredentialId param is required'
        
        def gradleOpts = JenkinsUtil.getGradleCmdOpts(body)
        
        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ec2/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        def buildTask = 'terminateInstances'
        
        gradleDeploy(targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "--stacktrace ${buildTask} ${gradleOpts}"
        }
    }
    /**
     * The stops asg Instances step with target roles to assume. 
     * If no parameters are specified it stops all asg instances 
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * stopAsgInstance ('target-role-credential-id') {
     *     asgGroupName = the Auto scaling name
     * }
     * </pre>
    */
    void stopAsgInstance (String targetRoleCredentialId, Closure body) {
        
        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        assert !StringUtils.isEmpty(targetRoleCredentialId) : 'targetRoleCredentialId param is required'

        StrBuilder cmdOptions = new StrBuilder()
        vars.each {
            cmdOptions.append("--${it.key}=${it.value} ")
        }

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/asg/deploy.gradle'
        script.writeFile file: 'asg/deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'asg/deploy.gradle'
            buildTasks = 'stopAsgInstance --stacktrace ' + cmdOptions.toString()
        }
    }

    /**
     * The starts asg Instances step with target roles to assume. 
     * If no parameters are specified it starts all asg instances 
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * startAsgInstance ('target-role-credential-id') {
     *     asgGroupName = the Auto scaling name
     * }
     * </pre>
    */
    void startAsgInstance (String targetRoleCredentialId, Closure body) {
        
        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        assert !StringUtils.isEmpty(targetRoleCredentialId) : 'targetRoleCredentialId param is required'

        StrBuilder cmdOptions = new StrBuilder()
        vars.each {
            cmdOptions.append("--${it.key}=${it.value} ")
        }

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/asg/deploy.gradle'
        script.writeFile file: 'asg/deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'asg/deploy.gradle'
            buildTasks = 'startAsgInstance --stacktrace ' + cmdOptions.toString()
        }
    }

    /**
     * The create DB cluster snapshot step with target roles to assume. 
     * If no parameters are specified it creates snapshots for all running clusters
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * createDBClusterSnapshot ('target-role-credential-id') {
     *     dbClusterIdentifier = the db cluster identifier (optional)
     *     nameTag = the db cluster name tag value (optional)
     * }
     * </pre>
     */
    void createDBClusterSnapshot (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()
        vars.each {
            cmdOptions.append("--${it.key}=${it.value} ")
        }

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/rds/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = 'createDBClusterSnapshot --stacktrace ' + cmdOptions.toString()
        }
    }

    /**
     * The delete DB cluster snapshot step with target roles to assume. 
     * If no parameters are specified it deletes all snapshots older than the specified days
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * createDBClusterSnapshot ('target-role-credential-id') {
     *     dbClusterIdentifier = the db cluster identifier (optional)
     *     nameTag = the db cluster name tag value (optional)
     *     snapshotIdentifier - the snapshot identifier (optional)
     *     deleteOlderThanDays - the number of days to keep snapshot/s (required)
     * }
     * </pre>
     */
    void deleteDBClusterSnapshot (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()
        vars.each {
            cmdOptions.append("--${it.key}=${it.value} ")
        }

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/rds/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = 'deleteDBClusterSnapshot --stacktrace ' + cmdOptions.toString()
        }
    }

    /**
     * The stops rds DB Instance step with target roles to assume. 
     * If no parameters are specified it stops all running instances
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * stopRDSInstance ('target-role-credential-id') {
     *     dbIdentifier = the db instance identifier (optional)
     *     nameTag = the db instance name tag value (optional)
     * }
     * </pre>
    */
    void stopRDSInstance (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()
        def stopTask
        vars.each {
            if(it.key != 'buildTask') {
              cmdOptions.append("--${it.key}=${it.value} ")
            } else {
              stopTask = it.value
            }
        }

        assert !StringUtils.isEmpty(stopTask) : 'buildTask param is required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/rds/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "${stopTask} --stacktrace " + cmdOptions.toString()
        }
    }

    /**
     * The starts rds DB Instance step with target roles to assume. 
     * If no parameters are specified it starts all running instances
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * startRDSInstance ('target-role-credential-id') {
     *     dbIdentifier = the db instance identifier (optional)
     *     nameTag = the db instance name tag value (optional)
     * }
     * </pre>
    */
    void startRDSInstance (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()
        def startTask
        vars.each {
            if(it.key != 'buildTask') {
              cmdOptions.append("--${it.key}=${it.value} ")
            } else {
              startTask = it.value
            }
        }

        assert !StringUtils.isEmpty(startTask) : 'buildTask param is required'
        
        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/rds/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "${startTask} --stacktrace " + cmdOptions.toString()
        }
    }
    /**
     * The create DB instance snapshot step with target roles to assume. 
     * If no parameters are specified it creates snapshots for all running instances
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * createRDSInstanceSnapshot ('target-role-credential-id') {
     *     dbIdentifier = the db instance identifier (optional)
     *     nameTag = the db instance name tag value (optional)
     * }
     * </pre>
     */
    void createRDSInstanceSnapshot (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()
        vars.each {
            cmdOptions.append("--${it.key}=${it.value} ")
        }

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/rds/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = 'createDBSnapshot --stacktrace ' + cmdOptions.toString()
        }
    }

    /**
     * The delete DB instance snapshot step with target roles to assume. 
     * If no parameters are specified it deletes all snapshots older than the specified days
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * deleteRDSInstanceSnapshot ('target-role-credential-id') {
     *     dbIdentifier = the db instance identifier (optional)
     *     nameTag = the db instance name tag value (optional)
     *     snapshotIdentifier - the snapshot identifier (optional)
     *     deleteOlderThanDays - the number of days to keep snapshot/s (required)
     * }
     * </pre>
     */
    void deleteRDSInstanceSnapshot (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()
        vars.each {
            cmdOptions.append("--${it.key}=${it.value} ")
        }

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/rds/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = 'deleteDBSnapshot --stacktrace ' + cmdOptions.toString()
        }
    }

     /**
     * Performs S3 bucket tasks.
     * uploadFile, deleteFile, deleteAllFiles, syncContents(Uploading the folder), deleteBucket 
     * 
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * s3Bucket ('target-role-credential-id') {
     *     fileName = file to be uploaded
     *     bucketName = S3 bucket name
     *     key = Path with file name in the S3 Bucket.
     *     buildTasks = uploadFile
     * }
     * s3Bucket ('target-role-credential-id') {
     *     bucketName = S3 bucket name
     *     key = Path with file name in the S3 Bucket.
     *     buildTasks = deleteFile
     * }
     * s3Bucket ('target-role-credential-id') {
     *     bucketName = S3 bucket name
     *     prefix = Folder appended with '/' under which all files to be deleted e.g. "upload/".
     *     buildTasks = deleteAllFiles
     * }
     * s3Bucket ('target-role-credential-id') {
     *     folder = Folder to be uploaded
     *     bucketName = S3 bucket name
     *     prefix = Files under folder to be uploaded e.g. "upload/".
     *     buildTasks = syncContents
     * }
     * </pre>
     */
    void s3Bucket (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()

        def s3Task
        vars.each {
          if(it.key != 'buildTasks') {
            cmdOptions.append("-P${it.key}=${it.value} ")
          } else {
            s3Task = it.value
          }
        }

        assert !StringUtils.isEmpty(s3Task) : 'buildTasks param is required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/s3/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId) {
            buildFile = 'deploy.gradle'
            buildTasks = "${s3Task} --stacktrace " + cmdOptions.toString()
        }
    }
    /**
     * The Ec2 Instance step with target roles to assume. 
     * - buildTask parameter defines which task to be executed. 
     *    If nothing is provided it will throw an error
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     * 
     * <pre>
     * Usage:
     * ec2Instance ('target-role-credential-id') {
     *     buildTask = 'startInstance' or 'stopInstance'
     *     nameTag = the db instance name tag value
     * }
     * </pre>
    */
    void ec2Instance (String targetRoleCredentialId, Closure body) {

        def vars = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = vars
        body()

        StrBuilder cmdOptions = new StrBuilder()
        def bTask
        vars.each {
            if(it.key != 'buildTask') {
              cmdOptions.append("--${it.key}=${it.value} ")
            } else {
              bTask = it.value
            }
        }

        assert !StringUtils.isEmpty(bTask) : 'buildTask param is required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ec2/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "${bTask} --stacktrace " + cmdOptions.toString()
        }
    }

    /**
     * The SSM Send Command step with target roles to assume. 
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     *  <p>
     *    @documentName step parameter is the ssm document name    
     *    @targets step parameter is a map of tag key value pair
     *    @parameters step parameter is a map of string key value pair
     *    <u>
     *      <li>parameters.Action='Install' // Mandatory
     *      <li>parameters.AllowReboot='True' // Optional
     *     </u>
     *    @options is a map of string key value pair
     *  <p>
     * <pre>
     * Usage:
     * sendSsmCommand ('target-role-credential-id') {
     *  documentName = 'SSM Document Name' // Mandatory
     *  targets.<<Tag Key Name 1>> = '(Comma separated)Tag Key Value 1>' // Atleat 1 tag is mandatory
     *  targets.<<Tag Key Name 2>> = '(Comma separated)Tag Key Value 2'
     *  parameters.<<SSM Doc Parameter 1>> = '(Comma separated) SSM Doc Parameter value 1' // Mandatory based on the Document
     *  parameters.<<SSM Doc Parameter 2>> = '(Comma separated) SSM Doc Parameter value 2'
     *  options.timeout = '3600' // Optional. By default it is 3600
     *  options.logGroupName = logGroupName // Optional. Command output will be sent to log group name. It will create a new log group if not present
     *  options.maxConcurrency = "100%" // Optional. By default it is 100%
     *  options.s3BucketName = outputName // Optional. Command output will be sent to the S3 bucket
     *  options.s3KeyPrefix = 'patches' //Optional.
     *
     * }
     *
     * Examples:-
     *
     * sendSsmCommand (assumeRoleDetailsId) {
     *     documentName = 'AWS-RunRemoteScript'//'AWS-RunPowerShellScript'//'AWS-InstallWindowsUpdates'
     *     targets.environment = environment
     *     targets.application = application
     *     targets.role = role
     *     options.timeout = '4500' // Optional. By default it is 3600
     *     // options.logGroupName = logGroupName // Optional. Command output will be sent to log group name. It will create a new log group if not present
     *     options.maxConcurrency = "100%" // Optional. By default it is 100%
     *     //options.s3BucketName = outputName // Optional. Command output will be sent to the S3 bucket
     *     //options.s3KeyPrefix = 'patches' //Optional.
     *   
     *   //'AWS-RunRemoteScript'
     *     //parameters.executionTimeout='3600'
     *     //parameters.sourceType='S3'
     *     //parameters.sourceInfo='{"path":"https://s3.ca-central-1.amazonaws.com/dev-websinc-01-ssm-logs/patches/TestPS.ps1"}'
     *     //parameters.commandLine='TestPS.ps1 "test"'
     *   
     *   //'AWS-RunPowerShellScript'
     *     //parameters.commands='echo "testing"\ndir'
     *     //parameters.executionTimeout='3600'
     *     //parameters.workingDirectory='C:/'
     *   
     *   //'AWS-InstallWindowsUpdates'
     *     parameters.IncludeKbs=params.includeKbs
     *     parameters.ExcludeKbs=params.excludeKbs
     *     parameters.Action=params.action // Mandatory
     *     parameters.AllowReboot=params.allowReboot // Optional
     *  }
     *
     *</pre>
    */
    void sendSsmCommand (String targetRoleCredentialId, Closure body) {

        AwsSSMDocumentValidator delegate = new AwsSSMDocumentValidator()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        String cmdOptions = delegate.buildSSMCmdOptions()

        assert !StringUtils.isEmpty(cmdOptions) : 'Gradle command line options are required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ssm/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "sendSsmCommand --stacktrace " + cmdOptions.toString()
        }
    } 
    
    /**
     * The SSM store secrets step with target roles to assume.
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     *  <p>
     *    @secretFileName step parameter is the branch name
     *  <p>
     * <pre>
     * Usage:
     * storeSsmSecrets ('target-role-credential-id') {
     *  secretFileName = environment name to go after yaml file.
     *
     * }
     *
     * Examples:-
     *
     * storeSsmSecrets (assumeRoleDetailsId) {
     *     secretFileName = environment name to go after yaml file. (Optional)
     *  }
     *
     *</pre>
    */
    void storeSsmSecrets (String targetRoleCredentialId, Closure body = {}) {

        AwsSSMDocumentValidator delegate = new AwsSSMDocumentValidator()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        String envName = delegate.secretFileName

        if(StringUtils.isEmpty(envName)) {
            envName = script.env.BRANCH_NAME
        }
        String projectName = JenkinsUtil.getProjectName(script)
        script.sh "echo Project name: ${projectName}"
        String key = JenkinsUtil.getBasicSSHUserPrivateKey( projectName + '-puppet-private-key')
        script.writeFile file: 'secrets/private_key.pkcs7.pem', text: key

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ssm/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "ssmStoreSecrets --info --stacktrace --environment " + envName
        }
    }
    
    /**
     * The ECS fargate run task step with target roles to assume.
     * Step function to execute the fargate task on a ECS cluster
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * fargateRunTask ('target-role-credential-id') {
     *     clusterId = The short name or full Amazon Resource Name (ARN) of the cluster on which to run your task
     *     count = No. of tasks to launch in a cluster. Optional. Default is "1"
     *     taskDefinition = The family and revision (family:revision) or full ARN of the task definition to run. If a revision is not specified, the latest ACTIVE revision is used
     *     subnets = The subnets associated with the task or service.
     *     securityGroups = The security groups associated with the task or service
     * }
     * </pre>
    */
    void fargateRunTask (String targetRoleCredentialId, Closure body) {
        
        AwsECSDelegate delegate = new AwsECSDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        String cmdOptions = delegate.buildRunTaskCmdOpts()

        assert !StringUtils.isEmpty(cmdOptions) : 'Gradle command line options are required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ecs/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "runFargateTasks --info --stacktrace " + cmdOptions.toString()
        }
    }

    /**
     * The DMS replication run task step with target roles to assume.
     * Step function to start the dms replication task
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * startReplicationTask ('target-role-credential-id') {
     *     taskArn = DMS task arn ssm parameter path
     *     startTaskType = DMS type of replication task - Possible values are:  start-replication, resume-processing, reload-target 
     * }
     * </pre>
    */
    void startReplicationTask (String targetRoleCredentialId, Closure body) {
        
        AwsDMSDelegate delegate = new AwsDMSDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        String cmdOptions = delegate.buildRunTaskCmdOpts()

        assert !StringUtils.isEmpty(cmdOptions) : 'Gradle command line options are required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/dms/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "startReplicationTask --info --stacktrace " + cmdOptions.toString()
        }
    }
    
    /**
     * The DMS replication stop task step with target roles to assume.
     * Step function to stop the dms replication task
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * stopReplicationTask ('target-role-credential-id') {
     *     taskArn = DMS task arn ssm parameter path
     * }
     * </pre>
    */
    void stopReplicationTask (String targetRoleCredentialId, Closure body) {
        
        AwsDMSDelegate delegate = new AwsDMSDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        String cmdOptions = delegate.buildStopTaskCmdOpts()

        assert !StringUtils.isEmpty(cmdOptions) : 'Gradle command line options are required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/dms/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "stopReplicationTask --info --stacktrace " + cmdOptions.toString()
        }
    }
    
    /**
     * The Cloudfront create invalidate task step with target roles to assume.
     * Step function to invalidate the cloudfront url's
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * createInvalidate ('target-role-credential-id') {
     *     distributionIdKey = The cloudfront distribution web id
     *     items = List of url's to be invalidated
     * }
     * </pre>
    */
    void createInvalidate (String targetRoleCredentialId, Closure body) {
        
        AwsCloudfrontDelegate delegate = new AwsCloudfrontDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        String cmdOptions = delegate.buildCmdOptions()

        assert !StringUtils.isEmpty(cmdOptions) : 'Gradle command line options are required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/cloudfront/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "createInvalidation --info --stacktrace " + cmdOptions.toString()
        }
    }
    
    /**
     * The ECS update service task step with target roles to assume.
     * Step function to udpate the ECS service on a cluster
     *
     * @param targetRoleCredentialId - the target role credential id
     * @param body - the script body
     *
     * <pre>
     * Usage:
     * updateECSService ('target-role-credential-id') {
     *     clusterId = The short name or full Amazon Resource Name (ARN) of the cluster on which to run your task
     *     desiredCount = No. of tasks to launch in a service
     * }
     * </pre>
    */
    void updateECSService (String targetRoleCredentialId, Closure body) {
        
        AwsECSDelegate delegate = new AwsECSDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        String cmdOptions = delegate.buildUpdateServiceCmdOpts()

        assert !StringUtils.isEmpty(cmdOptions) : 'Gradle command line options are required'

        //copy the deploy.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/aws/ecs/deploy.gradle'
        script.writeFile file: 'deploy.gradle', text: gradleFile

        gradleDeploy (targetRoleCredentialId, true) {
            buildFile = 'deploy.gradle'
            buildTasks = "updateECSService --info --stacktrace " + cmdOptions.toString()
        }
    }
}
