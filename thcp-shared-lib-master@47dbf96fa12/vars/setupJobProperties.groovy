import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

import org.apache.commons.lang3.StringUtils

/**
  * Pipeline function to create and validate job parameters
  * 
  *  @param body - the script body
  *
  * <pre>
  *  Usage:
  *  setupJobParams {
  *    defaultBranch = 'dev' - Required The branch which allowed to use default values for testing
  *    numToKeepStr = '15' - Optional The num of builds to keep, defaults to 15
  *    durabilityHint = 'MAX_SURVIVABILITY' - optional The build durability, default to MAX_SURVIVABILITY
  *    parameters = [
  *      [ type: 'string', name: 'agentLabel', defaultValue: 'taget agent label', description: 'Short description' ],
  *      [ type: 'string', name: 'assumeRoleDetailsId', defaultValue: 'targe assume role id', description: 'Short description' ],
  *      [ type: 'choice', name: 'task', choices: 'Create\nDelete',   description: 'Create or Delete the stack' ],
  *    ]
  *  }
  * </pre>
  */
boolean call(Closure body) {

    boolean isRefresh = false
    Map jobProperties = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = jobProperties
    body()

    echo 'Calling deploy pipeline with parameter list'

    jobProperties.numToKeepStr = StringUtils.defaultIfEmpty(jobProperties.numToKeepStr, '15')
    jobProperties.durabilityHint = StringUtils.defaultIfEmpty(jobProperties.durabilityHint, 'MAX_SURVIVABILITY')

    assert jobProperties.parameters != null : 'parameters is required!'
    assert !StringUtils.isEmpty(jobProperties.defaultBranch) : 'defaultBranch is required!'

    if (!jobProperties.parameters.isEmpty()) {
        JenkinsUtil.createJobParameters(this, jobProperties.parameters, jobProperties.defaultBranch, jobProperties.numToKeepStr, jobProperties.durabilityHint)
    }
    else {
        echo 'No parameters defined for this job'
    }

    //force a refresh if adding new parameters for the first time or user forces a refresh
    if ((params.isEmpty() && !jobProperties.parameters.isEmpty()) || params.refreshJob) {
        isRefresh = true
    }

    return isRefresh
}