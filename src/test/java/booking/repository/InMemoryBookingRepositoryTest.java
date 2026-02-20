package booking.repository;

import booking.entity.Booking;
import booking.repository.booking.impl.InMemoryBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the InMemoryBookingRepository implementation.
 *
 * <p>These tests verify the correct behavior of all CRUD operations
 * and edge cases for the in-memory repository implementation.
 */
@DisplayName("InMemoryBookingRepository Tests")
class InMemoryBookingRepositoryTest {

    private InMemoryBookingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookingRepository();
    }

    private Booking createTestBooking(String referenceId, String employeeId) {
        return Booking.builder()
                .bookingReferenceId(referenceId)
                .employeeId(employeeId)
                .resourceType("Flight")
                .destination("NYC")
                .departureDate("2024-11-05 08:00:00")
                .returnDate("2024-11-08 18:00:00")
                .travelerCount(1)
                .costCenterRef("CC-456")
                .tripPurpose("Client meeting")
                .build();
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("Should save new booking successfully")
        void shouldSaveNewBooking() {
            // Arrange
            Booking booking = createTestBooking("BKG-001", "EMP001");

            // Act
            Booking saved = repository.save(booking);

            // Assert
            assertNotNull(saved);
            assertEquals("BKG-001", saved.getBookingReferenceId());
            assertEquals("EMP001", saved.getEmployeeId());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());
            assertEquals("PENDING", saved.getStatus());
            assertEquals(1, repository.size());
        }

        @Test
        @DisplayName("Should update existing booking")
        void shouldUpdateExistingBooking() {
            // Arrange
            Booking booking = createTestBooking("BKG-001", "EMP001");
            repository.save(booking);

            // Act
            booking.setDestination("London");
            Booking updated = repository.save(booking);

            // Assert
            assertEquals("London", updated.getDestination());
            assertEquals(1, repository.size());
        }

        @Test
        @DisplayName("Should throw exception for null booking")
        void shouldThrowExceptionForNullBooking() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }

        @Test
        @DisplayName("Should throw exception for booking with null reference ID")
        void shouldThrowExceptionForNullReferenceId() {
            Booking booking = createTestBooking(null, "EMP001");
            assertThrows(IllegalArgumentException.class, () -> repository.save(booking));
        }
    }

    @Nested
    @DisplayName("Find By Reference ID Operations")
    class FindByReferenceIdOperations {

        @Test
        @DisplayName("Should find existing booking by reference ID")
        void shouldFindExistingBooking() {
            // Arrange
            Booking booking = createTestBooking("BKG-001", "EMP001");
            repository.save(booking);

            // Act
            Optional<Booking> found = repository.findByBookingReferenceId("BKG-001");

            // Assert
            assertTrue(found.isPresent());
            assertEquals("BKG-001", found.get().getBookingReferenceId());
        }

        @Test
        @DisplayName("Should return empty for non-existing booking")
        void shouldReturnEmptyForNonExisting() {
            // Act
            Optional<Booking> found = repository.findByBookingReferenceId("NON-EXISTING");

            // Assert
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Employee ID Operations")
    class FindByEmployeeIdOperations {

        @Test
        @DisplayName("Should find all bookings for employee")
        void shouldFindAllBookingsForEmployee() {
            // Arrange
            repository.save(createTestBooking("BKG-001", "EMP001"));
            repository.save(createTestBooking("BKG-002", "EMP001"));
            repository.save(createTestBooking("BKG-003", "EMP002"));

            // Act
            List<Booking> bookings = repository.findByEmployeeId("EMP001");

            // Assert
            assertEquals(2, bookings.size());
            assertTrue(bookings.stream().allMatch(b -> "EMP001".equals(b.getEmployeeId())));
        }

        @Test
        @DisplayName("Should return empty list for employee with no bookings")
        void shouldReturnEmptyListForNoBookings() {
            // Act
            List<Booking> bookings = repository.findByEmployeeId("NON-EXISTING");

            // Assert
            assertTrue(bookings.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find All Operations")
    class FindAllOperations {

        @Test
        @DisplayName("Should find all bookings")
        void shouldFindAllBookings() {
            // Arrange
            repository.save(createTestBooking("BKG-001", "EMP001"));
            repository.save(createTestBooking("BKG-002", "EMP002"));
            repository.save(createTestBooking("BKG-003", "EMP003"));

            // Act
            List<Booking> bookings = repository.findAll();

            // Assert
            assertEquals(3, bookings.size());
        }

        @Test
        @DisplayName("Should return empty list when no bookings exist")
        void shouldReturnEmptyListWhenEmpty() {
            // Act
            List<Booking> bookings = repository.findAll();

            // Assert
            assertTrue(bookings.isEmpty());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete existing booking")
        void shouldDeleteExistingBooking() {
            // Arrange
            repository.save(createTestBooking("BKG-001", "EMP001"));

            // Act
            boolean deleted = repository.deleteByBookingReferenceId("BKG-001");

            // Assert
            assertTrue(deleted);
            assertEquals(0, repository.size());
        }

        @Test
        @DisplayName("Should return false for non-existing booking")
        void shouldReturnFalseForNonExisting() {
            // Act
            boolean deleted = repository.deleteByBookingReferenceId("NON-EXISTING");

            // Assert
            assertFalse(deleted);
        }
    }

    @Nested
    @DisplayName("Update Status Operations")
    class UpdateStatusOperations {

        @Test
        @DisplayName("Should update booking status")
        void shouldUpdateBookingStatus() {
            // Arrange
            repository.save(createTestBooking("BKG-001", "EMP001"));

            // Act
            Optional<Booking> updated = repository.updateStatus("BKG-001", "CONFIRMED");

            // Assert
            assertTrue(updated.isPresent());
            assertEquals("CONFIRMED", updated.get().getStatus());
        }

        @Test
        @DisplayName("Should return empty for non-existing booking status update")
        void shouldReturnEmptyForNonExisting() {
            // Act
            Optional<Booking> updated = repository.updateStatus("NON-EXISTING", "CONFIRMED");

            // Assert
            assertTrue(updated.isEmpty());
        }
    }

    @Nested
    @DisplayName("Exists Operations")
    class ExistsOperations {

        @Test
        @DisplayName("Should return true for existing booking")
        void shouldReturnTrueForExisting() {
            // Arrange
            repository.save(createTestBooking("BKG-001", "EMP001"));

            // Act & Assert
            assertTrue(repository.existsByBookingReferenceId("BKG-001"));
        }

        @Test
        @DisplayName("Should return false for non-existing booking")
        void shouldReturnFalseForNonExisting() {
            // Act & Assert
            assertFalse(repository.existsByBookingReferenceId("NON-EXISTING"));
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountOperations {

        @Test
        @DisplayName("Should count bookings by employee ID")
        void shouldCountByEmployeeId() {
            // Arrange
            repository.save(createTestBooking("BKG-001", "EMP001"));
            repository.save(createTestBooking("BKG-002", "EMP001"));
            repository.save(createTestBooking("BKG-003", "EMP002"));

            // Act & Assert
            assertEquals(2, repository.countByEmployeeId("EMP001"));
            assertEquals(1, repository.countByEmployeeId("EMP002"));
            assertEquals(0, repository.countByEmployeeId("EMP003"));
        }
    }

    @Nested
    @DisplayName("Clear Operations")
    class ClearOperations {

        @Test
        @DisplayName("Should clear all bookings")
        void shouldClearAllBookings() {
            // Arrange
            repository.save(createTestBooking("BKG-001", "EMP001"));
            repository.save(createTestBooking("BKG-002", "EMP002"));

            // Act
            repository.clear();

            // Assert
            assertEquals(0, repository.size());
            assertTrue(repository.findAll().isEmpty());
        }
    }
}

