package booking.handler;

import booking.repository.employee.impl.InMemoryEmployeeRepository;
import booking.service.employee.impl.EmployeeServiceImpl;
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
 * Integration tests for EmployeeHandler.
 *
 * <p>Tests the Lambda handler's end-to-end request/response flow
 * for all employee operations: register, get, update status, delete.
 */
@DisplayName("EmployeeHandler Integration Tests")
class EmployeeHandlerTest {

    private EmployeeHandler handler;
    private InMemoryEmployeeRepository repository;
    private ObjectMapper objectMapper;
    private MockLambdaContext mockContext;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEmployeeRepository();
        handler = new EmployeeHandler();
        handler.setEmployeeService(new EmployeeServiceImpl(repository));
        objectMapper = new ObjectMapper();
        mockContext = new MockLambdaContext();
    }

    private APIGatewayProxyRequestEvent buildRequest(String method, String body) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod(method);
        request.setBody(body);
        return request;
    }

    private String validEmployeeJson() throws Exception {
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("employeeId", "EMP001"),
                Map.entry("name", "John Doe"),
                Map.entry("email", "john.doe@techquarter.com"),
                Map.entry("department", "Engineering"),
                Map.entry("costCenterRef", "CC-100")
        ));
    }

    /** Registers an employee and returns the response. */
    private APIGatewayProxyResponseEvent registerEmployee() throws Exception {
        APIGatewayProxyRequestEvent req = buildRequest("POST", validEmployeeJson());
        req.setPath("/employees");
        return handler.handleRequest(req, mockContext);
    }

    // ==================== POST Tests ====================

    @Nested
    @DisplayName("POST /employees")
    class RegisterEmployeeTests {

        @Test
        @DisplayName("Should return 201 and employee ID for valid request")
        void shouldRegisterEmployeeSuccessfully() throws Exception {
            APIGatewayProxyRequestEvent request = buildRequest("POST", validEmployeeJson());
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(201, response.getStatusCode());
            assertEquals("application/json", response.getHeaders().get("Content-Type"));

            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            assertEquals("SUCCESS", body.get("status"));
            assertEquals("EMP001", body.get("employeeId"));
        }

        @Test
        @DisplayName("Should return 409 for duplicate employee")
        void shouldReturn409ForDuplicateEmployee() throws Exception {
            registerEmployee();

            APIGatewayProxyRequestEvent request = buildRequest("POST", validEmployeeJson());
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(409, response.getStatusCode());
            assertTrue(response.getBody().contains("CONFLICT"));
        }

        @Test
        @DisplayName("Should return 400 for invalid JSON")
        void shouldReturn400ForInvalidJson() {
            APIGatewayProxyRequestEvent request = buildRequest("POST", "{broken json");
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Should return 400 for empty body")
        void shouldReturn400ForEmptyBody() {
            APIGatewayProxyRequestEvent request = buildRequest("POST", "");
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameIsMissing() throws Exception {
            String json = objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("employeeId", "EMP002"),
                    Map.entry("name", ""),
                    Map.entry("email", "test@test.com"),
                    Map.entry("department", "Engineering"),
                    Map.entry("costCenterRef", "CC-100")
            ));

            APIGatewayProxyRequestEvent request = buildRequest("POST", json);
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        void shouldReturn400ForInvalidEmail() throws Exception {
            String json = objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("employeeId", "EMP002"),
                    Map.entry("name", "Jane"),
                    Map.entry("email", "not-valid"),
                    Map.entry("department", "Engineering"),
                    Map.entry("costCenterRef", "CC-100")
            ));

            APIGatewayProxyRequestEvent request = buildRequest("POST", json);
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("Invalid email"));
        }
    }

    // ==================== GET Tests ====================

    @Nested
    @DisplayName("GET /employees")
    class GetEmployeeTests {

        @Test
        @DisplayName("Should return employee by ID")
        void shouldReturnEmployeeById() throws Exception {
            registerEmployee();

            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/employees/EMP001");
            request.setPathParameters(Map.of("id", "EMP001"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            assertEquals("SUCCESS", body.get("status"));
            assertEquals("EMP001", body.get("employeeId"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent employee")
        void shouldReturn404ForNonExistentEmployee() throws Exception {
            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/employees/EMP999");
            request.setPathParameters(Map.of("id", "EMP999"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(404, response.getStatusCode());
            assertTrue(response.getBody().contains("NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return employees by email query param")
        void shouldReturnEmployeesByEmail() throws Exception {
            registerEmployee();

            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/employees");
            request.setQueryStringParameters(Map.of("email", "john.doe@techquarter.com"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            List<?> list = objectMapper.readValue(response.getBody(), List.class);
            assertEquals(1, list.size());
        }

        @Test
        @DisplayName("Should return employees by department query param")
        void shouldReturnEmployeesByDepartment() throws Exception {
            registerEmployee();

            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/employees");
            request.setQueryStringParameters(Map.of("department", "Engineering"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            List<?> list = objectMapper.readValue(response.getBody(), List.class);
            assertEquals(1, list.size());
        }

        @Test
        @DisplayName("Should return empty list for non-existent department")
        void shouldReturnEmptyListForNonExistentDepartment() throws Exception {
            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/employees");
            request.setQueryStringParameters(Map.of("department", "NonExistent"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            List<?> list = objectMapper.readValue(response.getBody(), List.class);
            assertTrue(list.isEmpty());
        }

        @Test
        @DisplayName("Should return 200 with employee list when no query params (list all)")
        void shouldReturnAllEmployeesWhenNoParams() throws Exception {
            registerEmployee();

            APIGatewayProxyRequestEvent request = buildRequest("GET", null);
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            List<?> list = objectMapper.readValue(response.getBody(), List.class);
            assertEquals(1, list.size());
        }
    }

    // ==================== PATCH Tests ====================

    @Nested
    @DisplayName("PATCH /employees/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update employee status to INACTIVE")
        void shouldUpdateStatusToInactive() throws Exception {
            registerEmployee();

            String statusBody = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/employees/EMP001/status");
            request.setPathParameters(Map.of("id", "EMP001"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            assertEquals("SUCCESS", body.get("status"));
            assertTrue(body.get("message").toString().contains("INACTIVE"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent employee")
        void shouldReturn404ForNonExistentEmployee() throws Exception {
            String statusBody = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/employees/EMP999/status");
            request.setPathParameters(Map.of("id", "EMP999"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(404, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 for invalid status")
        void shouldReturn400ForInvalidStatus() throws Exception {
            registerEmployee();

            String statusBody = objectMapper.writeValueAsString(Map.of("status", "DELETED"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/employees/EMP001/status");
            request.setPathParameters(Map.of("id", "EMP001"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("Invalid status"));
        }

        @Test
        @DisplayName("Should return 400 when no path param")
        void shouldReturn400WhenNoPathParam() throws Exception {
            String statusBody = objectMapper.writeValueAsString(Map.of("status", "INACTIVE"));

            APIGatewayProxyRequestEvent request = buildRequest("PATCH", statusBody);
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
        }
    }

    // ==================== DELETE Tests ====================

    @Nested
    @DisplayName("DELETE /employees/{id}")
    class DeleteEmployeeTests {

        @Test
        @DisplayName("Should delete employee successfully")
        void shouldDeleteEmployeeSuccessfully() throws Exception {
            registerEmployee();

            APIGatewayProxyRequestEvent request = buildRequest("DELETE", null);
            request.setPath("/employees/EMP001");
            request.setPathParameters(Map.of("id", "EMP001"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(200, response.getStatusCode());
            Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
            assertEquals("SUCCESS", body.get("status"));
            assertTrue(body.get("message").toString().contains("deleted"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent employee")
        void shouldReturn404ForNonExistentEmployee() throws Exception {
            APIGatewayProxyRequestEvent request = buildRequest("DELETE", null);
            request.setPath("/employees/EMP999");
            request.setPathParameters(Map.of("id", "EMP999"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(404, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 when no path param")
        void shouldReturn400WhenNoPathParam() {
            APIGatewayProxyRequestEvent request = buildRequest("DELETE", null);
            request.setPath("/employees");

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

            assertEquals(400, response.getStatusCode());
        }
    }

    // ==================== Method Not Allowed ====================

    @Test
    @DisplayName("Should return 405 for unsupported HTTP method")
    void shouldReturn405ForUnsupportedMethod() {
        APIGatewayProxyRequestEvent request = buildRequest("PUT", "{}");
        request.setPath("/employees");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("METHOD_NOT_ALLOWED"));
    }

    /**
     * Mock Lambda Context for testing.
     */
    static class MockLambdaContext implements com.amazonaws.services.lambda.runtime.Context {
        @Override public String getAwsRequestId() { return "mock-request-id-456"; }
        @Override public String getLogGroupName() { return "mock-log-group"; }
        @Override public String getLogStreamName() { return "mock-log-stream"; }
        @Override public String getFunctionName() { return "techquarter-employee-handler"; }
        @Override public String getFunctionVersion() { return "1.0.0"; }
        @Override public String getInvokedFunctionArn() { return "arn:aws:lambda:eu-central-1:123456789012:function:techquarter-employee-handler"; }
        @Override public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() { return null; }
        @Override public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() { return null; }
        @Override public int getRemainingTimeInMillis() { return 30000; }
        @Override public int getMemoryLimitInMB() { return 512; }
        @Override public com.amazonaws.services.lambda.runtime.LambdaLogger getLogger() { return null; }
    }
}

