import com.telushealth.thcp.pipeline.project.bootstrap.ProjectBootstrapPipeline
import com.telushealth.thcp.pipeline.notify.Notification
import com.telushealth.thcp.pipeline.config.PipelineConfig
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

/**
 * The pipeline to manage project bootstrap pipelines
 * 
 * @param label - the node label
 * @param body - the script body
 */
def call(String label, Closure body) {
  call(label, new PipelineConfig(), body)
}

/**
 * The pipeline to manage bootstrap pipelines
 * 
 * @param label - the node label
 * @param config - the Pipeline configurations
 * @param body - the script body
 */
def call(String label, PipelineConfig config, Closure body) {
    
    try {
    
      ProjectBootstrapPipeline pipeline = new ProjectBootstrapPipeline(this)
      pipeline.call(label, body)

    
    } catch (Exception ex) {
    
      throw ex
    }       
}
