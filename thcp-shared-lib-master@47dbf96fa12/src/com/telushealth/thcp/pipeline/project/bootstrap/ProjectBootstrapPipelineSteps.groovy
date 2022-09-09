package com.telushealth.thcp.pipeline.project.bootstrap

import java.lang.reflect.*

import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import com.telushealth.thcp.pipeline.base.BaseStep
import com.telushealth.thcp.pipeline.common.THCPConstants
import com.telushealth.thcp.pipeline.common.util.AWSUtil
import com.telushealth.thcp.pipeline.common.util.JenkinsUtil
import com.telushealth.thcp.pipeline.project.bootstrap.delegate.ProjectBootstrapDelegate

import hudson.*
import hudson.security.*
import jenkins.model.*

/**
 * Copyright (c) 2017 - TELUS
 *    All rights reserved
 *
 * The release project bootstrap steps implementation class
 */
class ProjectBootstrapPipelineSteps extends BaseStep {

    private Script script
    
    ProjectBootstrapPipelineSteps(Script script) {

        this.script = script
    }

    private def getBootstrapConfig() {
      
        String yamlTxt = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/project/bootstrap/bootstrap.yaml'
        
        def bootstrapConfig = script.readYaml text: yamlTxt
        
        bootstrapConfig.envName = JenkinsUtil.getJenkinsEnv(this.script)
        
        bootstrapConfig.serverName = JenkinsUtil.getJenkinsServer(this.script)
       
        return bootstrapConfig 
    }
    /**
     * The checkoutProject step
     * This checks out the project branch for local updates for the provided scmUrl
     *
     * @param scmUrl - the scm url to clone
     * @param branch - the branch to checkout
     *
     * <pre>
     *  Usage:
     *  checkoutProject('https://someurl.com/project.git', 'master')
     * </pre>
     */
    void checkoutProject (String scmUrl, String branch) {
        script.checkout(
            [
                $class: 'GitSCM',
                branches: [[name: "*/${branch}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                    [$class: 'CleanCheckout'],
                    [$class: 'LocalBranch', localBranch: '**']
                ],
                submoduleCfg: [],
                userRemoteConfigs: [
                    [credentialsId: 'bitbucket-user', url: scmUrl ]
                ]
            ]
        )
    }

    /**
     * The bootstrapAssumeRole step
     * This runs the project-bootstrap-gradle-plugin task
     *
     * @param projectBootstrapTask - the gradle task to run. See project-bootstrap-gradle-plugin for valid tasks
     *
     * <pre>
     *  Usage:
     *  bootstrapAssumeRole {
     *       key = projectKey
     *       name = 'THCP TEST 1'
     *       accounts.nonp = '034196411046'
     *       accounts.prod = ''
     *       externalIds.nonp = '6e367c44-5d4d-11e9-8647-d663bd873d93'
     *       externalIds.prod = ''
     *  }
     * </pre>
     */
    void bootstrapAssumeRole (Closure body) {
      
        ProjectBootstrapDelegate delegate = new ProjectBootstrapDelegate(script, getBootstrapConfig())
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()
        
        def params = delegate.getBitBucketTaskParams()
        
        def bitBucketCmd = " setBitBucketProject ${params} --info --stacktrace"
        
        params = delegate.getAssumeRoleCfnTaskParams()
        
        def assumeRoleCmd = " runAssumeRoleCfn ${params} --info --stacktrace"
        
        params = delegate.getThcpSeebJobsParams()
        
        def thcpSeedJobsCmd = " setThcpSeedJobDsl ${params} --info --stacktrace"
        
        //copy the build.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/project/bootstrap/build.gradle'
        script.writeFile file: 'build.gradle', text: gradleFile

        script.withCredentials(
            [
                usernamePassword(
                        credentialsId: 'bitbucket-user',
                        passwordVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_PASSWORD',
                        usernameVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_USERNAME'
                )
            ]
        ) {
          if(!delegate.isProjectExists) {
              script.echo "${bitBucketCmd}"
              gradleBuild () {
                buildFile = 'build.gradle'
                buildTasks = "${bitBucketCmd}"
              }
          }
          
          script.echo "${assumeRoleCmd}"
          gradleBuild () {
            buildFile = 'build.gradle'
            buildTasks = "${assumeRoleCmd}"
          }
          script.echo "${thcpSeedJobsCmd}"
          gradleBuild () {
            buildFile = 'build.gradle'
            buildTasks = "${thcpSeedJobsCmd}"
          }
          //stash the file for crossAccountRole step
          script.stash name: THCPConstants.ASSUME_ROLE_STASH_NAME, includes: THCPConstants.ASSUME_ROLE_FILE_NAME
        }
    }
    
    /**
     * The bootstrapCrossAccountRole step
     * This runs the project-bootstrap-gradle-plugin task
     *
     * @param projectBootstrapTask - the gradle task to run. See project-bootstrap-gradle-plugin for valid tasks
     *
     * <pre>
     *  Usage:
     *  bootstrapCrossAccountRole(credentials-id, targetAccountName) {
     *       key = projectKey
     *  }
     * </pre>
     */
    void bootstrapCrossAccountRole (String credentialsId, String targetAccountName , Closure body) {

        List<String> envCredentials = new ArrayList<String>()
        
        ProjectBootstrapDelegate delegate = new ProjectBootstrapDelegate(script, getBootstrapConfig())
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()
        
        script.echo "CredentsId: " + credentialsId
        
        if(!credentialsId.equals(THCPConstants.ASSUME_INSTANCE_PROFILE)) {
            envCredentials.addAll(AWSUtil.getAssumedRoleCredentials(credentialsId, script, AWSUtil.NO_PREFIX))
        }
        
        try {
            script.unstash name: THCPConstants.ASSUME_ROLE_STASH_NAME
            script.echo "Using ${THCPConstants.ASSUME_ROLE_FILE_NAME} from assume role task to create cross account role"
        }
        catch (hudson.AbortException ex) {
            script.echo "Assume role json not found in stash: ${ex.message}"
            throw ex
        }
      
        def params = delegate.getCrossAccountRoleCfnTaskParams()
        
        //copy the build.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/project/bootstrap/build.gradle'
        script.writeFile file: 'build.gradle', text: gradleFile
  
        def assumeRoleJson = script.readJSON file: THCPConstants.ASSUME_ROLE_FILE_NAME
        
        script.echo "READ JSON ${assumeRoleJson}"
        
        if(envCredentials.empty) {
            throw new IllegalArgumentException("Environment credential id is missing for account ${targetAccountName}")
        }
        
        def crossAccountCmd = " runCrossAccountRoleCfn ${params} --targetAccoutName '${targetAccountName}' --info --stacktrace"
        
        script.withCredentials(
            [
                usernamePassword(
                        credentialsId: 'bitbucket-user',
                        passwordVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_PASSWORD',
                        usernameVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_USERNAME'
                )
            ]
        ) {
            script.withEnv(envCredentials) {
                script.echo "${crossAccountCmd}"
                gradleBuild () {
                    buildFile = 'build.gradle'
                    buildTasks = " ${crossAccountCmd}"
                }
            }
        }
    }
    
    /**
     * The bootstrapKmsKey step
     * This runs the project-bootstrap-gradle-plugin task
     *
     * @param projectBootstrapTask - the gradle task to run. See project-bootstrap-gradle-plugin for valid tasks
     *
     * <pre>
     *  Usage:
     *  bootstrapKmsKey {
     *       key = projectKey
     *       jenkinsEnv = 'stage'
     *       jenkinsServer = 'cd'
     *       amiBakingKeyId = 'a7f8c7dc-020f-4a1f-9b90-ac49754bd0e5'
     *       dryRun = isDryRun
     *  }
     * </pre>
     */
    void bootstrapKmsKey (Closure body) {

        List<String> envCredentials = new ArrayList<String>()
        
        ProjectBootstrapDelegate delegate = new ProjectBootstrapDelegate(script, getBootstrapConfig())
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = delegate
        body()
        
        try {
            script.unstash name: THCPConstants.ASSUME_ROLE_STASH_NAME
            script.echo "Using ${THCPConstants.ASSUME_ROLE_FILE_NAME} from assume role task to create cross account role"
        }
        catch (hudson.AbortException ex) {
            script.echo "Assume role json not found in stash: ${ex.message}"
            throw ex
        }
      
        def params = delegate.getCrossAccountRoleCfnTaskParams()
        
        //copy the build.gradle file
        String gradleFile = script.libraryResource 'com/telushealth/thcp/pipeline/gradle/project/bootstrap/build.gradle'
        script.writeFile file: 'build.gradle', text: gradleFile
  
            
        def kmsKeyCmd = " runKmsKeyCfn ${params} --info --stacktrace"
        
        script.withCredentials(
            [
                usernamePassword(
                        credentialsId: 'bitbucket-user',
                        passwordVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_PASSWORD',
                        usernameVariable: 'ORG_GRADLE_PROJECT_BITBUCKET_USERNAME'
                )
            ]
        ) {
            script.withEnv(envCredentials) {
                script.echo "${kmsKeyCmd}"
                gradleBuild () {
                    buildFile = 'build.gradle'
                    buildTasks = " ${kmsKeyCmd}"
                }
            }
        }
    }
    
    /**
     * @param projectKey - Project key to give jenkins permissions
     * This method adds and assigns jenkins roles to the project key
     */
    void addProjectRoles(String projectKey) {
        
        def bootstrapConfig = getBootstrapConfig()
        String jenkinsEnv = bootstrapConfig.envName
        String jenkinsServer = bootstrapConfig.serverName
        
        /**
         * ==========================================
         * roles / permissions / groups-users
         * ==========================================
         */
        Set<Permission> developerPermissions = [
                                                    Permission.fromId("hudson.model.Hudson.Read"),
                                                    Permission.fromId("hudson.model.Item.Cancel"),
                                                    Permission.fromId("hudson.model.Item.Build"),
                                                    Permission.fromId("hudson.model.Item.Discover")
                                               ]
        Role developerRole = new Role('developer', THCPConstants.GLOBAL_ROLE_PATTERN, developerPermissions);

        def access = [
            role: [
                [
                    name:"${projectKey}-project",
                    roleType: RoleBasedAuthorizationStrategy.PROJECT,
                    pattern: "^${projectKey.toUpperCase()}.*",
                    users:["ag-jenkins-${jenkinsEnv}-${jenkinsServer}-${projectKey}-developer"],
                    permissions:["hudson.model.Item.Read"
                    ]]
            ]
        ]

        def authStrategy = Jenkins.instance.getAuthorizationStrategy()

        if(authStrategy instanceof RoleBasedAuthorizationStrategy){
            RoleBasedAuthorizationStrategy roleAuthStrategy = (RoleBasedAuthorizationStrategy) authStrategy

            // Make constructors available
            Constructor[] constrs = Role.class.getConstructors();
            for (Constructor<?> c : constrs) {
                c.setAccessible(true);
            }

            access.role.each { r ->
                // Set this role
                Set<Permission> thisPermissionSet = new HashSet<Permission>();
                script.echo "\nGet ${r.name} role definition"

                // Get permissions for this role
                r.permissions.each { p ->
                    def permission = Permission.fromId(p);
                    if (permission != null) {
                        script.echo "Adding ${p} permission to ${r.name} role"
                        thisPermissionSet.add(permission);
                    } else {
                        script.echo "${p} is not a valid permission ID (ignoring)"
                    }
                }
                // Permissions -> Role
                Role thisRole = new Role(r.name, r.pattern, thisPermissionSet);
                roleAuthStrategy.addRole(RoleType.fromString(r.roleType), thisRole);

                //  Role -> Groups/Users
                r.users.each { l ->
                    script.echo "Granting ${l} to Developer global role"
                    roleAuthStrategy.assignRole(RoleType.fromString(RoleBasedAuthorizationStrategy.GLOBAL), developerRole, l);

                    script.echo "Granting ${r.name} to ${l}"
                    roleAuthStrategy.assignRole(RoleType.fromString(r.roleType), thisRole, l);

                }
            }
            Jenkins.instance.save()
        } else {
            throw new IllegalArgumentException("Role based strategy plugin is not installed")
        }
    }
}