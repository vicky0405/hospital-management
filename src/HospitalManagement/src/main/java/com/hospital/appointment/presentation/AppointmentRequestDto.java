package com.hospital.appointment.presentation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AppointmentRequestDto(
        @NotNull UUID patientId,
        @NotNull UUID doctorId,
        @NotNull UUID scheduleId,
        String notes
) {
}
