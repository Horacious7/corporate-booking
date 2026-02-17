package booking.service;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;
import booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BookingServiceImpl.
 *
 * <p>Tests the business logic layer of the booking service including:
 * <ul>
 *   <li>Successful booking creation</li>
 *   <li>Input validation for all fields</li>
 *   <li>Date format and logical constraint validation</li>
 *   <li>Unique booking reference generation</li>
 *   <li>Error handling and responses</li>
 * </ul>
 *
 * @author TechQuarter Engineering
 * @version 1.0.0
 */
@DisplayName("BookingService Tests")
class BookingServiceTest {

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl();
    }

    @Test
    @DisplayName("Should create booking successfully with valid request")
    void testCreateBookingSuccess() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting - Acme Corp")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getBookingReferenceId());
        assertTrue(response.getBookingReferenceId().startsWith("BKG-"));
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("EMP9876"));
    }

    @Test
    @DisplayName("Should return error when employee ID is missing")
    void testCreateBookingMissingEmployeeId() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertNull(response.getBookingReferenceId());
        assertTrue(response.getMessage().contains("Employee ID"));
    }

    @Test
    @DisplayName("Should return error when resource type is missing")
    void testCreateBookingMissingResourceType() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType(null)
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertNull(response.getBookingReferenceId());
    }

    @Test
    @DisplayName("Should return error when traveler count is zero")
    void testCreateBookingInvalidTravelerCount() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(0))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Traveler count"));
    }

    @Test
    @DisplayName("Should return error when departure date is after return date")
    void testCreateBookingInvalidDateOrder() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-08 18:00:00")
            .returnDate("2024-11-05 08:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Departure date must be before return date"));
    }

    @Test
    @DisplayName("Should return error when dates are identical")
    void testCreateBookingIdenticalDates() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-05 08:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("cannot be the same"));
    }

    @Test
    @DisplayName("Should return error when date format is invalid")
    void testCreateBookingInvalidDateFormat() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05")  // Missing time
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Invalid date format"));
    }

    @Test
    @DisplayName("Should return error when null request is provided")
    void testCreateBookingNullRequest() {
        // Act
        BookingResponse response = bookingService.createBooking(null);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertNull(response.getBookingReferenceId());
    }

    @Test
    @DisplayName("Should generate unique booking references")
    void testGenerateUniqueReferences() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response1 = bookingService.createBooking(request);
        BookingResponse response2 = bookingService.createBooking(request);

        // Assert
        assertNotEquals(response1.getBookingReferenceId(), response2.getBookingReferenceId());
    }

    @Test
    @DisplayName("Should return error when cost center is missing")
    void testCreateBookingMissingCostCenter() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("")
            .tripPurpose("Client meeting")
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Cost center"));
    }

    @Test
    @DisplayName("Should return error when trip purpose is missing")
    void testCreateBookingMissingTripPurpose() {
        // Arrange
        BookingRequest request = BookingRequest.builder()
            .employeeId("EMP9876")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose(null)
            .build();

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertTrue(response.getMessage().contains("Trip purpose"));
    }
}

