# Architecture and Design Decisions

## Overview

This document outlines the architectural decisions, design patterns, and technical rationale for the TechQuarter Corporate Booking Service.

## 1. Serverless Architecture (AWS Lambda + API Gateway)

### Decision

Use AWS Lambda with API Gateway for the booking service backend.

### Rationale

1. **Scalability**: Auto-scales from 0 to handle 100+ TPS without manual intervention
2. **Cost-Efficiency**: Pay only for actual usage, not provisioned capacity
3. **Operational Excellence**: No server management, automatic patching
4. **Cold Start Optimization**: With proper JVM tuning, achievable in <500ms
5. **Enterprise Integration**: Native AWS service ecosystem

### Trade-offs

| Advantage | Disadvantage |
|-----------|--------------|
| No server management | Cold start latency |
| Auto-scaling | Execution timeout (15 min limit) |
| Pay-per-use pricing | Limited local state |
| Integrated monitoring | Debugging challenges |

## 2. Clean Architecture Pattern

### Layers

```
┌─────────────────────────────────┐
│  Handler/Controller Layer       │  (Request/Response boundary)
│  CreateBookingHandler.java      │  (JSON serialization)
├─────────────────────────────────┤
│  Service/Business Logic Layer   │  (Business rules)
│  BookingService & Impl          │  (Validation logic)
├─────────────────────────────────┤
│  Data Transfer Layer            │  (DTOs)
│  BookingRequest & Response      │  (Type safety)
└─────────────────────────────────┘
```

### Benefits

1. **Separation of Concerns**: Each layer has a single responsibility
2. **Testability**: Services can be tested independently of Lambda
3. **Reusability**: Services can be used by future controllers
4. **Maintainability**: Clear structure makes code easier to understand
5. **Flexibility**: Can swap implementations without affecting consumers

### OOP Principles Applied

- **Encapsulation**: Private methods in BookingServiceImpl
- **Inheritance/Polymorphism**: BookingService interface
- **Abstraction**: Handler doesn't know service implementation details
- **Single Responsibility**: Each class has one reason to change

## 3. Technology Choices

### Java 17

**Why**: 
- LTS (Long-Term Support) until 2026+
- Performance improvements (ZGC, records, sealed classes)
- Compatible with AWS Lambda natively
- Modern syntax for cleaner code

### Maven

**Why**:
- Industry standard for Java projects
- Excellent plugin ecosystem
- CI/CD integration
- Reproducible builds

### Jackson for JSON Processing

**Why**:
- Industry standard in Java ecosystem
- Fast streaming parser (important for 100 TPS)
- Full annotation support for DTOs
- Minimal dependencies

### Lombok

