package booking.service.employee.impl;

import booking.dto.EmployeeRequest;
import booking.dto.EmployeeResponse;
import booking.entity.Employee;
import booking.repository.employee.EmployeeRepository;
import booking.repository.employee.EmployeeRepositoryFactory;
import booking.repository.exception.EmployeePersistenceException;
import booking.service.employee.EmployeeService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of the EmployeeService interface that contains
 * the business logic for employee registration and management.
 *
 * <p>This service handles:
 * <ul>
 *   <li>Employee registration with validation</li>
 *   <li>Employee lookup by ID, email, and department</li>
 *   <li>Status updates (ACTIVE, INACTIVE)</li>
 *   <li>Employee deletion</li>
 * </ul>
 */
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger logger = LogManager.getLogger(EmployeeServiceImpl.class);

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";
    private static final String STATUS_CONFLICT = "CONFLICT";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final EmployeeRepository employeeRepository;

    /**
     * Default constructor - uses EmployeeRepositoryFactory to get the appropriate repository
     * based on environment configuration (DynamoDB for production, InMemory for testing).
     */
    public EmployeeServiceImpl() {
        this.employeeRepository = EmployeeRepositoryFactory.create();
    }

    /**
     * Constructor for dependency injection (useful for testing).
     *
     * @param employeeRepository The repository to use for persistence
     */
    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public EmployeeResponse registerEmployee(EmployeeRequest request) {
        // Check for null request first
        if (request == null) {
            logger.error("Employee request is null");
            return buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Employee request cannot be null");
        }

        logger.info("Processing employee registration for: {}", request.getEmployeeId());

        try {
            // Validate required fields
            validateEmployeeRequest(request);

            // Check if employee already exists
            if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
                logger.warn("Employee already exists: {}", request.getEmployeeId());
                return buildErrorResponse(STATUS_CONFLICT, request.getEmployeeId(),
                        "Employee with ID " + request.getEmployeeId() + " already exists");
            }

            // Build the Employee entity from the request
            Employee employee = Employee.builder()
                    .employeeId(request.getEmployeeId())
                    .name(request.getName())
                    .email(request.getEmail())
                    .department(request.getDepartment())
                    .costCenterRef(request.getCostCenterRef())
                    .build();

            // Persist the employee
            Employee savedEmployee = employeeRepository.save(employee);

            logger.info("Employee registered successfully: {}", savedEmployee.getEmployeeId());

            return buildSuccessResponse(savedEmployee, "Employee registered successfully");

        } catch (IllegalArgumentException e) {
            logger.error("Validation error in employee request: {}", e.getMessage());
            return buildErrorResponse(STATUS_VALIDATION_ERROR, request.getEmployeeId(), e.getMessage());
        } catch (EmployeePersistenceException e) {
            logger.error("Failed to persist employee: {}", e.getMessage(), e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, request.getEmployeeId(),
                    "Failed to save employee. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error processing employee registration", e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, request.getEmployeeId(),
                    "An unexpected error occurred processing your request");
        }
    }

    @Override
    public EmployeeResponse getEmployeeById(String employeeId) {
        logger.info("Looking up employee: {}", employeeId);

        try {
            if (isBlank(employeeId)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Employee ID is required");
            }

            Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                logger.debug("Employee not found: {}", employeeId);
                return buildErrorResponse(STATUS_NOT_FOUND, employeeId,
                        "Employee not found: " + employeeId);
            }

            Employee employee = employeeOpt.get();
            logger.info("Found employee: {}", employeeId);

            return buildSuccessResponse(employee, "Employee found: " + employee.getName());

        } catch (EmployeePersistenceException e) {
            logger.error("Failed to retrieve employee: {}", employeeId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, employeeId,
                    "Failed to retrieve employee. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error retrieving employee: {}", employeeId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, employeeId,
                    "An unexpected error occurred");
        }
    }

    @Override
    public List<EmployeeResponse> getEmployeesByEmail(String email) {
        logger.info("Searching employees by email: {}", email);

        try {
            List<Employee> employees = employeeRepository.findByEmail(email);

            return employees.stream()
                    .map(emp -> buildSuccessResponse(emp, "Employee: " + emp.getName() + " (" + emp.getDepartment() + ")"))
                    .collect(Collectors.toList());

        } catch (EmployeePersistenceException e) {
            logger.error("Failed to search employees by email: {}", email, e);
            return List.of(buildErrorResponse(STATUS_SYSTEM_ERROR, null,
                    "Failed to search employees. Please try again later."));
        }
    }

    @Override
    public List<EmployeeResponse> getEmployeesByDepartment(String department) {
        logger.info("Searching employees by department: {}", department);

        try {
            List<Employee> employees = employeeRepository.findByDepartment(department);

            return employees.stream()
                    .map(emp -> buildSuccessResponse(emp, "Employee: " + emp.getName() + " (" + emp.getEmail() + ")"))
                    .collect(Collectors.toList());

        } catch (EmployeePersistenceException e) {
            logger.error("Failed to search employees by department: {}", department, e);
            return List.of(buildErrorResponse(STATUS_SYSTEM_ERROR, null,
                    "Failed to search employees. Please try again later."));
        }
    }

    @Override
    public EmployeeResponse updateEmployeeStatus(String employeeId, String newStatus) {
        logger.info("Updating status for employee: {} to {}", employeeId, newStatus);

        try {
            if (isBlank(employeeId)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Employee ID is required");
            }

            if (isBlank(newStatus)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, employeeId, "New status is required");
            }

            // Validate status value
            if (!isValidStatus(newStatus)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, employeeId,
                        "Invalid status. Allowed values: ACTIVE, INACTIVE, SUSPENDED");
            }

            Optional<Employee> updatedOpt = employeeRepository.updateStatus(employeeId, newStatus.toUpperCase());

            if (updatedOpt.isEmpty()) {
                return buildErrorResponse(STATUS_NOT_FOUND, employeeId,
                        "Employee not found: " + employeeId);
            }

            Employee updated = updatedOpt.get();
            logger.info("Successfully updated employee {} status to {}", employeeId, newStatus);

            return buildSuccessResponse(updated, "Employee status updated to " + updated.getStatus());

        } catch (EmployeePersistenceException e) {
            logger.error("Failed to update employee status: {}", employeeId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, employeeId,
                    "Failed to update employee status. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error updating employee status: {}", employeeId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, employeeId,
                    "An unexpected error occurred");
        }
    }

    @Override
    public EmployeeResponse deleteEmployee(String employeeId) {
        logger.info("Deleting employee: {}", employeeId);

        try {
            if (isBlank(employeeId)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Employee ID is required");
            }

            boolean deleted = employeeRepository.deleteByEmployeeId(employeeId);

            if (!deleted) {
                return buildErrorResponse(STATUS_NOT_FOUND, employeeId,
                        "Employee not found: " + employeeId);
            }

            logger.info("Successfully deleted employee: {}", employeeId);

            return EmployeeResponse.builder()
                    .status(STATUS_SUCCESS)
                    .employeeId(employeeId)
                    .message("Employee deleted successfully")
                    .build();

        } catch (EmployeePersistenceException e) {
            logger.error("Failed to delete employee: {}", employeeId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, employeeId,
                    "Failed to delete employee. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error deleting employee: {}", employeeId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, employeeId,
                    "An unexpected error occurred");
        }
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        logger.info("Retrieving all employees");

        try {
            List<Employee> employees = employeeRepository.findAll();

            return employees.stream()
                    .map(emp -> buildSuccessResponse(emp, "Employee: " + emp.getName()))
                    .collect(Collectors.toList());

        } catch (EmployeePersistenceException e) {
            logger.error("Failed to retrieve all employees", e);
            return List.of(buildErrorResponse(STATUS_SYSTEM_ERROR, null,
                    "Failed to retrieve employees. Please try again later."));
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Builds a full success response from an Employee entity.
     */
    private EmployeeResponse buildSuccessResponse(Employee employee, String message) {
        return EmployeeResponse.builder()
                .status(STATUS_SUCCESS)
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .email(employee.getEmail())
                .department(employee.getDepartment())
                .costCenterRef(employee.getCostCenterRef())
                .employeeStatus(employee.getStatus())
                .message(message)
                .build();
    }

    /**
     * Validates that all required fields in the employee request are present and valid.
     */
    private void validateEmployeeRequest(EmployeeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Employee request cannot be null");
        }

        if (isBlank(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("Employee name is required");
        }

        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (isBlank(request.getDepartment())) {
            throw new IllegalArgumentException("Department is required");
        }

        if (isBlank(request.getCostCenterRef())) {
            throw new IllegalArgumentException("Cost center reference is required");
        }
    }

    /**
     * Validates if the given status is an allowed value.
     */
    private boolean isValidStatus(String status) {
        if (status == null) return false;
        String upper = status.toUpperCase();
        return "ACTIVE".equals(upper) || "INACTIVE".equals(upper) || "SUSPENDED".equals(upper);
    }

    /**
     * Builds a standardized error response with the given status and message.
     */
    private EmployeeResponse buildErrorResponse(String status, String employeeId, String message) {
        return EmployeeResponse.builder()
                .status(status)
                .employeeId(employeeId)
                .message(message)
                .build();
    }

    /**
     * Check if a string is null or empty after trimming whitespace.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
