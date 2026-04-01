package com.hospital.appointment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private UUID scheduleId;
    private AppointmentStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private Integer version;
}
