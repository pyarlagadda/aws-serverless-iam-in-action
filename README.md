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

- AWS CLI configured with appropriate credentials
- Java 21 or higher
- Maven 3.6+

### Step 1: Build the Lambda Function

```bash
mvn clean package
```

### Step 2: Deploy Lambda Stack

```bash
aws cloudformation create-stack \
  --stack-name hello-lambda-stack \
  --template-body file://infra/lambda.yaml \
  --capabilities CAPABILITY_NAMED_IAM

# Wait for stack creation
aws cloudformation wait stack-create-complete --stack-name hello-lambda-stack
```

### Step 3: Update Lambda Function Code

```bash
aws lambda update-function-code \
  --function-name hello-lambda-function \
  --zip-file fileb://target/aws-serverless-iam-in-action-1.0-SNAPSHOT.jar
```

### Step 4: Deploy API Gateway Stack

```bash
aws cloudformation create-stack \
  --stack-name hello-api-stack \
  --template-body file://infra/api-gateway.yaml \
  --parameters ParameterKey=LambdaStackName,ParameterValue=hello-lambda-stack \
  --capabilities CAPABILITY_NAMED_IAM

# Wait for stack creation
aws cloudformation wait stack-create-complete --stack-name hello-api-stack
```

### Step 5: Get API Endpoint

```bash
aws cloudformation describe-stacks \
  --stack-name hello-api-stack \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
  --output text
```

## 🧪 Testing

### Test the API

```bash
curl -X POST https://YOUR_API_ID.execute-api.YOUR_REGION.amazonaws.com/dev/hello \
  -H "Content-Type: application/json" \
  -d '{"name":"Prasad"}'
```

Expected Response:
```json
{
  "message": "Hello Prasad"
}
```

### View CloudWatch Logs

```bash
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

```bash
# Delete API Gateway stack
aws cloudformation delete-stack --stack-name hello-api-stack
aws cloudformation wait stack-delete-complete --stack-name hello-api-stack

# Delete Lambda stack
aws cloudformation delete-stack --stack-name hello-lambda-stack
aws cloudformation wait stack-delete-complete --stack-name hello-lambda-stack
```

## 📝 License

MIT

## 📝 License

MIT

---

*This project focuses on the distinction between authentication (who can assume a role) and authorization (what actions are allowed) - a fundamental concept in AWS IAM design.*

