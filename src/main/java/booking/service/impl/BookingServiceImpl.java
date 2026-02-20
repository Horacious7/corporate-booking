package booking.service.impl;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;
import booking.entity.Booking;
import booking.repository.BookingPersistenceException;
import booking.repository.BookingRepository;
import booking.repository.BookingRepositoryFactory;
import booking.service.BookingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/// Implementation of the BookingService interface that contains the business logic for creating bookings.

public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LogManager.getLogger(BookingServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BOOKING_REF_PREFIX = "BKG-";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_VALIDATION_ERROR = "VALIDATION_ERROR";

    private final BookingRepository bookingRepository;

    /**
     * Default constructor - uses BookingRepositoryFactory to get the appropriate repository
     * based on environment configuration (DynamoDB for production, InMemory for testing).
     */
    public BookingServiceImpl() {
        this.bookingRepository = BookingRepositoryFactory.create();
    }

    /**
     * Constructor for dependency injection (useful for testing).
     *
     * @param bookingRepository The repository to use for persistence
     */
    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // Creates a new booking based on the provided request with validation and error handling
    @Override
    public BookingResponse createBooking(BookingRequest request) {
        // Check for null request first
        if (request == null) {
            logger.error("Booking request is null");
            return buildErrorResponse(STATUS_VALIDATION_ERROR, "Booking request cannot be null");
        }

        logger.info("Processing booking request for employee: {}", request.getEmployeeId());

        try {
            // Validate required fields
            validateBookingRequest(request);

            // Validate date format and logical constraints
            validateBookingDates(request);

            // Generate unique booking reference ID
            String bookingReferenceId = generateBookingReference();

            // Build the Booking entity from the request
            Booking booking = Booking.builder()
                .bookingReferenceId(bookingReferenceId)
                .employeeId(request.getEmployeeId())
                .resourceType(request.getResourceType())
                .destination(request.getDestination())
                .departureDate(request.getDepartureDate())
                .returnDate(request.getReturnDate())
                .travelerCount(request.getTravelerCount())
                .costCenterRef(request.getCostCenterRef())
                .tripPurpose(request.getTripPurpose())
                .build();

            // Persist the booking to the repository (DynamoDB or InMemory)
            Booking savedBooking = bookingRepository.save(booking);

            logger.info("Booking created and persisted successfully. Reference: {}", savedBooking.getBookingReferenceId());

            // Return success response
            return BookingResponse.builder()
                .status(STATUS_SUCCESS)
                .bookingReferenceId(savedBooking.getBookingReferenceId())
                .message("Booking created successfully for employee " + request.getEmployeeId())
                .build();

        } catch (IllegalArgumentException e) { //business logic errors
            logger.error("Validation error in booking request: {}", e.getMessage());
            return buildErrorResponse(STATUS_VALIDATION_ERROR, e.getMessage());
        } catch (BookingPersistenceException e) { // persistence errors
            logger.error("Failed to persist booking: {}", e.getMessage(), e);
            return buildErrorResponse("SYSTEM_ERROR", "Failed to save booking. Please try again later.");
        } catch (Exception e) { //generic errors
            logger.error("Unexpected error processing booking request", e);
            return buildErrorResponse("SYSTEM_ERROR", "An unexpected error occurred processing your booking");
        }
    }


    //Validates that all required fields in the booking request are present and valid
    private void validateBookingRequest(BookingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Booking request cannot be null");
        }

        if (isBlank(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        if (isBlank(request.getResourceType())) {
            throw new IllegalArgumentException("Resource type is required");
        }

        if (isBlank(request.getDestination())) {
            throw new IllegalArgumentException("Destination is required");
        }

        if (isBlank(request.getDepartureDate())) {
            throw new IllegalArgumentException("Departure date is required");
        }

        if (isBlank(request.getReturnDate())) {
            throw new IllegalArgumentException("Return date is required");
        }

        if (request.getTravelerCount() == null || request.getTravelerCount() < 1) {
            throw new IllegalArgumentException("Traveler count must be at least 1");
        }

        if (isBlank(request.getCostCenterRef())) {
            throw new IllegalArgumentException("Cost center reference is required");
        }

        if (isBlank(request.getTripPurpose())) {
            throw new IllegalArgumentException("Trip purpose is required");
        }
    }

    //Validates the format and logical constraints of booking dates
    private void validateBookingDates(BookingRequest request) {
        try {
            LocalDateTime departureDateTime = LocalDateTime.parse(
                request.getDepartureDate(),
                DATE_TIME_FORMATTER
            );
            LocalDateTime returnDateTime = LocalDateTime.parse(
                request.getReturnDate(),
                DATE_TIME_FORMATTER
            );

            if (departureDateTime.isAfter(returnDateTime)) {
                throw new IllegalArgumentException(
                    "Departure date must be before return date"
                );
            }

            if (departureDateTime.equals(returnDateTime)) {
                throw new IllegalArgumentException(
                    "Departure and return dates cannot be the same"
                );
            }

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Invalid date format. Expected 'yyyy-MM-dd HH:mm:ss'"
            );
        }
    }


    //Creates a reference in the format "BKG-{UUID}" where UUID is a randomly generated number.
    //even tho it's not truly unique, the probability of collision is so low, there's a bigger chance to mark a sand grain of sand, throw it on earth in any desert or beach and have someone guess from the first try which one is that one you picked :)
    private String generateBookingReference() {
        return BOOKING_REF_PREFIX + UUID.randomUUID();
    }

    //Builds a standardized error response with the given status and message
    private BookingResponse buildErrorResponse(String status, String message) {
        return BookingResponse.builder()
            .status(status)
            .bookingReferenceId(null)
            .message(message)
            .build();
    }

    //check if a string is null or empty after trimming whitespace
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

