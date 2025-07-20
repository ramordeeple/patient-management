#!/bin/bash
set -e

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

ENDPOINT=http://localhost:4566
BUCKET=localstack-deployments

# 1. Create bucket (ignore errors if it exists)
aws --endpoint-url=$ENDPOINT s3 mb s3://$BUCKET || true

# 2. Upload template to S3
aws --endpoint-url=$ENDPOINT s3 cp ./cdk.out/localstack.template.json s3://$BUCKET/template.json

# 3. Deploy via create-stack
aws --endpoint-url=$ENDPOINT cloudformation create-stack \
  --stack-name patient-management \
  --template-url http://localstack:4566/$BUCKET/template.json \
  --capabilities CAPABILITY_NAMED_IAM
