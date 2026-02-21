package booking.service.employee;

import booking.dto.EmployeeRequest;
import booking.dto.EmployeeResponse;

import java.util.List;

// Service interface for employee operations
public interface EmployeeService {

    EmployeeResponse registerEmployee(EmployeeRequest request);

    EmployeeResponse getEmployeeById(String employeeId);

    List<EmployeeResponse> getEmployeesByEmail(String email);

    List<EmployeeResponse> getEmployeesByDepartment(String department);

    EmployeeResponse updateEmployeeStatus(String employeeId, String newStatus);

    EmployeeResponse deleteEmployee(String employeeId);

    List<EmployeeResponse> getAllEmployees();
}
