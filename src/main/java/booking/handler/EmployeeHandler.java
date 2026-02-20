package booking.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import booking.dto.EmployeeRequest;
import booking.dto.EmployeeResponse;
import booking.service.employee.EmployeeService;
import booking.service.employee.impl.EmployeeServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * AWS Lambda handler for all employee operations.
 *
 * <p>Routes requests based on HTTP method and path:
 * <ul>
 *   <li>POST   /employees                        → Register a new employee</li>
 *   <li>GET    /employees/{id}                    → Get employee by ID</li>
 *   <li>GET    /employees?email=X                 → Search by email</li>
 *   <li>GET    /employees?department=X            → Search by department</li>
 *   <li>PATCH  /employees/{id}/status             → Update employee status</li>
 *   <li>DELETE /employees/{id}                    → Delete employee</li>
 * </ul>
 */
public class EmployeeHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(EmployeeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private EmployeeService employeeService;

    public EmployeeHandler() {
        this.employeeService = new EmployeeServiceImpl();
    }

    /** Package-private setter for testing. */
    void setEmployeeService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        logger.info("EmployeeHandler invoked. Request ID: {}", context.getAwsRequestId());

        String httpMethod = request.getHttpMethod();
        String path = request.getPath();

        logger.info("Method: {}, Path: {}", httpMethod, path);

        try {
            return switch (httpMethod) {
                case "POST" -> handleRegisterEmployee(request);
                case "GET" -> handleGetEmployee(request);
                case "PATCH" -> handleUpdateStatus(request);
                case "DELETE" -> handleDeleteEmployee(request);
                default -> buildJsonResponse(HTTP_METHOD_NOT_ALLOWED,
                        new EmployeeResponse("METHOD_NOT_ALLOWED", null, "Method not allowed: " + httpMethod));
            };
        } catch (Exception e) {
            logger.error("Unexpected error in EmployeeHandler", e);
            return buildJsonResponse(HTTP_INTERNAL_SERVER_ERROR,
                    new EmployeeResponse("SYSTEM_ERROR", null, "An unexpected error occurred"));
        }
    }

    // ==================== POST /employees ====================

    private APIGatewayProxyResponseEvent handleRegisterEmployee(APIGatewayProxyRequestEvent request) {
        try {
            EmployeeRequest employeeRequest = parseBody(request.getBody(), EmployeeRequest.class);
            EmployeeResponse response = employeeService.registerEmployee(employeeRequest);

            int httpStatus = statusToHttpCode(response.getStatus(), true);
            return buildJsonResponse(httpStatus, response);

        } catch (InvalidRequestException e) {
            logger.warn("Invalid request format: {}", e.getMessage());
            return buildJsonResponse(HTTP_BAD_REQUEST,
                    new EmployeeResponse("INVALID_REQUEST", null, e.getMessage()));
        }
    }

    // ==================== GET /employees/{id} or /employees?email=X&department=X ====================

    private APIGatewayProxyResponseEvent handleGetEmployee(APIGatewayProxyRequestEvent request) {
        // Check path parameters first (GET /employees/{id})
        Map<String, String> pathParams = request.getPathParameters();
        if (pathParams != null && pathParams.containsKey("id")) {
            String employeeId = pathParams.get("id");
            EmployeeResponse response = employeeService.getEmployeeById(employeeId);

            int httpStatus = statusToHttpCode(response.getStatus(), false);
            return buildJsonResponse(httpStatus, response);
        }

        // Check query parameters
        Map<String, String> queryParams = request.getQueryStringParameters();
        if (queryParams != null) {
            if (queryParams.containsKey("email")) {
                List<EmployeeResponse> responses = employeeService.getEmployeesByEmail(queryParams.get("email"));
                return buildJsonListResponse(HTTP_OK, responses);
            }

            if (queryParams.containsKey("department")) {
                List<EmployeeResponse> responses = employeeService.getEmployeesByDepartment(queryParams.get("department"));
                return buildJsonListResponse(HTTP_OK, responses);
            }
        }

        // No params → return a hint
        return buildJsonResponse(HTTP_BAD_REQUEST,
                new EmployeeResponse("VALIDATION_ERROR", null,
                        "Provide an employee ID in path or email/department as query parameter"));
    }

    // ==================== PATCH /employees/{id}/status ====================

    private APIGatewayProxyResponseEvent handleUpdateStatus(APIGatewayProxyRequestEvent request) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return buildJsonResponse(HTTP_BAD_REQUEST,
                        new EmployeeResponse("VALIDATION_ERROR", null, "Employee ID is required in path"));
            }

            String employeeId = pathParams.get("id");

            // Parse body for the new status {"status": "INACTIVE"}
            @SuppressWarnings("unchecked")
            Map<String, String> body = parseBody(request.getBody(), Map.class);
            String newStatus = body != null ? body.get("status") : null;

            if (newStatus == null || newStatus.isBlank()) {
                return buildJsonResponse(HTTP_BAD_REQUEST,
                        new EmployeeResponse("VALIDATION_ERROR", employeeId, "Status field is required in body"));
            }

            EmployeeResponse response = employeeService.updateEmployeeStatus(employeeId, newStatus);

            int httpStatus = statusToHttpCode(response.getStatus(), false);
            return buildJsonResponse(httpStatus, response);

        } catch (InvalidRequestException e) {
            logger.warn("Invalid request format: {}", e.getMessage());
            return buildJsonResponse(HTTP_BAD_REQUEST,
                    new EmployeeResponse("INVALID_REQUEST", null, e.getMessage()));
        }
    }

    // ==================== DELETE /employees/{id} ====================

    private APIGatewayProxyResponseEvent handleDeleteEmployee(APIGatewayProxyRequestEvent request) {
        Map<String, String> pathParams = request.getPathParameters();
        if (pathParams == null || !pathParams.containsKey("id")) {
            return buildJsonResponse(HTTP_BAD_REQUEST,
                    new EmployeeResponse("VALIDATION_ERROR", null, "Employee ID is required in path"));
        }

        String employeeId = pathParams.get("id");
        EmployeeResponse response = employeeService.deleteEmployee(employeeId);

        int httpStatus = statusToHttpCode(response.getStatus(), false);
        return buildJsonResponse(httpStatus, response);
    }

    // ==================== Utility Methods ====================

    private <T> T parseBody(String body, Class<T> clazz) {
        try {
            if (body == null || body.isEmpty()) {
                throw new InvalidRequestException("Request body cannot be empty");
            }
            return objectMapper.readValue(body, clazz);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid request format: " + e.getMessage(), e);
        }
    }

    /**
     * Maps service status to HTTP code.
     *
     * @param status    The service response status
     * @param isCreate  Whether this is a creation operation (uses 201 for success)
     */
    private int statusToHttpCode(String status, boolean isCreate) {
        if (status == null) return HTTP_INTERNAL_SERVER_ERROR;
        return switch (status) {
            case "SUCCESS" -> isCreate ? HTTP_CREATED : HTTP_OK;
            case "NOT_FOUND" -> HTTP_NOT_FOUND;
            case "VALIDATION_ERROR" -> HTTP_BAD_REQUEST;
            case "CONFLICT" -> HTTP_CONFLICT;
            default -> HTTP_INTERNAL_SERVER_ERROR;
        };
    }

    private APIGatewayProxyResponseEvent buildJsonResponse(int statusCode, Object response) {
        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(statusCode);
        apiResponse.setHeaders(Map.of(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON));

        try {
            apiResponse.setBody(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("Failed to serialize response", e);
            apiResponse.setBody("{\"status\":\"SYSTEM_ERROR\",\"message\":\"Failed to serialize response\"}");
        }

        return apiResponse;
    }

    private APIGatewayProxyResponseEvent buildJsonListResponse(int statusCode, List<?> responses) {
        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(statusCode);
        apiResponse.setHeaders(Map.of(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON));

        try {
            apiResponse.setBody(objectMapper.writeValueAsString(responses));
        } catch (Exception e) {
            logger.error("Failed to serialize list response", e);
            apiResponse.setBody("{\"status\":\"SYSTEM_ERROR\",\"message\":\"Failed to serialize response\"}");
        }

        return apiResponse;
    }

    /** Custom exception for invalid request format errors. */
    static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }

        public InvalidRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

