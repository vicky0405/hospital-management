package com.hospital.appointment.domain;

import java.time.LocalDateTime;

public class SlotInPastException extends RuntimeException {

    public SlotInPastException(LocalDateTime slot) {
        super("Slot is in the past: " + slot);
    }
}
