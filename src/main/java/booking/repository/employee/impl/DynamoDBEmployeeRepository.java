package booking.repository.employee.impl;

import booking.entity.Employee;
import booking.repository.employee.EmployeeRepository;
import booking.repository.exception.EmployeePersistenceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DynamoDB implementation of the EmployeeRepository interface.
 *
 * <p>This implementation uses the AWS SDK v2 Enhanced DynamoDB Client
 * for type-safe operations against a DynamoDB table. It provides
 * efficient data access patterns optimized for AWS Lambda execution.
 *
 * <p>Table Configuration:
 * <ul>
 *   <li>Table Name: Configurable via environment variable or constructor</li>
 *   <li>Partition Key: employeeId</li>
 *   <li>GSI: email-index (Partition Key: email)</li>
 * </ul>
 *
 * <p>Performance Considerations:
 * <ul>
 *   <li>Connection reuse: DynamoDB client is reused across invocations</li>
 *   <li>GSI queries: Used for email-based lookups to avoid scans</li>
 *   <li>Pagination: Large result sets are handled automatically</li>
 * </ul>
 */
public class DynamoDBEmployeeRepository implements EmployeeRepository {

    private static final Logger logger = LogManager.getLogger(DynamoDBEmployeeRepository.class);

    private static final String DEFAULT_TABLE_NAME = "corporate-employees";
    private static final String EMAIL_INDEX_NAME = "email-index";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Employee> employeeTable;
    private final String tableName;

    /**
     * Default constructor using environment configuration.
     *
     * <p>Reads table name from EMPLOYEES_TABLE_NAME environment variable,
     * falls back to "corporate-employees" if not set.
     */
    public DynamoDBEmployeeRepository() {
        this(System.getenv().getOrDefault("EMPLOYEES_TABLE_NAME", DEFAULT_TABLE_NAME));
    }

    /**
     * Constructor with custom table name.
     *
     * @param tableName The DynamoDB table name to use
     */
    public DynamoDBEmployeeRepository(String tableName) {
        this.tableName = tableName;

        // Create DynamoDB client with region from environment or default
        String region = System.getenv().getOrDefault("AWS_REGION", "eu-central-1");
        String endpoint = System.getenv("DYNAMODB_ENDPOINT");

        DynamoDbClient dynamoDbClient;
        if (endpoint != null && !endpoint.isEmpty()) {
            dynamoDbClient = DynamoDbClient.builder()
                    .region(Region.of(region))
                    .endpointOverride(URI.create(endpoint))
                    .build();
            logger.info("Using custom DynamoDB endpoint: {}", endpoint);
        } else {
            dynamoDbClient = DynamoDbClient.builder()
                    .region(Region.of(region))
                    .build();
        }

        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        this.employeeTable = enhancedClient.table(tableName, TableSchema.fromBean(Employee.class));

        logger.info("Initialized DynamoDB employee repository with table: {} in region: {}", tableName, region);
    }

    /**
     * Constructor for dependency injection (useful for testing).
     *
     * @param enhancedClient Pre-configured DynamoDB Enhanced Client
     * @param tableName The DynamoDB table name
     */
    public DynamoDBEmployeeRepository(DynamoDbEnhancedClient enhancedClient, String tableName) {
        this.enhancedClient = enhancedClient;
        this.tableName = tableName;
        this.employeeTable = enhancedClient.table(tableName, TableSchema.fromBean(Employee.class));

        logger.info("Initialized DynamoDB employee repository with injected client, table: {}", tableName);
    }

