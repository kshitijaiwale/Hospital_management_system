package com.hospital.app.queue.repository;

import com.hospital.app.queue.entity.QueueEntry;
import com.hospital.app.queue.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntry, UUID> {

    /** All entries for a given date, ordered by queue number — used for the receptionist's daily view. */
    List<QueueEntry> findByQueueDateOrderByQueueNumberAsc(LocalDate date);

    /** All WAITING entries for today, ordered — used to compute positions and find "next". */
    List<QueueEntry> findByQueueDateAndStatusOrderByQueueNumberAsc(LocalDate date, QueueStatus status);

    /** Find the entry for a specific appointment — used for patient status lookup. */
    Optional<QueueEntry> findByAppointmentAppointmentId(UUID appointmentId);

    /**
     * The highest queue number issued today — used to generate the next sequential token.
     * Returns empty if no entries exist for the date yet (first check-in of the day).
     */
    @Query("SELECT MAX(q.queueNumber) FROM QueueEntry q WHERE q.queueDate = :date")
    Optional<Integer> findMaxQueueNumberForDate(@Param("date") LocalDate date);

    /**
     * Count of WAITING entries ahead of a given queue number today.
     * "Ahead" means lower queue number (they checked in earlier).
     */
    @Query("""
            SELECT COUNT(q) FROM QueueEntry q
            WHERE q.queueDate = :date
              AND q.status = 'WAITING'
              AND q.queueNumber < :myQueueNumber
            """)
    int countWaitingAhead(@Param("date") LocalDate date, @Param("myQueueNumber") int myQueueNumber);

    /** The entry currently IN_CONSULTATION for a given date — at most one should exist at a time. */
    Optional<QueueEntry> findFirstByQueueDateAndStatus(LocalDate date, QueueStatus status);

    /** Check if an appointment already has a queue entry (prevents duplicate check-ins). */
    boolean existsByAppointmentAppointmentId(UUID appointmentId);
}