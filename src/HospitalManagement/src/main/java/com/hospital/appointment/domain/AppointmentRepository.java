package com.hospital.appointment.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

public interface AppointmentRepository {

    boolean existsByDoctorIdAndSlot(UUID doctorId, LocalDateTime slot);

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);
}
