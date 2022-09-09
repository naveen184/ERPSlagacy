package com.telushealth.thcp.pipeline.deploy.delegate


import org.apache.commons.text.StrBuilder
import org.apache.commons.lang3.StringUtils

/**
 * Copyright (c) 2019 - TELUS
 *    All rights reserved
 *
 * The delegation class for the aws ssm document step
 */
class AwsSSMDocumentValidator implements Serializable {

    private static final SPACE = ' '

    private static final SINGLE_QUOTE = '\''

    private static final EQUALS = '='

    private static final MINUS = '-'

    private String targetCmd = '--targets'

    private String paramCmd = '--parameters'

    private String docCmd = '--documentName'

    String documentName
    
    String secretFileName
    
    Map parameters = [:]

    Map options = [:] // timeout, s3BucketName, s3KeyPrefix, logGroupName, maxConcurrency

    Map targets = [:]

    /**
     * Helper method to get the ssm targets
     * 
     * @return the cmd line to execute
     */
    private String getSSMTargets () {
        assert !targets.isEmpty() : 'SSM targets param is required'

        StrBuilder builder = new StrBuilder()
        
        targets.each {
            builder.
                append(targetCmd).
                append(EQUALS).
                append(SINGLE_QUOTE).
                append('tag:').
                append(it.key).
                append(EQUALS).
                append(it.value).
                append(SINGLE_QUOTE).
                append(SPACE)
        }
        
        return builder.toString()
    }

     /**
     * Helper method to get the ssm parameters
     * 
     * @return the cmd line to execute
     */
    private String getOptsParams () {

        StrBuilder builder = new StrBuilder()
        
        parameters.each {
            builder.
                append(paramCmd).
                append(SPACE).
                append(it.key).
                append(EQUALS).
                append(SINGLE_QUOTE).
                append(it.value).
                append(SINGLE_QUOTE).
                append(SPACE)
        }

        options.each {
            builder.
                append('--').
                append(it.key).
                append(SPACE).
                append(it.value).
                append(SPACE)
        }

        return builder.toString()
    }

    /**
     * Helper method to build the sendSsmCommand options
     * 
     * @return the cmd line to execute
     */
    String buildSSMCmdOptions () {
      
      if(StringUtils.isEmpty(documentName)) {
        throw new MissingPropertyException('SSM documentName param is required')
      }

      def methodName = documentName.replaceAll("-", "")
      this."validate$methodName"()

      StrBuilder builder = new StrBuilder()

      builder.append(docCmd)
      builder.append(SPACE)
      builder.append(documentName)
      builder.append(SPACE)
      builder.append(getOptsParams())
      builder.append(SPACE)
      builder.append(getSSMTargets())
      
      return builder.toString()
    }

    /**
     * Helper method to validate the AWS-InstallWindowsUpdates document parameters
     * 
     * @return whether validation is success or not
     */

    boolean validateAWSInstallWindowsUpdates () {
      if(StringUtils.isEmpty(parameters.Action)) {
        throw new MissingPropertyException('Action parameter is mandatory for ' + documentName + ' document')
      }
      if(parameters.Action == 'Install') {
        if(!parameters.containsKey('IncludeKbs') && !parameters.containsKey('ExcludeKbs')) {
          throw new MissingPropertyException('One of the IncludeKbs or ExcludeKbs parameter is required for ' + documentName + ' document')
        } 
        if(StringUtils.isEmpty(parameters.IncludeKbs) && StringUtils.isEmpty(parameters.ExcludeKbs)) {
          throw new IllegalArgumentException('One of the IncludeKbs or ExcludeKbs value is required for ' + documentName + ' document')
        }
        if(StringUtils.equalsIgnoreCase(parameters.ExcludeKbs, 'no') && StringUtils.equalsIgnoreCase(parameters.IncludeKbs, 'no')) {
          throw new IllegalArgumentException('Invalid value for IncludeKbs or ExcludeKbs for ' + documentName + ' document')
        }
        if(StringUtils.equalsIgnoreCase(parameters.IncludeKbs, 'no')) {
          parameters.remove('IncludeKbs')
        } else if(StringUtils.equalsIgnoreCase(parameters.ExcludeKbs, 'no')) {
          parameters.remove('ExcludeKbs')
        }
      } else if(parameters.Action == 'Scan') {
        if(StringUtils.equalsIgnoreCase(parameters.IncludeKbs, 'no')) {
          parameters.remove('IncludeKbs')
        }
        if(StringUtils.equalsIgnoreCase(parameters.ExcludeKbs, 'no')) {
          parameters.remove('ExcludeKbs')
        }
      } else {
        throw new IllegalArgumentException('Only Scan/Install options is applicable for Action parameter')
      }
      return true
    }

    /**
     * Helper method to validate the AWS-RunRemoteScript document parameters
     *
     * @return whether validation is success or not
     */

    boolean validateAWSRunRemoteScript () {
      if(!parameters.containsKey('sourceType')) {
        throw new MissingPropertyException('sourceType parameter is required for ' + documentName + ' document')
      }
      if(parameters.sourceType != 'S3') {
        throw new IllegalArgumentException('Only S3 Source Type is supported for ' + documentName + ' document')
      }
      return true
    }

    /**
     * Helper method to validate the AWS-RunPowerShellScript document parameters
     * 
     * @return whether validation is success or not
     */

    boolean validateAWSRunPowerShellScript () {
      validateShellScript()
    }

    /**
     * Helper method to validate the AWS-RunShellScript document parameters
     *
     * @return whether validation is success or not
     */

    boolean validateAWSRunShellScript () {
      validateShellScript()
    }

    private boolean validateShellScript() {
      if(!parameters.containsKey('commands')) {
        throw new MissingPropertyException('commands parameter is required for ' + documentName + ' document')
      } 
      if(StringUtils.isEmpty(parameters.commands)) {
        throw new IllegalArgumentException('commands parameter value is required for ' + documentName + ' document')
      }
      return true
    }

    boolean methodMissing () {
      return true
    }
}
