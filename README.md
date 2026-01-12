# AWS Serverless IAM in Action

> A minimal but production-grade Serverless API demonstrating IAM-first design using AWS Lambda and API Gateway.

## 🎯 Overview

A practical example of IAM-first serverless design. The business logic is deliberately minimal—this project focuses on the security architecture, proper role configuration, and operational patterns that matter in production systems.

## 🏗️ Architecture

```
Client → API Gateway (REST API) → Lambda Function → CloudWatch Logs
```

No databases, no external dependencies. Pure focus on IAM and infrastructure.

## 📋 API Functionality

### POST /hello

**Request:**
```json
{
  "name": "Prasad"
}
```

**Response:**
```json
{
  "message": "Hello Prasad"
}
```

## 🔐 IAM Design (Critical Section)

### 1. Lambda Execution Role

**Trust Relationship:**
- Principal: `lambda.amazonaws.com`
- Allows Lambda service to assume the role

**Permissions:**
- `logs:CreateLogGroup`
- `logs:CreateLogStream`
- `logs:PutLogEvents`
- Scoped to: `/aws/lambda/*`

**Why this matters:**
- Least privilege principle in action
- Only CloudWatch Logs permissions, nothing else
- Resource-scoped to Lambda log groups only

### 2. API Gateway to Lambda Permission

**Resource-Based Policy:**
- Principal: `apigateway.amazonaws.com`
- Action: `lambda:InvokeFunction`
- Restricted by SourceArn: API Gateway REST API ID + stage + method

**Why this matters:**
- Prevents unauthorized API Gateway instances from invoking the Lambda
- Specific to POST /hello endpoint only
- Defense-in-depth security model

### 3. Separation of Concerns

- **Trust Policy**: Defines WHO can assume a role (Lambda service)
- **Permission Policy**: Defines WHAT actions are allowed (CloudWatch Logs)

### 4. S3 Deployment Bucket Security

Since Java Lambda functions require S3 deployment, the bucket must be hardened:

**Encryption:**
- **At Rest**: AES-256 server-side encryption enforced
- **In Transit**: HTTPS/TLS required (HTTP denied by bucket policy)
- **Policy Enforcement**: Unencrypted uploads are automatically denied

**Access Control:**
- **Public Access**: All 4 public access block settings enabled
- **Principal Restriction**: Only Lambda service from same account can read
- **Bucket Policy**: Explicit deny rules for insecure operations

**Data Lifecycle:**
- **Versioning**: Enabled to track all changes and prevent accidental deletion
- **Old Version Cleanup**: Auto-delete non-current versions after 30 days
- **Storage Optimization**: Auto-transition to cheaper storage classes

**Why this matters:**
- Lambda deployment JARs could contain sensitive business logic
- Unencrypted buckets expose code to potential data breaches
- Versioning enables rollback if malicious code is uploaded
- Lifecycle policies reduce costs and minimize attack surface
- HTTPS enforcement prevents man-in-the-middle attacks during upload/download


## 📁 Project Structure

```
aws-serverless-iam-in-action/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── yarleesols/
│                   └── HelloHandler.java
├── infra/
│   ├── iam/
│   │   ├── lambda-execution-role.json
│   │   ├── lambda-execution-policy.json
│   │   └── api-gateway-invoke-policy.json
│   ├── api-gateway.yaml
│   └── lambda.yaml
├── pom.xml
└── README.md
```

## 🚀 Deployment

### Prerequisites

Before you begin, ensure you have:
- AWS CLI configured with appropriate credentials
- Java 17 or higher installed
- Maven 3.6+ installed
- AWS account with permissions to create CloudFormation stacks, Lambda functions, API Gateway, IAM roles, and S3 buckets

### Step 1: Deploy Secure S3 Bucket

Java Lambda functions must be deployed from S3. Create a **secure, encrypted** S3 bucket using CloudFormation:

