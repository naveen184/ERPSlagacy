package com.telushealth.thcp.pipeline.common.util

import com.telushealth.thcp.pipeline.common.THCPConstants

/**
 * Helper class for AWS functions
 */
class AWSUtil {

    static final String AWS_ACCESS_KEY_ID = 'AWS_ACCESS_KEY_ID='
    static final String AWS_SECRET_ACCESS_KEY = 'AWS_SECRET_ACCESS_KEY='
    static final String AWS_SESSION_TOKEN = 'AWS_SESSION_TOKEN='
    static final String AWS_ROLE_ARN = 'ROLE_ARN='
    static final String AWS_EXTERNAL_ID = 'EXTERNAL_ID='
    static final String NO_PREFIX = ''
    static final String SOURCE_PREFIX = 'SOURCE_'
    static final String TARGET_PREFIX = 'TARGET_'

    static List<String> getAssumedRoleCredentials(String roleCredentialStoreId, Script script, String varPrefix = NO_PREFIX) {
        
        //Properties props = JenkinsUtil.getStringCredentialProperties(roleCredentialStoreId)
        Properties props = JenkinsUtil.getAssumeRoleProperties(roleCredentialStoreId)
        String output = null
        if(script.isUnix()) {
            output = script.sh(returnStdout: true, script: String.format(THCPConstants.ASSUME_ROLE_CMD, props['role-arn'], props['external-id']))
        } else {
            output = script.powershell(returnStdout: true, script: String.format(THCPConstants.ASSUME_ROLE_CMD, props['role-arn'], props['external-id']))
        }
        Object assumedRoleResult = JsonUtil.parseJsonText(output)
        return AWSUtil.getEnvCredentials(assumedRoleResult, varPrefix)
    }
    
    private static List<String> getEnvCredentials (Object assumedRoleResult, String varPrefix) {

        List<String> list = new ArrayList<String>()

        list.add(varPrefix + AWS_ACCESS_KEY_ID + assumedRoleResult.Credentials.AccessKeyId)
        list.add(varPrefix + AWS_SECRET_ACCESS_KEY + assumedRoleResult.Credentials.SecretAccessKey)
        list.add(varPrefix + AWS_SESSION_TOKEN + assumedRoleResult.Credentials.SessionToken)
        return list
    }

     static List<String> getStSRoleDetails(String roleCredentialStoreId, Script script, String varPrefix = NO_PREFIX) {

        List<String> list = new ArrayList<String>()
        Properties props = JenkinsUtil.getAssumeRoleProperties(roleCredentialStoreId)
        String output = null
        if(script.isUnix()) {
            output = script.sh(returnStdout: true, script: String.format(THCPConstants.ASSUME_ROLE_CMD, props['role-arn'], props['external-id']))
        } else {
            output = script.powershell(returnStdout: true, script: String.format(THCPConstants.ASSUME_ROLE_CMD, props['role-arn'], props['external-id']))
        }
        Object assumedRoleResult = JsonUtil.parseJsonText(output)
        list.add(varPrefix + AWS_ROLE_ARN + props['role-arn'])
        list.add(varPrefix + AWS_EXTERNAL_ID + props['external-id'])
        return list
    }
}
