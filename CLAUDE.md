# Hospital Management System — CLAUDE.md

## Mục tiêu project

Hệ thống quản lý bệnh viện làm portfolio để xin việc Backend Java (Fresher/Junior).
Timeline: 8 tuần. Domain: Healthcare. Architecture: Modular Monolith + Clean Architecture.

---

## Tech stack

- Java 21, Spring Boot 3
- PostgreSQL (production), Testcontainers PostgreSQL (integration test)
- Flyway (DB migration)
- Kafka (async notification — chưa làm)
- Redis (cache — chưa làm)
- Lombok, Spring Security, Spring Data JPA
- JUnit 5, Mockito, AssertJ, Testcontainers
- Docker Compose
- Maven

---

## Cấu trúc thư mục

```
src/
├── main/java/com/hospital/
│   ├── HospitalManagementApplication.java
│   └── appointment/
│       ├── domain/
│       │   ├── Appointment.java
│       │   ├── AppointmentRepository.java       ← interface, no Spring/JPA
│       │   ├── AppointmentStatus.java            ← enum: PENDING/CONFIRMED/CANCELLED/COMPLETED
│       │   ├── SlotAlreadyBookedException.java
│       │   └── SlotInPastException.java
│       ├── application/
│       │   ├── AppointmentService.java           ← public, @Service, no @Transactional
│       │   └── AppointmentBookingExecutor.java   ← package-private, @Component, @Transactional
│       ├── infrastructure/
│       │   ├── AppointmentJpaEntity.java         ← @Entity, @Version, @Builder.Default version = 0
│       │   ├── AppointmentJpaRepository.java     ← package-private, extends JpaRepository
│       │   ├── AppointmentRepositoryImpl.java    ← @Repository, implements domain interface
│       │   └── AppointmentMapper.java            ← package-private, static methods
│       └── presentation/                         ← chưa làm, làm sau khi xong Auth
│
├── main/resources/
│   ├── application.yml                           ← spring.jpa.open-in-view=false
│   └── db/migration/
│       ├── V1__create_initial_schema.sql
│       └── V2__add_unique_constraint_appointments_schedule.sql
│
└── test/java/com/hospital/
    ├── HospitalManagementApplicationTests.java
    └── appointment/
        ├── application/
        │   └── AppointmentServiceTest.java       ← unit test, Mockito only
        └── integration/
            └── AppointmentConcurrentBookingTest.java  ← Testcontainers PostgreSQL
```

---

## DB schema (Flyway)

### V1 — 4 bảng chính

```sql
patients       (id uuid PK, full_name, email UNIQUE, phone, date_of_birth, insurance_number, created_at)
doctors        (id uuid PK, full_name, specialization, email UNIQUE)
doctor_schedules (id uuid PK, doctor_id FK, work_date, slot_start, slot_end, is_available bool,
                  UNIQUE(doctor_id, work_date, slot_start))
appointments   (id uuid PK, patient_id FK, doctor_id FK, schedule_id FK,
                status varchar(20) DEFAULT 'PENDING', notes text,
                version int NOT NULL DEFAULT 0, created_at)
```

### V2 — unique constraint

```sql
ALTER TABLE appointments
ADD CONSTRAINT uk_appointments_schedule_id UNIQUE (schedule_id);
-- 1 schedule chỉ có thể book bởi 1 bệnh nhân
-- schedule_id đã FK sang doctor_schedules nên không cần kèm doctor_id
```

---

## Những gì đã làm xong

### Module: auth (domain + application + infrastructure + presentation)

- [x] `Role` enum (ADMIN, DOCTOR, PATIENT)
- [x] `User` domain entity
- [x] `UserRepository` domain interface
- [x] `EmailAlreadyExistsException`
- [x] `JwtService` — generate/validate JWT, `@Value` for secret and expiry config
- [x] `AuthService` — register (hardcode PATIENT role) / login
- [x] `UserDetailsServiceImpl` — load user từ DB cho Spring Security
- [x] `UserJpaEntity` — `@Builder.Default createdAt = LocalDateTime.now()`
- [x] `UserJpaRepository` — package-private
- [x] `UserMapper`, `UserRepositoryImpl`
- [x] `RegisterRequest`, `LoginRequest`, `AuthResponseDto` (record types)
- [x] `AuthController` — `POST /api/auth/register`, `POST /api/auth/login`
- [x] `JwtAuthFilter` — `OncePerRequestFilter`, inject `UserDetailsService`
- [x] `SecurityConfig` — stateless, JWT filter, `/error` permitAll
- [x] `AuthExceptionHandler` — 409 EmailAlreadyExists, 401 BadCredentials (replaced by GlobalExceptionHandler)
- [x] Unit test: `JwtServiceTest`, `AuthServiceTest`
- [x] Integration test: `AuthIntegrationTest` (Testcontainers, TestRestTemplate)

