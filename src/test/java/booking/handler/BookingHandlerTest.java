package booking.handler;

import booking.repository.booking.impl.InMemoryBookingRepository;
import booking.repository.employee.impl.InMemoryEmployeeRepository;
import booking.entity.Employee;
import booking.service.booking.impl.BookingServiceImpl;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BookingHandler.
 *
 * <p>Tests the unified Lambda handler's end-to-end request/response flow
 * for all booking operations: create, get, update status.
 */
@DisplayName("BookingHandler Integration Tests")
class BookingHandlerTest {

    private BookingHandler handler;
    private InMemoryBookingRepository repository;
    private ObjectMapper objectMapper;
    private MockLambdaContext mockContext;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookingRepository();
        InMemoryEmployeeRepository employeeRepo = new InMemoryEmployeeRepository();
        employeeRepo.save(Employee.builder()
                .employeeId("EMP9876")
                .name("Test User")
                .email("test@techquarter.com")
                .department("Engineering")
                .costCenterRef("CC-100")
                .status("ACTIVE")
                .build());
        handler = new BookingHandler();
        handler.setBookingService(new BookingServiceImpl(repository, employeeRepo));
        objectMapper = new ObjectMapper();
        mockContext = new MockLambdaContext();
    }

    private APIGatewayProxyRequestEvent buildRequest(String method, String body) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod(method);
        request.setBody(body);
        return request;
    }

    private String validBookingJson() throws Exception {
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("employeeId", "EMP9876"),
                Map.entry("resourceType", "FLIGHT"),
                Map.entry("destination", "NYC"),
                Map.entry("departureDate", "2024-11-05 08:00:00"),
                Map.entry("returnDate", "2024-11-08 18:00:00"),
                Map.entry("travelerCount", 1),
                Map.entry("costCenterRef", "CC-456"),
                Map.entry("tripPurpose", "Client meeting - Acme Corp")
        ));
    }

    /** Creates a booking and returns the bookingReferenceId. */
    private String createBookingAndGetRefId() throws Exception {
        APIGatewayProxyRequestEvent req = buildRequest("POST", validBookingJson());
        req.setPath("/bookings");
        APIGatewayProxyResponseEvent resp = handler.handleRequest(req, mockContext);
        Map<String, Object> body = objectMapper.readValue(resp.getBody(), Map.class);
        return (String) body.get("bookingReferenceId");
    }

    // ==================== POST Tests ====================

    @Nested
    @DisplayName("POST /bookings")
    class CreateBookingTests {

        @Test
        @DisplayName("Should return 200 and booking reference for valid request")
        void shouldCreateBookingSuccessfully() throws Exception {
            APIGatewayProxyRequestEvent request = buildRequest("POST", validBookingJson());
            request.setPath("/bookings");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            assertEquals("application/json", response.getHeaders().get("Content-Type"));

            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            assertEquals("SUCCESS", body.get("status"));
            assertNotNull(body.get("bookingReferenceId"));
            assertTrue(body.get("bookingReferenceId").toString().startsWith("BKG-"));
        }

        @Test
        @DisplayName("Should return 400 for invalid JSON")
        void shouldReturn400ForInvalidJson() {
            APIGatewayProxyRequestEvent request = buildRequest("POST", "{invalid}");
            request.setPath("/bookings");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Should return 400 for empty body")
        void shouldReturn400ForEmptyBody() {
            APIGatewayProxyRequestEvent request = buildRequest("POST", "");
            request.setPath("/bookings");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 for null body")
        void shouldReturn400ForNullBody() {
            APIGatewayProxyRequestEvent request = buildRequest("POST", null);
            request.setPath("/bookings");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 when employee ID is missing")
        void shouldReturn400WhenEmployeeIdMissing() throws Exception {
            String json = objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("employeeId", ""),
                    Map.entry("resourceType", "FLIGHT"),
                    Map.entry("destination", "NYC"),
                    Map.entry("departureDate", "2024-11-05 08:00:00"),
                    Map.entry("returnDate", "2024-11-08 18:00:00"),
                    Map.entry("travelerCount", 1),
                    Map.entry("costCenterRef", "CC-456"),
                    Map.entry("tripPurpose", "Meeting")
            ));

            APIGatewayProxyRequestEvent request = buildRequest("POST", json);
            request.setPath("/bookings");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("VALIDATION_ERROR"));
        }
    }

    // ==================== GET Tests ====================

    @Nested
    @DisplayName("GET /bookings")
    class GetBookingTests {

        @Test
        @DisplayName("Should return booking by reference ID")
        void shouldReturnBookingByReferenceId() throws Exception {
            String refId = createBookingAndGetRefId();

            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/bookings/" + refId);
            request.setPathParameters(Map.of("id", refId));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            assertEquals("SUCCESS", body.get("status"));
            assertEquals(refId, body.get("bookingReferenceId"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent booking")
        void shouldReturn404ForNonExistentBooking() throws Exception {
            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/bookings/BKG-non-existent");
            request.setPathParameters(Map.of("id", "BKG-non-existent"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(404, response.getStatusCode());
            assertTrue(response.getBody().contains("NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return bookings by employee ID")
        void shouldReturnBookingsByEmployeeId() throws Exception {
            createBookingAndGetRefId(); // creates for EMP9876

            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/bookings");
            request.setQueryStringParameters(Map.of("employeeId", "EMP9876"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            List<?> list = objectMapper.readValue(response.getBody(), List.class);
            assertEquals(1, list.size());
        }

        @Test
        @DisplayName("Should return empty list for employee with no bookings")
        void shouldReturnEmptyListForNoBookings() throws Exception {
            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/bookings");
            request.setQueryStringParameters(Map.of("employeeId", "EMP-NONE"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            List<?> list = objectMapper.readValue(response.getBody(), List.class);
            assertTrue(list.isEmpty());
        }

        @Test
        @DisplayName("Should return 200 with booking list when no query params (list all)")
        void shouldReturnAllBookingsWhenNoParams() throws Exception {
            createBookingAndGetRefId();

            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/bookings");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            List<?> list = objectMapper.readValue(response.getBody(), List.class);
            assertEquals(1, list.size());
        }
    }

    // ==================== PATCH Tests ====================

    @Nested
    @DisplayName("PATCH /bookings/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update booking status to CONFIRMED")
        void shouldUpdateStatusToConfirmed() throws Exception {
            String refId = createBookingAndGetRefId();

            String statusBody = objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/bookings/" + refId + "/status");
            request.setPathParameters(Map.of("id", refId));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            assertEquals("SUCCESS", body.get("status"));
            assertTrue(body.get("message").toString().contains("CONFIRMED"));
        }

        @Test
        @DisplayName("Should cancel booking via PATCH")
        void shouldCancelBooking() throws Exception {
            String refId = createBookingAndGetRefId();

            String statusBody = objectMapper.writeValueAsString(Map.of("status", "CANCELLED"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/bookings/" + refId + "/status");
            request.setPathParameters(Map.of("id", refId));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("CANCELLED"));
        }

        @Test
        @DisplayName("Should prevent status update on cancelled booking")
        void shouldPreventUpdateOnCancelledBooking() throws Exception {
            String refId = createBookingAndGetRefId();

            // Cancel first
            String cancelBody = objectMapper.writeValueAsString(Map.of("status", "CANCELLED"));
            APIGatewayProxyRequestEvent cancelReq = buildRequest("PATCH", cancelBody);
            cancelReq.setPath("/bookings/" + refId + "/status");
            cancelReq.setPathParameters(Map.of("id", refId));
            handler.handleRequest(cancelReq, mockContext);

            // Try to update again
            String confirmBody = objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"));
            APIGatewayProxyRequestEvent req = buildRequest("PATCH", confirmBody);
            req.setPath("/bookings/" + refId + "/status");
            req.setPathParameters(Map.of("id", refId));

            APIGatewayProxyResponseEvent response = handler.handleRequest(req, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("Cannot update booking in CANCELLED state"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent booking")
        void shouldReturn404ForNonExistentBooking() throws Exception {
            String statusBody = objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/bookings/BKG-none/status");
            request.setPathParameters(Map.of("id", "BKG-none"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(404, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 when no path param")
        void shouldReturn400WhenNoPathParam() throws Exception {
            String statusBody = objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/bookings");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 for invalid status value")
        void shouldReturn400ForInvalidStatus() throws Exception {
            String refId = createBookingAndGetRefId();

            String statusBody = objectMapper.writeValueAsString(Map.of("status", "INVALID"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/bookings/" + refId + "/status");
            request.setPathParameters(Map.of("id", refId));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("Invalid status"));
        }
    }

    // ==================== Method Not Allowed ====================

    @Test
    @DisplayName("Should return 405 for unsupported HTTP method")
    void shouldReturn405ForUnsupportedMethod() {
        APIGatewayProxyRequestEvent request = buildRequest("PUT", "{}");
        request.setPath("/bookings");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("METHOD_NOT_ALLOWED"));
    }

    /**
     * Mock Lambda Context for testing.
     */
    static class MockLambdaContext implements com.amazonaws.services.lambda.runtime.Context {
        @Override public String getAwsRequestId() { return "mock-request-id-123"; }
        @Override public String getLogGroupName() { return "mock-log-group"; }
        @Override public String getLogStreamName() { return "mock-log-stream"; }
        @Override public String getFunctionName() { return "techquarter-booking-handler"; }
        @Override public String getFunctionVersion() { return "1.0.0"; }
        @Override public String getInvokedFunctionArn() { return "arn:aws:lambda:eu-central-1:123456789012:function:techquarter-booking-handler"; }
        @Override public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() { return null; }
        @Override public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() { return null; }
        @Override public int getRemainingTimeInMillis() { return 30000; }
        @Override public int getMemoryLimitInMB() { return 512; }
        @Override public com.amazonaws.services.lambda.runtime.LambdaLogger getLogger() { return null; }
    }
}

