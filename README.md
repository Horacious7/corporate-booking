# TechQuarter Corporate Booking Service

A production-ready, serverless booking service built with AWS SAM, Java 17, and Maven. Designed to handle high-volume corporate travel bookings with 100+ transactions per second (TPS).

## Project Overview

This service implements a **Workflow Service** for managing corporate booking requests. It follows a **Clean Architecture** pattern with clear separation of concerns (Handler → Service → Repository) and is fully optimized for AWS Lambda execution.

### Key Features

- **Serverless Architecture**: AWS Lambda + API Gateway
- **High Performance**: Optimized for 100 TPS peak traffic
- **Clean Code**: Well-documented, SOLID principles, OOP design patterns
- **Enterprise Ready**: Comprehensive error handling, logging, and monitoring
- **Infrastructure as Code**: AWS SAM template for rapid deployment
- **Fully Tested**: Unit and integration tests with JUnit 5

## Architecture

### Clean Architecture Pattern

```
┌─────────────────────────────────────────────────────────┐
│                      HTTP Layer                         │
│              (API Gateway → Lambda)                      │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                  Handler Layer                          │
│           (CreateBookingHandler.java)                   │
│         - JSON Deserialization                          │
│         - Request/Response Mapping                      │
│         - HTTP Status Code Selection                    │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                  Service Layer                          │
│         (BookingService & BookingServiceImpl)            │
│         - Business Logic & Validation                   │
│         - Booking Reference Generation                  │
│         - Domain Rules Enforcement                      │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│              Exception Handling                         │
│      (GlobalExceptionHandler.java)                      │
│      - Standardized Error Responses                     │
│      - Centralized Logging                             │
└─────────────────────────────────────────────────────────┘
```

## Project Structure

```
booking-service/
├── pom.xml                          # Maven configuration
├── template.yaml                    # AWS SAM template
├── samconfig.toml                   # SAM deployment config
├── README.md                        # This file
│
└── src/com/techquarter/booking/
    ├── handler/
    │   ├── CreateBookingHandler.java        # Lambda entry point
    │   └── CreateBookingHandlerTest.java    # Integration tests
    │
    ├── service/
    │   ├── BookingService.java              # Service interface
    │   ├── impl/
    │   │   └── BookingServiceImpl.java       # Service implementation
    │   └── BookingServiceTest.java          # Unit tests
    │
    ├── dto/
    │   ├── BookingRequest.java              # Request DTO
    │   └── BookingResponse.java             # Response DTO
    │
    └── exception/
        └── GlobalExceptionHandler.java      # Error handling
```

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Runtime | Java | 17 |
| Build Tool | Maven | 3.8.0+ |
| AWS | Lambda + API Gateway + SAM | Latest |
| JSON Processing | Jackson | 2.15.2 |
| Logging | Log4j2 | 2.20.0 |
| Testing | JUnit 5 | 5.9.3 |
| Boilerplate Reduction | Lombok | 1.18.30 |

## Quick Start

### Prerequisites

- AWS Account
- AWS CLI configured
- AWS SAM CLI v1.0+
- Java 17 SDK
- Maven 3.8.0+
- Docker (for local testing with SAM)

### Build

```bash
# Build the project
mvn clean package

# Output: target/booking-service-1.0.0.jar
```

### Test

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest
mvn test -Dtest=CreateBookingHandlerTest
```

### Deploy to AWS

```bash
# Build and deploy using SAM
sam build

# Deploy to AWS (interactive)
sam deploy --guided

# Or use samconfig.toml for non-interactive deployment
sam deploy
```

### Local Testing

```bash
# Start SAM local API
sam local start-api

# Test the endpoint (in another terminal)
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

## API Documentation

### Create Booking Endpoint

**POST** `/booking`

#### Request Format

```json
{
  "employeeId": "EMP9876",
  "resourceType": "Flight",
  "destination": "NYC",
  "departureDate": "2024-11-05 08:00:00",
  "returnDate": "2024-11-08 18:00:00",
  "travelerCount": 1,
  "costCenterRef": "CC-456",
  "tripPurpose": "Client meeting - Acme Corp"
}
```

#### Response Format (Success - HTTP 200)

```json
{
  "status": "SUCCESS",
  "bookingReferenceId": "BKG-550e8400-e29b-41d4-a716-446655440000",
  "message": "Booking created successfully for employee EMP9876"
}
```

#### Response Format (Validation Error - HTTP 400)

```json
{
  "status": "VALIDATION_ERROR",
  "bookingReferenceId": null,
  "message": "Employee ID is required",
  "errorCode": "VALIDATION_FAILED",
  "timestamp": 1630705200000
}
```

#### Response Format (System Error - HTTP 500)

```json
{
  "status": "SYSTEM_ERROR",
  "bookingReferenceId": null,
  "message": "An unexpected error occurred. Please contact support.",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "timestamp": 1630705200000
}
```

### Validation Rules

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| employeeId | String | Yes | Non-empty |
| resourceType | String | Yes | "Flight" or "Hotel" |
| destination | String | Yes | Non-empty |
| departureDate | String | Yes | Format: `yyyy-MM-dd HH:mm:ss` |
| returnDate | String | Yes | Format: `yyyy-MM-dd HH:mm:ss`, must be after departure date |
| travelerCount | Integer | Yes | Minimum: 1 |
| costCenterRef | String | Yes | Non-empty |
| tripPurpose | String | Yes | Non-empty |

## Performance Optimization

### Cold Start Mitigation

The following optimizations are implemented to minimize Lambda cold start times:

