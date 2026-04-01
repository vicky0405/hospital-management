package com.hospital.appointment.domain;

import java.util.UUID;
import java.util.Optional;

public interface AppointmentRepository {

    boolean existsByDoctorIdAndScheduleId(UUID doctorId, UUID scheduleId);

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);
}
