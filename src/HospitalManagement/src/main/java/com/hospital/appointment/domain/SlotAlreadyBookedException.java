package com.hospital.appointment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(UUID doctorId, UUID scheduleId) {
        super("Slot already booked for doctor " + doctorId + " at " + scheduleId);
    }
}