**Bash/Linux/Mac:**
```bash
aws cloudformation create-stack \
  --stack-name lambda-deployment-bucket-stack \
  --template-body file://infra/s3-deployment-bucket.yaml

# Wait for bucket creation
aws cloudformation wait stack-create-complete --stack-name lambda-deployment-bucket-stack

# Get the bucket name
BUCKET_NAME=$(aws cloudformation describe-stacks \
  --stack-name lambda-deployment-bucket-stack \
  --query 'Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue' \
  --output text)
echo "Bucket created: $BUCKET_NAME"
```

**PowerShell/Windows:**
```powershell
aws cloudformation create-stack `
  --stack-name lambda-deployment-bucket-stack `
  --template-body file://infra/s3-deployment-bucket.yaml

# Wait for bucket creation
aws cloudformation wait stack-create-complete --stack-name lambda-deployment-bucket-stack

# Get the bucket name
$BUCKET_NAME = aws cloudformation describe-stacks `
  --stack-name lambda-deployment-bucket-stack `
  --query 'Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue' `
  --output text
Write-Host "Bucket created: $BUCKET_NAME"
```

> 🔒 **Security Features**:
> - **Encryption at rest** (AES-256)
> - **Versioning enabled** (track changes, rollback if needed)
> - **Public access blocked** (all 4 settings enabled)
> - **HTTPS enforced** (deny non-SSL requests)
> - **Encrypted uploads enforced** (deny unencrypted objects)
> - **Lifecycle policies** (auto-cleanup old versions after 30 days)

### Step 2: Build the Lambda Function

```bash
mvn clean package
```

### Step 3: Upload Lambda JAR to S3

**Bash/Linux/Mac:**
```bash
aws s3 cp target/aws-serverless-iam-in-action-1.0-SNAPSHOT.jar s3://${BUCKET_NAME}/
```

**PowerShell/Windows:**
```powershell
aws s3 cp target/aws-serverless-iam-in-action-1.0-SNAPSHOT.jar s3://$BUCKET_NAME/
```

> **Note**: The S3 bucket automatically encrypts all objects with AES-256. HTTPS is enforced by bucket policy.


### Step 4: Deploy Lambda Stack

**Bash/Linux/Mac:**
```bash
aws cloudformation create-stack \
  --stack-name hello-lambda-stack \
  --template-body file://infra/lambda.yaml \
  --parameters ParameterKey=S3Bucket,ParameterValue=${BUCKET_NAME} \
  --capabilities CAPABILITY_NAMED_IAM

# Wait for stack creation
aws cloudformation wait stack-create-complete --stack-name hello-lambda-stack
```

**PowerShell/Windows:**
```powershell
aws cloudformation create-stack `
  --stack-name hello-lambda-stack `
  --template-body file://infra/lambda.yaml `
  --parameters ParameterKey=S3Bucket,ParameterValue=$BUCKET_NAME `
  --capabilities CAPABILITY_NAMED_IAM

# Wait for stack creation
aws cloudformation wait stack-create-complete --stack-name hello-lambda-stack
```

### Step 5: Deploy API Gateway Stack

**Bash/Linux/Mac:**
```bash
aws cloudformation create-stack \
  --stack-name hello-api-stack \
  --template-body file://infra/api-gateway.yaml \
  --parameters ParameterKey=LambdaStackName,ParameterValue=hello-lambda-stack \
  --capabilities CAPABILITY_NAMED_IAM

# Wait for stack creation
aws cloudformation wait stack-create-complete --stack-name hello-api-stack
```

**PowerShell/Windows:**
```powershell
aws cloudformation create-stack `
  --stack-name hello-api-stack `
  --template-body file://infra/api-gateway.yaml `
  --parameters ParameterKey=LambdaStackName,ParameterValue=hello-lambda-stack `
  --capabilities CAPABILITY_NAMED_IAM

# Wait for stack creation
aws cloudformation wait stack-create-complete --stack-name hello-api-stack
```

### Step 6: Get API Endpoint

**Bash/Linux/Mac:**
```bash
aws cloudformation describe-stacks \
  --stack-name hello-api-stack \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
  --output text
```

**PowerShell/Windows:**
```powershell
aws cloudformation describe-stacks `
  --stack-name hello-api-stack `
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' `
  --output text
