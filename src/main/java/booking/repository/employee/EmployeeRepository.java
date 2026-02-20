package booking.repository.employee;

import booking.entity.Employee;
import booking.repository.exception.EmployeePersistenceException;

import java.util.List;
import java.util.Optional;


public interface EmployeeRepository {

    Employee save(Employee employee);

    Optional<Employee> findByEmployeeId(String employeeId);

    List<Employee> findByEmail(String email);

    List<Employee> findByDepartment(String department);

    List<Employee> findAll();

    boolean deleteByEmployeeId(String employeeId);

    Optional<Employee> updateStatus(String employeeId, String newStatus);

    boolean existsByEmployeeId(String employeeId);

    long count();
}
