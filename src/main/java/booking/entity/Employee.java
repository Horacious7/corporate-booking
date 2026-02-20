package booking.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.io.Serializable;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@DynamoDbBean
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private String employeeId;
    private String name;
    private String email;
    private String department;
    private String costCenterRef; //cost center - who pays for the booking
    private String status;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    public String getEmployeeId() {
        return employeeId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    public String getEmail() {
        return email;
        }

}
