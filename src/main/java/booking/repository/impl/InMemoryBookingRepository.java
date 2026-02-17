package booking.repository.impl;

import booking.entity.Booking;
import booking.repository.BookingPersistenceException;
import booking.repository.BookingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of BookingRepository for testing and local development.
 *
 * <p>This implementation stores bookings in a ConcurrentHashMap, making it
 * thread-safe for concurrent access. It's ideal for:
 * <ul>
 *   <li>Unit and integration testing without AWS dependencies</li>
 *   <li>Local development and debugging</li>
 *   <li>Prototype and demo purposes</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Data is not persisted between application restarts.
 * Use {@link DynamoDBBookingRepository} for production persistence.
 */
public class InMemoryBookingRepository implements BookingRepository {

    private static final Logger logger = LogManager.getLogger(InMemoryBookingRepository.class);
    private static final String STATUS_PENDING = "PENDING";

    private final Map<String, Booking> bookingStore = new ConcurrentHashMap<>();

    /**
     * Default constructor.
     */
    public InMemoryBookingRepository() {
        logger.info("Initialized InMemory booking repository");
    }

    @Override
    public Booking save(Booking booking) {
        try {
            if (booking == null) {
                throw new IllegalArgumentException("Booking cannot be null");
            }

            if (booking.getBookingReferenceId() == null || booking.getBookingReferenceId().isBlank()) {
                throw new IllegalArgumentException("Booking reference ID cannot be null or blank");
            }

            logger.debug("Saving booking: {}", booking.getBookingReferenceId());

            // Set timestamps
            String now = Instant.now().toString();
            if (booking.getCreatedAt() == null) {
                booking.setCreatedAt(now);
                booking.setStatus(STATUS_PENDING);
            }
            booking.setUpdatedAt(now);

            // Store the booking
            bookingStore.put(booking.getBookingReferenceId(), booking);

            logger.info("Successfully saved booking: {}", booking.getBookingReferenceId());
            return booking;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to save booking: {}", booking.getBookingReferenceId(), e);
            throw new BookingPersistenceException("Failed to save booking: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Booking> findByBookingReferenceId(String bookingReferenceId) {
        try {
            logger.debug("Finding booking by reference: {}", bookingReferenceId);

            Booking booking = bookingStore.get(bookingReferenceId);

            if (booking != null) {
                logger.debug("Found booking: {}", bookingReferenceId);
            } else {
                logger.debug("Booking not found: {}", bookingReferenceId);
            }

            return Optional.ofNullable(booking);

        } catch (Exception e) {
            logger.error("Failed to find booking: {}", bookingReferenceId, e);
            throw new BookingPersistenceException("Failed to find booking: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Booking> findByEmployeeId(String employeeId) {
        try {
            logger.debug("Finding bookings for employee: {}", employeeId);

            List<Booking> bookings = bookingStore.values().stream()
                    .filter(b -> employeeId.equals(b.getEmployeeId()))
                    .collect(Collectors.toList());

            logger.info("Found {} bookings for employee: {}", bookings.size(), employeeId);
            return bookings;

        } catch (Exception e) {
            logger.error("Failed to find bookings for employee: {}", employeeId, e);
            throw new BookingPersistenceException("Failed to query bookings by employee: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Booking> findAll() {
        try {
            logger.debug("Retrieving all bookings");

            List<Booking> bookings = new ArrayList<>(bookingStore.values());

            logger.info("Found {} total bookings", bookings.size());
            return bookings;

        } catch (Exception e) {
            logger.error("Failed to retrieve all bookings", e);
            throw new BookingPersistenceException("Failed to retrieve bookings: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByBookingReferenceId(String bookingReferenceId) {
        try {
            logger.debug("Deleting booking: {}", bookingReferenceId);

            Booking removed = bookingStore.remove(bookingReferenceId);

            if (removed != null) {
                logger.info("Successfully deleted booking: {}", bookingReferenceId);
                return true;
            } else {
                logger.debug("Booking not found for deletion: {}", bookingReferenceId);
                return false;
            }

        } catch (Exception e) {
            logger.error("Failed to delete booking: {}", bookingReferenceId, e);
            throw new BookingPersistenceException("Failed to delete booking: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Booking> updateStatus(String bookingReferenceId, String newStatus) {
        try {
            logger.debug("Updating status for booking: {} to {}", bookingReferenceId, newStatus);

            Booking booking = bookingStore.get(bookingReferenceId);

            if (booking == null) {
                logger.debug("Booking not found for status update: {}", bookingReferenceId);
                return Optional.empty();
            }

            booking.setStatus(newStatus);
            booking.setUpdatedAt(Instant.now().toString());

            logger.info("Successfully updated booking {} status to {}", bookingReferenceId, newStatus);
            return Optional.of(booking);

        } catch (Exception e) {
            logger.error("Failed to update booking status: {}", bookingReferenceId, e);
            throw new BookingPersistenceException("Failed to update booking status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByBookingReferenceId(String bookingReferenceId) {
        return bookingStore.containsKey(bookingReferenceId);
    }

    @Override
    public long countByEmployeeId(String employeeId) {
        return bookingStore.values().stream()
                .filter(b -> employeeId.equals(b.getEmployeeId()))
                .count();
    }

    /**
     * Clears all bookings from the in-memory store.
     * Useful for testing to reset state between tests.
     */
    public void clear() {
        bookingStore.clear();
        logger.info("Cleared all bookings from in-memory store");
    }

    /**
     * Returns the current count of bookings in the store.
     *
     * @return Total number of stored bookings
     */
    public int size() {
        return bookingStore.size();
    }
}

