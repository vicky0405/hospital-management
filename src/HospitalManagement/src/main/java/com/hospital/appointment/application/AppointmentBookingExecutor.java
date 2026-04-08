package com.hospital.appointment.application;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentRepository;
import com.hospital.appointment.domain.AppointmentStatus;
import com.hospital.appointment.domain.SlotAlreadyBookedException;

@Component
class AppointmentBookingExecutor {

    private final AppointmentRepository appointmentRepository;

    AppointmentBookingExecutor(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    Appointment execute(UUID patientId, UUID doctorId, UUID scheduleId) {
        if (appointmentRepository.existsByDoctorIdAndScheduleId(doctorId, scheduleId)) {
            throw new SlotAlreadyBookedException(doctorId, scheduleId);
        }

        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .scheduleId(scheduleId)
                .status(AppointmentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return appointmentRepository.save(appointment);
    }
}