package booking.repository.impl;

import booking.entity.Booking;
import booking.repository.BookingPersistenceException;
import booking.repository.BookingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.regions.Region;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DynamoDB implementation of the BookingRepository interface.
 *
 * <p>This implementation uses the AWS SDK v2 Enhanced DynamoDB Client
 * for type-safe operations against a DynamoDB table. It provides
 * efficient data access patterns optimized for AWS Lambda execution.
 *
 * <p>Table Configuration:
 * <ul>
 *   <li>Table Name: Configurable via environment variable or constructor</li>
 *   <li>Partition Key: bookingReferenceId</li>
 *   <li>GSI: employee-index (Partition Key: employeeId)</li>
 * </ul>
 *
 * <p>Performance Considerations:
 * <ul>
 *   <li>Connection reuse: DynamoDB client is reused across invocations</li>
 *   <li>GSI queries: Used for employee-based lookups to avoid scans</li>
 *   <li>Pagination: Large result sets are handled automatically</li>
 * </ul>
 */
public class DynamoDBBookingRepository implements BookingRepository {

    private static final Logger logger = LogManager.getLogger(DynamoDBBookingRepository.class);

    private static final String DEFAULT_TABLE_NAME = "corporate-bookings";
    private static final String EMPLOYEE_INDEX_NAME = "employee-index";
    private static final String STATUS_PENDING = "PENDING";

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Booking> bookingTable;
    private final String tableName;

    /**
     * Default constructor using environment configuration.
     *
     * <p>Reads table name from DYNAMODB_TABLE_NAME environment variable,
     * falls back to "corporate-bookings" if not set.
     */
    public DynamoDBBookingRepository() {
        this(System.getenv().getOrDefault("DYNAMODB_TABLE_NAME", DEFAULT_TABLE_NAME));
    }

    /**
     * Constructor with custom table name.
     *
     * @param tableName The DynamoDB table name to use
     */
    public DynamoDBBookingRepository(String tableName) {
        this.tableName = tableName;

        // Create DynamoDB client with region from environment or default
        String region = System.getenv().getOrDefault("AWS_REGION", "eu-central-1");
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .build();

        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        this.bookingTable = enhancedClient.table(tableName, TableSchema.fromBean(Booking.class));

        logger.info("Initialized DynamoDB repository with table: {} in region: {}", tableName, region);
    }

    /**
     * Constructor for dependency injection (useful for testing).
     *
     * @param enhancedClient Pre-configured DynamoDB Enhanced Client
     * @param tableName The DynamoDB table name
     */
    public DynamoDBBookingRepository(DynamoDbEnhancedClient enhancedClient, String tableName) {
        this.enhancedClient = enhancedClient;
        this.tableName = tableName;
        this.bookingTable = enhancedClient.table(tableName, TableSchema.fromBean(Booking.class));

        logger.info("Initialized DynamoDB repository with injected client, table: {}", tableName);
    }

    @Override
    public Booking save(Booking booking) {
        try {
            logger.debug("Saving booking: {}", booking.getBookingReferenceId());

            // Set timestamps
            String now = Instant.now().toString();
            if (booking.getCreatedAt() == null) {
                booking.setCreatedAt(now);
                booking.setStatus(STATUS_PENDING);
            }
            booking.setUpdatedAt(now);

            // Put item (creates or replaces)
            bookingTable.putItem(booking);

            logger.info("Successfully saved booking: {}", booking.getBookingReferenceId());
            return booking;

        } catch (Exception e) {
            logger.error("Failed to save booking: {}", booking.getBookingReferenceId(), e);
            throw new BookingPersistenceException("Failed to save booking: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Booking> findByBookingReferenceId(String bookingReferenceId) {
        try {
            logger.debug("Finding booking by reference: {}", bookingReferenceId);

            Key key = Key.builder()
                    .partitionValue(bookingReferenceId)
                    .build();

            Booking booking = bookingTable.getItem(key);

            if (booking != null) {
                logger.debug("Found booking: {}", bookingReferenceId);
            } else {
                logger.debug("Booking not found: {}", bookingReferenceId);
            }

            return Optional.ofNullable(booking);

        } catch (Exception e) {
            logger.error("Failed to find booking: {}", bookingReferenceId, e);
            throw new BookingPersistenceException("Failed to find booking: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Booking> findByEmployeeId(String employeeId) {
        try {
            logger.debug("Finding bookings for employee: {}", employeeId);

            DynamoDbIndex<Booking> employeeIndex = bookingTable.index(EMPLOYEE_INDEX_NAME);

            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder().partitionValue(employeeId).build());

            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .build();

            List<Booking> bookings = new ArrayList<>();
            employeeIndex.query(request)
                    .forEach(page -> bookings.addAll(page.items()));

            logger.info("Found {} bookings for employee: {}", bookings.size(), employeeId);
            return bookings;

        } catch (Exception e) {
            logger.error("Failed to find bookings for employee: {}", employeeId, e);
            throw new BookingPersistenceException("Failed to query bookings by employee: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Booking> findAll() {
        try {
            logger.debug("Scanning all bookings (use with caution)");

            List<Booking> bookings = new ArrayList<>();
            bookingTable.scan().forEach(page -> bookings.addAll(page.items()));

            logger.info("Found {} total bookings", bookings.size());
            return bookings;

        } catch (Exception e) {
            logger.error("Failed to scan bookings", e);
            throw new BookingPersistenceException("Failed to scan bookings: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByBookingReferenceId(String bookingReferenceId) {
        try {
            logger.debug("Deleting booking: {}", bookingReferenceId);

            // First check if exists
            Optional<Booking> existing = findByBookingReferenceId(bookingReferenceId);
            if (existing.isEmpty()) {
                logger.debug("Booking not found for deletion: {}", bookingReferenceId);
                return false;
            }

            Key key = Key.builder()
                    .partitionValue(bookingReferenceId)
                    .build();

            bookingTable.deleteItem(key);

            logger.info("Successfully deleted booking: {}", bookingReferenceId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to delete booking: {}", bookingReferenceId, e);
            throw new BookingPersistenceException("Failed to delete booking: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Booking> updateStatus(String bookingReferenceId, String newStatus) {
        try {
            logger.debug("Updating status for booking: {} to {}", bookingReferenceId, newStatus);

            Optional<Booking> existingOpt = findByBookingReferenceId(bookingReferenceId);

            if (existingOpt.isEmpty()) {
                logger.debug("Booking not found for status update: {}", bookingReferenceId);
                return Optional.empty();
            }

            Booking booking = existingOpt.get();
            booking.setStatus(newStatus);
            booking.setUpdatedAt(Instant.now().toString());

            bookingTable.putItem(booking);

            logger.info("Successfully updated booking {} status to {}", bookingReferenceId, newStatus);
            return Optional.of(booking);

        } catch (Exception e) {
            logger.error("Failed to update booking status: {}", bookingReferenceId, e);
            throw new BookingPersistenceException("Failed to update booking status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByBookingReferenceId(String bookingReferenceId) {
        return findByBookingReferenceId(bookingReferenceId).isPresent();
    }

    @Override
    public long countByEmployeeId(String employeeId) {
        return findByEmployeeId(employeeId).size();
    }
}

