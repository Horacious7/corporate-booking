# Build and Deployment Guide

## Building the Project

### Prerequisites

Ensure you have the following installed:

- **Java 17 SDK**: https://www.oracle.com/java/technologies/downloads/#java17
- **Maven 3.8.0+**: https://maven.apache.org/download.cgi
- **AWS CLI v2**: https://aws.amazon.com/cli/
- **AWS SAM CLI v1.0+**: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html
- **Docker**: https://www.docker.com/products/docker-desktop

### Verify Installation

```bash
# Check Java
java -version

# Check Maven
mvn --version

# Check AWS CLI
aws --version

# Check SAM CLI
sam --version

# Check Docker
docker --version
```

## Local Development

### 1. Build the Project

```bash
cd booking-service
mvn clean package
```

This creates `target/booking-service-1.0.0.jar` - an uber JAR with all dependencies.

### 2. Run Unit Tests

```bash
mvn test
```

Expected output:
```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.techquarter.booking.service.BookingServiceTest
Running com.techquarter.booking.handler.CreateBookingHandlerTest
...
Results: 21 tests passed, 0 failures
```

### 3. Local SAM Testing

```bash
# Build for SAM
sam build

# Start local API server
sam local start-api

# In another terminal, test the endpoint:
curl -X POST http://localhost:3000/booking \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP9876",
    "resourceType": "Flight",
    "destination": "NYC",
    "departureDate": "2024-11-05 08:00:00",
    "returnDate": "2024-11-08 18:00:00",
    "travelerCount": 1,
    "costCenterRef": "CC-456",
    "tripPurpose": "Client meeting - Acme Corp"
  }'
```

Expected response:
```json
{
  "status": "SUCCESS",
  "bookingReferenceId": "BKG-550e8400-e29b-41d4-a716-446655440000",
  "message": "Booking created successfully for employee EMP9876"
}
```

## AWS Deployment

### Prerequisites

1. AWS Account with appropriate permissions
2. AWS CLI configured with credentials:
   ```bash
   aws configure
   aws sts get-caller-identity  # Verify setup
   ```

### Step 1: Prepare for Deployment

```bash
# Create S3 bucket for SAM artifacts (if not using managed bucket)
aws s3 mb s3://techquarter-booking-service-artifacts-${AWS_ACCOUNT_ID} \
  --region us-east-1

# Where AWS_ACCOUNT_ID is your 12-digit AWS account ID
```

### Step 2: Deploy Using SAM

#### Option A: Interactive Deployment (Guided)

```bash
sam deploy --guided
```

You'll be prompted for:

1. **Stack Name**: `techquarter-booking-service-stack`
2. **AWS Region**: `us-east-1` (or your preferred region)
3. **S3 Bucket**: Your artifact bucket or create new
4. **Capabilities**: Accept IAM role creation (CAPABILITY_IAM)
5. **Confirm Changes**: `Y`

#### Option B: Non-Interactive Deployment

Update `samconfig.toml`:

```toml
[default.deploy]
stack_name = "techquarter-booking-service-stack"
s3_bucket = "techquarter-booking-service-artifacts-123456789012"
s3_prefix = "techquarter-booking-service"
region = "us-east-1"
confirm_changeset = false
capabilities = "CAPABILITY_IAM"
```

Then deploy:

```bash
sam deploy
```

### Step 3: Verify Deployment

```bash
# List all CloudFormation stacks
aws cloudformation describe-stacks \
  --stack-name techquarter-booking-service-stack \
  --query 'Stacks[0].StackStatus'

# Get stack outputs
aws cloudformation describe-stacks \
  --stack-name techquarter-booking-service-stack \
  --query 'Stacks[0].Outputs'

# Get the API endpoint URL
aws cloudformation describe-stacks \
  --stack-name techquarter-booking-service-stack \
  --query 'Stacks[0].Outputs[?OutputKey==`BookingServiceApiEndpoint`].OutputValue' \
  --output text
```

### Step 4: Test the Deployed Service

