# NovaTech Project Tracker

This document provides a complete guide to setting up, running, and testing the NovaTech Project Tracker application and its associated monitoring stack.

---

## 1. Project Overview

NovaTech Project Tracker is a Spring Boot-based web application for managing software development projects, tasks, and developers. After experiencing performance degradation under load, the application was optimized for scalability, efficiency, and observability.

**Key Features:**
- User Authentication (Registration & Login)
- Project, Task, and Developer CRUD Operations
- Advanced Task Filtering (by project, by developer, overdue)
- Role-based access control (Admin, Developer)
- Real-time performance monitoring (Actuator/Prometheus/Grafana)

---

## 2. Technology Stack

- **Backend:** Java 17, Spring Boot 3
- **Data:** Spring Data JPA, PostgreSQL
- **Security:** Spring Security, JWT
- **Caching:** Spring Caching, Caffeine
- **Monitoring:** Micrometer, Prometheus, Grafana
- **Containerization:** Docker, Docker Compose
- **Load Testing:** Apache JMeter

---

## 3. Prerequisites

- Docker and Docker Compose
- Java 17 or higher
- Apache JMeter
- API tool (Postman or curl)

---

## 4. Running the Application Stack

1. **Clone the repository**
   ```sh
   git clone https://github.com/baaki20/Project-Tracker.git
   cd Project-Tracker
   ```

2. **Build the Spring Boot Application**
   ```sh
   ./mvnw clean package
   ```

3. **Launch the Environment**
   ```sh
   docker-compose up --build -d
   ```

   This will build and run containers for the Spring Boot app, PostgreSQL, Prometheus, and Grafana.

4. **Verify Services**
   ```sh
   docker-compose ps
   ```
   All services should show a status of `Up`.

---

## 5. Accessing Services

- **Project Tracker API:** http://localhost:8080
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (Login: admin / admin)

Prometheus data source and JVM dashboard are pre-configured.

---

## 6. Performance Testing

- JMeter test plan (`.jmx`) is included in `src/test/jmeter`.
- Open in JMeter, target `localhost:8080`, and run the test.
- Observe results in JMeter listeners and Grafana dashboards.

---

## 7. Grading Criteria & Implementation

### 7.1 Profiling & Load Testing (15)
- **Tools Used:** JProfiler for profiling, JMeter for load testing.
- **Implementation:** JMeter scripts simulate concurrent users; JProfiler used to analyze CPU/memory hotspots.
- **Evidence:** Profiling screenshots and JMeter reports included in `/docs/performance`.

### 7.2 Memory & GC Analysis (10)
- **Heap & GC:** JVM options enabled for GC logging; heap dumps analyzed post-load.
- **Thread States:** Monitored via VisualVM and Actuator `/actuator/metrics/jvm.threads.states`.
- **Findings:** Memory leaks resolved; GC pauses minimized.

### 7.3 DTO Mapping & API Optimization (15)
- **DTOs:** All API responses use DTOs (see `dto/` package).
- **Mapping:** MapStruct used for entity-to-DTO conversion.
- **Result:** Reduced payload size and improved serialization speed.

### 7.4 Spring Caching Implementation (10)
- **Cache:** Caffeine configured with TTL and size limits.
- **Usage:** `@Cacheable` applied to read-heavy endpoints (e.g., project/task lists).
- **Eviction:** TTL and max size ensure stale data is purged.

### 7.5 Advanced Exception Handling & Logging (10)
- **Global Handler:** Centralized `@ControllerAdvice` for consistent error responses.
- **Logging:** Errors logged with context; sensitive info masked.

### 7.6 Spring Boot Actuator & Monitoring (10)
- **Endpoints:** `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` exposed.
- **Custom Metrics:** Application-specific metrics registered via Micrometer.
- **Visualization:** Grafana dashboards for JVM and business metrics.

### 7.7 Optimization Benchmarking Report (15)
- **Before/After:** Detailed report in `/docs/performance/benchmark.md`.
- **Metrics:** Latency, throughput, memory, and CPU usage compared pre/post-optimization.

### 7.8 Code Quality, Extensibility & Documentation (10)
- **Code:** Modular, well-documented, and follows SOLID principles.
- **Docs:** This README and inline code comments explain architecture and optimizations.

### 7.9 Bonus: Prometheus & Docker (5)
- **Prometheus:** Integrated for metrics scraping.
- **Docker:** Full stack containerized; `docker-compose.yml` provided for easy deployment.

---

## 8. Key Optimizations Implemented

- **Memory & Database:** `FetchType.LAZY` for relationships, `JOIN FETCH` to avoid N+1.
- **API Layer:** DTOs via MapStruct for efficient serialization.
- **Caching:** Caffeine for fast, in-memory caching of frequent queries.
- **Monitoring:** Actuator, Prometheus, and Grafana for real-time insights.

[Performance Enhancement Analysis](https://docs.google.com/document/d/1Jn4xHDcLPnPmP_oqQhclInU1OnL83Dg5OOytQX5aT4k/edit?usp=sharing)

---

## 9. References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [JMeter User Manual](https://jmeter.apache.org/usermanual/)
- [Prometheus Docs](https://prometheus.io/docs/)
- [Grafana Docs](https://grafana.com/docs/)

---

