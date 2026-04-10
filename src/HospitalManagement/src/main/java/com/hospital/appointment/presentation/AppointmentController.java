package com.hospital.appointment.presentation;

import com.hospital.appointment.application.AppointmentService;
import com.hospital.appointment.domain.Appointment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    AppointmentResponseDto bookAppointment(@Valid @RequestBody AppointmentRequestDto request) {
        Appointment appointment = appointmentService.bookAppointment(
                request.patientId(),
                request.doctorId(),
                request.scheduleId(),
                request.notes()
        );
        return toDto(appointment);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    AppointmentResponseDto getAppointment(@PathVariable UUID id) {
        Appointment appointment = appointmentService.findById(id);
        return toDto(appointment);
    }

    private AppointmentResponseDto toDto(Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getScheduleId(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt()
        );
    }
}
