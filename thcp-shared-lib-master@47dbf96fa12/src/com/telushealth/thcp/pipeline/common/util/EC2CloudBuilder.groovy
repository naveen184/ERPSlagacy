package com.telushealth.thcp.pipeline.common.util

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import jenkins.model.Jenkins
import hudson.plugins.ec2.AmazonEC2Cloud
import hudson.plugins.ec2.EC2Cloud
import hudson.plugins.ec2.SlaveTemplate
import java.util.List

@Builder(builderStrategy = SimpleStrategy, prefix = '')
public class EC2CloudBuilder {

    String cloudName
    Boolean useInstanceProfileForCredentials = true
    String credentialsId = ''
    String region = 'ca-central-1'
    String privateKey = ''
    String sshKeysCredentialsId = ''
    String instanceCapStr = ''
    List<SlaveTemplate> templates = []
    String roleArn = ''
    String roleSessionName = ''

    void build () {
        def instance = Jenkins.getInstance()
        
        def cloud = instance.getCloud('ec2-' + cloudName)

        if (cloud != null) {
            instance.clouds.remove(cloud)
        }

        if(sshKeysCredentialsId != '') {
            cloud = new AmazonEC2Cloud(
                cloudName,
                useInstanceProfileForCredentials,
                credentialsId,
                region,
                privateKey,
                sshKeysCredentialsId,
                instanceCapStr,
                templates,
                roleArn,
                roleSessionName
            )
        } else { // for backward compatibility
            cloud = new AmazonEC2Cloud(
                cloudName,
                useInstanceProfileForCredentials,
                credentialsId,
                region,
                privateKey,
                instanceCapStr,
                templates,
                roleArn,
                roleSessionName
            )
        }

        instance.clouds.add(cloud)
        instance.save()
        instance.reload()
    }
}