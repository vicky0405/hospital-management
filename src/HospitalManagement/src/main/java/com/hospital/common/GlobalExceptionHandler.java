package com.hospital.common;

import com.hospital.appointment.domain.AppointmentNotFoundException;
import com.hospital.appointment.domain.SlotAlreadyBookedException;
import com.hospital.auth.domain.EmailAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    Map<String, String> handleBadCredentials(BadCredentialsException ex) {
        return Map.of("error", "Invalid email or password");
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> handleSlotAlreadyBooked(SlotAlreadyBookedException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> handleAppointmentNotFound(AppointmentNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }
}
