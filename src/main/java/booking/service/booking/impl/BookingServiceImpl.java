package booking.service.booking.impl;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;
import booking.entity.Booking;
import booking.repository.exception.BookingPersistenceException;
import booking.repository.booking.BookingRepository;
import booking.repository.booking.BookingRepositoryFactory;
import booking.service.booking.BookingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the BookingService interface that contains
 * the business logic for the full booking lifecycle.
 *
 * <p>Supports: create → confirm → complete, with cancel as terminal state.
 * Uses soft-delete (status = CANCELLED) instead of hard deletion
 * to preserve audit trails for enterprise compliance.
 */
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LogManager.getLogger(BookingServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BOOKING_REF_PREFIX = "BKG-";

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";

    private static final Set<String> VALID_STATUSES =
        Set.of("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED");

    private static final Set<String> TERMINAL_STATUSES =
        Set.of("CANCELLED", "COMPLETED");

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

    // ==================== Create Booking ====================

    @Override
    public BookingResponse createBooking(BookingRequest request) {
        if (request == null) {
            logger.error("Booking request is null");
            return buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Booking request cannot be null");
        }

        logger.info("Processing booking request for employee: {}", request.getEmployeeId());

        try {
            validateBookingRequest(request);
            validateBookingDates(request);

            String bookingReferenceId = generateBookingReference();

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

            Booking savedBooking = bookingRepository.save(booking);

            logger.info("Booking created successfully. Reference: {}", savedBooking.getBookingReferenceId());

            return BookingResponse.builder()
                .status(STATUS_SUCCESS)
                .bookingReferenceId(savedBooking.getBookingReferenceId())
                .message("Booking created successfully for employee " + request.getEmployeeId())
                .build();

        } catch (IllegalArgumentException e) {
            logger.error("Validation error in booking request: {}", e.getMessage());
            return buildErrorResponse(STATUS_VALIDATION_ERROR, null, e.getMessage());
        } catch (BookingPersistenceException e) {
            logger.error("Failed to persist booking: {}", e.getMessage(), e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, null, "Failed to save booking. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error processing booking request", e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, null, "An unexpected error occurred processing your booking");
        }
    }

    // ==================== Get Booking ====================

    @Override
    public BookingResponse getBookingByReferenceId(String bookingReferenceId) {
        logger.info("Looking up booking: {}", bookingReferenceId);

        try {
            if (isBlank(bookingReferenceId)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Booking reference ID is required");
            }

            Optional<Booking> bookingOpt = bookingRepository.findByBookingReferenceId(bookingReferenceId);

            if (bookingOpt.isEmpty()) {
                logger.debug("Booking not found: {}", bookingReferenceId);
                return buildErrorResponse(STATUS_NOT_FOUND, bookingReferenceId,
                    "Booking not found: " + bookingReferenceId);
            }

            Booking booking = bookingOpt.get();
            logger.info("Found booking: {} (status: {})", bookingReferenceId, booking.getStatus());

            return BookingResponse.builder()
                .status(STATUS_SUCCESS)
                .bookingReferenceId(booking.getBookingReferenceId())
                .message(booking.getResourceType() + " to " + booking.getDestination()
                    + " [" + booking.getStatus() + "]")
                .build();

        } catch (BookingPersistenceException e) {
            logger.error("Failed to retrieve booking: {}", bookingReferenceId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, bookingReferenceId,
                "Failed to retrieve booking. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error retrieving booking: {}", bookingReferenceId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, bookingReferenceId,
                "An unexpected error occurred");
        }
    }

    @Override
    public List<BookingResponse> getBookingsByEmployeeId(String employeeId) {
        logger.info("Searching bookings for employee: {}", employeeId);

        try {
            if (isBlank(employeeId)) {
                return List.of(buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Employee ID is required"));
            }

            List<Booking> bookings = bookingRepository.findByEmployeeId(employeeId);

            return bookings.stream()
                .map(booking -> BookingResponse.builder()
                    .status(STATUS_SUCCESS)
                    .bookingReferenceId(booking.getBookingReferenceId())
                    .message(booking.getResourceType() + " to " + booking.getDestination()
                        + " [" + booking.getStatus() + "]")
                    .build())
                .collect(Collectors.toList());

        } catch (BookingPersistenceException e) {
            logger.error("Failed to search bookings for employee: {}", employeeId, e);
            return List.of(buildErrorResponse(STATUS_SYSTEM_ERROR, null,
                "Failed to search bookings. Please try again later."));
        }
    }

    // ==================== Cancel / Update Status ====================

    @Override
    public BookingResponse cancelBooking(String bookingReferenceId) {
        logger.info("Cancelling booking: {}", bookingReferenceId);
        return updateBookingStatus(bookingReferenceId, "CANCELLED");
    }

    @Override
    public BookingResponse updateBookingStatus(String bookingReferenceId, String newStatus) {
        logger.info("Updating status for booking: {} to {}", bookingReferenceId, newStatus);

        try {
            if (isBlank(bookingReferenceId)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, null, "Booking reference ID is required");
            }

            if (isBlank(newStatus)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, bookingReferenceId, "New status is required");
            }

            String normalizedStatus = newStatus.toUpperCase();

            if (!VALID_STATUSES.contains(normalizedStatus)) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, bookingReferenceId,
                    "Invalid status. Allowed values: PENDING, CONFIRMED, CANCELLED, COMPLETED");
            }

            // Check current state before updating
            Optional<Booking> currentOpt = bookingRepository.findByBookingReferenceId(bookingReferenceId);

            if (currentOpt.isEmpty()) {
                return buildErrorResponse(STATUS_NOT_FOUND, bookingReferenceId,
                    "Booking not found: " + bookingReferenceId);
            }

            Booking current = currentOpt.get();

            // Prevent transitions from terminal states
            if (TERMINAL_STATUSES.contains(current.getStatus())) {
                return buildErrorResponse(STATUS_VALIDATION_ERROR, bookingReferenceId,
                    "Cannot update booking in " + current.getStatus() + " state");
            }

            Optional<Booking> updatedOpt = bookingRepository.updateStatus(bookingReferenceId, normalizedStatus);

            if (updatedOpt.isEmpty()) {
                return buildErrorResponse(STATUS_NOT_FOUND, bookingReferenceId,
                    "Booking not found: " + bookingReferenceId);
            }

            Booking updated = updatedOpt.get();
            logger.info("Successfully updated booking {} status to {}", bookingReferenceId, normalizedStatus);

            return BookingResponse.builder()
                .status(STATUS_SUCCESS)
                .bookingReferenceId(updated.getBookingReferenceId())
                .message("Booking status updated to " + updated.getStatus())
                .build();

        } catch (BookingPersistenceException e) {
            logger.error("Failed to update booking status: {}", bookingReferenceId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, bookingReferenceId,
                "Failed to update booking status. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error updating booking status: {}", bookingReferenceId, e);
            return buildErrorResponse(STATUS_SYSTEM_ERROR, bookingReferenceId,
                "An unexpected error occurred");
        }
    }

    // ==================== Private Helper Methods ====================

    private void validateBookingRequest(BookingRequest request) {
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

    private void validateBookingDates(BookingRequest request) {
        try {
            LocalDateTime departureDateTime = LocalDateTime.parse(request.getDepartureDate(), DATE_TIME_FORMATTER);
            LocalDateTime returnDateTime = LocalDateTime.parse(request.getReturnDate(), DATE_TIME_FORMATTER);

            if (departureDateTime.isAfter(returnDateTime)) {
                throw new IllegalArgumentException("Departure date must be before return date");
            }
            if (departureDateTime.equals(returnDateTime)) {
                throw new IllegalArgumentException("Departure and return dates cannot be the same");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected 'yyyy-MM-dd HH:mm:ss'");
        }
    }

    private String generateBookingReference() {
        return BOOKING_REF_PREFIX + UUID.randomUUID();
    }

    private BookingResponse buildErrorResponse(String status, String bookingReferenceId, String message) {
        return BookingResponse.builder()
            .status(status)
            .bookingReferenceId(bookingReferenceId)
            .message(message)
            .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

