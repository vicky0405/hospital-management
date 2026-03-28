package com.hospital.appointment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(UUID doctorId, LocalDateTime slot) {
        super("Slot already booked for doctor " + doctorId + " at " + slot);
    }
}