```bash
# Get the API endpoint (replace with actual endpoint from step 3)
API_ENDPOINT="https://xxxxx.execute-api.us-east-1.amazonaws.com/prod/booking"

# Test with curl
curl -X POST $API_ENDPOINT \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP9876",
    "resourceType": "Flight",
    "destination": "NYC",
    "departureDate": "2024-11-05 08:00:00",
    "returnDate": "2024-11-08 18:00:00",
    "travelerCount": 1,
    "costCenterRef": "CC-456",
    "tripPurpose": "Client meeting - Acme Corp"
  }'
```

### Step 5: View Logs

```bash
# View Lambda logs in real-time
sam logs -n CreateBookingFunction \
  --stack-name techquarter-booking-service-stack \
  --tail

# Or use CloudWatch directly
aws logs tail /aws/lambda/techquarter-create-booking --follow
```

## CI/CD Integration

### GitHub Actions Workflow Example

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy Booking Service

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      
      - name: Run Maven Tests
        run: mvn test
  
  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      
      - name: Build with Maven
        run: mvn package -DskipTests
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v1
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1
      
      - name: Deploy with SAM
        run: |
          sam build
          sam deploy --no-confirm-changeset --no-fail-on-empty-changeset
```

## Troubleshooting

### Issue: SAM Build Fails

```bash
# Clear SAM cache
rm -rf .aws-sam/

# Rebuild
mvn clean package
sam build

# Check for errors
sam build --debug
```

### Issue: Deployment Fails with IAM Errors

```bash
# Verify IAM permissions
aws iam get-user

# Check CloudFormation stack events
aws cloudformation describe-stack-events \
  --stack-name techquarter-booking-service-stack \
  --query 'StackEvents[?ResourceStatus==`CREATE_FAILED`]'
```

### Issue: Lambda Function Errors

```bash
# Check function logs
aws logs get-log-events \
  --log-group-name /aws/lambda/techquarter-create-booking \
  --log-stream-name $(aws logs describe-log-streams \
    --log-group-name /aws/lambda/techquarter-create-booking \
    --query 'logStreams[0].logStreamName' --output text)

# Invoke function manually
aws lambda invoke \
  --function-name techquarter-create-booking \
  --payload '{"resourceType":"APPLICATION/JSON","body":"{\"employeeId\":\"EMP9876\"}"}' \
  response.json
```

### Issue: Local SAM Testing Fails

```bash
# Check Docker is running
docker ps

# Rebuild SAM
sam build

# Start with verbose output
sam local start-api --debug

# Check Lambda runtime image
docker images | grep lambda
```

## Performance Testing

### Load Testing with Apache JMeter

1. Download JMeter: https://jmeter.apache.org/
2. Create a test plan:
   - Add HTTP Request sampler
   - Set URL to your API endpoint
   - Configure for 100 TPS (10 thread groups × 10 requests/sec)
3. Run the test and monitor CloudWatch metrics

### Load Testing with `ab` (ApacheBench)

```bash
# 1000 requests with 100 concurrent
ab -n 1000 -c 100 \
  -p booking.json \
  -T application/json \
  https://xxxxx.execute-api.us-east-1.amazonaws.com/prod/booking
```

## Cost Optimization

### AWS Lambda Pricing

- **Compute**: $0.0000002 per 100ms (free tier: 1M requests/month)
- **Data Transfer**: $0.09 per GB (free tier: 1 GB/month)

For 100 TPS over 1 month:
- Requests: 100 TPS × 86,400 sec/day × 30 days = 259.2M requests
- Duration: ~100ms per request = 4,320,000 GB-seconds
- Cost: ~$17/month compute

### Cost Reduction Tips

1. Right-size Lambda memory (512 MB optimal for this workload)
2. Enable X-Ray sampling instead of full tracing
3. Use API Gateway caching for repeated requests
4. Archive logs to S3 after 30 days

## Cleanup

To remove the deployment:

```bash
# Delete the CloudFormation stack
aws cloudformation delete-stack \
  --stack-name techquarter-booking-service-stack

# Delete S3 bucket (if created)
aws s3 rb s3://techquarter-booking-service-artifacts-123456789012 --force
```

## Next Steps

1. Add DynamoDB for booking persistence
2. Implement authentication with API Gateway authorizers
3. Set up CloudWatch alarms for errors and latency
4. Create monitoring dashboard
5. Add API rate limiting
6. Implement booking search and cancellation endpoints

