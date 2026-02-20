package booking.service.employee;

import booking.dto.EmployeeRequest;
import booking.dto.EmployeeResponse;

import java.util.List;

/**
 * Service interface for employee operations.
 *
 * <p>Defines the contract for employee business logic operations.
 * This interface enforces the separation of concerns principle
 * by abstracting the business logic from the request handling layer.
 *
 * <p>Implementations should handle:
 * <ul>
 *   <li>Validation of employee requests</li>
 *   <li>Employee registration and management</li>
 *   <li>Persistence of employee records</li>
 *   <li>Business rule enforcement (e.g., no duplicate emails)</li>
 * </ul>
 */
public interface EmployeeService {

    /**
     * Registers a new employee in the system.
     *
     * @param request The employee registration request
     * @return Response with status and employee ID
     */
    EmployeeResponse registerEmployee(EmployeeRequest request);

    /**
     * Retrieves an employee by their unique ID.
     *
     * @param employeeId The employee ID to look up
     * @return Response with employee data or error if not found
     */
    EmployeeResponse getEmployeeById(String employeeId);

    /**
     * Retrieves all employees matching the given email.
     *
     * @param email The email address to search for
     * @return List of matching employee responses
     */
    List<EmployeeResponse> getEmployeesByEmail(String email);

    /**
     * Retrieves all employees in a given department.
     *
     * @param department The department name to search for
     * @return List of matching employee responses
     */
    List<EmployeeResponse> getEmployeesByDepartment(String department);

    /**
     * Updates the status of an existing employee.
     *
     * @param employeeId The employee ID to update
     * @param newStatus The new status value (e.g., "ACTIVE", "INACTIVE")
     * @return Response with updated employee data or error if not found
     */
    EmployeeResponse updateEmployeeStatus(String employeeId, String newStatus);

    /**
     * Deletes an employee by their ID.
     *
     * @param employeeId The employee ID to delete
     * @return Response indicating success or failure
     */
    EmployeeResponse deleteEmployee(String employeeId);

    /**
     * Retrieves all employees in the system.
     *
     * @return List of all employee responses
     */
    List<EmployeeResponse> getAllEmployees();
}
