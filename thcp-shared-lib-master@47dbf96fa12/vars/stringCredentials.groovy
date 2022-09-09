import com.telushealth.thcp.pipeline.common.util.JenkinsUtil

import org.apache.commons.lang3.StringUtils

import hudson.util.Secret
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.Credentials
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain



boolean call(Closure body) {
  Boolean exists = false
  Map credsMap = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = credsMap
  body()
  
  StringCredentialsImpl c = new StringCredentialsImpl(CredentialsScope.GLOBAL,
      credsMap.id,
      credsMap.description,
      Secret.decrypt(credsMap.secret)
   )
  SystemCredentialsProvider.getInstance().getStore().getCredentials(Domain.global()).each {
      if (it.id == c.id) {
          SystemCredentialsProvider.getInstance().getStore().updateCredentials(Domain.global(), it, c)
          exists = true
          return true
      }
  }
  
  if (!exists) {
      SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
  }
    
}