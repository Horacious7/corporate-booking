package booking.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import booking.dto.BookingRequest;
import booking.dto.BookingResponse;
import booking.exception.GlobalExceptionHandler;
import booking.service.booking.BookingService;
import booking.service.booking.impl.BookingServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AWS Lambda handler for processing booking creation requests.
 *
 * <p>This is the entry point for the Lambda function invoked by API Gateway.
 * It implements the RequestHandler interface, making it directly callable
 * by the AWS Lambda runtime.
 *
 * <p>Request flow:
 * <ol>
 *   <li>API Gateway receives HTTP POST request at /booking</li>
 *   <li>API Gateway invokes this Lambda handler</li>
 *   <li>Handler deserializes JSON body to BookingRequest</li>
 *   <li>Handler delegates to BookingService</li>
 *   <li>Handler serializes response and returns to API Gateway</li>
 *   <li>API Gateway returns HTTP response to client</li>
 * </ol>
 *
 * <p>Performance optimizations for 100 TPS:
 * <ul>
 *   <li>Stateless design: Each invocation is independent</li>
 *   <li>Efficient JSON parsing with Jackson</li>
 *   <li>Minimal dependencies to reduce cold start time</li>
 *   <li>Log4j2 for fast asynchronous logging</li>
 *   <li>JVM tuning in template.yaml for Lambda environments</li>
 * </ul>
 *
 * @author TechQuarter Engineering
 * @version 1.0.0
 */
public class CreateBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(CreateBookingHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private BookingService bookingService;

    public CreateBookingHandler() {
        this.bookingService = new BookingServiceImpl();
    }

    // Package-private setter for testing
    void setBookingService(BookingService bookingService) {
        this.bookingService = bookingService;
    }
    // HTTP Status Codes
    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    // Content Type Header
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * Handles an API Gateway proxy request for booking creation.
     *
     * <p>This method is invoked by the AWS Lambda runtime when a POST request
     * is received at /booking. It orchestrates the full request-response cycle:
     *
     * <ol>
     *   <li>Logs the incoming request for traceability</li>
     *   <li>Deserializes the JSON body to a BookingRequest object</li>
     *   <li>Validates the request (handled by BookingService)</li>
     *   <li>Creates the booking via BookingService</li>
     *   <li>Serializes the response to JSON</li>
     *   <li>Returns appropriate HTTP status code</li>
     * </ol>
     *
     * <p>Error handling:
     * <ul>
     *   <li>Invalid JSON: Returns 400 with INVALID_REQUEST error</li>
     *   <li>Validation failure: Returns 400 with VALIDATION_ERROR</li>
     *   <li>System errors: Returns 500 with SYSTEM_ERROR</li>
     * </ul>
     *
     * @param request the API Gateway proxy request containing the booking data
     * @param context the Lambda invocation context with execution metadata
     * @return an API Gateway proxy response with the booking result or error
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        logger.info("CreateBookingHandler invoked. Request ID: {}", context.getAwsRequestId());
        logger.debug("Incoming request body: {}", request.getBody());

        try {
            // Parse the JSON request body into a BookingRequest object
            BookingRequest bookingRequest = parseRequest(request.getBody());

            // Delegate to the BookingService for business logic
            BookingResponse bookingResponse = bookingService.createBooking(bookingRequest);

            // Determine HTTP status based on response status
            int httpStatus = "SUCCESS".equals(bookingResponse.getStatus())
                ? HTTP_OK
                : HTTP_BAD_REQUEST;

            logger.info("Booking processed successfully. Reference: {}",
                bookingResponse.getBookingReferenceId());

            // Return the response
            return buildResponse(httpStatus, bookingResponse);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return buildErrorResponse(
                HTTP_BAD_REQUEST,
                GlobalExceptionHandler.handleValidationException(e)
            );
        } catch (InvalidRequestException e) {
            logger.warn("Invalid request format: {}", e.getMessage());
            return buildErrorResponse(
                HTTP_BAD_REQUEST,
                GlobalExceptionHandler.handleJsonException(e, "INVALID_REQUEST")
            );
        } catch (Exception e) {
            logger.error("Unexpected error processing booking", e);
            return buildErrorResponse(
                HTTP_INTERNAL_SERVER_ERROR,
                GlobalExceptionHandler.handleSystemException(e)
            );
        }
    }

    /**
     * Parses the JSON request body into a BookingRequest object.
     *
     * <p>Uses Jackson ObjectMapper to deserialize the JSON string into
     * a strongly-typed BookingRequest object. This provides type safety
     * and automatic validation against the class structure.
     *
     * @param body the JSON body string from the HTTP request
     * @return a fully constructed BookingRequest object
     * @throws IllegalArgumentException if JSON parsing or deserialization fails
     */
    private BookingRequest parseRequest(String body) {
        try {
            if (body == null || body.isEmpty()) {
                throw new InvalidRequestException("Request body cannot be empty");
            }
            return objectMapper.readValue(body, BookingRequest.class);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRequestException(
                "Invalid request format: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Custom exception for invalid request format errors.
     */
    private static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }

        public InvalidRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Builds a successful HTTP response containing the booking result.
     *
     * <p>Constructs an API Gateway proxy response with:
     * <ul>
     *   <li>Specified HTTP status code</li>
     *   <li>JSON Content-Type header</li>
     *   <li>Serialized BookingResponse as body</li>
     * </ul>
     *
     * @param statusCode the HTTP status code to return
     * @param response the BookingResponse to include in the body
     * @return an APIGatewayProxyResponseEvent ready to be returned to API Gateway
     */
    private APIGatewayProxyResponseEvent buildResponse(
            int statusCode,
            BookingResponse response) {

        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(Integer.valueOf(statusCode));
        apiResponse.setHeaders(java.util.Map.of(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON));

        try {
            apiResponse.setBody(GlobalExceptionHandler.toJson(response));
        } catch (Exception e) {
            logger.error("Failed to serialize response", e);
            apiResponse.setBody("{\"status\":\"SYSTEM_ERROR\",\"message\":\"Failed to serialize response\"}");
        }

        return apiResponse;
    }

    /**
     * Builds an error HTTP response with the error details JSON.
     *
     * <p>Constructs an API Gateway proxy response with:
     * <ul>
     *   <li>Specified HTTP error status code</li>
     *   <li>JSON Content-Type header</li>
     *   <li>Error details JSON as body</li>
     * </ul>
     *
     * @param statusCode the HTTP error status code to return
     * @param errorBody the error response JSON body
     * @return an APIGatewayProxyResponseEvent ready to be returned to API Gateway
     */
    private APIGatewayProxyResponseEvent buildErrorResponse(String errorBody, int statusCode) {
        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(Integer.valueOf(statusCode));
        apiResponse.setHeaders(java.util.Map.of(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON));
        apiResponse.setBody(errorBody);
        return apiResponse;
    }

    /**
     * Overloaded version of buildErrorResponse for convenience.
     *
     * @param statusCode the HTTP status code
     * @param errorBody the error response JSON
     * @return the API response
     */
    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String errorBody) {
        return buildErrorResponse(errorBody, statusCode);
    }
}

