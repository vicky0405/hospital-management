package com.hospital.appointment.presentation;

import com.hospital.appointment.domain.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id,
        UUID patientId,
        UUID doctorId,
        UUID scheduleId,
        AppointmentStatus status,
        String notes,
        LocalDateTime createdAt
) {
}
