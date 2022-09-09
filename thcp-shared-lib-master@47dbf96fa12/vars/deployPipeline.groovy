import com.telushealth.thcp.pipeline.deploy.DeployPipeline
import com.telushealth.thcp.pipeline.notify.Notification
import com.telushealth.thcp.pipeline.config.PipelineConfig
import org.apache.commons.lang3.StringUtils
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

/**
 * The pipeline to deploy AWS CFN templates
 * 
 * @param label - the node label
 * @param body - the script body
 */
def call(String label, Closure body) {
  call(label, new PipelineConfig(), body)
}

/**
 * The pipeline to deploy AWS CFN templates
 * 
 * @param label - the node label
 * @param config - the Pipeline configurations
 * @param body - the script body
 */
def call(String label, PipelineConfig config, Closure body) {
    
    Notification notification = new Notification(this,config)
    try {
      notification.sendMsg('Started', 'good')

      DeployPipeline pipeline = new DeployPipeline(this)
      pipeline.call(label, body)

      notification.sendMsg('Completed', 'good')
    } catch (Exception ex) {
      notification.sendMsg('Failed', 'danger', JenkinsUtil.getExceptionAsString(ex, this))
      throw ex
    } 

}