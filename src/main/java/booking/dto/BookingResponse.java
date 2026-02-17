package booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/// Data Transfer Object for booking responses.


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse implements Serializable {

    private static final long serialVersionUID = 1L; // For serialization compatibility, best practice, avoids warnings and errors during deserialization across different versions of the class.

    // Status of the booking operation, ex. "success" or "error"
    @JsonProperty("status") // Maps to the "status" field in the JSON response
    private String status;

    // Unique booking reference ID generated for successful bookings.
    @JsonProperty("bookingReferenceId")
    private String bookingReferenceId;

    // Message providing additional information
    @JsonProperty("message")
    private String message;
}