### Module: appointment (domain + application + infrastructure)

- [x] `Appointment` domain entity (UUID id, patientId, doctorId, scheduleId, status, notes, createdAt, version)
- [x] `AppointmentRepository` domain interface (pure Java, no Spring)
- [x] `AppointmentStatus` enum
- [x] `SlotAlreadyBookedException`, `SlotInPastException`
- [x] `AppointmentBookingExecutor` — package-private, xử lý transaction boundary
- [x] `AppointmentService` — catch `DataIntegrityViolationException` → convert sang `SlotAlreadyBookedException`
- [x] `AppointmentJpaEntity` — có `@Version`, `@Builder.Default private Integer version = 0`
- [x] `AppointmentJpaRepository` — package-private Spring Data interface
- [x] `AppointmentRepositoryImpl` — implements domain interface
- [x] `AppointmentMapper` — convert domain ↔ JPA entity
- [x] Unit test: `AppointmentServiceTest` (mock `AppointmentBookingExecutor`)
- [x] Integration test: `AppointmentConcurrentBookingTest` (Testcontainers, 2 threads cùng book)

### Module: appointment (presentation)

- [x] `AppointmentRequestDto`, `AppointmentResponseDto` (record types)
- [x] `AppointmentController` — `POST /api/appointments`, `GET /api/appointments/{id}`
- [x] `@PreAuthorize("hasRole('PATIENT')")` for booking
- [x] `GlobalExceptionHandler` — replaces `AuthExceptionHandler`, covers all modules
- [x] `AppointmentNotFoundException`
- [x] Integration test: `AppointmentControllerIntegrationTest` (MockMvc + `@WithMockUser`)

### Quy trình

- [x] Feature branch → PR → self review → merge
- [x] Commit theo TDD order: test commit trước, implement commit sau
- [x] `.github/copilot-instructions.md` setup
- [x] `docs/decisions.md` ghi trade-off kiến trúc
- [x] `CLAUDE.md` setup cho Claude Code

---

## Vấn đề đã gặp và cách giải quyết

### 1. `@Transactional` không catch được `DataIntegrityViolationException`

**Vấn đề:** `DataIntegrityViolationException` throw SAU khi transaction commit, nên `try/catch` bên trong method `@Transactional` không bắt được.

**Giải pháp:** Tách ra `AppointmentBookingExecutor` — bean riêng có `@Transactional`. `AppointmentService` không có `@Transactional`, gọi executor rồi catch exception sau khi executor commit.

```
AppointmentService.bookAppointment()          ← no @Transactional, catch ở đây
    → AppointmentBookingExecutor.execute()    ← @Transactional, commit ở đây
    → DataIntegrityViolationException throw ra đúng chỗ catch
    → convert thành SlotAlreadyBookedException
```

### 2. Package name convention

Dùng `com.hospital.appointment.*` — không phải `com.example` hay `com.hospitalmanagement`.

### 3. `@Builder` + default value

Lombok `@Builder` ignore initializing expression. Phải dùng `@Builder.Default`:

```java
@Builder.Default
private Integer version = 0;
```

### 4. Concurrent booking — 2 tầng bảo vệ

- Tầng 1: `existsByDoctorIdAndScheduleId` check trước khi save
- Tầng 2: DB unique constraint `uk_appointments_schedule_id` làm lưới cuối

### 5. V2 migration — không có conflict

V1 không có unique constraint trên `appointments` — chỉ có FK constraints.
V2 thêm `UNIQUE(schedule_id)` là constraint duy nhất trên appointments.

### 6. Spring Security `/error` dispatch

