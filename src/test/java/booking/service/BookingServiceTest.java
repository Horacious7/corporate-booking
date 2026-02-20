package booking.service;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;
import booking.entity.Booking;
import booking.repository.BookingPersistenceException;
import booking.repository.BookingRepository;
import booking.repository.impl.InMemoryBookingRepository;
import booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
 *   <li>Repository persistence integration</li>
 * </ul>
 *
 * @author TechQuarter Engineering
 * @version 1.0.0
 */
@DisplayName("BookingService Tests")
class BookingServiceTest {

    private BookingService bookingService;
    private InMemoryBookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        bookingRepository = new InMemoryBookingRepository();
        bookingService = new BookingServiceImpl(bookingRepository);
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

    // ==================== Persistence Tests ====================

    @Test
    @DisplayName("Should persist booking to repository on successful creation")
    void testBookingIsPersistedToRepository() {
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

        // Assert - verify booking is persisted
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(bookingRepository.existsByBookingReferenceId(response.getBookingReferenceId()));

        var savedBooking = bookingRepository.findByBookingReferenceId(response.getBookingReferenceId());
        assertTrue(savedBooking.isPresent());
        assertEquals("EMP9876", savedBooking.get().getEmployeeId());
        assertEquals("Flight", savedBooking.get().getResourceType());
        assertEquals("NYC", savedBooking.get().getDestination());
        assertEquals("PENDING", savedBooking.get().getStatus());
        assertNotNull(savedBooking.get().getCreatedAt());
        assertNotNull(savedBooking.get().getUpdatedAt());
    }

    @Test
    @DisplayName("Should persist multiple bookings for same employee")
    void testMultipleBookingsPersistedForSameEmployee() {
        // Arrange
        BookingRequest request1 = BookingRequest.builder()
            .employeeId("EMP1111")
            .resourceType("Flight")
            .destination("NYC")
            .departureDate("2024-11-05 08:00:00")
            .returnDate("2024-11-08 18:00:00")
            .travelerCount(Integer.valueOf(1))
            .costCenterRef("CC-456")
            .tripPurpose("Client meeting")
            .build();

        BookingRequest request2 = BookingRequest.builder()
            .employeeId("EMP1111")
            .resourceType("Hotel")
            .destination("LAX")
            .departureDate("2024-12-01 10:00:00")
            .returnDate("2024-12-05 16:00:00")
            .travelerCount(Integer.valueOf(2))
            .costCenterRef("CC-789")
            .tripPurpose("Conference")
            .build();

        // Act
        bookingService.createBooking(request1);
        bookingService.createBooking(request2);

        // Assert
        assertEquals(2, bookingRepository.countByEmployeeId("EMP1111"));
        var bookings = bookingRepository.findByEmployeeId("EMP1111");
        assertEquals(2, bookings.size());
    }

    @Test
    @DisplayName("Should not persist booking when validation fails")
    void testBookingNotPersistedOnValidationError() {
        // Arrange - invalid request (missing employee ID)
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

        // Assert - no booking should be persisted
        assertEquals("VALIDATION_ERROR", response.getStatus());
        assertEquals(0, bookingRepository.findAll().size());
    }

    @Test
    @DisplayName("Should return SYSTEM_ERROR when repository throws persistence exception")
    void testSystemErrorOnPersistenceFailure() {
        // Arrange - use a mock repository that always throws
        BookingRepository failingRepo = new BookingRepository() {
            @Override public Booking save(Booking booking) {
                throw new BookingPersistenceException("DynamoDB connection failed");
            }
            @Override public Optional<Booking> findByBookingReferenceId(String id) { return Optional.empty(); }
            @Override public List<Booking> findByEmployeeId(String id) { return List.of(); }
            @Override public List<Booking> findAll() { return List.of(); }
            @Override public boolean deleteByBookingReferenceId(String id) { return false; }
            @Override public Optional<Booking> updateStatus(String id, String status) { return Optional.empty(); }
            @Override public boolean existsByBookingReferenceId(String id) { return false; }
            @Override public long countByEmployeeId(String id) { return 0; }
        };

        BookingService failingService = new BookingServiceImpl(failingRepo);

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
        BookingResponse response = failingService.createBooking(request);

        // Assert
        assertEquals("SYSTEM_ERROR", response.getStatus());
        assertNull(response.getBookingReferenceId());
        assertTrue(response.getMessage().contains("Failed to save booking"));
    }
}

