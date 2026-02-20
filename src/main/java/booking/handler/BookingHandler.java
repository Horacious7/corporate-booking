package booking.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import booking.dto.BookingRequest;
import booking.dto.BookingResponse;
import booking.service.booking.BookingService;
import booking.service.booking.impl.BookingServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * AWS Lambda handler for all booking operations.
 *
 * <p>Routes requests based on HTTP method and path:
 * <ul>
 *   <li>POST   /bookings              → Create a new booking</li>
 *   <li>GET    /bookings/{id}         → Get booking by reference ID</li>
 *   <li>GET    /bookings?employeeId=X → Get bookings for employee</li>
 *   <li>PATCH  /bookings/{id}/status  → Update booking status (or cancel)</li>
 * </ul>
 *
 * <p>Performance optimizations for 100 TPS:
 * <ul>
 *   <li>Stateless design: Each invocation is independent</li>
 *   <li>Service/repo instances reused across warm invocations</li>
 *   <li>Efficient JSON parsing with Jackson</li>
 *   <li>Log4j2 for fast asynchronous logging</li>
 * </ul>
 */
public class BookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(BookingHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private BookingService bookingService;

    public BookingHandler() {
        this.bookingService = new BookingServiceImpl();
    }

    /** Package-private setter for testing. */
    void setBookingService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        logger.info("BookingHandler invoked. Request ID: {}", context.getAwsRequestId());

        String httpMethod = request.getHttpMethod();
        String path = request.getPath();

        logger.info("Method: {}, Path: {}", httpMethod, path);

        try {
            return switch (httpMethod) {
                case "POST" -> handleCreateBooking(request);
                case "GET" -> handleGetBooking(request);
                case "PATCH" -> handleUpdateStatus(request);
                default -> buildJsonResponse(HTTP_METHOD_NOT_ALLOWED,
                        new BookingResponse("METHOD_NOT_ALLOWED", null, "Method not allowed: " + httpMethod));
            };
        } catch (Exception e) {
            logger.error("Unexpected error in BookingHandler", e);
            return buildJsonResponse(HTTP_INTERNAL_SERVER_ERROR,
                    new BookingResponse("SYSTEM_ERROR", null, "An unexpected error occurred"));
        }
    }

    // ==================== POST /bookings ====================

    private APIGatewayProxyResponseEvent handleCreateBooking(APIGatewayProxyRequestEvent request) {
        try {
            BookingRequest bookingRequest = parseBody(request.getBody(), BookingRequest.class);
            BookingResponse response = bookingService.createBooking(bookingRequest);

            int httpStatus = "SUCCESS".equals(response.getStatus()) ? HTTP_OK : HTTP_BAD_REQUEST;
            return buildJsonResponse(httpStatus, response);

        } catch (InvalidRequestException e) {
            logger.warn("Invalid request format: {}", e.getMessage());
            return buildJsonResponse(HTTP_BAD_REQUEST,
                    new BookingResponse("INVALID_REQUEST", null, e.getMessage()));
        }
    }

    // ==================== GET /bookings/{id} or /bookings?employeeId=X ====================

    private APIGatewayProxyResponseEvent handleGetBooking(APIGatewayProxyRequestEvent request) {
        // Check path parameters first (GET /bookings/{id})
        Map<String, String> pathParams = request.getPathParameters();
        if (pathParams != null && pathParams.containsKey("id")) {
            String bookingRefId = pathParams.get("id");
            BookingResponse response = bookingService.getBookingByReferenceId(bookingRefId);

            int httpStatus = statusToHttpCode(response.getStatus());
            return buildJsonResponse(httpStatus, response);
        }

        // Check query parameters (GET /bookings?employeeId=X)
        Map<String, String> queryParams = request.getQueryStringParameters();
        if (queryParams != null && queryParams.containsKey("employeeId")) {
            String employeeId = queryParams.get("employeeId");
            List<BookingResponse> responses = bookingService.getBookingsByEmployeeId(employeeId);
            return buildJsonListResponse(HTTP_OK, responses);
        }

        // No params → list all bookings
        List<BookingResponse> allBookings = bookingService.getAllBookings();
        return buildJsonListResponse(HTTP_OK, allBookings);
    }

    // ==================== PATCH /bookings/{id}/status ====================

    private APIGatewayProxyResponseEvent handleUpdateStatus(APIGatewayProxyRequestEvent request) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            if (pathParams == null || !pathParams.containsKey("id")) {
                return buildJsonResponse(HTTP_BAD_REQUEST,
                        new BookingResponse("VALIDATION_ERROR", null, "Booking reference ID is required in path"));
            }

            String bookingRefId = pathParams.get("id");

            // Parse body for the new status {"status": "CANCELLED"}
            Map<String, String> body = parseBody(request.getBody(), Map.class);
            String newStatus = body != null ? body.get("status") : null;

            if (newStatus == null || newStatus.isBlank()) {
                return buildJsonResponse(HTTP_BAD_REQUEST,
                        new BookingResponse("VALIDATION_ERROR", bookingRefId, "Status field is required in body"));
            }

            BookingResponse response = bookingService.updateBookingStatus(bookingRefId, newStatus);

            int httpStatus = statusToHttpCode(response.getStatus());
            return buildJsonResponse(httpStatus, response);

        } catch (InvalidRequestException e) {
            logger.warn("Invalid request format: {}", e.getMessage());
            return buildJsonResponse(HTTP_BAD_REQUEST,
                    new BookingResponse("INVALID_REQUEST", null, e.getMessage()));
        }
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

    private int statusToHttpCode(String status) {
        if (status == null) return HTTP_INTERNAL_SERVER_ERROR;
        return switch (status) {
            case "SUCCESS" -> HTTP_OK;
            case "NOT_FOUND" -> HTTP_NOT_FOUND;
            case "VALIDATION_ERROR", "CONFLICT" -> HTTP_BAD_REQUEST;
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

