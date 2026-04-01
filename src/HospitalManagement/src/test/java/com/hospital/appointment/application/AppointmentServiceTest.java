package com.hospital.appointment.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hospital.appointment.application.AppointmentService;
import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentRepository;
import com.hospital.appointment.domain.AppointmentStatus;
import com.hospital.appointment.domain.SlotAlreadyBookedException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

        @Mock
        private AppointmentRepository appointmentRepository;

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
                given(appointmentRepository.existsByDoctorIdAndScheduleId(doctorId, scheduleId))
                                .willReturn(false);

                Appointment expected = Appointment.builder()
                                .id(UUID.randomUUID())
                                .patientId(patientId)
                                .doctorId(doctorId)
                                .scheduleId(scheduleId)
                                .status(AppointmentStatus.PENDING)
                                .build();

                given(appointmentRepository.save(any(Appointment.class)))
                                .willReturn(expected);

                // when
                Appointment result = appointmentService.bookAppointment(patientId, doctorId, scheduleId);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getPatientId()).isEqualTo(patientId);
                assertThat(result.getDoctorId()).isEqualTo(doctorId);
                assertThat(result.getScheduleId()).isEqualTo(scheduleId);
                verify(appointmentRepository).existsByDoctorIdAndScheduleId(doctorId, scheduleId);
                verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        void bookAppointment_shouldThrowException_whenSlotAlreadyBooked() {
                // given
                given(appointmentRepository.existsByDoctorIdAndScheduleId(doctorId, scheduleId))
                                .willReturn(true);

                // when / then
                assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, scheduleId))
                                .isInstanceOf(SlotAlreadyBookedException.class);
                verify(appointmentRepository, never()).save(any());
        }
}
