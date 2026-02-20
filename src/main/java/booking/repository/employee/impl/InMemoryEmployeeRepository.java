package booking.repository.employee.impl;

import booking.entity.Employee;
import booking.repository.employee.EmployeeRepository;
import booking.repository.exception.EmployeePersistenceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of EmployeeRepository for testing and local development.
 *
 * <p>This implementation stores employees in a ConcurrentHashMap, making it
 * thread-safe for concurrent access. It's ideal for:
 * <ul>
 *   <li>Unit and integration testing without AWS dependencies</li>
 *   <li>Local development and debugging</li>
 *   <li>Prototype and demo purposes</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Data is not persisted between application restarts.
 * Use {@link booking.repository.employee.impl.DynamoDBEmployeeRepository} for production persistence.
 */
public class InMemoryEmployeeRepository implements EmployeeRepository {

    private static final Logger logger = LogManager.getLogger(InMemoryEmployeeRepository.class);
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final Map<String, Employee> employeeStore = new ConcurrentHashMap<>();

    /**
     * Default constructor.
     */
    public InMemoryEmployeeRepository() {
        logger.info("Initialized InMemory employee repository");
    }

    @Override
    public Employee save(Employee employee) {
        try {
            if (employee == null) {
                throw new IllegalArgumentException("Employee cannot be null");
            }

            if (employee.getEmployeeId() == null || employee.getEmployeeId().isBlank()) {
                throw new IllegalArgumentException("Employee ID cannot be null or blank");
            }

            logger.debug("Saving employee: {}", employee.getEmployeeId());

            // Set timestamps
            String now = Instant.now().toString();
            if (employee.getCreatedAt() == null) {
                employee.setCreatedAt(now);
                employee.setStatus(STATUS_ACTIVE);
            }
            employee.setUpdatedAt(now);

            // Store the employee
            employeeStore.put(employee.getEmployeeId(), employee);

            logger.info("Successfully saved employee: {}", employee.getEmployeeId());
            return employee;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to save employee: {}", employee.getEmployeeId(), e);
            throw new EmployeePersistenceException("Failed to save employee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Employee> findByEmployeeId(String employeeId) {
        try {
            logger.debug("Finding employee by ID: {}", employeeId);

            Employee employee = employeeStore.get(employeeId);

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

            List<Employee> employees = employeeStore.values().stream()
                    .filter(emp -> email.equals(emp.getEmail()))
                    .collect(Collectors.toList());

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

            List<Employee> employees = employeeStore.values().stream()
                    .filter(emp -> department.equals(emp.getDepartment()))
                    .collect(Collectors.toList());

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
            logger.debug("Retrieving all employees");

            List<Employee> employees = new ArrayList<>(employeeStore.values());

            logger.info("Found {} total employees", employees.size());
            return employees;

        } catch (Exception e) {
            logger.error("Failed to retrieve all employees", e);
            throw new EmployeePersistenceException("Failed to retrieve employees: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByEmployeeId(String employeeId) {
        try {
            logger.debug("Deleting employee: {}", employeeId);

            Employee removed = employeeStore.remove(employeeId);

            if (removed != null) {
                logger.info("Successfully deleted employee: {}", employeeId);
                return true;
            } else {
                logger.debug("Employee not found for deletion: {}", employeeId);
                return false;
            }

        } catch (Exception e) {
            logger.error("Failed to delete employee: {}", employeeId, e);
            throw new EmployeePersistenceException("Failed to delete employee: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Employee> updateStatus(String employeeId, String newStatus) {
        try {
            logger.debug("Updating status for employee: {} to {}", employeeId, newStatus);

            Employee employee = employeeStore.get(employeeId);

            if (employee == null) {
                logger.debug("Employee not found for status update: {}", employeeId);
                return Optional.empty();
            }

            employee.setStatus(newStatus);
            employee.setUpdatedAt(Instant.now().toString());

            logger.info("Successfully updated employee {} status to {}", employeeId, newStatus);
            return Optional.of(employee);

        } catch (Exception e) {
            logger.error("Failed to update employee status: {}", employeeId, e);
            throw new EmployeePersistenceException("Failed to update employee status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByEmployeeId(String employeeId) {
        return employeeStore.containsKey(employeeId);
    }

    @Override
    public long count() {
        return employeeStore.size();
    }

    /**
     * Clears all employees from the in-memory store.
     * Useful for testing to reset state between tests.
     */
    public void clear() {
        employeeStore.clear();
        logger.info("Cleared all employees from in-memory store");
    }

    /**
     * Returns the current count of employees in the store.
     *
     * @return Total number of stored employees
     */
    public int size() {
        return employeeStore.size();
    }
}


