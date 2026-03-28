package com.hospital.appointment.domain;

public class SlotInPastException extends RuntimeException {

    public SlotInPastException(String message) {
        super(message);
    }
}
