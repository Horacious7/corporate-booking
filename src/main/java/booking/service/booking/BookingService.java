package booking.service.booking;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;

import java.util.List;

/**
 * Service interface for booking operations.
 *
 * <p>Defines the contract for booking business logic operations including
 * the full booking lifecycle: create → confirm → complete, with cancel
 * as a terminal state at any point.
 *
 * <p>Design decision: Bookings use soft-delete via status updates (CANCELLED)
 * rather than hard deletion, preserving audit trails for enterprise compliance.
 */
public interface BookingService {

    /**
     * Creates a new booking based on the provided request.
     *
     * @param request The booking request with all required fields
     * @return Response with status, booking reference ID, and message
     */
    BookingResponse createBooking(BookingRequest request);

    /**
     * Retrieves a booking by its unique reference ID.
     *
     * @param bookingReferenceId The unique booking reference (e.g., "BKG-...")
     * @return Response with booking data or error if not found
     */
    BookingResponse getBookingByReferenceId(String bookingReferenceId);

    /**
     * Retrieves all bookings for a specific employee.
     *
     * @param employeeId The employee ID to search for
     * @return List of booking responses for the employee
     */
    List<BookingResponse> getBookingsByEmployeeId(String employeeId);

    /**
     * Cancels a booking (soft delete via status = CANCELLED).
     *
     * @param bookingReferenceId The booking reference to cancel
     * @return Response indicating success or failure
     */
    BookingResponse cancelBooking(String bookingReferenceId);

    /**
     * Updates the status of an existing booking.
     *
     * <p>Enforces state machine: terminal states (CANCELLED, COMPLETED)
     * cannot transition to other states.
     *
     * @param bookingReferenceId The booking reference to update
     * @param newStatus The new status value
     * @return Response with updated booking data or error
     */
    BookingResponse updateBookingStatus(String bookingReferenceId, String newStatus);
}