1. **Minimal Dependencies**: Only essential AWS and Jackson libraries
2. **JVM Configuration** (in template.yaml):
   ```yaml
   JAVA_TOOL_OPTIONS: >
     -XX:+TieredCompilation
     -XX:TieredStopAtLevel=1
     -XX:+UseSerialGC
     -Xshare:off
   ```
3. **512 MB Memory**: Provides sufficient resources for JIT compilation
4. **Stateless Design**: No connection pooling or initialization overhead

### Achieving 100 TPS

- **API Gateway**: Auto-scales to handle traffic
- **Lambda**: Concurrent execution limit (default 1000)
- **Efficient JSON Processing**: Jackson streaming parser
- **Async Logging**: Log4j2 asynchronous appenders

## Code Quality Standards

### Documentation

- **Comprehensive Javadoc**: Every class and public method documented
- **Example Usage**: All DTOs include JSON examples
- **Why Comments**: Implementation comments explain reasoning

### Testing

- **Unit Tests**: `BookingServiceTest.java` (10+ test cases)
- **Integration Tests**: `CreateBookingHandlerTest.java` (11+ test cases)
- **Coverage**: Validation logic, error paths, edge cases
- **Test Organization**: @DisplayName annotations for clarity

### Design Patterns

1. **Service Layer Pattern**: Separation of concerns (Handler → Service)
2. **DTO Pattern**: Data transfer between layers
3. **Exception Handler Pattern**: Centralized error handling
4. **Builder Pattern**: Fluent object construction (via Lombok)
5. **Strategy Pattern**: Flexible validation logic

## Error Handling Strategy

### Error Types

| Status Code | Scenario | Handler |
|------------|----------|---------|
| 200 | Booking created successfully | BookingResponse with SUCCESS |
| 400 | Invalid JSON or validation failure | GlobalExceptionHandler |
| 500 | Unexpected system error | GlobalExceptionHandler |

### Logging Strategy

- **INFO**: Successful bookings and key milestones
- **WARN**: Validation failures
- **ERROR**: System exceptions with stack traces
- **DEBUG**: Request/response details

## Deployment

### AWS SAM Commands

```bash
# Build for deployment
sam build

# Deploy with interactive prompts
sam deploy --guided

# Deploy using samconfig.toml
sam deploy

# List deployed stack
aws cloudformation describe-stacks \
  --stack-name techquarter-booking-service-stack

# View Lambda function logs
sam logs -n CreateBookingFunction --stack-name techquarter-booking-service-stack
```

### Stack Outputs

After deployment, CloudFormation provides:

- **BookingServiceApiEndpoint**: URL to POST booking requests
- **CreateBookingFunctionName**: Lambda function name
- **CreateBookingFunctionArn**: Lambda function ARN

## Monitoring

### CloudWatch Metrics

- **Invocations**: Count of Lambda invocations
- **Duration**: Execution time per request
- **Errors**: Failed invocations
- **Throttles**: Rate limit hits

### CloudWatch Logs

- Log Group: `/aws/lambda/techquarter-create-booking`
- Log Streams: Organized by Lambda version and instance

### X-Ray Tracing

Enable X-Ray in template.yaml:
```yaml
TracingConfig:
  Mode: Active
```

## Future Enhancements

1. **Database Integration**: Persist bookings to DynamoDB
2. **Email Notifications**: Send confirmation to employee
3. **Caching**: Redis for destination validation
4. **Authentication**: API Key or OAuth2
5. **Rate Limiting**: Prevent abuse
6. **Metrics**: Custom CloudWatch metrics
7. **Scheduled Tasks**: Cleanup expired bookings
8. **Frontend UI**: React Native mobile app

## Troubleshooting

### Local Testing Issues

```bash
# Ensure SAM is updated
sam --version

# Clear SAM cache
rm -rf .aws-sam

# Rebuild
sam build
```

### Deployment Issues

```bash
# Check S3 bucket exists
aws s3 ls | grep samclisourcebucket

# View stack events
aws cloudformation describe-stack-events \
  --stack-name techquarter-booking-service-stack
```

### Test Failures

```bash
# Run with verbose output
mvn test -X

# Run specific test
mvn test -Dtest=BookingServiceTest#testCreateBookingSuccess
```

## Contributing

### Code Standards

- Follow Google Java Style Guide
- Add Javadoc for all public methods
- Write tests for all logic
- Keep methods focused (single responsibility)

### Pull Request Process

1. Create feature branch: `git checkout -b feature/booking-search`
2. Write tests first (TDD)
3. Implement feature
4. Ensure all tests pass: `mvn test`
5. Submit PR with description

## License

Copyright © 2024 TechQuarter. All rights reserved.

## Support

For issues or questions:
- Email: engineering@techquarter.com
- Slack: #booking-service
- Wiki: https://wiki.techquarter.com/booking-service

## Appendix: JSON Contract Examples

### Flight Booking Request

```json
{
  "employeeId": "EMP9876",
  "resourceType": "Flight",
  "destination": "NYC",
  "departureDate": "2024-11-05 08:00:00",
  "returnDate": "2024-11-08 18:00:00",
  "travelerCount": 1,
  "costCenterRef": "CC-456",
  "tripPurpose": "Client meeting - Acme Corp"
}
```

### Hotel Booking Request

```json
{
  "employeeId": "EMP5555",
  "resourceType": "Hotel",
  "destination": "London",
  "departureDate": "2024-12-01 14:00:00",
  "returnDate": "2024-12-04 11:00:00",
  "travelerCount": 2,
  "costCenterRef": "CC-789",
  "tripPurpose": "Annual conference attendance"
}
```

### Success Response Example

```json
{
  "status": "SUCCESS",
  "bookingReferenceId": "BKG-f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "message": "Booking created successfully for employee EMP9876"
}
```

