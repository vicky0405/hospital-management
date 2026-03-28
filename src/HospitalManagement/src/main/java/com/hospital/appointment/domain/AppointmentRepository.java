package com.hospital.appointment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AppointmentRepository {

    boolean existsByDoctorIdAndSlot(UUID doctorId, LocalDateTime slot);

    Appointment save(Appointment appointment);
}
