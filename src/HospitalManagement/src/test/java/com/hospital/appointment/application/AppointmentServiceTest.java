package com.hospital.appointment.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentStatus;
import com.hospital.appointment.domain.SlotAlreadyBookedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

        @Mock
        private AppointmentBookingExecutor bookingExecutor;

        @InjectMocks
        private AppointmentService appointmentService;

        private UUID patientId;
        private UUID doctorId;
        private UUID scheduleId;

        @BeforeEach
        void setUp() {
                patientId = UUID.randomUUID();
                doctorId = UUID.randomUUID();
                scheduleId = UUID.randomUUID();
        }

        @Test
        void bookAppointment_shouldSuccess_whenSlotAvailable() {
                // given
                Appointment expected = Appointment.builder()
                                .id(UUID.randomUUID())
                                .patientId(patientId)
                                .doctorId(doctorId)
                                .scheduleId(scheduleId)
                                .status(AppointmentStatus.PENDING)
                                .build();

                given(bookingExecutor.execute(patientId, doctorId, scheduleId))
                                .willReturn(expected);

                // when
                Appointment result = appointmentService.bookAppointment(patientId, doctorId, scheduleId);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getPatientId()).isEqualTo(patientId);
                assertThat(result.getDoctorId()).isEqualTo(doctorId);
                assertThat(result.getScheduleId()).isEqualTo(scheduleId);
                verify(bookingExecutor).execute(patientId, doctorId, scheduleId);
        }

        @Test
        void bookAppointment_shouldThrowSlotAlreadyBookedException_whenExecutorThrowsDataIntegrityViolationException() {
                // given
                given(bookingExecutor.execute(patientId, doctorId, scheduleId))
                                .willThrow(new DataIntegrityViolationException("Unique constraint violation"));

                // when / then
                assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, scheduleId))
                                .isInstanceOf(SlotAlreadyBookedException.class);
                verify(bookingExecutor).execute(patientId, doctorId, scheduleId);
        }

        @Test
        void bookAppointment_shouldThrowSlotAlreadyBookedException_whenExecutorThrowsItDirectly() {
                // given
                given(bookingExecutor.execute(patientId, doctorId, scheduleId))
                                .willThrow(new SlotAlreadyBookedException(doctorId, scheduleId));

                // when / then
                assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, scheduleId))
                                .isInstanceOf(SlotAlreadyBookedException.class);
                verify(bookingExecutor).execute(patientId, doctorId, scheduleId);
        }
}
