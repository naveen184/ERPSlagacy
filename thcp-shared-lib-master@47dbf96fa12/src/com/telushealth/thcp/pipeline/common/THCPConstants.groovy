package com.telushealth.thcp.pipeline.common

 /**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The global static variables
 */
class THCPConstants {

    static final String ARTIFACTORY_SERVER_ID = 'thcp-artifactory'

    static final String GRADLE_TOOL_NAME = 'gradle-5.4'
    
    static final String MAVEN_TOOL_NAME = 'mvn-3.6.1'

    static final String PACKER_TOOL_NAME = 'packer-1.3.3'

    static final String PACKER_WINDOWS_TOOL_NAME = 'packer-windows-1.6.2'

    static final String NPM_TOOL_NAME = 'nodejs-12.9.1'

    static final String PACKER_MANIFEST_FILE_NAME = 'packer-manifest.json'

    static final String BAKING_MANIFEST_FILE_NAME = 'manifest.json'
    
    static final String ASSUME_ROLE_FILE_NAME = 'assumeRole.json'
    
    static final String ASSUME_ROLE_STASH_NAME = 'assume-role'

    static final String PACKER_MANIFEST_STASH_NAME = 'packer-manifest'

    static final String GRADLE_SNAPSHOT_LOCAL_REPO = 'gradle-snapshots-local'

    static final String GRADLE_RELEASE_LOCAL_REPO = 'gradle-releases-local'
    
    static final String MAVEN_SNAPSHOT_LOCAL_REPO = 'maven-snapshots-local'
    
    static final String MAVEN_RELEASE_LOCAL_REPO = 'maven-releases-local'

    static final String PUPPET_LOCAL_REPO = 'puppet-local'
    
    static final String PYTHON_LOCAL_REPO = 'python-local'

    static final String NUGET_LOCAL_REPO = 'nuget-local'

    static final String NUGET_RELEASE_REPO = 'nuget-releases-local'

    static final String NUGET_SNAPSHOT_REPO = 'nuget-snapshots-local'
    
    static final String RPM_RELEASE_REPO = 'rpm-releases-local'
    
    static final String RPM_SNAPSHOT_REPO = 'rpm-snapshots-local'

    static final String PROJECT_SLAVE_WHITELIST_CONFIG_NAME = 'whitelisted-slaves'

    static final String PROJECT_NOTIFICATION_CONFIG_NAME = 'notification-config'

    static final String ASSUME_INSTANCE_PROFILE = 'instance-profile'

    static final String SLACK_CHANNEL_KEY = 'channel'

    static final String ASSUME_ROLE_CMD = "set +x\n aws sts assume-role --role-arn %s --external-id %s --role-session-name JenkinsDeploySession"
    
    static final String PACKER_VAR_RELEASE_NAME = "release_name"
    
    static final String GLOBAL_ROLE_PATTERN = '.*'
}
