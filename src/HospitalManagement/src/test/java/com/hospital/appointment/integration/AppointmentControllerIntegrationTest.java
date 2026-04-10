package com.hospital.appointment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.appointment.application.AppointmentService;
import com.hospital.appointment.domain.Appointment;
import com.hospital.appointment.domain.AppointmentNotFoundException;
import com.hospital.appointment.domain.AppointmentStatus;
import com.hospital.appointment.domain.SlotAlreadyBookedException;
import com.hospital.appointment.presentation.AppointmentRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
class AppointmentControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    // ── POST /api/appointments ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "PATIENT")
    void bookAppointment_shouldReturn201WithBody_whenPatientRoleAndValidRequest() throws Exception {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        Appointment booked = Appointment.builder()
                .id(id)
                .patientId(patientId)
                .doctorId(doctorId)
                .scheduleId(scheduleId)
                .status(AppointmentStatus.PENDING)
                .notes("Khám tổng quát")
                .createdAt(LocalDateTime.now())
                .build();

        when(appointmentService.bookAppointment(any(), any(), any(), any())).thenReturn(booked);

        AppointmentRequestDto request = new AppointmentRequestDto(patientId, doctorId, scheduleId, "Khám tổng quát");

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.notes").value("Khám tổng quát"));
    }

    @Test
    void bookAppointment_shouldReturn401_whenNoTokenProvided() throws Exception {
        AppointmentRequestDto request = new AppointmentRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void bookAppointment_shouldReturn403_whenDoctorRole() throws Exception {
        AppointmentRequestDto request = new AppointmentRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void bookAppointment_shouldReturn409_whenSlotAlreadyBooked() throws Exception {
        UUID doctorId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        when(appointmentService.bookAppointment(any(), any(), any(), any()))
                .thenThrow(new SlotAlreadyBookedException(doctorId, scheduleId));

        AppointmentRequestDto request = new AppointmentRequestDto(
                UUID.randomUUID(), doctorId, scheduleId, null);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ── GET /api/appointments/{id} ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "PATIENT")
    void getAppointment_shouldReturn200WithBody_whenAppointmentExists() throws Exception {
        UUID id = UUID.randomUUID();
        Appointment appointment = Appointment.builder()
                .id(id)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .scheduleId(UUID.randomUUID())
                .status(AppointmentStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(appointmentService.findById(id)).thenReturn(appointment);

        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getAppointment_shouldReturn401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/appointments/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void getAppointment_shouldReturn404_whenAppointmentNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(appointmentService.findById(id)).thenThrow(new AppointmentNotFoundException(id));

        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isNotFound());
    }
}