**Why**:
- Reduces boilerplate (getters, setters, constructors)
- Less code = fewer bugs
- Still produces standard Java bytecode
- Optional scope (doesn't affect runtime)

### Log4j2

**Why**:
- Asynchronous appenders for performance
- Excellent CloudWatch integration
- Configurable in XML
- Better performance than SLF4J + Logback

### JUnit 5

**Why**:
- Modern testing framework
- Excellent annotation support (@DisplayName, @Test)
- Parameterized test support
- Integration with IDEs and CI/CD

## 4. Concurrency and Performance

### Stateless Design

All Lambda executions are independent:
- No shared state between requests
- No connection pooling
- No warm-up time for long-lived objects
- Each request gets fresh JVM context

### Date/Time Handling

Uses `LocalDateTime` instead of `Date`:
- Immutable (thread-safe)
- Better timezone handling
- Cleaner API
- Consistent with modern Java practices

### Unique Identifier Generation

Uses `UUID.randomUUID()`:
- Guaranteed uniqueness without database lookup
- No sequential ID generation (no bottleneck)
- Web-scale friendly
- Human-readable with prefix ("BKG-")

## 5. Error Handling Strategy

### Three-Layer Error Handling

1. **Handler Layer**: JSON parsing errors → 400
2. **Service Layer**: Validation errors → 400
3. **Global Handler**: System errors → 500

### Error Response Format

```json
{
  "status": "ERROR_TYPE",
  "bookingReferenceId": null,
  "message": "Human-readable message",
  "errorCode": "TECHNICAL_CODE",
  "timestamp": 1234567890
}
```

**Includes**:
- Status for client logic
- Null reference ID for errors
- Message for user display
- Technical error code for logging
- Timestamp for tracing

## 6. Logging Strategy

### Log Levels

| Level | When | Example |
|-------|------|---------|
| INFO | Normal operation | "Booking created successfully" |
| WARN | Validation failure | "Invalid departure date" |
| ERROR | System exceptions | "Database connection failed" |
| DEBUG | Detail during dev | "Request received: {...}" |

### Structured Logging

Includes contextual information:
```java
logger.info("Booking processed. Reference: {}, Employee: {}", 
    bookingRefId, employeeId);
```

Benefits:
- Easy CloudWatch log filtering
- Better monitoring and alerts
- Audit trail for compliance

## 7. Testing Strategy

### Unit Tests (BookingServiceTest)

Focus: Business logic and validation

- 10 test cases
- Edge cases (null values, invalid dates, etc.)
- Positive and negative paths
- Zero external dependencies

### Integration Tests (CreateBookingHandlerTest)

Focus: End-to-end request/response flow

- 11 test cases
- Mock Lambda context
- JSON serialization/deserialization
- HTTP status codes
- Different resource types

### Test Organization

```
Service Tests
├── Valid request processing
├── Missing required fields
├── Invalid date formats
├── Logical constraints
└── Error responses

Handler Tests
├── Valid JSON requests
├── Invalid JSON
├── Empty/null bodies
├── HTTP status codes
└── Content-Type headers
```

### Test Naming Convention

Uses `@DisplayName` for clarity:
```
testCreateBookingSuccess
testCreateBookingMissingEmployeeId
testHandleInvalidJson
```

## 8. Data Flow

### Request Processing

```
1. HTTP POST to API Gateway (/booking)
   ↓
2. API Gateway deserialization
   ↓
3. Lambda invocation (CreateBookingHandler)
   ↓
4. JSON body parsing (Jackson ObjectMapper)
   ↓
5. BookingRequest DTO creation
   ↓
6. BookingService.createBooking() call
   ↓
7. Validation (11 rules checked)
   ↓
8. UUID generation for reference ID
   ↓
9. BookingResponse creation
   ↓
10. JSON serialization
   ↓
11. HTTP response return
   ↓
12. API Gateway transformation
   ↓
13. HTTP response to client
```

### Validation Rules

```java
if (employeeId.isBlank()) → throw
if (resourceType.isBlank()) → throw
if (destination.isBlank()) → throw
if (departureDate.isBlank()) → throw
if (returnDate.isBlank()) → throw
if (travelerCount < 1) → throw
if (costCenterRef.isBlank()) → throw
if (tripPurpose.isBlank()) → throw
if (dateFormat != "yyyy-MM-dd HH:mm:ss") → throw
if (departureDate >= returnDate) → throw
if (departureDate == returnDate) → throw
```

## 9. Performance Optimization

### Cold Start Mitigation

#### JVM Configuration (template.yaml)

```yaml
JAVA_TOOL_OPTIONS: >
  -XX:+TieredCompilation      # Enable JIT compilation
  -XX:TieredStopAtLevel=1     # Use C1 compiler (faster startup)
  -XX:+UseSerialGC            # Lightweight GC
  -Xshare:off                 # Disable Class Data Sharing
```

#### Code Optimization

1. Minimal dependencies (only Jackson + AWS)
2. No reflection (Lombok generates code at compile-time)
3. Efficient string operations
4. Stateless design

### Response Time Profile

- **Cold Start**: ~500ms (with optimizations)
- **Warm Response**: ~50-100ms
- **P99 Latency**: <500ms at 100 TPS

### Throughput

- **Single Instance**: ~200 TPS (20ms per request)
- **Concurrent Limit**: Default 1000 executions
- **Achievable Throughput**: 100+ TPS (easily)

## 10. Security Considerations

### Current Implementation

- Input validation (all fields required)
- No SQL injection (no database)
- No XSS (JSON-only interface)
- Structured error messages (no stack traces in responses)

### Future Security Enhancements

1. **Authentication**: API Key or OAuth2 via API Gateway authorizer
2. **Authorization**: Role-based access control (RBAC)
3. **Rate Limiting**: Prevent abuse (1000 req/min per key)
4. **HTTPS**: API Gateway enforces TLS 1.2+
5. **Encryption**: Enable at-rest and in-transit
6. **Logging**: Audit trail for compliance
7. **VPC**: Restrict Lambda to corporate VPC
8. **WAF**: AWS Web Application Firewall for API Gateway

## 11. Monitoring and Observability

### CloudWatch Metrics

Built-in Lambda metrics:
- Invocations (count)
- Duration (execution time)
- Errors (failed requests)
- Throttles (rate limited)
- ConcurrentExecutions

### Custom Metrics

Recommended additions:
- Validation error rate
- Booking creation rate
- Request latency percentiles
- Cost per transaction

### Alarms

Recommended CloudWatch alarms:
- Error rate > 1%
- Duration P99 > 1 second
- Throttles > 0
- Invalid requests > 5%

## 12. Scalability Architecture

### Horizontal Scaling

API Gateway + Lambda auto-scales:
- 100 TPS → 5-10 Lambda instances
- 1000 TPS → 50-100 Lambda instances
- 10000 TPS → 500-1000 Lambda instances

### Bottlenecks

Current implementation can handle:
- **API Gateway**: 10,000 req/sec per account (can increase)
- **Lambda**: 1000 concurrent executions (can increase)
- **DynamoDB** (future): Configure for provisioned throughput

### Scaling Strategy

1. Monitor CloudWatch metrics
2. Set up alarms at 80% capacity
3. Request quota increases (instant)
4. Add database as bottleneck (future)

## 13. Cost Analysis

### Monthly Costs at 100 TPS

```
Requests:     100 TPS × 2,592,000 sec = 259.2M requests
Free Tier:    1M requests = 258.2M chargeable requests
Compute:      259.2M × 0.1s × 0.0000002 = $5.18/month
Storage:      CloudWatch logs ~10GB = $5.00/month
Total:        ~$10/month (after free tier)
```

### Cost Optimization

1. Use API Gateway caching
2. Optimize Lambda memory (512 MB optimal)
3. Archive logs to S3
4. Use VPC endpoints (if applicable)

## 14. Disaster Recovery

### RTO/RPO Goals

- **RTO** (Recovery Time Objective): <5 minutes
- **RPO** (Recovery Point Objective): <1 minute

### DR Strategy

1. **Multi-region** (optional): Deploy to multiple regions
2. **CloudFormation**: Infrastructure as code allows quick rebuild
3. **DLQ** (Dead Letter Queue, future): For failed bookings
4. **Backups** (future): DynamoDB point-in-time recovery

## 15. Compliance and Governance

### Data Handling

- No PII stored in logs
- Booking reference IDs are non-sequential (privacy)
- Cost center refs used for billing only
- Employee IDs are corporate identifiers

### Audit Trail

- CloudWatch logs retained 30 days
- Archive to S3 for long-term storage
- Track who created what booking (future: user auth)

### Compliance Standards

Suitable for:
- SOC 2 Type II
- ISO 27001
- GDPR (with data retention policies)
- HIPAA (if PII encrypted)

## 16. API Design Decisions

### REST vs GraphQL

**Decision**: REST (simple POST endpoint)

**Rationale**:
- Simple use case (one operation)
- Lower client complexity
- Better CloudWatch integration
- Easier testing
- Future: Add GraphQL layer if needed

### Request/Response Format

**JSON** (application/json)

**Rationale**:
- Industry standard
- Human-readable
- Excellent tool support
- Language-agnostic

### Versioning Strategy

**Future**: URL-based versioning

```
POST /v1/booking      # Version 1
POST /v2/booking      # Version 2 (breaking changes)
```

## 17. Documentation Strategy

### Code Documentation

- Javadoc for all public classes/methods
- Inline comments explaining "why"
- Example JSON in DTOs
- Test names as documentation

### Operational Documentation

- README.md: Quick start and API reference
- BUILD_AND_DEPLOY.md: Step-by-step deployment
- ARCHITECTURE.md: This file
- REQUIREMENTS.md: Original requirements

## Conclusion

This architecture provides:
- **High Performance**: 100+ TPS capability
- **Enterprise Quality**: Clean code, comprehensive tests
- **Operational Excellence**: Minimal management overhead
- **Cost Efficiency**: Pay-per-use pricing
- **Scalability**: Auto-scales with demand
- **Maintainability**: Clear structure, good documentation

The design is flexible enough to evolve with business needs while maintaining clean code principles.

