package com.hospital.appointment.application;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentRepository;
import com.hospital.appointment.domain.SlotAlreadyBookedException;
import com.hospital.appointment.domain.SlotInPastException;

public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment bookAppointment(UUID patientId, UUID doctorId, LocalDateTime slot) {
        if (slot.isBefore(LocalDateTime.now())) {
            throw new SlotInPastException("Slot is in the past: " + slot);
        }
        if (appointmentRepository.existsByDoctorIdAndSlot(doctorId, slot)) {
            throw new SlotAlreadyBookedException("Slot already booked for doctor " + doctorId + " at " + slot);
        }
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .slot(slot)
                .build();
        return appointmentRepository.save(appointment);
    }
}
