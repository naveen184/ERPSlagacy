package com.telushealth.thcp.pipeline.common.util
import com.cloudbees.groovy.cps.NonCPS

import groovy.text.GStringTemplateEngine

class TemplateUtil implements Serializable {

    static String JOB_PATH_PREFIX = 'com/telushealth/thcp/pipeline/gradle/release/template'
    
    static String PROJECT_FOLDER_JOB_GROOVY_FILE = "setup/%sfolder.groovy"

    static String CREATE_SETUP_JOB_GROOVY_FILE = '%s_%s.groovy'
    
    static String JOBDSL_FOLDER_TEMPLATE = "${JOB_PATH_PREFIX}/jobdsl/Folder.tpl"

    static String JOBDSL_PIPELINE_TEMPLATE = "${JOB_PATH_PREFIX}/jobdsl/PipelineJob.tpl"
    
    static String RELEASE_PIPELINE_JOB_TEMPLATE = "${JOB_PATH_PREFIX}/pipeline/ReleasePipelineJob.tpl"
    
    static String PARENT_RELEASE_PIPELINE_JOB_TEMPLATE = "${JOB_PATH_PREFIX}/pipeline/ParentReleasePipelineJob.tpl"

    static String CREATE_SETUP_JOB_JENKINS_FILE = 'setup/%s_%s.Jenkinsfile'
    
    static String UTF_CHARSET = "UTF-8"

    static GStringTemplateEngine engine = new GStringTemplateEngine()

    @NonCPS
    static void generateTemplate(String templateFile, String fileName, Map<String, Object> bindings, Script script)
    throws IOException, ClassNotFoundException {

        String groovyTemplate = script.libraryResource templateFile
        def template = engine.createTemplate(groovyTemplate).make(bindings)
        String content = template.toString()
        script.writeFile file: fileName, text: content
    }

    static void generateProductCIReleasesFolder(String productFolder, String projectKey, Script script)
            throws IOException, ClassNotFoundException {

        Map<String, Object> bindings = new HashMap<>()
        bindings.put("productFolder", productFolder.toUpperCase())
        bindings.put("projectFolder", projectKey)
        generateTemplate(JOBDSL_FOLDER_TEMPLATE, String.format(PROJECT_FOLDER_JOB_GROOVY_FILE, projectKey), bindings, script)
    }

    static void generateCIReleasePipelineJob(String productFolder, String projectKey,
            String buildType, def repo, String branchName, Script script) throws IOException, ClassNotFoundException {

        Map<String, Object> bindings
        String repoName = repo.name.replaceAll('-','_')
        String jenkinsFileName = String.format(CREATE_SETUP_JOB_JENKINS_FILE, projectKey, repoName)

        bindings = new HashMap<>()
        bindings.put("credsId", repo.sshCredsId)
        bindings.put("scmUrl", repo.sshUrl)
        bindings.put("repoName", repo.name)
        bindings.put("releaseType", buildType)
        bindings.put("branchName", branchName)
        generateTemplate(RELEASE_PIPELINE_JOB_TEMPLATE, jenkinsFileName, bindings, script)

        //create job dsl job file
        bindings = new HashMap<>()
        bindings.put("jobPath", String.format("%s/releases/%s/%s", productFolder, projectKey, repo.name))
        bindings.put("jobFile", jenkinsFileName)
        generateTemplate(JOBDSL_PIPELINE_TEMPLATE, String.format(CREATE_SETUP_JOB_GROOVY_FILE, projectKey, repoName), bindings, script)
    }
    
    static void generateCIParentReleasePipelineJob(String productFolder, String projectKey,
        String buildType, def repos, String branchName, Script script) throws IOException, ClassNotFoundException {

        Map<String, Object> bindings
        def name = buildType + "_" + projectKey.toLowerCase() + "_job"
        String jenkinsFileName = String.format(CREATE_SETUP_JOB_JENKINS_FILE, projectKey, name)
    
        bindings = new HashMap<>()
        bindings.put("repos", repos)
        bindings.put("productFolder", productFolder)
        bindings.put("projectKey", projectKey)
        bindings.put("branchName", branchName)
        generateTemplate(PARENT_RELEASE_PIPELINE_JOB_TEMPLATE, jenkinsFileName, bindings, script)
    
        //create job dsl job file
        bindings = new HashMap<>()
        bindings.put("jobPath", String.format("%s/releases/%s/%s", productFolder, projectKey, name))
        bindings.put("jobFile", jenkinsFileName)
        generateTemplate(JOBDSL_PIPELINE_TEMPLATE, String.format(CREATE_SETUP_JOB_GROOVY_FILE, projectKey,projectKey), bindings, script)
    }
}
