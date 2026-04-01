package com.hospital.appointment.infrastructure;

import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

class AppointmentMapper {

        private AppointmentMapper() {
        }

        static AppointmentJpaEntity toJpaEntity(Appointment appointment) {
                return AppointmentJpaEntity.builder()
                                .id(appointment.getId() != null ? appointment.getId() : UUID.randomUUID())
                                .patientId(appointment.getPatientId())
                                .doctorId(appointment.getDoctorId())
                                .scheduleId(appointment.getScheduleId()) // ← đổi
                                .notes(appointment.getNotes()) // ← thêm
                                .status(appointment.getStatus() != null
                                                ? appointment.getStatus()
                                                : AppointmentStatus.PENDING)
                                .version(appointment.getVersion())
                                .createdAt(appointment.getCreatedAt() != null
                                                ? appointment.getCreatedAt()
                                                : LocalDateTime.now())
                                .build();
        }

        static Appointment toDomain(AppointmentJpaEntity entity) {
                return Appointment.builder()
                                .id(entity.getId())
                                .patientId(entity.getPatientId())
                                .doctorId(entity.getDoctorId())
                                .scheduleId(entity.getScheduleId()) // ← đổi
                                .notes(entity.getNotes()) // ← thêm
                                .status(entity.getStatus())
                                .version(entity.getVersion())
                                .createdAt(entity.getCreatedAt())
                                .build();
        }
}
