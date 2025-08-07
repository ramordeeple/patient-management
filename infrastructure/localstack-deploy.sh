#!/bin/bash
set -e

#export AWS_ACCESS_KEY_ID=test
#export AWS_SECRET_ACCESS_KEY=test
#export AWS_DEFAULT_REGION=us-east-1

ENDPOINT=http://localhost:4566
BUCKET=localstack-deployments

# 1. Create bucket (ignore errors if it exists)
#aws --endpoint-url=$ENDPOINT s3 cp ./cdk.out/localstack.template.json s3://$BUCKET/template.json


aws --endpoint-url=$ENDPOINT cloudformation deploy \
  --stack-name patient-management \
  --template-file "./cdk.out/localstack.template.json" \
  --capabilities CAPABILITY_NAMED_IAM

aws --endpoint-url=$ENDPOINT elbv2 describe-load-balance \
    --query "LoadBalancers[0].DNSName" --output text