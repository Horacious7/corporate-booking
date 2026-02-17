package booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/// Data Transfer Object for booking requests.
///

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest implements Serializable {

    private static final long serialVersionUID = 1L; // For serialization compatibility, best practice, avoids warnings and errors during deserialization across different versions of the class.

    //Employee ID of the person making the booking
    @JsonProperty("employeeId")
    private String employeeId;

    // Type of resource being booked (ex. "Flight", "Hotel")
    @JsonProperty("resourceType")
    private String resourceType;

    // Destination for the booking (ex. "Cluj-Napoca", "Bucuresti", "Timisoara")
    @JsonProperty("destination")
    private String destination;

    // Departure date and time in ISO format
    @JsonProperty("departureDate")
    private String departureDate;

    // Return date and time in ISO format
    @JsonProperty("returnDate")
    private String returnDate;

    // Number of travelers included in the booking
    @JsonProperty("travelerCount")
    private Integer travelerCount;

    // Cost center reference for billing purposes
    @JsonProperty("costCenterRef")
    private String costCenterRef;

    // Purpose of the trip (ex. "Business meeting", "Conference", "Training")
    @JsonProperty("tripPurpose")
    private String tripPurpose;
}

