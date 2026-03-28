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
import com.hospital.appointment.domain.SlotAlreadyBookedException;
import com.hospital.appointment.domain.SlotInPastException;

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
        private LocalDateTime futureSlot;

        @BeforeEach
        void setUp() {
                patientId = UUID.randomUUID();
                doctorId = UUID.randomUUID();
                futureSlot = LocalDateTime.now().plusDays(1);
        }

        @Test
        void bookAppointment_shouldSuccess_whenSlotAvailable() {
                // given
                given(appointmentRepository.existsByDoctorIdAndSlot(doctorId, futureSlot))
                                .willReturn(false);

                Appointment expected = Appointment.builder()
                                .id(UUID.randomUUID())
                                .patientId(patientId)
                                .doctorId(doctorId)
                                .slot(futureSlot)
                                .build();

                given(appointmentRepository.save(any(Appointment.class)))
                                .willReturn(expected);

                // when
                Appointment result = appointmentService.bookAppointment(patientId, doctorId, futureSlot);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getPatientId()).isEqualTo(patientId);
                assertThat(result.getDoctorId()).isEqualTo(doctorId);
                assertThat(result.getSlot()).isEqualTo(futureSlot);
                verify(appointmentRepository).existsByDoctorIdAndSlot(doctorId, futureSlot);
                verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        void bookAppointment_shouldThrowException_whenSlotAlreadyBooked() {
                // given
                given(appointmentRepository.existsByDoctorIdAndSlot(doctorId, futureSlot))
                                .willReturn(true);

                // when / then
                assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, futureSlot))
                                .isInstanceOf(SlotAlreadyBookedException.class);
                verify(appointmentRepository, never()).save(any());
        }

        @Test
        void bookAppointment_shouldThrowException_whenSlotInPast() {
                // given
                LocalDateTime pastSlot = LocalDateTime.now().minusDays(1);

                // when / then
                assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, pastSlot))
                                .isInstanceOf(SlotInPastException.class);
        }

        @Test
        void bookAppointment_shouldThrowException_whenSlotIsToday_butAlreadyPassed() {
                LocalDateTime pastSlotToday = LocalDateTime.now().minusHours(1);

                assertThatThrownBy(() -> appointmentService.bookAppointment(patientId, doctorId, pastSlotToday))
                                .isInstanceOf(SlotInPastException.class);
        }
}
