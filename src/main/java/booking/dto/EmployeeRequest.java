package booking.dto;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EmployeeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("employeeId")
    private String employeeId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("department")
    private String department;

    @JsonProperty("costCenterRef")
    private String costCenterRef; //cost center - who pays for the booking

    @JsonProperty("status")
    private String status;
}
