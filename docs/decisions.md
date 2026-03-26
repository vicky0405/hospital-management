# Kiến trúc: Modular Monolith

**Quyết định:** 1 Spring Boot app, chia module rõ ràng
**Lý do:** timeline 2 tháng, 1 người làm
**Trade-off:** không scale độc lập từng module,
nhưng boundary rõ để tách microservice sau nếu cần

# Database: PostgreSQL

**Lý do:** transaction thật, phù hợp healthcare data

# Async: Kafka

**Lý do:** notification không nên block main flow

# Kiến trúc code: Clean Architecture

**Quyết định:** Tổ chức code theo Clean Architecture
**Lý do:** Tách biệt business logic khỏi framework,
dễ test, dễ thay đổi infrastructure sau này

**Cấu trúc mỗi module:**
domain/ ← Entity, business rules, interface
application/ ← Use case, service
infrastructure/← Repository impl, Kafka, Email
presentation/ ← Controller, DTO, mapper

**Trade-off:** Boilerplate nhiều hơn, nhưng
business logic test được mà không cần Spring context
