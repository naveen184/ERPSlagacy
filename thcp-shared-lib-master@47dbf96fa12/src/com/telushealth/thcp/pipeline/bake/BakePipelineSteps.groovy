package com.telushealth.thcp.pipeline.bake

import com.telushealth.thcp.pipeline.bake.delegate.PackerStepDelegate
import com.telushealth.thcp.pipeline.base.BaseStep
import com.telushealth.thcp.pipeline.common.THCPConstants
import com.telushealth.thcp.pipeline.common.util.AWSUtil
import com.telushealth.thcp.pipeline.common.util.BuildUtil
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil
import com.telushealth.thcp.pipeline.common.util.JsonUtil
import org.apache.commons.lang3.StringUtils

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The bake pipeline steps implementation class
 */
class BakePipelineSteps extends BaseStep {

    private Script script

    BakePipelineSteps (Script script) {

        this.script = script
    }
    
    /**
     * The packer step
     * @param body - the closure script
     * 
     * <pre>
     *  Usage:
     *   packer {
     *       template = 'template/aws-ebs.json'
     *       options.debug = false // packer cmd line options
     *       vars.aws_region = 'ca-central-1' // template variables 
     *       vars.aws_vpc_id = 'vpc-xxxxx'
     *       vars.aws_subnet_id = 'subnet-xxxxx'
     *       vars.aws_iam_instance_profile = 'JenkinsSlaveDefaultInstanceRole' // the slave instance profile name
     *       vars.aws_security_group_id = 'sg-xxxxx'
     *       vars.aws_ssh_username = 'ec2-user'
     *       vars.aws_instance_type = 't2.nano'
     *       vars.aws_source_ami = 'thcp-base-linux-hardened-image-*' // the source ami name, this takes a wildcard to get the latest image
     *       vars.aws_target_ami = 'thcp-base-httpd' 
     *       vars.package_type = 'rpm'
     *       vars.packages =  'git, httpd' // yum packages to install
     *   }
     * </pre>
     */
    PackerStepDelegate packer (Closure body) {
        
        script.echo 'Calling packer step'
        
        PackerStepDelegate delegate = new PackerStepDelegate()
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()

        if (delegate.puppet_key_id != null) {
            String key = JenkinsUtil.getBasicSSHUserPrivateKey(delegate.puppet_key_id + '-private-key')
            script.writeFile file: 'files/private_key.pkcs7.pem', text: key

            key = JenkinsUtil.getBasicSSHUserPrivateKey(delegate.puppet_key_id + '-public-key')
            script.writeFile file: 'files/public_key.pkcs7.pem', text: key
        }

        String cmd = delegate.buildCmdOptions(BuildUtil.getPackerCmd(script))

        script.echo 'Running packer build cmd for template:' + delegate.template
        script.echo 'CMD: ' + cmd
        
        List<String> envCredentials = new ArrayList<String>()
        
        if(delegate.isRapid7ScanEnabled()) {
            script.withCredentials(
                [
                    sshUserPrivateKey(credentialsId: 'rapid7-ssh-private-key',keyFileVariable: 'AWS_SSH_PRIVATE_KEY_FILE',usernameVariable: 'AWS_SSH_KEYPAIR_NAME'),
                    string(credentialsId: 'rapid7-api-credentials', variable: 'RAPID7_API_CREDENTIALS'),
                    usernamePassword(credentialsId: 'rapid7-windows-user-pwd', passwordVariable: 'RAPID7_WINDOWS_PASSWORD', usernameVariable: 'RAPID7_WINDOWS_USER')
                ]
            ) {
                  if(script.isUnix()) {
                      script.sh cmd
                  } else {
                      script.powershell cmd
                  }
            }
        } else {
              if(script.isUnix()) {
                  script.sh cmd
              } else {
                  script.powershell cmd
              }
        }
        
        def manifest = script.readJSON file: THCPConstants.PACKER_MANIFEST_FILE_NAME

        if (manifest.builds[0].builder_type == 'amazon-ebs') {

            def bakingManifestFileName = THCPConstants.BAKING_MANIFEST_FILE_NAME
            def packerManifestStashName = THCPConstants.PACKER_MANIFEST_STASH_NAME
            def releaseName = delegate.getReleaseName()
            if(!StringUtils.isEmpty(releaseName)) {
                bakingManifestFileName = releaseName + "-" + bakingManifestFileName
                packerManifestStashName = releaseName + "-" + packerManifestStashName
            }
            script.echo 'MANIFEST JSON FILE NAME: ' + bakingManifestFileName
            def bakingManifest = script.readJSON text: JsonUtil.generateSourceAmiManifest(manifest)
            script.writeJSON file: bakingManifestFileName, json: bakingManifest

            //stash the file for copyAmi step
            script.stash name: packerManifestStashName, includes: bakingManifestFileName
        }

        return delegate
    }

