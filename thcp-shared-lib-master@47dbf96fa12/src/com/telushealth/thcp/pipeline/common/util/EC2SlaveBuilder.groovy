package com.telushealth.thcp.pipeline.common.util

import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import jenkins.model.Jenkins
import hudson.model.Node
import hudson.plugins.ec2.AMITypeData
import hudson.plugins.ec2.EC2Cloud
//import hudson.plugins.ec2.EC2Tag
import hudson.plugins.ec2.SlaveTemplate
import hudson.plugins.ec2.SpotConfiguration
import hudson.plugins.ec2.UnixData
import hudson.plugins.ec2.WindowsData
import com.amazonaws.services.ec2.model.InstanceType
import hudson.plugins.ec2.HostKeyVerificationStrategyEnum

@Builder(builderStrategy = SimpleStrategy, prefix = '')
public class EC2SlaveBuilder {

    String ami = ''
    String zone = ''
    SpotConfiguration spotConfig
    String securityGroups = ''
    String remoteFS = ''
    String type = ''
    Boolean ebsOptimized = false
    String labelString = ''
    String mode = 'NORMAL'
    String description = ''
    String initScript = ''
    String tmpDir = ''
    String userData = ''
    String numExecutors = '2'
    String remoteAdmin = 'ec2-user'
    AMITypeData amiType = new UnixData(null, null, null, '22', null)
    String osType = 'linux'
    String jvmopts = ''
    Boolean stopOnTerminate = true
    String subnetId = ''
    List<EC2Tag> tags
    String idleTerminationMinutes = '1'
    Boolean usePrivateDnsName = false
    String instanceCapStr = '1'
    String iamInstanceProfile = ''
    Boolean deleteRootOnTermination = true
    Boolean useEphemeralDevices = false
    Boolean useDedicatedTenancy = false
    String launchTimeoutStr = '2147483647'
    Boolean associatePublicIp = false
    String customDeviceMapping = ''
    Boolean connectBySSHProcess = false
    Boolean connectUsingPublicIp = false
    Script script

    SlaveTemplate build () {
        List<hudson.plugins.ec2.EC2Tag> ec2Tags = new ArrayList<hudson.plugins.ec2.EC2Tag>()
        for(EC2Tag tag : tags) {
            ec2Tags.add(new hudson.plugins.ec2.EC2Tag(tag.name, tag.value))
        }
        if("windows".equalsIgnoreCase(osType)) {
            amiType= new WindowsData('', false, '60')
            userData = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/project/bootstrap/userdata/windows.userdata'
        }
        SlaveTemplate slaveTemplate = new SlaveTemplate(
            ami,
            zone,
            spotConfig,
            securityGroups,
            remoteFS,
            InstanceType.valueOf(type),
            ebsOptimized,
            labelString,
            Node.Mode.valueOf(mode),
            description,
            initScript,
            tmpDir,
            userData,
            numExecutors,
            remoteAdmin,
            amiType,
            jvmopts,
            stopOnTerminate,
            subnetId,
            ec2Tags,
            idleTerminationMinutes,
            usePrivateDnsName,
            instanceCapStr,
            iamInstanceProfile,
            deleteRootOnTermination,
            useEphemeralDevices,
            useDedicatedTenancy,
            launchTimeoutStr,
            associatePublicIp,
            customDeviceMapping,
            connectBySSHProcess,
            connectUsingPublicIp
        )
        slaveTemplate.setHostKeyVerificationStrategy(HostKeyVerificationStrategyEnum.OFF)
        return slaveTemplate
    }
}
