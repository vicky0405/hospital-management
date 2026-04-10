package com.hospital.appointment.application;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentNotFoundException;
import com.hospital.appointment.domain.AppointmentRepository;
import com.hospital.appointment.domain.AppointmentStatus;
import com.hospital.appointment.domain.SlotAlreadyBookedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentBookingExecutor bookingExecutor;

    @Mock
    private AppointmentRepository appointmentRepository;

    private AppointmentService appointmentService;

    private UUID patientId;
    private UUID doctorId;
    private UUID scheduleId;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(bookingExecutor, appointmentRepository);
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
    }

    // ── bookAppointment ───────────────────────────────────────────────────────

    @Test
    void bookAppointment_shouldSuccess_whenSlotAvailable() {
        Appointment expected = Appointment.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .doctorId(doctorId)
                .scheduleId(scheduleId)
                .status(AppointmentStatus.PENDING)
                .notes("Khám tổng quát")
                .build();

        given(bookingExecutor.execute(patientId, doctorId, scheduleId, "Khám tổng quát"))
                .willReturn(expected);

        Appointment result = appointmentService.bookAppointment(patientId, doctorId, scheduleId, "Khám tổng quát");

        assertThat(result.getPatientId()).isEqualTo(patientId);
        assertThat(result.getNotes()).isEqualTo("Khám tổng quát");
        verify(bookingExecutor).execute(patientId, doctorId, scheduleId, "Khám tổng quát");
    }

    @Test
    void bookAppointment_shouldThrowSlotAlreadyBookedException_whenExecutorThrowsDataIntegrityViolationException() {
        given(bookingExecutor.execute(patientId, doctorId, scheduleId, null))
                .willThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, scheduleId, null))
                .isInstanceOf(SlotAlreadyBookedException.class);
    }

    @Test
    void bookAppointment_shouldThrowSlotAlreadyBookedException_whenExecutorThrowsItDirectly() {
        given(bookingExecutor.execute(patientId, doctorId, scheduleId, null))
                .willThrow(new SlotAlreadyBookedException(doctorId, scheduleId));

        assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, scheduleId, null))
                .isInstanceOf(SlotAlreadyBookedException.class);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_shouldReturnAppointment_whenExists() {
        UUID id = UUID.randomUUID();
        Appointment appointment = Appointment.builder()
                .id(id)
                .patientId(patientId)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        given(appointmentRepository.findById(id)).willReturn(Optional.of(appointment));

        Appointment result = appointmentService.findById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void findById_shouldThrowAppointmentNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(appointmentRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.findById(id))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
