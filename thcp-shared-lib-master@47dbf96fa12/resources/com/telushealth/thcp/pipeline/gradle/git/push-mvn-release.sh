#!/bin/bash
FILE_NAME='pom.xml'
BRANCH_NAME=$1
echo "Pushing file $FILE_NAME to branch ${BRANCH_NAME}"
git config user.name "Pipeline User"
git config user.email "thcp-prod-pipeline@telus.com"
git add $FILE_NAME
git push origin $BRANCH_NAME
git push --tags