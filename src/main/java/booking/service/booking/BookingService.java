package booking.service.booking;

import booking.dto.BookingRequest;
import booking.dto.BookingResponse;

import java.util.List;

///Service interface for booking operations
public interface BookingService {

    BookingResponse createBooking(BookingRequest request);

    BookingResponse getBookingByReferenceId(String bookingReferenceId);

    List<BookingResponse> getBookingsByEmployeeId(String employeeId);

    BookingResponse cancelBooking(String bookingReferenceId);

    BookingResponse updateBookingStatus(String bookingReferenceId, String newStatus);

    List<BookingResponse> getAllBookings();
}

