package com.hospital.appointment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.hospital.appointment.application.AppointmentService;
import com.hospital.appointment.domain.SlotAlreadyBookedException;

@Testcontainers
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AppointmentConcurrentBookingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID patientId;
    private UUID doctorId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();

        jdbcTemplate.update("DELETE FROM appointments");
        jdbcTemplate.update("DELETE FROM doctor_schedules");
        jdbcTemplate.update("DELETE FROM doctors");
        jdbcTemplate.update("DELETE FROM patients");

        jdbcTemplate.update(
                "INSERT INTO patients (id, full_name, email, phone, date_of_birth, insurance_number) VALUES (?, ?, ?, ?, ?, ?)",
                patientId,
                "Test Patient",
                "patient+" + patientId + "@hospital.com",
                "0123456789",
                LocalDate.of(1995, 1, 1),
                "INS-001");

        jdbcTemplate.update(
                "INSERT INTO doctors (id, full_name, specialization, email) VALUES (?, ?, ?, ?)",
                doctorId,
                "Test Doctor",
                "Cardiology",
                "doctor+" + doctorId + "@hospital.com");

        jdbcTemplate.update(
                "INSERT INTO doctor_schedules (id, doctor_id, work_date, slot_start, slot_end, is_available) VALUES (?, ?, ?, ?, ?, ?)",
                scheduleId,
                doctorId,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                true);
    }

    @Test
    void bookAppointment_shouldCreateSingleAppointment_whenTwoThreadsBookSameScheduleConcurrently() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Runnable bookingTask = () -> {
            readyLatch.countDown();

            try {
                startLatch.await();
                appointmentService.bookAppointment(patientId, doctorId, scheduleId, null);
                successCount.incrementAndGet();
            } catch (SlotAlreadyBookedException ex) {
                failureCount.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            } finally {
                doneLatch.countDown();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstAttempt = executorService.submit(bookingTask);
            Future<?> secondAttempt = executorService.submit(bookingTask);

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();

            firstAttempt.get(5, TimeUnit.SECONDS);
            secondAttempt.get(5, TimeUnit.SECONDS);
        } finally {
            executorService.shutdownNow();
        }

        Integer appointmentsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE schedule_id = ?",
                Integer.class,
                scheduleId);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(appointmentsCount).isEqualTo(1);
    }

}
