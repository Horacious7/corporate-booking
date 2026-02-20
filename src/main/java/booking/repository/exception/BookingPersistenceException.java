package booking.repository.exception;

/**
 * Custom exception for booking persistence operations.
 *
 * <p>This exception wraps underlying persistence errors (DynamoDB, network issues, etc.)
 * and provides a consistent exception type for the service layer to handle.
 *
 * <p>Usage example:
 * <pre>
 * try {
 *     repository.save(booking);
 * } catch (BookingPersistenceException e) {
 *     logger.error("Failed to save booking: {}", e.getMessage());
 *     // Handle or rethrow as appropriate
 * }
 * </pre>
 */
public class BookingPersistenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new persistence exception with the specified message.
     *
     * @param message The detail message
     */
    public BookingPersistenceException(String message) {
        super(message);
    }

    /**
     * Constructs a new persistence exception with the specified message and cause.
     *
     * @param message The detail message
     * @param cause The underlying cause of the exception
     */
    public BookingPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new persistence exception with the specified cause.
     *
     * @param cause The underlying cause of the exception
     */
    public BookingPersistenceException(Throwable cause) {
        super(cause);
    }
}

