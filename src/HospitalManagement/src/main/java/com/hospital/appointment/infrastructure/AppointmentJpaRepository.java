package com.hospital.appointment.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    boolean existsByDoctorIdAndScheduleId(UUID doctorId, UUID scheduleId);
}
