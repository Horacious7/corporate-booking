package booking.repository.employee;

import booking.repository.employee.impl.DynamoDBEmployeeRepository;
import booking.repository.employee.impl.InMemoryEmployeeRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory class for creating EmployeeRepository instances.
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
 * EmployeeRepository repository = EmployeeRepositoryFactory.create();
 * </pre>
 */
public class EmployeeRepositoryFactory {

    private static final Logger logger = LogManager.getLogger(EmployeeRepositoryFactory.class);

    private static final String ENV_REPOSITORY_TYPE = "REPOSITORY_TYPE";
    private static final String TYPE_DYNAMODB = "DYNAMODB";
    private static final String TYPE_INMEMORY = "INMEMORY";

    // Singleton instance for reuse across Lambda invocations
    private static volatile EmployeeRepository instance;

    private EmployeeRepositoryFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates or returns the singleton EmployeeRepository instance.
     *
     * <p>The repository type is determined by the REPOSITORY_TYPE environment variable.
     * If not set, defaults to DynamoDB for production use.
     *
     * <p>The instance is created lazily and cached for reuse, which is optimal
     * for AWS Lambda's execution model where the same instance should be reused
     * across invocations in the same container.
     *
     * @return The configured EmployeeRepository instance
     */
    public static EmployeeRepository create() {
        if (instance == null) {
            synchronized (EmployeeRepositoryFactory.class) {
                if (instance == null) {
                    instance = createInstance();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new EmployeeRepository instance based on environment configuration.
     *
     * @return A new EmployeeRepository instance
     */
    private static EmployeeRepository createInstance() {
        String repositoryType = System.getenv().getOrDefault(ENV_REPOSITORY_TYPE, TYPE_DYNAMODB);

        logger.info("Creating EmployeeRepository of type: {}", repositoryType);

        return switch (repositoryType.toUpperCase()) {
            case TYPE_INMEMORY -> {
                logger.info("Using InMemory employee repository (for development/testing)");
                yield new InMemoryEmployeeRepository();
            }
            case TYPE_DYNAMODB -> {
                logger.info("Using DynamoDB employee repository (production)");
                yield new DynamoDBEmployeeRepository();
            }
            default -> {
                logger.warn("Unknown repository type '{}', defaulting to DynamoDB", repositoryType);
                yield new DynamoDBEmployeeRepository();
            }
        };
    }

    /**
     * Creates an in-memory repository instance.
     *
     * <p>Convenience method for testing scenarios where an in-memory
     * repository is explicitly needed regardless of environment configuration.
     *
     * @return A new InMemoryEmployeeRepository instance
     */
    public static EmployeeRepository createInMemory() {
        logger.debug("Creating explicit InMemory employee repository instance");
        return new InMemoryEmployeeRepository();
    }

    /**
     * Creates a DynamoDB repository instance.
     *
     * <p>Convenience method for scenarios where a DynamoDB repository
     * is explicitly needed regardless of environment configuration.
     *
     * @return A new DynamoDBEmployeeRepository instance
     */
    public static EmployeeRepository createDynamoDB() {
        logger.debug("Creating explicit DynamoDB employee repository instance");
        return new DynamoDBEmployeeRepository();
    }

    /**
     * Creates a DynamoDB repository with a custom table name.
     *
     * @param tableName The DynamoDB table name to use
     * @return A new DynamoDBEmployeeRepository instance configured for the specified table
     */
    public static EmployeeRepository createDynamoDB(String tableName) {
        logger.debug("Creating DynamoDB employee repository for table: {}", tableName);
        return new DynamoDBEmployeeRepository(tableName);
    }

    /**
     * Resets the singleton instance.
     *
     * <p><strong>Warning:</strong> This method should only be used in testing
     * scenarios to reset the factory state between tests.
     */
    public static void reset() {
        synchronized (EmployeeRepositoryFactory.class) {
            instance = null;
            logger.debug("Employee repository factory singleton reset");
        }
    }
}

