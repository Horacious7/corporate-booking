package booking.service;

import booking.dto.EmployeeRequest;
import booking.dto.EmployeeResponse;
import booking.entity.Employee;
import booking.repository.employee.EmployeeRepository;
import booking.repository.employee.impl.InMemoryEmployeeRepository;
import booking.repository.exception.EmployeePersistenceException;
import booking.service.employee.EmployeeService;
import booking.service.employee.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmployeeServiceImpl.
 *
 * <p>Tests the business logic layer of the employee service including:
 * <ul>
 *   <li>Successful employee registration</li>
 *   <li>Input validation for all fields</li>
 *   <li>Email format validation</li>
 *   <li>Duplicate employee detection</li>
 *   <li>Employee lookup operations</li>
 *   <li>Status update logic</li>
 *   <li>Employee deletion</li>
 *   <li>Error handling and responses</li>
 *   <li>Repository persistence integration</li>
 * </ul>
 */
@DisplayName("EmployeeService Tests")
class EmployeeServiceTest {

    private EmployeeService employeeService;
    private InMemoryEmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository = new InMemoryEmployeeRepository();
        employeeService = new EmployeeServiceImpl(employeeRepository);
    }

    /**
     * Creates a valid EmployeeRequest for reuse across tests.
     */
    private EmployeeRequest createValidRequest() {
        return EmployeeRequest.builder()
                .employeeId("EMP001")
                .name("John Doe")
                .email("john.doe@techquarter.com")
                .department("Engineering")
                .costCenterRef("CC-100")
                .build();
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("Register Employee Operations")
    class RegisterEmployeeOperations {

        @Test
        @DisplayName("Should register employee successfully with valid request")
        void shouldRegisterEmployeeSuccessfully() {
            // Arrange
            EmployeeRequest request = createValidRequest();

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertNotNull(response);
            assertEquals("SUCCESS", response.getStatus());
            assertEquals("EMP001", response.getEmployeeId());
            assertEquals("Employee registered successfully", response.getMessage());
        }

        @Test
        @DisplayName("Should persist employee to repository on successful registration")
        void shouldPersistEmployeeToRepository() {
            // Arrange
            EmployeeRequest request = createValidRequest();

            // Act
            employeeService.registerEmployee(request);

            // Assert - verify employee is persisted
            assertTrue(employeeRepository.existsByEmployeeId("EMP001"));

            Optional<Employee> savedEmployee = employeeRepository.findByEmployeeId("EMP001");
            assertTrue(savedEmployee.isPresent());
            assertEquals("John Doe", savedEmployee.get().getName());
            assertEquals("john.doe@techquarter.com", savedEmployee.get().getEmail());
            assertEquals("Engineering", savedEmployee.get().getDepartment());
            assertEquals("CC-100", savedEmployee.get().getCostCenterRef());
            assertEquals("ACTIVE", savedEmployee.get().getStatus());
            assertNotNull(savedEmployee.get().getCreatedAt());
            assertNotNull(savedEmployee.get().getUpdatedAt());
        }

        @Test
        @DisplayName("Should return CONFLICT when employee already exists")
        void shouldReturnConflictWhenEmployeeAlreadyExists() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            employeeService.registerEmployee(request);

            // Act - try to register same employee again
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("CONFLICT", response.getStatus());
            assertEquals("EMP001", response.getEmployeeId());
            assertTrue(response.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Should return error when request is null")
        void shouldReturnErrorWhenRequestIsNull() {
            // Act
            EmployeeResponse response = employeeService.registerEmployee(null);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertNull(response.getEmployeeId());
            assertTrue(response.getMessage().contains("cannot be null"));
        }
    }

    // ==================== Validation Tests ====================

    @Nested
    @DisplayName("Validation Operations")
    class ValidationOperations {

        @Test
        @DisplayName("Should return error when employee ID is missing")
        void shouldReturnErrorWhenEmployeeIdIsMissing() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setEmployeeId("");

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Employee ID"));
        }

        @Test
        @DisplayName("Should return error when employee ID is null")
        void shouldReturnErrorWhenEmployeeIdIsNull() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setEmployeeId(null);

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Employee ID"));
        }

        @Test
        @DisplayName("Should return error when name is missing")
        void shouldReturnErrorWhenNameIsMissing() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setName("");

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("name"));
        }

        @Test
        @DisplayName("Should return error when email is missing")
        void shouldReturnErrorWhenEmailIsMissing() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setEmail("");

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Email"));
        }

        @Test
        @DisplayName("Should return error when email format is invalid")
        void shouldReturnErrorWhenEmailFormatIsInvalid() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setEmail("not-a-valid-email");

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Invalid email"));
        }

        @Test
        @DisplayName("Should return error when email has no domain")
        void shouldReturnErrorWhenEmailHasNoDomain() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setEmail("john@");

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Invalid email"));
        }

        @Test
        @DisplayName("Should return error when department is missing")
        void shouldReturnErrorWhenDepartmentIsMissing() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setDepartment(null);

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Department"));
        }

        @Test
        @DisplayName("Should return error when cost center reference is missing")
        void shouldReturnErrorWhenCostCenterRefIsMissing() {
            // Arrange
            EmployeeRequest request = createValidRequest();
            request.setCostCenterRef("");

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Cost center"));
        }

        @Test
        @DisplayName("Should not persist employee when validation fails")
        void shouldNotPersistEmployeeWhenValidationFails() {
            // Arrange - invalid request (missing name)
            EmployeeRequest request = createValidRequest();
            request.setName("");

            // Act
            EmployeeResponse response = employeeService.registerEmployee(request);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertEquals(0, employeeRepository.count());
        }
    }

    // ==================== Get Employee By ID Tests ====================

    @Nested
    @DisplayName("Get Employee By ID Operations")
    class GetEmployeeByIdOperations {

        @Test
        @DisplayName("Should find employee by ID")
        void shouldFindEmployeeById() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());

            // Act
            EmployeeResponse response = employeeService.getEmployeeById("EMP001");

            // Assert
            assertEquals("SUCCESS", response.getStatus());
            assertEquals("EMP001", response.getEmployeeId());
            assertTrue(response.getMessage().contains("John Doe"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND for non-existent employee")
        void shouldReturnNotFoundForNonExistentEmployee() {
            // Act
            EmployeeResponse response = employeeService.getEmployeeById("EMP999");

            // Assert
            assertEquals("NOT_FOUND", response.getStatus());
            assertEquals("EMP999", response.getEmployeeId());
            assertTrue(response.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should return VALIDATION_ERROR when employee ID is blank")
        void shouldReturnValidationErrorWhenEmployeeIdIsBlank() {
            // Act
            EmployeeResponse response = employeeService.getEmployeeById("");

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Employee ID is required"));
        }

        @Test
        @DisplayName("Should return VALIDATION_ERROR when employee ID is null")
        void shouldReturnValidationErrorWhenEmployeeIdIsNull() {
            // Act
            EmployeeResponse response = employeeService.getEmployeeById(null);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Employee ID is required"));
        }
    }

    // ==================== Search By Email Tests ====================

    @Nested
    @DisplayName("Get Employees By Email Operations")
    class GetEmployeesByEmailOperations {

        @Test
        @DisplayName("Should find employees by email")
        void shouldFindEmployeesByEmail() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());

            // Act
            List<EmployeeResponse> responses = employeeService.getEmployeesByEmail("john.doe@techquarter.com");

            // Assert
            assertEquals(1, responses.size());
            assertEquals("SUCCESS", responses.get(0).getStatus());
            assertEquals("EMP001", responses.get(0).getEmployeeId());
        }

        @Test
        @DisplayName("Should return empty list for non-existent email")
        void shouldReturnEmptyListForNonExistentEmail() {
            // Act
            List<EmployeeResponse> responses = employeeService.getEmployeesByEmail("nobody@example.com");

            // Assert
            assertTrue(responses.isEmpty());
        }
    }

    // ==================== Search By Department Tests ====================

    @Nested
    @DisplayName("Get Employees By Department Operations")
    class GetEmployeesByDepartmentOperations {

        @Test
        @DisplayName("Should find employees by department")
        void shouldFindEmployeesByDepartment() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());

            EmployeeRequest request2 = EmployeeRequest.builder()
                    .employeeId("EMP002")
                    .name("Jane Smith")
                    .email("jane.smith@techquarter.com")
                    .department("Engineering")
                    .costCenterRef("CC-100")
                    .build();
            employeeService.registerEmployee(request2);

            EmployeeRequest request3 = EmployeeRequest.builder()
                    .employeeId("EMP003")
                    .name("Bob Johnson")
                    .email("bob.johnson@techquarter.com")
                    .department("Marketing")
                    .costCenterRef("CC-200")
                    .build();
            employeeService.registerEmployee(request3);

            // Act
            List<EmployeeResponse> responses = employeeService.getEmployeesByDepartment("Engineering");

            // Assert
            assertEquals(2, responses.size());
            assertTrue(responses.stream().allMatch(r -> "SUCCESS".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Should return empty list for non-existent department")
        void shouldReturnEmptyListForNonExistentDepartment() {
            // Act
            List<EmployeeResponse> responses = employeeService.getEmployeesByDepartment("NonExistent");

            // Assert
            assertTrue(responses.isEmpty());
        }
    }

    // ==================== Update Status Tests ====================

    @Nested
    @DisplayName("Update Employee Status Operations")
    class UpdateEmployeeStatusOperations {

        @Test
        @DisplayName("Should update employee status to INACTIVE")
        void shouldUpdateStatusToInactive() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());

            // Act
            EmployeeResponse response = employeeService.updateEmployeeStatus("EMP001", "INACTIVE");

            // Assert
            assertEquals("SUCCESS", response.getStatus());
            assertEquals("EMP001", response.getEmployeeId());
            assertTrue(response.getMessage().contains("INACTIVE"));

            // Verify in repository
            Optional<Employee> updated = employeeRepository.findByEmployeeId("EMP001");
            assertTrue(updated.isPresent());
            assertEquals("INACTIVE", updated.get().getStatus());
        }

        @Test
        @DisplayName("Should update employee status to SUSPENDED")
        void shouldUpdateStatusToSuspended() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());

            // Act
            EmployeeResponse response = employeeService.updateEmployeeStatus("EMP001", "SUSPENDED");

            // Assert
            assertEquals("SUCCESS", response.getStatus());
            assertTrue(response.getMessage().contains("SUSPENDED"));
        }

        @Test
        @DisplayName("Should handle case-insensitive status values")
        void shouldHandleCaseInsensitiveStatus() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());

            // Act
            EmployeeResponse response = employeeService.updateEmployeeStatus("EMP001", "inactive");

            // Assert
            assertEquals("SUCCESS", response.getStatus());
            assertTrue(response.getMessage().contains("INACTIVE"));
        }

        @Test
        @DisplayName("Should return VALIDATION_ERROR for invalid status")
        void shouldReturnValidationErrorForInvalidStatus() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());

            // Act
            EmployeeResponse response = employeeService.updateEmployeeStatus("EMP001", "DELETED");

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Invalid status"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when updating non-existent employee")
        void shouldReturnNotFoundWhenUpdatingNonExistentEmployee() {
            // Act
            EmployeeResponse response = employeeService.updateEmployeeStatus("EMP999", "ACTIVE");

            // Assert
            assertEquals("NOT_FOUND", response.getStatus());
            assertTrue(response.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should return VALIDATION_ERROR when employee ID is blank")
        void shouldReturnValidationErrorWhenEmployeeIdIsBlank() {
            // Act
            EmployeeResponse response = employeeService.updateEmployeeStatus("", "ACTIVE");

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Employee ID is required"));
        }

        @Test
        @DisplayName("Should return VALIDATION_ERROR when status is blank")
        void shouldReturnValidationErrorWhenStatusIsBlank() {
            // Act
            EmployeeResponse response = employeeService.updateEmployeeStatus("EMP001", "");

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("New status is required"));
        }
    }

    // ==================== Delete Employee Tests ====================

    @Nested
    @DisplayName("Delete Employee Operations")
    class DeleteEmployeeOperations {

        @Test
        @DisplayName("Should delete employee successfully")
        void shouldDeleteEmployeeSuccessfully() {
            // Arrange
            employeeService.registerEmployee(createValidRequest());
            assertTrue(employeeRepository.existsByEmployeeId("EMP001"));

            // Act
            EmployeeResponse response = employeeService.deleteEmployee("EMP001");

            // Assert
            assertEquals("SUCCESS", response.getStatus());
            assertEquals("EMP001", response.getEmployeeId());
            assertEquals("Employee deleted successfully", response.getMessage());
            assertFalse(employeeRepository.existsByEmployeeId("EMP001"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when deleting non-existent employee")
        void shouldReturnNotFoundWhenDeletingNonExistentEmployee() {
            // Act
            EmployeeResponse response = employeeService.deleteEmployee("EMP999");

            // Assert
            assertEquals("NOT_FOUND", response.getStatus());
            assertTrue(response.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should return VALIDATION_ERROR when employee ID is blank")
        void shouldReturnValidationErrorWhenEmployeeIdIsBlank() {
            // Act
            EmployeeResponse response = employeeService.deleteEmployee("");

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Employee ID is required"));
        }

        @Test
        @DisplayName("Should return VALIDATION_ERROR when employee ID is null")
        void shouldReturnValidationErrorWhenEmployeeIdIsNull() {
            // Act
            EmployeeResponse response = employeeService.deleteEmployee(null);

            // Assert
            assertEquals("VALIDATION_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Employee ID is required"));
        }
    }

    // ==================== Persistence Error Handling Tests ====================

    @Nested
    @DisplayName("Persistence Error Handling")
    class PersistenceErrorHandling {

        @Test
        @DisplayName("Should return SYSTEM_ERROR when repository throws on register")
        void shouldReturnSystemErrorOnRegistrationPersistenceFailure() {
            // Arrange - use a mock repository that always throws
            EmployeeRepository failingRepo = new EmployeeRepository() {
                @Override public Employee save(Employee e) {
                    throw new EmployeePersistenceException("DynamoDB connection failed");
                }
                @Override public Optional<Employee> findByEmployeeId(String id) { return Optional.empty(); }
                @Override public List<Employee> findByEmail(String email) { return List.of(); }
                @Override public List<Employee> findByDepartment(String dept) { return List.of(); }
                @Override public List<Employee> findAll() { return List.of(); }
                @Override public boolean deleteByEmployeeId(String id) { return false; }
                @Override public Optional<Employee> updateStatus(String id, String s) { return Optional.empty(); }
                @Override public boolean existsByEmployeeId(String id) { return false; }
                @Override public long count() { return 0; }
            };

            EmployeeService failingService = new EmployeeServiceImpl(failingRepo);

            EmployeeRequest request = createValidRequest();

            // Act
            EmployeeResponse response = failingService.registerEmployee(request);

            // Assert
            assertEquals("SYSTEM_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Failed to save employee"));
        }

        @Test
        @DisplayName("Should return SYSTEM_ERROR when repository throws on getById")
        void shouldReturnSystemErrorOnGetByIdPersistenceFailure() {
            // Arrange
            EmployeeRepository failingRepo = new EmployeeRepository() {
                @Override public Employee save(Employee e) { return e; }
                @Override public Optional<Employee> findByEmployeeId(String id) {
                    throw new EmployeePersistenceException("DynamoDB connection failed");
                }
                @Override public List<Employee> findByEmail(String email) { return List.of(); }
                @Override public List<Employee> findByDepartment(String dept) { return List.of(); }
                @Override public List<Employee> findAll() { return List.of(); }
                @Override public boolean deleteByEmployeeId(String id) { return false; }
                @Override public Optional<Employee> updateStatus(String id, String s) { return Optional.empty(); }
                @Override public boolean existsByEmployeeId(String id) { return false; }
                @Override public long count() { return 0; }
            };

            EmployeeService failingService = new EmployeeServiceImpl(failingRepo);

            // Act
            EmployeeResponse response = failingService.getEmployeeById("EMP001");

            // Assert
            assertEquals("SYSTEM_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Failed to retrieve employee"));
        }

        @Test
        @DisplayName("Should return SYSTEM_ERROR when repository throws on delete")
        void shouldReturnSystemErrorOnDeletePersistenceFailure() {
            // Arrange
            EmployeeRepository failingRepo = new EmployeeRepository() {
                @Override public Employee save(Employee e) { return e; }
                @Override public Optional<Employee> findByEmployeeId(String id) { return Optional.empty(); }
                @Override public List<Employee> findByEmail(String email) { return List.of(); }
                @Override public List<Employee> findByDepartment(String dept) { return List.of(); }
                @Override public List<Employee> findAll() { return List.of(); }
                @Override public boolean deleteByEmployeeId(String id) {
                    throw new EmployeePersistenceException("DynamoDB connection failed");
                }
                @Override public Optional<Employee> updateStatus(String id, String s) { return Optional.empty(); }
                @Override public boolean existsByEmployeeId(String id) { return false; }
                @Override public long count() { return 0; }
            };

            EmployeeService failingService = new EmployeeServiceImpl(failingRepo);

            // Act
            EmployeeResponse response = failingService.deleteEmployee("EMP001");

            // Assert
            assertEquals("SYSTEM_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Failed to delete employee"));
        }

        @Test
        @DisplayName("Should return SYSTEM_ERROR when repository throws on updateStatus")
        void shouldReturnSystemErrorOnUpdateStatusPersistenceFailure() {
            // Arrange
            EmployeeRepository failingRepo = new EmployeeRepository() {
                @Override public Employee save(Employee e) { return e; }
                @Override public Optional<Employee> findByEmployeeId(String id) { return Optional.empty(); }
                @Override public List<Employee> findByEmail(String email) { return List.of(); }
                @Override public List<Employee> findByDepartment(String dept) { return List.of(); }
                @Override public List<Employee> findAll() { return List.of(); }
                @Override public boolean deleteByEmployeeId(String id) { return false; }
                @Override public Optional<Employee> updateStatus(String id, String s) {
                    throw new EmployeePersistenceException("DynamoDB connection failed");
                }
                @Override public boolean existsByEmployeeId(String id) { return false; }
                @Override public long count() { return 0; }
            };

            EmployeeService failingService = new EmployeeServiceImpl(failingRepo);

            // Act
            EmployeeResponse response = failingService.updateEmployeeStatus("EMP001", "INACTIVE");

            // Assert
            assertEquals("SYSTEM_ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Failed to update employee status"));
        }

        @Test
        @DisplayName("Should return SYSTEM_ERROR list when repository throws on email search")
        void shouldReturnSystemErrorOnEmailSearchPersistenceFailure() {
            // Arrange
            EmployeeRepository failingRepo = new EmployeeRepository() {
                @Override public Employee save(Employee e) { return e; }
                @Override public Optional<Employee> findByEmployeeId(String id) { return Optional.empty(); }
                @Override public List<Employee> findByEmail(String email) {
                    throw new EmployeePersistenceException("DynamoDB connection failed");
                }
                @Override public List<Employee> findByDepartment(String dept) { return List.of(); }
                @Override public List<Employee> findAll() { return List.of(); }
                @Override public boolean deleteByEmployeeId(String id) { return false; }
                @Override public Optional<Employee> updateStatus(String id, String s) { return Optional.empty(); }
                @Override public boolean existsByEmployeeId(String id) { return false; }
                @Override public long count() { return 0; }
            };

            EmployeeService failingService = new EmployeeServiceImpl(failingRepo);

            // Act
            List<EmployeeResponse> responses = failingService.getEmployeesByEmail("test@test.com");

            // Assert
            assertEquals(1, responses.size());
            assertEquals("SYSTEM_ERROR", responses.get(0).getStatus());
        }

        @Test
        @DisplayName("Should return SYSTEM_ERROR list when repository throws on department search")
        void shouldReturnSystemErrorOnDepartmentSearchPersistenceFailure() {
            // Arrange
            EmployeeRepository failingRepo = new EmployeeRepository() {
                @Override public Employee save(Employee e) { return e; }
                @Override public Optional<Employee> findByEmployeeId(String id) { return Optional.empty(); }
                @Override public List<Employee> findByEmail(String email) { return List.of(); }
                @Override public List<Employee> findByDepartment(String dept) {
                    throw new EmployeePersistenceException("DynamoDB connection failed");
                }
                @Override public List<Employee> findAll() { return List.of(); }
                @Override public boolean deleteByEmployeeId(String id) { return false; }
                @Override public Optional<Employee> updateStatus(String id, String s) { return Optional.empty(); }
                @Override public boolean existsByEmployeeId(String id) { return false; }
                @Override public long count() { return 0; }
            };

            EmployeeService failingService = new EmployeeServiceImpl(failingRepo);

            // Act
            List<EmployeeResponse> responses = failingService.getEmployeesByDepartment("Engineering");

            // Assert
            assertEquals(1, responses.size());
            assertEquals("SYSTEM_ERROR", responses.get(0).getStatus());
        }
    }
}

