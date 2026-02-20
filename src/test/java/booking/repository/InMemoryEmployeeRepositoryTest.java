package booking.repository;

import booking.entity.Employee;
import booking.repository.employee.impl.InMemoryEmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the InMemoryEmployeeRepository implementation.
 *
 * <p>These tests verify the correct behavior of all CRUD operations
 * and edge cases for the in-memory employee repository implementation.
 */
@DisplayName("InMemoryEmployeeRepository Tests")
class InMemoryEmployeeRepositoryTest {

    private InMemoryEmployeeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEmployeeRepository();
    }

    private Employee createTestEmployee(String employeeId, String name, String email, String department) {
        return Employee.builder()
                .employeeId(employeeId)
                .name(name)
                .email(email)
                .department(department)
                .costCenterRef("CC-100")
                .build();
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("Should save new employee successfully")
        void shouldSaveNewEmployee() {
            // Arrange
            Employee employee = createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering");

            // Act
            Employee saved = repository.save(employee);

            // Assert
            assertNotNull(saved);
            assertEquals("EMP001", saved.getEmployeeId());
            assertEquals("John Doe", saved.getName());
            assertEquals("john@techquarter.com", saved.getEmail());
            assertEquals("Engineering", saved.getDepartment());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
            assertEquals("ACTIVE", saved.getStatus());
            assertEquals(1, repository.size());
        }

        @Test
        @DisplayName("Should update existing employee")
        void shouldUpdateExistingEmployee() {
            // Arrange
            Employee employee = createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering");
            repository.save(employee);

            // Act
            employee.setName("John Updated");
            Employee updated = repository.save(employee);

            // Assert
            assertEquals("John Updated", updated.getName());
            assertEquals(1, repository.size());
        }

        @Test
        @DisplayName("Should throw exception for null employee")
        void shouldThrowExceptionForNullEmployee() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }

        @Test
        @DisplayName("Should throw exception for employee with null ID")
        void shouldThrowExceptionForNullEmployeeId() {
            Employee employee = createTestEmployee(null, "John Doe", "john@techquarter.com", "Engineering");
            assertThrows(IllegalArgumentException.class, () -> repository.save(employee));
        }

        @Test
        @DisplayName("Should throw exception for employee with blank ID")
        void shouldThrowExceptionForBlankEmployeeId() {
            Employee employee = createTestEmployee("", "John Doe", "john@techquarter.com", "Engineering");
            assertThrows(IllegalArgumentException.class, () -> repository.save(employee));
        }
    }

    @Nested
    @DisplayName("Find By Employee ID Operations")
    class FindByEmployeeIdOperations {

        @Test
        @DisplayName("Should find existing employee by ID")
        void shouldFindExistingEmployee() {
            // Arrange
            Employee employee = createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering");
            repository.save(employee);

            // Act
            Optional<Employee> found = repository.findByEmployeeId("EMP001");

            // Assert
            assertTrue(found.isPresent());
            assertEquals("EMP001", found.get().getEmployeeId());
            assertEquals("John Doe", found.get().getName());
        }

        @Test
        @DisplayName("Should return empty for non-existing employee")
        void shouldReturnEmptyForNonExisting() {
            // Act
            Optional<Employee> found = repository.findByEmployeeId("NON-EXISTING");

            // Assert
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Email Operations")
    class FindByEmailOperations {

        @Test
        @DisplayName("Should find employees by email")
        void shouldFindEmployeesByEmail() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));
            repository.save(createTestEmployee("EMP002", "Jane Doe", "jane@techquarter.com", "Marketing"));

            // Act
            List<Employee> employees = repository.findByEmail("john@techquarter.com");

            // Assert
            assertEquals(1, employees.size());
            assertEquals("EMP001", employees.get(0).getEmployeeId());
        }

        @Test
        @DisplayName("Should return empty list for non-existing email")
        void shouldReturnEmptyListForNonExistingEmail() {
            // Act
            List<Employee> employees = repository.findByEmail("nobody@techquarter.com");

            // Assert
            assertTrue(employees.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Department Operations")
    class FindByDepartmentOperations {

        @Test
        @DisplayName("Should find all employees in department")
        void shouldFindAllEmployeesInDepartment() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));
            repository.save(createTestEmployee("EMP002", "Jane Doe", "jane@techquarter.com", "Engineering"));
            repository.save(createTestEmployee("EMP003", "Bob Smith", "bob@techquarter.com", "Marketing"));

            // Act
            List<Employee> employees = repository.findByDepartment("Engineering");

            // Assert
            assertEquals(2, employees.size());
            assertTrue(employees.stream().allMatch(e -> "Engineering".equals(e.getDepartment())));
        }

        @Test
        @DisplayName("Should return empty list for non-existing department")
        void shouldReturnEmptyListForNonExistingDepartment() {
            // Act
            List<Employee> employees = repository.findByDepartment("NonExistent");

            // Assert
            assertTrue(employees.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find All Operations")
    class FindAllOperations {

        @Test
        @DisplayName("Should find all employees")
        void shouldFindAllEmployees() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));
            repository.save(createTestEmployee("EMP002", "Jane Doe", "jane@techquarter.com", "Marketing"));
            repository.save(createTestEmployee("EMP003", "Bob Smith", "bob@techquarter.com", "Sales"));

            // Act
            List<Employee> employees = repository.findAll();

            // Assert
            assertEquals(3, employees.size());
        }

        @Test
        @DisplayName("Should return empty list when no employees exist")
        void shouldReturnEmptyListWhenEmpty() {
            // Act
            List<Employee> employees = repository.findAll();

            // Assert
            assertTrue(employees.isEmpty());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete existing employee")
        void shouldDeleteExistingEmployee() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));

            // Act
            boolean deleted = repository.deleteByEmployeeId("EMP001");

            // Assert
            assertTrue(deleted);
            assertEquals(0, repository.size());
        }

        @Test
        @DisplayName("Should return false for non-existing employee")
        void shouldReturnFalseForNonExisting() {
            // Act
            boolean deleted = repository.deleteByEmployeeId("NON-EXISTING");

            // Assert
            assertFalse(deleted);
        }
    }

    @Nested
    @DisplayName("Update Status Operations")
    class UpdateStatusOperations {

        @Test
        @DisplayName("Should update employee status")
        void shouldUpdateEmployeeStatus() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));

            // Act
            Optional<Employee> updated = repository.updateStatus("EMP001", "INACTIVE");

            // Assert
            assertTrue(updated.isPresent());
            assertEquals("INACTIVE", updated.get().getStatus());
        }

        @Test
        @DisplayName("Should return empty for non-existing employee status update")
        void shouldReturnEmptyForNonExisting() {
            // Act
            Optional<Employee> updated = repository.updateStatus("NON-EXISTING", "INACTIVE");

            // Assert
            assertTrue(updated.isEmpty());
        }
    }

    @Nested
    @DisplayName("Exists Operations")
    class ExistsOperations {

        @Test
        @DisplayName("Should return true for existing employee")
        void shouldReturnTrueForExisting() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));

            // Act & Assert
            assertTrue(repository.existsByEmployeeId("EMP001"));
        }

        @Test
        @DisplayName("Should return false for non-existing employee")
        void shouldReturnFalseForNonExisting() {
            // Act & Assert
            assertFalse(repository.existsByEmployeeId("NON-EXISTING"));
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountOperations {

        @Test
        @DisplayName("Should count all employees")
        void shouldCountAllEmployees() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));
            repository.save(createTestEmployee("EMP002", "Jane Doe", "jane@techquarter.com", "Marketing"));
            repository.save(createTestEmployee("EMP003", "Bob Smith", "bob@techquarter.com", "Sales"));

            // Act & Assert
            assertEquals(3, repository.count());
        }

        @Test
        @DisplayName("Should return zero when no employees exist")
        void shouldReturnZeroWhenEmpty() {
            // Act & Assert
            assertEquals(0, repository.count());
        }
    }

    @Nested
    @DisplayName("Clear Operations")
    class ClearOperations {

        @Test
        @DisplayName("Should clear all employees")
        void shouldClearAllEmployees() {
            // Arrange
            repository.save(createTestEmployee("EMP001", "John Doe", "john@techquarter.com", "Engineering"));
            repository.save(createTestEmployee("EMP002", "Jane Doe", "jane@techquarter.com", "Marketing"));

            // Act
            repository.clear();

            // Assert
            assertEquals(0, repository.size());
            assertTrue(repository.findAll().isEmpty());
        }
    }
}

