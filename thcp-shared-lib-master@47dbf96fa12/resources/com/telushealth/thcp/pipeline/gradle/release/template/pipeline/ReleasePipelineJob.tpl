def scmUrl = '$scmUrl'
def scmCredsId = '$credsId'
def repoName = '$repoName'
def releaseType = '$releaseType'
def defaultBranch = <% if (branchName == null || branchName == '') { %> 'master' <% } else { %> '$branchName' <% } %>

properties([
    parameters([
        listGitBranches(
            branchFilter: '.*'
            , credentialsId: scmCredsId
            , defaultValue: defaultBranch
            , name: 'BRANCH'
            , quickFilterEnabled: true
            , remoteURL: scmUrl
            , selectedValue: 'DEFAULT'
            , sortMode: 'ASCENDING_SMART'
            , tagFilter: 'None'
            , type: 'PT_BRANCH'
        )
        , string(defaultValue: '', description: '', name: 'RELEASE_VERSION', trim: false)
    ]),
    durabilityHint('PERFORMANCE_OPTIMIZED'),
    disableConcurrentBuilds()
])

def branch = params.BRANCH
if (branch == null || branch == "") {
    error 'Please select Parameter "BRANCH" value'
}
def releaseVersion = params.RELEASE_VERSION

branch = branch.replace('refs/','').replace('heads/','')

println "BRANCH NAME:::" + branch

releasePipeline('ecs&&linux&&gradle') {
    stage ('Clean WS') {
       cleanWs()
    }

    stage ('Checkout') {
        checkoutProject(scmUrl, branch)
    }

    stage('CI Release') {
        artifactCIRelease( releaseType, repoName, branch, releaseVersion )
    }
}