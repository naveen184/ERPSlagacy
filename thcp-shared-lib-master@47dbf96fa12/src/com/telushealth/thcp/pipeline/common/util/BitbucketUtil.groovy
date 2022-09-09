package com.telushealth.thcp.pipeline.common.util

@Grapes([
    @GrabResolver(name='jcenter-gradle-releases', root='https://artifactory.tools.thcoe.ca/artifactory/gradle-jcenter-remote/'),
    @Grab(group = 'com.google.code.findbugs', module = 'jsr305', version = '3.0.0'),
    @GrabResolver(name='thcp-gradle-releases', root='https://artifactory.tools.thcoe.ca/artifactory/gradle-releases-local/'),
    @Grab(group = 'com.telushealth.thcp.tool.stash', module = 'bitbucket-client', version = '0.1.2')
])

import java.util.logging.Logger

import com.atlassian.stash.rest.client.api.entity.Repository
import com.atlassian.stash.rest.client.httpclient.HttpClientConfig
import com.cloudbees.groovy.cps.NonCPS
import com.telushealth.thcp.stash.rest.client.ClientStashClientFactoryImpl
import com.telushealth.thcp.stash.rest.client.HttpClientStashClientFactory
import com.telushealth.thcp.stash.rest.client.StashClient

class BitbucketUtil {

    private static final Logger log = Logger.getLogger(BitbucketUtil.class.name)

    private static final BITBUCKET_NONP_URL = "https://bitbucket.tools.telushealth.com"

    private static final BITBUCKET_PROD_URL = "https://bitbucket.tools.telushealth.com"

    static String getBitbucketUrl(Script script) {
        def jenkinsEnv = JenkinsUtil.getJenkinsEnv(script)
        return "prod".equalsIgnoreCase(jenkinsEnv) ? BITBUCKET_PROD_URL : BITBUCKET_NONP_URL
    }

    static StashClient getStashClient(Script script) {
        try {
            
            String bitbucketUrl = getBitbucketUrl(script)
            
            log.info("Bitbucket Url: ${bitbucketUrl}")
            
            script.withCredentials([
                script.usernamePassword(credentialsId: 'bitbucket-user', passwordVariable: 'bitbucketPassword', usernameVariable: 'bitbucketUsername')
            ]) {
                
                HttpClientStashClientFactory factory = new ClientStashClientFactoryImpl();
                HttpClientConfig config = new HttpClientConfig(new URL(bitbucketUrl), script.env.bitbucketUsername, script.env.bitbucketPassword);
                return factory.getStashClient(config);
    
            }
                        
        } catch(MalformedURLException ex) {
            throw new InterruptedException("Bitbucket url is invalid", ex)
        }
    }
    
    static List getProjectRepos(String projectKey, Script script) {
        log.info("Project Key:${projectKey}")
        StashClient client = getStashClient(script)
        def page = client.getProjectRepositories(projectKey, 0, 500)
        List<Repository> repos = page.getValues()
        List repoList = []
        for(Repository repo : repos) {
            def repoMap = [:]
            repoMap.put('name', repo.getName())
            repoMap.put('httpUrl', repo.getHttpCloneUrl())
            repoMap.put('sshUrl', repo.getSshCloneUrl())
            repoList.add(repoMap)
        }
        return repoList
    }
}
