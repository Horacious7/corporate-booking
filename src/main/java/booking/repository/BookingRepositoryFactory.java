package booking.repository;

import booking.repository.impl.DynamoDBBookingRepository;
import booking.repository.impl.InMemoryBookingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory class for creating BookingRepository instances.
 *
 * <p>This factory provides a centralized way to obtain the appropriate
 * repository implementation based on the runtime environment. It supports:
 * <ul>
 *   <li>DynamoDB: Production environment with AWS infrastructure</li>
 *   <li>In-Memory: Local development and testing</li>
 * </ul>
 *
 * <p>Configuration is driven by the {@code REPOSITORY_TYPE} environment variable:
 * <ul>
 *   <li>{@code DYNAMODB}: Uses DynamoDB (default for production)</li>
 *   <li>{@code INMEMORY}: Uses in-memory store (for testing/development)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * BookingRepository repository = BookingRepositoryFactory.create();
 * </pre>
 */
public class BookingRepositoryFactory {

    private static final Logger logger = LogManager.getLogger(BookingRepositoryFactory.class);

    private static final String ENV_REPOSITORY_TYPE = "REPOSITORY_TYPE";
    private static final String TYPE_DYNAMODB = "DYNAMODB";
    private static final String TYPE_INMEMORY = "INMEMORY";

    // Singleton instance for reuse across Lambda invocations
    private static volatile BookingRepository instance;

    private BookingRepositoryFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates or returns the singleton BookingRepository instance.
     *
     * <p>The repository type is determined by the REPOSITORY_TYPE environment variable.
     * If not set, defaults to DynamoDB for production use.
     *
     * <p>The instance is created lazily and cached for reuse, which is optimal
     * for AWS Lambda's execution model where the same instance should be reused
     * across invocations in the same container.
     *
     * @return The configured BookingRepository instance
     */
    public static BookingRepository create() {
        if (instance == null) {
            synchronized (BookingRepositoryFactory.class) {
                if (instance == null) {
                    instance = createInstance();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new BookingRepository instance based on environment configuration.
     *
     * @return A new BookingRepository instance
     */
    private static BookingRepository createInstance() {
        String repositoryType = System.getenv().getOrDefault(ENV_REPOSITORY_TYPE, TYPE_DYNAMODB);

        logger.info("Creating BookingRepository of type: {}", repositoryType);

        return switch (repositoryType.toUpperCase()) {
            case TYPE_INMEMORY -> {
                logger.info("Using InMemory repository (for development/testing)");
                yield new InMemoryBookingRepository();
            }
            case TYPE_DYNAMODB -> {
                logger.info("Using DynamoDB repository (production)");
                yield new DynamoDBBookingRepository();
            }
            default -> {
                logger.warn("Unknown repository type '{}', defaulting to DynamoDB", repositoryType);
                yield new DynamoDBBookingRepository();
            }
        };
    }

    /**
     * Creates an in-memory repository instance.
     *
     * <p>Convenience method for testing scenarios where an in-memory
     * repository is explicitly needed regardless of environment configuration.
     *
     * @return A new InMemoryBookingRepository instance
     */
    public static BookingRepository createInMemory() {
        logger.debug("Creating explicit InMemory repository instance");
        return new InMemoryBookingRepository();
    }

    /**
     * Creates a DynamoDB repository instance.
     *
     * <p>Convenience method for scenarios where a DynamoDB repository
     * is explicitly needed regardless of environment configuration.
     *
     * @return A new DynamoDBBookingRepository instance
     */
    public static BookingRepository createDynamoDB() {
        logger.debug("Creating explicit DynamoDB repository instance");
        return new DynamoDBBookingRepository();
    }

    /**
     * Creates a DynamoDB repository with a custom table name.
     *
     * @param tableName The DynamoDB table name to use
     * @return A new DynamoDBBookingRepository instance configured for the specified table
     */
    public static BookingRepository createDynamoDB(String tableName) {
        logger.debug("Creating DynamoDB repository for table: {}", tableName);
        return new DynamoDBBookingRepository(tableName);
    }

    /**
     * Resets the singleton instance.
     *
     * <p><strong>Warning:</strong> This method should only be used in testing
     * scenarios to reset the factory state between tests.
     */
    public static void reset() {
        synchronized (BookingRepositoryFactory.class) {
            instance = null;
            logger.debug("Repository factory singleton reset");
        }
    }
}

