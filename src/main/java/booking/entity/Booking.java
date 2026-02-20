package booking.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.io.Serializable;
import java.time.Instant;

/// Entity class representing a booking record in DynamoDB.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    // serves as the primary key in DynamoDB.
    private String bookingReferenceId;

    private String employeeId;
    private String resourceType;
    private String destination;
    private String departureDate;
    private String returnDate;
    private Integer travelerCount;
    private String costCenterRef;
    private String tripPurpose;
    private String status;

   // Timestamps for record creation and last update, stored as ISO 8601 strings for consistency and ease of querying.
    private String createdAt;

   // UpdatedAt is automatically set to the current timestamp whenever the record is modified, allowing for tracking changes over time.
    private String updatedAt;

   // Primary partition key for DynamoDB, uniquely identifies each booking record.
    @DynamoDbPartitionKey
    public String getBookingReferenceId() {
        return bookingReferenceId;
    }

    // Secondary partition key for DynamoDB Global Secondary Index (GSI) named "employee-index", allows efficient querying by employee ID.
    @DynamoDbSecondaryPartitionKey(indexNames = "employee-index")
    public String getEmployeeId() {
        return employeeId;
    }
}

