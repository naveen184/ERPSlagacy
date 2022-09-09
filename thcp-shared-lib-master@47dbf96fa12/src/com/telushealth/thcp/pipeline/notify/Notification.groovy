package com.telushealth.thcp.pipeline.notify

import org.jenkinsci.plugins.configfiles.GlobalConfigFiles
import org.jenkinsci.lib.configprovider.model.Config
import com.telushealth.thcp.pipeline.config.PipelineConfig
import com.telushealth.thcp.pipeline.common.util.BuildUtil
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil
import com.telushealth.thcp.pipeline.common.THCPConstants
import org.apache.commons.lang3.StringUtils
/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The class for the sending notifications
 */
class Notification implements Serializable {

  private Script script

  private PipelineConfig config

  Notification(Script script, PipelineConfig config) {
    this.script = script
    this.config = config
  }

  void sendToChannel(String step , String color, String ex) {
    if(config.slackEnabled) { 
      def branchName = script.env.BRANCH_NAME
      def channel = JenkinsUtil.getSlackProjectChannel(script, branchName, null)
      script.echo "channel: ${channel}"
      if(!StringUtils.isEmpty(channel)) {
        def buildName = script.env.JOB_NAME
        def buildNumber = script.env.BUILD_NUMBER
        def buildUrl = script.env.BUILD_URL
        def user = JenkinsUtil.getBuildUser(script)
        
        script.slackSend channel: "${channel}", failOnError: false, message: "Build#:${buildNumber} ${step} By:${user} Job:${buildName} (<${buildUrl}|Build Url>) ${ex}", color: "${color}"
      }
    }
  }

  /**
  * Jenkins to send alerts/messages to Slack Channel based on the configuration
  *
  * @param step = When to send the notification to slack channel
  * @param ex = Exception message during falure
  * @param color = color of the slack channel based on the step
  *
  **/
  void notifySlackChannel(String step, String color, String ex) {
    if(step == 'Started' && config.slackAtStart) {
      sendToChannel(step, color, ex)
      return
    }
    if(step == 'Completed' && config.slackAtSuccess) {
      sendToChannel(step, color, ex)
      return
    }
    if(step == 'Failed' && config.slackAtFailure) {
      sendToChannel(step, color, ex)
      return
    }
  }

  void sendMsg(String step, String color) {
    sendMsg(step, color, '')
  }

  void sendMsg(String step, String color , String ex) {
    notifySlackChannel(step, color, ex)
  }

}