    @Override
    public Employee save(Employee employee) {
        try {
            logger.debug("Saving employee: {}", employee.getEmployeeId());

            // Set timestamps
            String now = Instant.now().toString();
            if (employee.getCreatedAt() == null) {
                employee.setCreatedAt(now);
                employee.setStatus(STATUS_ACTIVE);
            }
            employee.setUpdatedAt(now);

            // Put item (creates or replaces)
            employeeTable.putItem(employee);

            logger.info("Successfully saved employee: {}", employee.getEmployeeId());
            return employee;

        } catch (Exception e) {
            logger.error("Failed to save employee: {}", employee.getEmployeeId(), e);
            throw new EmployeePersistenceException("Failed to save employee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Employee> findByEmployeeId(String employeeId) {
        try {
            logger.debug("Finding employee by ID: {}", employeeId);

            Key key = Key.builder()
                    .partitionValue(employeeId)
                    .build();

            Employee employee = employeeTable.getItem(key);

            if (employee != null) {
                logger.debug("Found employee: {}", employeeId);
            } else {
                logger.debug("Employee not found: {}", employeeId);
            }

            return Optional.ofNullable(employee);

        } catch (Exception e) {
            logger.error("Failed to find employee: {}", employeeId, e);
            throw new EmployeePersistenceException("Failed to find employee: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Employee> findByEmail(String email) {
        try {
            logger.debug("Finding employees by email: {}", email);

            DynamoDbIndex<Employee> emailIndex = employeeTable.index(EMAIL_INDEX_NAME);

            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder().partitionValue(email).build());

            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .build();

            List<Employee> employees = new ArrayList<>();
            emailIndex.query(request)
                    .forEach(page -> employees.addAll(page.items()));

            logger.info("Found {} employees with email: {}", employees.size(), email);
            return employees;

        } catch (Exception e) {
            logger.error("Failed to find employees by email: {}", email, e);
            throw new EmployeePersistenceException("Failed to query employees by email: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Employee> findByDepartment(String department) {
        try {
            logger.debug("Finding employees by department: {}", department);

            // Department doesn't have a GSI, so we scan and filter
            List<Employee> employees = new ArrayList<>();
            employeeTable.scan().forEach(page ->
                    page.items().stream()
                            .filter(emp -> department.equals(emp.getDepartment()))
                            .forEach(employees::add)
            );

            logger.info("Found {} employees in department: {}", employees.size(), department);
            return employees;

        } catch (Exception e) {
            logger.error("Failed to find employees by department: {}", department, e);
            throw new EmployeePersistenceException("Failed to query employees by department: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Employee> findAll() {
        try {
            logger.debug("Scanning all employees (use with caution)");

            List<Employee> employees = new ArrayList<>();
            employeeTable.scan().forEach(page -> employees.addAll(page.items()));

            logger.info("Found {} total employees", employees.size());
            return employees;

        } catch (Exception e) {
            logger.error("Failed to scan employees", e);
            throw new EmployeePersistenceException("Failed to scan employees: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByEmployeeId(String employeeId) {
        try {
            logger.debug("Deleting employee: {}", employeeId);

            // First check if exists
            Optional<Employee> existing = findByEmployeeId(employeeId);
            if (existing.isEmpty()) {
                logger.debug("Employee not found for deletion: {}", employeeId);
                return false;
            }

            Key key = Key.builder()
                    .partitionValue(employeeId)
                    .build();

            employeeTable.deleteItem(key);

            logger.info("Successfully deleted employee: {}", employeeId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to delete employee: {}", employeeId, e);
            throw new EmployeePersistenceException("Failed to delete employee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Employee> updateStatus(String employeeId, String newStatus) {
        try {
            logger.debug("Updating status for employee: {} to {}", employeeId, newStatus);

            Optional<Employee> existingOpt = findByEmployeeId(employeeId);

            if (existingOpt.isEmpty()) {
                logger.debug("Employee not found for status update: {}", employeeId);
                return Optional.empty();
            }

            Employee employee = existingOpt.get();
            employee.setStatus(newStatus);
            employee.setUpdatedAt(Instant.now().toString());

            employeeTable.putItem(employee);

            logger.info("Successfully updated employee {} status to {}", employeeId, newStatus);
            return Optional.of(employee);

        } catch (Exception e) {
            logger.error("Failed to update employee status: {}", employeeId, e);
            throw new EmployeePersistenceException("Failed to update employee status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByEmployeeId(String employeeId) {
        return findByEmployeeId(employeeId).isPresent();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}

