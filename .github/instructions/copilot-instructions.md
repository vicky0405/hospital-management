# Hospital Management System

## Architecture

- Clean Architecture: domain → application → infrastructure → presentation
- Never import Spring annotations into domain layer
- Never import infrastructure classes into domain layer

## Tech stack

- Spring Boot 3, Java 21, PostgreSQL, Kafka, Redis
- Lombok: use @Getter @Builder @NoArgsConstructor @AllArgsConstructor
- Never use @Data on JPA entities

## Database

- All migrations via Flyway only
- Table names: snake_case plural (patients, doctors, appointments)
- Always include created_at timestamp
- appointments table has version column for optimistic locking
- Use UUID for all primary keys

## Naming conventions

- Exceptions: SlotAlreadyBookedException, PatientNotFoundException
- Tests: methodName_shouldDoX_whenY
- Branch: feature/module-name

## Testing rules

- Unit test: no Spring context, use Mockito only
- Integration test: @SpringBootTest + Testcontainers
- Always test happy path AND edge cases

## Commit message format

- feat: add appointment booking
- fix: handle concurrent slot booking
- test: add integration test for slot conflict
- refactor: extract slot validation logic
- chore: add flyway migration
