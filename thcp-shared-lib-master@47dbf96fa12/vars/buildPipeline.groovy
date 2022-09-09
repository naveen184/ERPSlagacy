import com.telushealth.thcp.pipeline.build.BuildPipeline
import com.telushealth.thcp.pipeline.notify.Notification
import com.telushealth.thcp.pipeline.config.PipelineConfig
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

/**
 * The pipeline to build and publish artifacts
 * 
 * @param label - the node label
 * @param body - the script body
 */
def call(String label, Closure body) {
  call(label, new PipelineConfig(), body)
}

/**
 * The pipeline to build and publish artifacts
 * 
 * @param label - the node label
 * @param allowProperties - To allow the properties from the jenkins job
 * @param body - the script body
 */

def call(String label, allowProperties, Closure body) {
  call(label, new PipelineConfig(), allowProperties, body)
}

/**
 * The pipeline to build and publish artifacts
 * 
 * @param label - the node label
 * @param config - the Pipeline configurations
 * @param allowProperties - To allow the properties from the jenkins job
 * @param body - the script body
 */

def call(String label, PipelineConfig config, Boolean allowProperties=false, Closure body) {
    if(!allowProperties) {

        properties(
            [
                buildDiscarder(
                    logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
                ), 
                durabilityHint('PERFORMANCE_OPTIMIZED'),
                disableConcurrentBuilds()
            ]
        )
    }
    
    if(!label.contains("master") && !label.contains("ecs") && !label.contains("docker") && !label.contains("windows") && config.useEcsSlave) {
        label = "ecs && " + label
    }
    
    Notification notification = new Notification(this,config)
    try {
      notification.sendMsg('Started', 'good')
      BuildPipeline pipeline = new BuildPipeline(this)
      pipeline.call(label, body)

      notification.sendMsg('Completed', 'good')
    } catch (Exception ex) {
      notification.sendMsg('Failed', 'danger', JenkinsUtil.getExceptionAsString(ex, this))
      throw ex
    }       
}