    /**
     * The packer step with source role to assume
     * 
     * @param sourceRoleCredentialId - the source role credential id
     * @param body - the script body
     * 
     * <pre>
     *  Usage:
     *   packer ('source-role-credential-id') {
     *       template = 'template/aws-ebs.json'
     *       options.debug = false // packer cmd line options
     *       vars.aws_region = 'ca-central-1' // template variables 
     *       vars.aws_vpc_id = 'vpc-xxxxx'
     *       vars.aws_subnet_id = 'subnet-xxxxx'
     *       vars.aws_iam_instance_profile = 'JenkinsSlaveDefaultInstanceRole' // the slave instance profile name
     *       vars.aws_security_group_id = 'sg-xxxxx'
     *       vars.aws_ssh_username = 'ec2-user'
     *       vars.aws_instance_type = 't2.nano'
     *       vars.aws_source_ami = 'thcp-base-linux-hardened-image-*' // the source ami name, this takes a wildcard to get the latest image
     *       vars.aws_target_ami = 'thcp-base-httpd' 
     *       vars.package_type = 'rpm'
     *       vars.packages =  'git, httpd' // yum packages to install
     *   }
     * </pre>
     */
    void packer (String sourceRoleCredentialId, Closure body) {

        List<String> envCredentials = new ArrayList<String>()

        assert !StringUtils.isEmpty(sourceRoleCredentialId) : 'sourceRoleCredentialId param is required'

        //assume role if not set to instance profile
        if (!StringUtils.equalsIgnoreCase(sourceRoleCredentialId, THCPConstants.ASSUME_INSTANCE_PROFILE)) {
        
            envCredentials.addAll(AWSUtil.getAssumedRoleCredentials(sourceRoleCredentialId, script, AWSUtil.NO_PREFIX))

            script.withEnv(envCredentials) {
                packer(body)
            }
        }
        else {
            //use the instance profile instead
            packer(body)
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
     *  Usage:
     *   packer ('source-role-credential-id', 'target-role-credential-id') {
     *       template = 'template/aws-ebs.json'
     *       options.debug = false // packer cmd line options
     *       vars.aws_region = 'ca-central-1' // template variables 
     *       vars.aws_vpc_id = 'vpc-xxxxx'
     *       vars.aws_subnet_id = 'subnet-xxxxx'
     *       vars.aws_iam_instance_profile = 'JenkinsSlaveDefaultInstanceRole' // the slave instance profile name
     *       vars.aws_security_group_id = 'sg-xxxxx'
     *       vars.aws_ssh_username = 'ec2-user'
     *       vars.aws_instance_type = 't2.nano'
     *       vars.aws_source_ami = 'thcp-base-linux-hardened-image- *' // the source ami name, this takes a wildcard to get the latest image
     *       vars.aws_target_ami = 'thcp-base-httpd' 
     *       vars.package_type = 'rpm'
     *       vars.packages =  'git, httpd' // yum packages to install
     *   }
     * </pre>
     */
    void packer (String sourceRoleCredentialId, String targetRoleCredentialId, Closure body) {

        PackerStepDelegate delegate
        List<String> envCredentials = new ArrayList<String>()
        def packerManifest = THCPConstants.BAKING_MANIFEST_FILE_NAME
        def branchName = script.env.BRANCH_NAME

        assert !StringUtils.isEmpty(sourceRoleCredentialId) : 'sourceRoleCredentialId param is required'
        assert !StringUtils.isEmpty(targetRoleCredentialId) : 'targetRoleCredentialId param is required'

        //assume role if not set to instance profile
        if (!StringUtils.equalsIgnoreCase(sourceRoleCredentialId, THCPConstants.ASSUME_INSTANCE_PROFILE)) {
            envCredentials.addAll(AWSUtil.getAssumedRoleCredentials(sourceRoleCredentialId, script, AWSUtil.SOURCE_PREFIX))
        }
        envCredentials.addAll(AWSUtil.getAssumedRoleCredentials(targetRoleCredentialId, script, AWSUtil.TARGET_PREFIX))

        script.withEnv(envCredentials) {
            delegate = packer(body)
        }

        //check if this is a cross account copy task, prefix the branch name to manifest file for non default branches
        if (!delegate.isDefaultBranch) {
            packerManifest = "${branchName}-${THCPConstants.BAKING_MANIFEST_FILE_NAME}"
        }
        def releaseName = delegate.getReleaseName()
        if(!StringUtils.isEmpty(releaseName)) {
            packerManifest = releaseName + "-" + packerManifest
        }
        script.echo 'DOCKER MANIFEST JSON FILE NAME: ' + packerManifest
        
        //generate the docker baking manifest.json file
        def bakingManifest = script.readJSON text: JsonUtil.generateDockerManifest(delegate.vars)
        script.writeJSON file: packerManifest, json: bakingManifest, pretty: 4

        //copy the shell script
        def fileExt = 'sh'
        if(!script.isUnix()) {
            fileExt = 'ps1'
        }
        String gitCommitFile = script.libraryResource "com/telushealth/thcp/pipeline/gradle/git/commit-and-push-file.${fileExt}"
        script.writeFile file: "commit-and-push-file.${fileExt}", text: gitCommitFile

        //commit the manifest file to repo
        script.withCredentials([sshUserPrivateKey(credentialsId: 'git-ssh-user', keyFileVariable: 'SSH_KEY')]) {
            if(script.isUnix()) {
                script.sh "chmod 755 ./commit-and-push-file.${fileExt}"
                script.sh "./commit-and-push-file.${fileExt} ${packerManifest}"
            } else {
                script.powershell "./commit-and-push-file.${fileExt} -FileToCommit ${packerManifest}"
            }
        }
    }
}