```

### Understanding the `wait` Command

The `aws cloudformation wait` command monitors the stack status and blocks until:
- **Success**: Stack reaches `CREATE_COMPLETE` or `UPDATE_COMPLETE`
- **Failure**: Stack reaches a terminal failure state like `ROLLBACK_COMPLETE` or `UPDATE_ROLLBACK_COMPLETE`

This is useful in automation scripts to ensure the stack is fully deployed before proceeding to the next step.

**To check stack status manually:**
```bash
# Bash/Linux/Mac
aws cloudformation describe-stacks --stack-name hello-lambda-stack --query 'Stacks[0].StackStatus' --output text
```

```powershell
# PowerShell/Windows
aws cloudformation describe-stacks --stack-name hello-lambda-stack --query 'Stacks[0].StackStatus' --output text
```

**Common Stack Statuses:**
- `CREATE_IN_PROGRESS` - Stack is being created
- `CREATE_COMPLETE` - Stack created successfully ✓
- `ROLLBACK_IN_PROGRESS` - Creation failed, rolling back
- `ROLLBACK_COMPLETE` - Stack creation failed, need to delete and retry
- `DELETE_IN_PROGRESS` - Stack is being deleted
- `DELETE_COMPLETE` - Stack deleted successfully

**If you get `ROLLBACK_COMPLETE`:**
```bash
# Delete the failed stack
aws cloudformation delete-stack --stack-name hello-lambda-stack
aws cloudformation wait stack-delete-complete --stack-name hello-lambda-stack

# Then retry the create-stack command
```

## 🧪 Testing

### Test the API

**Bash/Linux/Mac:**
```bash
curl -X POST https://YOUR_API_ID.execute-api.YOUR_REGION.amazonaws.com/dev/hello \
  -H "Content-Type: application/json" \
  -d '{"name":"Prasad"}'
```

**PowerShell/Windows:**
```powershell
$response = Invoke-WebRequest -Uri "https://YOUR_API_ID.execute-api.YOUR_REGION.amazonaws.com/dev/hello" -Method POST -ContentType "application/json" -Body '{"name":"Prasad"}'
$response.Content
```

Expected Response:
```json
{
  "message": "Hello Prasad"
}
```

### View CloudWatch Logs

**Bash/Linux/Mac:**
```bash
aws logs tail /aws/lambda/hello-lambda-function --follow
```

**PowerShell/Windows:**
```powershell
aws logs tail /aws/lambda/hello-lambda-function --follow
```

## 📊 API Gateway Configuration

### Stage Configuration

- **Stage name**: `dev`
- **Access logging**: Enabled
- **Log format**: Includes requestId, sourceIp, httpMethod, status, responseLength

### Throttling

- **Rate limit**: 5 requests per second
- **Burst limit**: 10 concurrent requests

### Gateway Responses

- Customized `DEFAULT_4XX` and `DEFAULT_5XX` responses
- CORS headers included in all responses

## 🎓 What Recruiters Should Notice

1. **Least Privilege IAM Policies**
   - Lambda execution role has ONLY the permissions it needs
   - No wildcard (*) permissions
   - Resource-scoped policies

2. **Explicit Trust Relationships**
   - Clear separation between trust policies and permission policies
   - Service-to-service authentication properly configured

3. **Scoped Lambda Invoke Permissions**
   - API Gateway can only invoke specific Lambda function
   - Source ARN restriction prevents unauthorized invocations

4. **Production-Grade Logging**
   - Structured logging in Lambda
   - API Gateway access logs with custom format
   - CloudWatch Log Groups with retention policies

5. **Clean Infrastructure Separation**
   - Infrastructure as Code (CloudFormation)
   - Separate stacks for Lambda and API Gateway
   - Reusable and maintainable architecture

6. **Security Best Practices**
   - CORS properly configured
   - Error messages don't leak sensitive information
   - Throttling prevents abuse

## 🔍 Deep Dive: IAM Concepts

### Trust Policy vs Permission Policy

**Trust Policy** (AssumeRolePolicyDocument):
```json
{
  "Principal": {
    "Service": "lambda.amazonaws.com"
  },
  "Action": "sts:AssumeRole"
}
```
This says: "The Lambda service can assume this role"

**Permission Policy**:
```json
{
  "Action": ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"],
  "Resource": "arn:aws:logs:*:*:/aws/lambda/*"
}
```
This says: "Whatever assumes this role can write to CloudWatch Logs"

### Resource-Based Policy vs Identity-Based Policy

**Identity-Based Policy**: Attached to a role, defines what that identity can do

**Resource-Based Policy**: Attached to a resource (like Lambda), defines who can access it

The API Gateway invoke permission is a **resource-based policy** on the Lambda function.

## 📈 Monitoring and Observability

### CloudWatch Metrics

- Lambda invocations
- API Gateway request count
- 4XX and 5XX errors
- Latency metrics

### CloudWatch Logs

- Lambda execution logs with request IDs
- API Gateway access logs with:
  - Request ID
  - Source IP
  - HTTP method
  - Status code
  - Response length
  - Latency

## 🧹 Cleanup

**Bash/Linux/Mac:**
```bash
# Delete API Gateway stack
aws cloudformation delete-stack --stack-name hello-api-stack
aws cloudformation wait stack-delete-complete --stack-name hello-api-stack

