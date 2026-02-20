package booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("status")
    private String status;

    @JsonProperty("employeeId")
    private String employeeId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("department")
    private String department;

    @JsonProperty("costCenterRef")
    private String costCenterRef;

    @JsonProperty("employeeStatus")
    private String employeeStatus;

}
