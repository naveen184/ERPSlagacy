package com.telushealth.thcp.pipeline.common.util

import java.util.logging.Logger
import com.cloudbees.hudson.plugins.folder.Folder
import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.telushealth.thcp.pipeline.common.THCPConstants
import org.apache.commons.lang3.StringUtils
import org.jenkinsci.plugins.plaincredentials.StringCredentials
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles
import org.jenkinsci.lib.configprovider.model.Config
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import hudson.model.Cause.UserIdCause
import hudson.model.Cause
import java.io.StringWriter;
import java.io.PrintWriter;
import jenkins.model.Jenkins;

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *     
 * Helper class for Jenkins related functions
 */
class JenkinsUtil implements Serializable {

    private static final Logger log = Logger.getLogger(JenkinsUtil.class.name)
    
    static Properties getStringCredentialProperties (String credentialStoreId) {

        Properties props = null
        log.info('Looking up credentialStoreId:' + credentialStoreId)

        StringCredentials creds = 
            CredentialsMatchers.firstOrNull (
                CredentialsProvider.lookupCredentials(
                StringCredentials.class,
                Jenkins.instance,
                null,
                null
            ),
            CredentialsMatchers.withId(credentialStoreId )
        )

        if (creds != null) {
            log.info('Parsing credential store content')
            props = new Properties();
            String plainText = creds.getSecret().getPlainText()
            String[] lines = StringUtils.split(plainText)
            lines.each {
                String[] keyValue = StringUtils.split(it, '=')
                props.put(keyValue[0], keyValue[1])
            }
            log.info('Credential store property size:' + props.size())
        }

        assert props != null : 'Role credential store not found, please setup and try again: '  + credentialStoreId
        assert !StringUtils.isEmpty(props['role-arn']) : 'Role ARN is missing in the credential store'
        assert !StringUtils.isEmpty(props['external-id']) : 'External id is missing in the credential store'

        return props
    }

    static String getBasicSSHUserPrivateKey (String credentialStoreId) {

        String sshKey = null
        log.info('Looking up credentialStoreId:' + credentialStoreId)

        BasicSSHUserPrivateKey  creds = 
            CredentialsMatchers.firstOrNull (
                CredentialsProvider.lookupCredentials(
                BasicSSHUserPrivateKey.class,
                Jenkins.instance,
                null,
                null
            ),
            CredentialsMatchers.withId(credentialStoreId )
        )

        if (creds != null) {
            log.info('Getting private key from credential store')
            sshKey = creds.getPrivateKey()
        }

        assert sshKey != null : 'Private key not found, please setup and try again: '  + credentialStoreId

        return sshKey
    }

    static Properties getAssumeRoleProperties (String credentialStoreId) {

        Properties props = null
        credentialStoreId = credentialStoreId.trim()
        log.info('Looking up assume role config:' + credentialStoreId)

        Config config = GlobalConfigFiles.get().getById(credentialStoreId)
        assert config != null : 'Cross account config store [' + credentialStoreId + '] not found, please setup and try again'

        log.info('Parsing cross account content')
        props = new Properties();
        String plainText = config.content
        String[] lines = StringUtils.split(plainText, "\r?\n")
        
        lines.each {
            String[] keyValue = StringUtils.split(StringUtils.trimToEmpty(it), '=')
            if (keyValue.size() == 2) {
                props.put(keyValue[0], keyValue[1])
            }
        }

        log.info('Cross account property size:' + props.size())

        assert !StringUtils.isEmpty(props['role-arn']) : 'role-arn property is missing in the config store'
        assert !StringUtils.isEmpty(props['external-id']) : 'external-id is missing in the config store'

        return props
    }

    static boolean isSlaveAllowed(Script script, String slaveLabel) {

        boolean isAllowed = false
        Folder projectFolder = getCurrentBuildProjectFolderName(script)

        if (projectFolder != null) {
            //remove os labels and whitespaces
            String[] searchStrings = ['ecs', 'linux', 'window', ' ']
            String[] replaceStrings = ['','','','']
            String[] labels =  StringUtils.split(StringUtils.replaceEach(slaveLabel, searchStrings, replaceStrings), '&&')
            labels = StringUtils.stripAll(labels)
            Config config = GlobalConfigFiles.get().getById(StringUtils.lowerCase(projectFolder.name) + '-' + THCPConstants.PROJECT_SLAVE_WHITELIST_CONFIG_NAME)
            log.info config

            if (config != null) {
                String allowedSlaves = config.content
                if (!StringUtils.isEmpty(allowedSlaves)) {
                    allowedSlaves += " deploy"
                    String[] allowedLabels = StringUtils.split(allowedSlaves)
                    for(allowedLbl in allowedLabels) {
                        def labelPattern = new WildcardPattern(allowedLbl, false)
                        for(lbl in labels) {
                            if(labelPattern.matches(lbl)) {
                                isAllowed = true
                                break
                            }
                        }
                        if(isAllowed) {
                            break
                        }
                    }
                }
            }
            else {
                script.error 'No slave config file defined for this project. Please create ' + THCPConstants.PROJECT_SLAVE_WHITELIST_CONFIG_NAME + ' config to whitelist slaves'
            }
        }
        else {
            script.error 'Unauthorized project, no slaves allowed to run on this project'
        }

        return isAllowed
    }

    static Folder getCurrentBuildProjectFolderName (Script script) {

        boolean isFound = false
        String projectFolderName = null
        def projectBuild = script.currentBuild.rawBuild.project

        while(!isFound && projectBuild != null) {
            if (projectBuild instanceof com.cloudbees.hudson.plugins.folder.Folder) {
               isFound = true
            }
            else if (projectBuild instanceof  hudson.model.Hudson) {
                projectBuild = null
            }
            else {
                projectBuild = projectBuild.parent
            }
        }

        return projectBuild
    }
    
