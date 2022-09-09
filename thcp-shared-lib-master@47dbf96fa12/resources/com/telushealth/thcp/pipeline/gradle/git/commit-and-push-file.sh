#!/bin/bash
FILE_NAME=$1
echo "Commiting file $FILE_NAME to branch $BRANCH_NAME --$SSH_KEY"
git config user.name "Pipeline User"
git config user.email "thcp-prod-pipeline@telus.com"
git add $FILE_NAME
git commit -m "New manifest file"
GIT_SSH_COMMAND="ssh -i $SSH_KEY" git pull --rebase origin $BRANCH_NAME 
GIT_SSH_COMMAND="ssh -i $SSH_KEY" git push origin $BRANCH_NAME