# Delete Lambda stack
aws cloudformation delete-stack --stack-name hello-lambda-stack
aws cloudformation wait stack-delete-complete --stack-name hello-lambda-stack

# Empty S3 bucket (including all versions because versioning is enabled)
BUCKET_NAME=$(aws cloudformation describe-stacks \
  --stack-name lambda-deployment-bucket-stack \
  --query 'Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue' \
  --output text)

# Delete all object versions
aws s3api delete-objects --bucket ${BUCKET_NAME} \
  --delete "$(aws s3api list-object-versions --bucket ${BUCKET_NAME} \
  --query='{Objects: Versions[].{Key:Key,VersionId:VersionId}}' --output json)"

# Delete all delete markers
aws s3api delete-objects --bucket ${BUCKET_NAME} \
  --delete "$(aws s3api list-object-versions --bucket ${BUCKET_NAME} \
  --query='{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}' --output json)"

# Delete S3 bucket stack
aws cloudformation delete-stack --stack-name lambda-deployment-bucket-stack
aws cloudformation wait stack-delete-complete --stack-name lambda-deployment-bucket-stack
```

**PowerShell/Windows:**
```powershell
# Delete API Gateway stack
aws cloudformation delete-stack --stack-name hello-api-stack
aws cloudformation wait stack-delete-complete --stack-name hello-api-stack

# Delete Lambda stack
aws cloudformation delete-stack --stack-name hello-lambda-stack
aws cloudformation wait stack-delete-complete --stack-name hello-lambda-stack

# Empty S3 bucket (including all versions because versioning is enabled)
$BUCKET_NAME = aws cloudformation describe-stacks `
  --stack-name lambda-deployment-bucket-stack `
  --query 'Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue' `
  --output text

# Delete all object versions
aws s3api delete-objects --bucket $BUCKET_NAME `
  --delete "$(aws s3api list-object-versions --bucket $BUCKET_NAME `
  --query='{Objects: Versions[].{Key:Key,VersionId:VersionId}}' --output json)"

# Delete all delete markers
aws s3api delete-objects --bucket $BUCKET_NAME `
  --delete "$(aws s3api list-object-versions --bucket $BUCKET_NAME `
  --query='{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}' --output json)"

# Delete S3 bucket stack
aws cloudformation delete-stack --stack-name lambda-deployment-bucket-stack
aws cloudformation wait stack-delete-complete --stack-name lambda-deployment-bucket-stack
```

> **Note**: Because versioning is enabled on the S3 bucket, you must delete all object versions and delete markers before CloudFormation can delete the bucket. The commands above handle this automatically.

## 📝 License

MIT

## 📝 License

MIT

---

*This project focuses on the distinction between authentication (who can assume a role) and authorization (what actions are allowed) - a fundamental concept in AWS IAM design.*

