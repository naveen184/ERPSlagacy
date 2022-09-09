releasePipeline('master') {
	<% repos.eachWithIndex { repo, index -> 
	if(index == 0 || index%2 == 0) { %>
	parallel (
	<%}%>
        ('$repo.name job'): {
	        stage('Running $repo.name job') {
		        build job: '$productFolder/releases/$projectKey/$repo.name', propagate: true
	        }
	    },
    <% if(index%2 != 0 || index == (repos.size()-1)) { %>)<%}}%>
}
