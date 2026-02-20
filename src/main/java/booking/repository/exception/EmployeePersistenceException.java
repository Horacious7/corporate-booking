package booking.repository.exception;

/**
 * Custom exception for employee persistence operations.
 *
 * <p>This exception wraps underlying persistence errors (DynamoDB, network issues, etc.)
 * and provides a consistent exception type for the service layer to handle.
 *
 * <p>Usage example:
 * <pre>
 * try {
 *     repository.save(employee);
 * } catch (EmployeePersistenceException e) {
 *     logger.error("Failed to save employee: {}", e.getMessage());
 *     // Handle or rethrow as appropriate
 * }
 * </pre>
 */
public class EmployeePersistenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new persistence exception with the specified message.
     *
     * @param message The detail message
     */
    public EmployeePersistenceException(String message) {
        super(message);
    }

    /**
     * Constructs a new persistence exception with the specified message and cause.
     *
     * @param message The detail message
     * @param cause The underlying cause of the exception
     */
    public EmployeePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new persistence exception with the specified cause.
     *
     * @param cause The underlying cause of the exception
     */
    public EmployeePersistenceException(Throwable cause) {
        super(cause);
    }
}
