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
│       │   ├── AppointmentJpaEntity.java         ← @Entity, @Version for optimistic locking
│       │   ├── AppointmentJpaRepository.java     ← package-private, extends JpaRepository
│       │   ├── AppointmentRepositoryImpl.java    ← @Repository, implements domain interface
│       │   └── AppointmentMapper.java            ← package-private, static methods
│       └── presentation/                         ← chưa làm (Controller, DTO)
│
├── main/resources/
│   ├── application.yml
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
                status varchar(20), notes text, version int NOT NULL DEFAULT 0, created_at)
```

### V2 — unique constraint

ALTER TABLE appointments
ADD CONSTRAINT uk_appointments_schedule_id UNIQUE (schedule_id);

---

## Những gì đã làm xong

### Module: appointment (domain + application + infrastructure)

- [x] `Appointment` domain entity (UUID id, patientId, doctorId, scheduleId, status, notes, createdAt, version)
- [x] `AppointmentRepository` domain interface (pure Java, no Spring)
- [x] `AppointmentStatus` enum
- [x] `SlotAlreadyBookedException`, `SlotInPastException`
- [x] `AppointmentBookingExecutor` — xử lý transaction boundary
- [x] `AppointmentService` — catch `DataIntegrityViolationException` → convert sang `SlotAlreadyBookedException`
- [x] `AppointmentJpaEntity` — có `@Version`, `@Builder.Default private Integer version = 0`
- [x] `AppointmentJpaRepository` — package-private Spring Data interface
- [x] `AppointmentRepositoryImpl` — implements domain interface
- [x] `AppointmentMapper` — convert domain ↔ JPA entity
- [x] Unit test: `AppointmentServiceTest` (mock `AppointmentBookingExecutor`)
- [x] Integration test: `AppointmentConcurrentBookingTest` (Testcontainers, 2 threads cùng book)

### Quy trình

- [x] Feature branch → PR → self review → merge
- [x] Commit theo TDD order: test commit trước, implement commit sau
- [x] `.github/copilot-instructions.md` setup
- [x] `docs/decisions.md` ghi trade-off kiến trúc

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
- Tầng 2: DB unique constraint làm lưới cuối — khi 2 thread vượt qua tầng 1 cùng lúc

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
@Getter @Builder @NoArgsConstructor @AllArgsConstructor

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

---

## Bước tiếp theo (theo thứ tự)

1. **Fix V2 migration** — làm rõ unique constraint: `(schedule_id)` hay `(doctor_id, schedule_id)`
2. **Module: auth** — Spring Security, JWT, Refresh Token, 3 roles: Admin/Doctor/Patient
3. **Module: EMR** — hồ sơ bệnh án, append-only, versioning, không cho xoá
4. **Module: pharmacy** — kê đơn thuốc, kiểm tra tồn kho, concurrent inventory lock
5. **Module: billing** — viện phí, BHYT giả lập, transaction rollback
6. **Kafka** — notification async: appointment.booked → email xác nhận, nhắc lịch 24h trước
7. **Controller + DTO** — REST API cho appointment module, Swagger/OpenAPI
8. **Deploy** — Railway free tier, Docker Compose production config
9. **README** — mô tả hệ thống, kiến trúc, Postman collection

---

## Cần làm ngay (pending items)

- [ ] Kiểm tra conflict giữa V1 (`uq_appointments_doctor_schedule` trên `doctor_id, schedule_id`) và V2 (`uk_appointments_schedule_id` trên `schedule_id`) — quyết định giữ cái nào
- [ ] `AppointmentController` và `AppointmentDTO` cho presentation layer
- [ ] `DoctorScheduleService` để validate slot còn hợp lệ trước khi booking
- [ ] Fix warning: `@Builder will ignore the initializing expression` — thêm `@Builder.Default` vào `AppointmentJpaEntity.version`
