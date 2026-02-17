package booking.repository;

import booking.entity.Booking;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Booking persistence operations.
 *
 * <p>This interface defines the contract for all booking data access operations.
 * Implementations may use different persistence backends (DynamoDB, in-memory, etc.)
 * following the Repository pattern for clean separation of concerns.
 *
 * <p>Design Pattern: Repository Pattern
 * <ul>
 *   <li>Abstracts data access logic from business logic</li>
 *   <li>Enables easy swapping of persistence implementations</li>
 *   <li>Facilitates unit testing with mock implementations</li>
 * </ul>
 */
public interface BookingRepository {

    /**
     * Saves a new booking or updates an existing one.
     *
     * <p>If a booking with the same bookingReferenceId exists, it will be updated.
     * Otherwise, a new booking will be created.
     *
     * @param booking The booking entity to save
     * @return The saved booking entity with any generated fields populated
     * @throws BookingPersistenceException if the save operation fails
     */
    Booking save(Booking booking);

    /**
     * Finds a booking by its unique reference ID.
     *
     * @param bookingReferenceId The unique booking reference (e.g., "BKG-...")
     * @return Optional containing the booking if found, empty otherwise
     * @throws BookingPersistenceException if the query operation fails
     */
    Optional<Booking> findByBookingReferenceId(String bookingReferenceId);

    /**
     * Finds all bookings for a specific employee.
     *
     * <p>Uses the employee-index GSI for efficient querying.
     *
     * @param employeeId The employee ID to search for
     * @return List of bookings for the employee (empty list if none found)
     * @throws BookingPersistenceException if the query operation fails
     */
    List<Booking> findByEmployeeId(String employeeId);

    /**
     * Retrieves all bookings in the system.
     *
     * <p><strong>Warning:</strong> This operation performs a full table scan.
     * Use with caution in production environments with large datasets.
     *
     * @return List of all bookings
     * @throws BookingPersistenceException if the scan operation fails
     */
    List<Booking> findAll();

    /**
     * Deletes a booking by its reference ID.
     *
     * @param bookingReferenceId The unique booking reference to delete
     * @return true if the booking was deleted, false if it didn't exist
     * @throws BookingPersistenceException if the delete operation fails
     */
    boolean deleteByBookingReferenceId(String bookingReferenceId);

    /**
     * Updates the status of an existing booking.
     *
     * @param bookingReferenceId The booking reference to update
     * @param newStatus The new status value
     * @return Optional containing the updated booking if found, empty otherwise
     * @throws BookingPersistenceException if the update operation fails
     */
    Optional<Booking> updateStatus(String bookingReferenceId, String newStatus);

    /**
     * Checks if a booking exists with the given reference ID.
     *
     * @param bookingReferenceId The booking reference to check
     * @return true if the booking exists, false otherwise
     */
    boolean existsByBookingReferenceId(String bookingReferenceId);

    /**
     * Counts the total number of bookings for an employee.
     *
     * @param employeeId The employee ID to count bookings for
     * @return The number of bookings for the employee
     */
    long countByEmployeeId(String employeeId);
}

