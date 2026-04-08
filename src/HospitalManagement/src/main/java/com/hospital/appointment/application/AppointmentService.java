package com.hospital.appointment.application;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.SlotAlreadyBookedException;

@Service
public class AppointmentService {

    private final AppointmentBookingExecutor bookingExecutor;

    public AppointmentService(AppointmentBookingExecutor bookingExecutor) {
        this.bookingExecutor = bookingExecutor;
    }

    public Appointment bookAppointment(UUID patientId, UUID doctorId, UUID scheduleId) {
        try {
            return bookingExecutor.execute(patientId, doctorId, scheduleId);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException(doctorId, scheduleId);
        }
    }
}
