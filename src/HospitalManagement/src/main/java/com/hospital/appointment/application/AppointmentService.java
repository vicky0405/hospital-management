package com.hospital.appointment.application;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentRepository;
import com.hospital.appointment.domain.AppointmentStatus;
import com.hospital.appointment.domain.SlotAlreadyBookedException;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment bookAppointment(UUID patientId, UUID doctorId, UUID scheduleId) {
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
