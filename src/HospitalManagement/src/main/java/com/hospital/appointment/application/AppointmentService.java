package com.hospital.appointment.application;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentNotFoundException;
import com.hospital.appointment.domain.AppointmentRepository;
import com.hospital.appointment.domain.SlotAlreadyBookedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentBookingExecutor bookingExecutor;
    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentBookingExecutor bookingExecutor,
                              AppointmentRepository appointmentRepository) {
        this.bookingExecutor = bookingExecutor;
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment bookAppointment(UUID patientId, UUID doctorId, UUID scheduleId, String notes) {
        try {
            return bookingExecutor.execute(patientId, doctorId, scheduleId, notes);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException(doctorId, scheduleId);
        }
    }

    public Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }
}
