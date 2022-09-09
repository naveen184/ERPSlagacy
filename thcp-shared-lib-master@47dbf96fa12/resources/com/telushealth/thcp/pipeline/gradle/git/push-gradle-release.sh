#!/bin/bash
BRANCH_NAME=$1
git config user.name "Pipeline User"
git config user.email "thcp-prod-pipeline@telus.com"
git push --tags