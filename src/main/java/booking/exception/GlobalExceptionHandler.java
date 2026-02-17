package booking.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import booking.dto.BookingResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Global exception handler for the booking application.
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles JSON parsing and deserialization exceptions.
     *
     * <p>Called when the request body cannot be parsed as valid JSON
     * or deserialized to a BookingRequest object.
     *
     * @param exception the exception that occurred during parsing
     * @param errorCode the error code to include in the response
     * @return a JSON string representation of the error response
     */
    public static String handleJsonException(Exception exception, String errorCode) {
        logger.error("JSON parsing error: {}", exception.getMessage());
        return buildErrorResponseJson(
            "INVALID_REQUEST",
            "Invalid JSON format: " + exception.getMessage(),
            errorCode
        );
    }

    /**
     * Handles validation and business logic exceptions.
     *
     * <p>Called when a booking request fails validation or violates
     * business rules.
     *
     * @param exception the validation exception that occurred
     * @return a JSON string representation of the validation error response
     */
    public static String handleValidationException(Exception exception) {
        logger.warn("Validation error: {}", exception.getMessage());
        return buildErrorResponseJson(
            "VALIDATION_ERROR",
            exception.getMessage(),
            "VALIDATION_FAILED"
        );
    }

    /**
     * Handles unexpected runtime and system exceptions.
     *
     * <p>Called for any unexpected errors that occur during request processing.
     * These errors are logged with full stack traces for debugging.
     *
     * @param exception the unexpected exception that occurred
     * @return a JSON string representation of the system error response
     */
    public static String handleSystemException(Exception exception) {
        logger.error("System error occurred", exception);
        return buildErrorResponseJson(
            "SYSTEM_ERROR",
            "An unexpected error occurred. Please contact support.",
            "INTERNAL_SERVER_ERROR"
        );
    }

    /**
     * Builds a standardized error response JSON string.
     *
     * <p>Creates a consistent error response format that includes:
     * <ul>
     *   <li>status: The operation status (always an error code)</li>
     *   <li>bookingReferenceId: Always null for errors</li>
     *   <li>message: Human-readable error description</li>
     *   <li>errorCode: Technical error code for logging and monitoring</li>
     *   <li>timestamp: ISO 8601 timestamp for traceability</li>
     * </ul>
     *
     * @param status the error status to return
     * @param message the human-readable error message
     * @param errorCode the technical error code for logging
     * @return a JSON string ready to be returned as the response body
     */
    private static String buildErrorResponseJson(String status, String message, String errorCode) {
        try {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("status", status);
            errorMap.put("bookingReferenceId", null);
            errorMap.put("message", message);
            errorMap.put("errorCode", errorCode);
            errorMap.put("timestamp", System.currentTimeMillis());

            return objectMapper.writeValueAsString(errorMap);
        } catch (IOException e) {
            logger.error("Failed to serialize error response", e);
            // Fallback response if JSON serialization fails
            return "{\"status\":\"SYSTEM_ERROR\",\"message\":\"An unexpected error occurred\"}";
        }
    }

    /**
     * Converts a BookingResponse object to JSON string.
     *
     * <p>Used to serialize successful booking responses to JSON format
     * suitable for API Gateway responses.
     *
     * @param response the BookingResponse to serialize
     * @return a JSON string representation of the response
     * @throws IOException if JSON serialization fails
     */
    public static String toJson(BookingResponse response) throws IOException {
        return objectMapper.writeValueAsString(response);
    }
}

