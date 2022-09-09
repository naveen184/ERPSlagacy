import com.telushealth.thcp.pipeline.release.ReleasePipeline
import com.telushealth.thcp.pipeline.notify.Notification
import com.telushealth.thcp.pipeline.config.PipelineConfig
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

/**
 * The pipeline to manage release pipelines
 * 
 * @param label - the node label
 * @param body - the script body
 */
def call(String label, Closure body) {
  call(label, new PipelineConfig(), body)
}

/**
 * The pipeline to manage release pipelines
 * 
 * @param label - the node label
 * @param config - the Pipeline configurations
 * @param body - the script body
 */
def call(String label, PipelineConfig config, Closure body) {
    Notification notification = new Notification(this,config)
    try {
      notification.sendMsg('Started', 'good')
      ReleasePipeline pipeline = new ReleasePipeline(this)
      pipeline.call(label, body)

      notification.sendMsg('Completed', 'good')
    } catch (Exception ex) {
      notification.sendMsg('Failed', 'danger', JenkinsUtil.getExceptionAsString(ex, this))
      throw ex
    }       
}