**Vấn đề:** Spring Boot forward 404 → `/error` như một internal error dispatch. `OncePerRequestFilter` không chạy lại cho error dispatch → `SecurityContext` bị clear → `/error` trả 401 thay vì 404.

**Giải pháp:** Thêm `/error` vào `permitAll` trong `SecurityConfig`.

```java
.requestMatchers("/error").permitAll()
```

---

## Coding conventions

### Clean Architecture layers

```
domain       ← pure Java, không import Spring/JPA/Lombok @Data
application  ← @Service, @Component OK, không import infrastructure
infrastructure ← @Repository, JPA, Spring Data
presentation ← @RestController, DTO, mapper
```

### Lombok

```java
// Dùng
@Getter @Builder @NoArgsConstructor @AllArgsConstructor @Builder.Default

// Không dùng
@Data  // gây vấn đề với JPA lazy loading
```

### Test naming

```java
methodName_shouldDoX_whenY()
// Ví dụ:
bookAppointment_shouldThrowException_whenSlotAlreadyBooked()
```

### Exception

```java
// Có message format trong constructor, không hardcode string ở caller
public SlotAlreadyBookedException(UUID doctorId, UUID scheduleId) {
    super("Slot already booked for doctor " + doctorId + " at schedule " + scheduleId);
}
```

### Commit message

```
feat: add appointment booking with slot conflict detection
fix: handle concurrent booking via transaction boundary separation
test: add integration test for concurrent booking with Testcontainers
chore: add flyway migration for unique constraint
refactor: extract AppointmentBookingExecutor for proper transaction handling
```

### Constructor injection — không dùng `@Autowired`

```java
public AppointmentService(AppointmentBookingExecutor bookingExecutor) {
    this.bookingExecutor = bookingExecutor;
}
```

---

## Kiến trúc decisions quan trọng

### Modular Monolith (không phải Microservices)

Timeline 2 tháng, 1 người. Boundary rõ để tách microservice sau nếu cần.

### Schedule-based booking (không phải datetime-based)

`Appointment` lưu `scheduleId` FK sang `doctor_schedules`, không lưu `LocalDateTime slot` trực tiếp.
Validate slot còn hợp lệ (chưa qua, còn available) là trách nhiệm của `DoctorScheduleService` — chưa làm.

### AppointmentRepository là domain interface

Infrastructure không leak vào domain. `AppointmentJpaRepository` là package-private implementation detail.

### Appointment presentation layer làm sau Auth

Mọi API đều cần authenticate. Làm Controller trước khi có Auth thì phải sửa lại — tốn công gấp đôi.

---

## Bước tiếp theo ngay bây giờ

**DoctorSchedule module — `feature/doctor-schedule`:**

- Tạo branch `feature/doctor-schedule`
- CRUD lịch làm việc bác sĩ
- `GET /api/doctors/{doctorId}/schedules` — xem lịch còn trống
- `POST /api/doctors/{doctorId}/schedules` — ADMIN/DOCTOR tạo lịch
- `DoctorScheduleService` validate slot còn hợp lệ trước khi booking
- Làm theo TDD

---

## Bước tiếp theo (theo thứ tự)

1. **DoctorSchedule module** — CRUD lịch làm việc bác sĩ, validate slot trước booking ← đang làm
2. **Module: EMR** — hồ sơ bệnh án, append-only, versioning, không cho xoá
3. **Module: pharmacy** — kê đơn thuốc, kiểm tra tồn kho, concurrent inventory lock
4. **Module: billing** — viện phí, BHYT giả lập, transaction rollback
5. **Kafka** — notification async: `appointment.booked` → email xác nhận, nhắc lịch 24h trước
6. **Deploy** — Railway free tier, Docker Compose production config
7. **README + Postman collection + Swagger**

---

## Pending items

- [ ] `DoctorScheduleService` — validate slot còn hợp lệ trước khi booking
- [x] `AppointmentController`, `AppointmentRequestDto`, `AppointmentResponseDto` — done
- [x] `GlobalExceptionHandler` — done
- [x] Auth module — Spring Security + JWT + 3 roles — done
- [x] V3 migration — bảng users — done
- [x] Fix `@Builder.Default` trên `AppointmentJpaEntity.version` — done
- [x] `spring.jpa.open-in-view=false` — done
- [x] V2 migration conflict — không có conflict, đã xác nhận
