param($FileToCommit)
$BranchName=$env:BRANCH_NAME
$SshKey=$env:SSH_KEY
echo "Commiting file $FileToCommit to branch $BranchName"
cp $SshKey ./ssh-key.pem
git config user.name "Pipeline User"
git config user.email "thcp-prod-pipeline@telus.com"
git add $FileToCommit
git commit -m "New manifest file"
$env:GIT_SSH_COMMAND="ssh -o 'StrictHostKeyChecking no' -i ./ssh-key.pem"
git pull --rebase origin $BranchName
git push origin $BranchName
rm ./ssh-key.pem