    static String getSlackProjectChannel(Script script, String branchName, String projectName=null) {
        if(StringUtils.isEmpty(projectName)) {
            projectName = getProjectName(script)
        }
        def channelName = null
        if (!StringUtils.isEmpty(projectName)) {
            Config config = GlobalConfigFiles.get().getById(projectName + '-' + THCPConstants.PROJECT_NOTIFICATION_CONFIG_NAME)
            if (config != null) {
                Object notificationResult = JsonUtil.parseJsonText(config.content)
                if(notificationResult != null) {
                    Object slackResult = notificationResult.slack
                    if(slackResult != null) {
                        slackResult.channels.each { chName, details ->
                            if(channelName == null) {
                                details.branches.each {
                                    def branchPattern = new WildcardPattern(it, false)
                                    if(branchPattern.matches(branchName)) {
                                        channelName = chName
                                        return
                                    }
                                }
                                if(channelName != null) {
                                    return
                                }
                            }
                        }
                    }
                }
            }
        }
        return channelName
    }

    static String getProjectName(Script script) {
      Folder projectFolder = getCurrentBuildProjectFolderName(script)

      if (projectFolder != null) {
        return StringUtils.lowerCase(projectFolder.name)
      }

      return null
    }

    static String getBuildUser(Script script) {
      if(script.currentBuild.rawBuild == null || 
        script.currentBuild.rawBuild.getCause(Cause.UserIdCause) == null) {
          return "Jenkins"
        }
      return script.currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
    }

    static void createJobParameters (Script script, List jobParamsList, String defaultBranch, String numToKeepStr, String durabilityHint) {

        List jobParams = []
        boolean missingParams = false
        boolean isDefaultBranch = (script.env.BRANCH_NAME == defaultBranch)
        String paramStr = "Job Parameters:\n"

        script.echo 'Creating job parameters from list'

        //map the param definitions to jenkins parameters4
        jobParamsList.each {

            if (it.type == 'choice') {
                jobParams.add(script."${it.type}"(name: it.name, description: it.description, choices: it.choices, defaultValue: (isDefaultBranch? it.defaultValue : '')))
            }
            else if (it.type == 'booleanParam') {
                jobParams.add(script."${it.type}"(name: it.name, description: it.description, defaultValue: it.defaultValue))
            }
            else {
                jobParams.add(script."${it.type}"(name: it.name, description: it.description, defaultValue: (isDefaultBranch? it.defaultValue : '')))
            }
        }

        if (!jobParamsList.isEmpty()) {
            // add the default parameters to all jobs if the job is using parameters
            jobParams.add(script.booleanParam(name: 'refreshJob', defaultValue: false, description: 'Refresh the job parameters'))
        }

        //create the job parameters
        script.properties(
            [
                script.buildDiscarder(
                    script.logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: numToKeepStr)
                ), 
                script.durabilityHint(durabilityHint),
                script.parameters(jobParams)
            ]
        )

        //validate the job parameters are set
        script.params.each { k,v ->
            if (v == null || v == '') {
                missingParams = true
                paramStr = paramStr + "${k} = Missing required value\n"
            }
            else {
                paramStr = paramStr + "${k} = ${v}\n"
            }
            
        }

        //display the current job parameters
        script.echo "${paramStr.toString()}"

        //report an error if it's not a refresh action
        if (!script.params.isEmpty() && !script.params.refreshJob && missingParams) {
            script.error 'Missing required job parameter/s'
        }
    }

    static String getGradleCmdOpts(Closure body) {
      Map vars = [:]
      body.resolveStrategy = Closure.DELEGATE_FIRST
      body.delegate = vars
      body()

      StringBuilder builder = new StringBuilder()
      vars.each {
          builder.
            append(" --").
            append(it.key).
            append("=").
            append(it.value)
      }
      return builder.toString()
    }

    static String getExceptionAsString(Throwable ex, Script script) {
      if (ex instanceof InterruptedException) {
        return "```Job aborted!!```";
      }
      StringWriter sw = new StringWriter()
      ex.printStackTrace(new PrintWriter(sw))
      def excAsStr = sw.toString()
      if(script.currentBuild.rawBuild != null) {
        def consoleLogs = script.currentBuild.rawBuild.getLog(Integer.MAX_VALUE)
        if (consoleLogs != null && !consoleLogs.empty) {       
          int failIdx = consoleLogs.indexOf('FAILURE: Build failed with an exception.')
          if(failIdx >= 0) {
            consoleLogs = consoleLogs.subList(failIdx, consoleLogs.size)
            excAsStr = consoleLogs.join('\n')
          }
        }
      }
      if(!StringUtils.isEmpty(excAsStr) && excAsStr.length() >= 1000) {
        excAsStr = excAsStr.substring(0, 1000)
      }
      return "```" + excAsStr + "```";
    }
    
    static String getJenkinsHostname(Script script) {
        def buildUrl = script.env.BUILD_URL
        def hostName = buildUrl.split('/')[2].split(':')[0]
        return hostName
    }
    
    static String getJenkinsServer(Script script) {
        def server = 'cd'
        if(getJenkinsHostname(script).contains('-ci-')) {
            server = 'ci'
        }
        return server
    }
    
    static String getJenkinsEnv(Script script) {
        def buildUrl = script.env.BUILD_URL
        def env = 'prod'
        if(getJenkinsHostname(script).contains('staging')) {
            env = 'stage'
        }
        return env
    }
 
    static Boolean isJenkinsJobRunning(String jobFullName) {
        def upStreamBuild = Jenkins.getInstance().getItemByFullName(jobFullName)
        return upStreamBuild.lastBuild.building
    } 
}
