package com.hospital.appointment.infrastructure;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppointmentJpaRepository jpaRepository;

    public AppointmentRepositoryImpl(AppointmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByDoctorIdAndScheduleId(UUID doctorId, UUID scheduleId) {
        return jpaRepository.existsByDoctorIdAndScheduleId(doctorId, scheduleId);
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentJpaEntity entity = AppointmentMapper.toJpaEntity(appointment);
        AppointmentJpaEntity saved = jpaRepository.save(entity);
        return AppointmentMapper.toDomain(saved);
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(AppointmentMapper::toDomain);
    }
}
