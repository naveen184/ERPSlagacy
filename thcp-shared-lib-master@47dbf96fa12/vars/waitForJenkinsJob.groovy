import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

boolean call(String jobFullName) {
    boolean jobRunning = true
    echo "waiting for job: ${jobFullName} to finish"
    while(true) {
        sleep 10
        jobRunning = JenkinsUtil.isJenkinsJobRunning(jobFullName)
        if(!jobRunning) { break }
    }
    echo "job: ${jobFullName} is completed"
    return jobRunning
}