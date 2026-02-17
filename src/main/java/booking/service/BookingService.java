package booking.service;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;

/**
 * Service interface for booking operations.
 *
 * <p>Defines the contract for booking business logic operations.
 * This interface enforces the separation of concerns principle
 * by abstracting the business logic from the request handling layer.
 *
 * <p>Implementations should handle:
 * <ul>
 *   <li>Validation of booking requests</li>
 *   <li>Generation of unique booking references</li>
 *   <li>Persistence of booking records</li>
 *   <li>Business rule enforcement</li>
 * </ul>
 *
 */
public interface BookingService {
    BookingResponse createBooking(BookingRequest request);
}

