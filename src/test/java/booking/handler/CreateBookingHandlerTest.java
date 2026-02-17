package booking.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CreateBookingHandler.
 *
 * <p>Tests the Lambda handler's end-to-end request/response flow including:
 * <ul>
 *   <li>Successful booking creation via Lambda</li>
 *   <li>JSON deserialization and serialization</li>
 *   <li>HTTP status codes and headers</li>
 *   <li>Error handling and error responses</li>
 *   <li>Invalid input handling</li>
 * </ul>
 *
 * @author TechQuarter Engineering
 * @version 1.0.0
 */
@DisplayName("CreateBookingHandler Integration Tests")
class CreateBookingHandlerTest {

    private CreateBookingHandler handler;
    private ObjectMapper objectMapper;
    private MockLambdaContext mockContext;

    @BeforeEach
    void setUp() {
        handler = new CreateBookingHandler();
        objectMapper = new ObjectMapper();
        mockContext = new MockLambdaContext();
    }

    @Test
    @DisplayName("Should return 200 status and booking reference for valid request")
    void testHandleValidBookingRequest() throws Exception {
        // Arrange
        String validJson = objectMapper.writeValueAsString(
            Map.ofEntries(
                Map.entry("employeeId", "EMP9876"),
                Map.entry("resourceType", "Flight"),
                Map.entry("destination", "NYC"),
                Map.entry("departureDate", "2024-11-05 08:00:00"),
                Map.entry("returnDate", "2024-11-08 18:00:00"),
                Map.entry("travelerCount", 1),
                Map.entry("costCenterRef", "CC-456"),
                Map.entry("tripPurpose", "Client meeting - Acme Corp")
            )
        );

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(validJson);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));

        Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("SUCCESS", responseBody.get("status"));
        assertNotNull(responseBody.get("bookingReferenceId"));
        assertTrue(responseBody.get("bookingReferenceId").toString().startsWith("BKG-"));
    }

    @Test
    @DisplayName("Should return 400 status for invalid JSON")
    void testHandleInvalidJson() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{invalid json}");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertTrue(response.getBody().contains("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should return 400 status for empty request body")
    void testHandleEmptyBody() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should return 400 status for null request body")
    void testHandleNullBody() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should return 400 status when employee ID is missing")
    void testHandleMissingEmployeeId() throws Exception {
        // Arrange
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("employeeId", "");
        requestData.put("resourceType", "Flight");
        requestData.put("destination", "NYC");
        requestData.put("departureDate", "2024-11-05 08:00:00");
        requestData.put("returnDate", "2024-11-08 18:00:00");
        requestData.put("travelerCount", "1");
        requestData.put("costCenterRef", "CC-456");
        requestData.put("tripPurpose", "Client meeting");

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(objectMapper.writeValueAsString(requestData));

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Should return 400 status when dates are invalid")
    void testHandleInvalidDates() throws Exception {
        // Arrange
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("employeeId", "EMP9876");
        requestData.put("resourceType", "Flight");
        requestData.put("destination", "NYC");
        requestData.put("departureDate", "2024-11-08 18:00:00");
        requestData.put("returnDate", "2024-11-05 08:00:00");
        requestData.put("travelerCount", "1");
        requestData.put("costCenterRef", "CC-456");
        requestData.put("tripPurpose", "Client meeting");

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(objectMapper.writeValueAsString(requestData));

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("before return date"));
    }

    @Test
    @DisplayName("Should return JSON response with correct Content-Type header")
    void testResponseContentTypeHeader() throws Exception {
        // Arrange
        String validJson = objectMapper.writeValueAsString(
            Map.ofEntries(
                Map.entry("employeeId", "EMP9876"),
                Map.entry("resourceType", "Flight"),
                Map.entry("destination", "NYC"),
                Map.entry("departureDate", "2024-11-05 08:00:00"),
                Map.entry("returnDate", "2024-11-08 18:00:00"),
                Map.entry("travelerCount", 1),
                Map.entry("costCenterRef", "CC-456"),
                Map.entry("tripPurpose", "Client meeting")
            )
        );

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(validJson);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    @DisplayName("Should handle hotel bookings")
    void testHandleHotelBooking() throws Exception {
        // Arrange
        String validJson = objectMapper.writeValueAsString(
            Map.ofEntries(
                Map.entry("employeeId", "EMP5555"),
                Map.entry("resourceType", "Hotel"),
                Map.entry("destination", "London"),
                Map.entry("departureDate", "2024-12-01 14:00:00"),
                Map.entry("returnDate", "2024-12-04 11:00:00"),
                Map.entry("travelerCount", 2),
                Map.entry("costCenterRef", "CC-789"),
                Map.entry("tripPurpose", "Conference attendance")
            )
        );

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(validJson);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("SUCCESS", responseBody.get("status"));
    }

    /**
     * Mock implementation of Lambda Context for testing.
     */
    static class MockLambdaContext implements com.amazonaws.services.lambda.runtime.Context {
        @Override
        public String getAwsRequestId() {
            return "mock-request-id-123";
        }

        @Override
        public String getLogGroupName() {
            return "mock-log-group";
        }

        @Override
        public String getLogStreamName() {
            return "mock-log-stream";
        }

        @Override
        public String getFunctionName() {
            return "techquarter-create-booking";
        }

        @Override
        public String getFunctionVersion() {
            return "1.0.0";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "arn:aws:lambda:us-east-1:123456789012:function:techquarter-create-booking";
        }

        @Override
        public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 30000;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 512;
        }

        @Override
        public com.amazonaws.services.lambda.runtime.LambdaLogger getLogger() {
            return null;
        }
    }
}

