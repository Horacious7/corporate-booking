package booking.service.booking;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;

// Service interface for booking operations.
public interface BookingService {
    BookingResponse createBooking(BookingRequest request);

    //no need for the other RUD opertations, we just update the status (cancelled, completed)
}

