# FM School

FM School is a Spring Boot school-management application for the SEPM lab. It
manages database accounts, Teacher and Student profiles, courses, enrollment
requests, accepted enrollments and marks.

## Stack

- Java 25
- Spring Boot 4.1.1
- Maven
- PostgreSQL 18.6 through Docker Compose
- Spring Data JPA / Hibernate
- Spring Security
- Thymeleaf
- Lombok
- Liquibase
- Tailwind CSS 4.3.3 with committed compiled CSS

## Fast lab demo

The fastest Windows setup is documented in
[docs/Run on a Fresh PC.md](docs/Run%20on%20a%20Fresh%20PC.md). After Docker,
JDK 25 and Git are installed:

```powershell
git clone https://github.com/FahimHossen121/fm-school-academic.git
Set-Location .\fm-school-academic
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-demo.ps1
```

The runner starts PostgreSQL, starts Spring Boot if necessary, waits for the
application, loads the demo seed and opens the login page. It does not delete
the Docker volume.

Demo credentials:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `12345678` |
| Teacher | `teacher2` | `teacher2` |
| Student | `student2` | `student2` |

The seed creates five Teachers, fifteen Students and fifteen accepted
enrollments. See [demo/README.md](demo/README.md) for the complete list.

## Features

- Public Student and Teacher signup with role fixed by the endpoint.
- Database-backed login, logout, bcrypt password hashing and CSRF protection.
- Admin management of Teacher and Student accounts and profiles.
- Teacher and Student self-service profiles and account deletion.
- Student applications and Teacher invitations.
- Recipient-only acceptance/rejection of pending requests.
- Admin enrollment management.
- One accepted course per Student and many Students per Teacher.
- Teacher/Admin editing of accepted Student name, roll and marks from 0 to 100.
- JSON API with DTOs, validation, status codes and permission checks.
- Server-rendered Thymeleaf pages with locally compiled Tailwind CSS.

Attendance is intentionally outside the current lab scope. Responsive/mobile
behavior is also not a required deliverable; the project is demonstrated as a
desktop/lab application.

## Important database note

GitHub stores source code and migrations, not PostgreSQL records or Docker
volumes. The included seed recreates disposable demo data on another PC. For lab demonstrations, recreate the sample data using the seed. Moving actual
records requires a separate database backup/restore procedure; the current
complete guide focuses on seeded setup. Do not commit database dumps or private
credentials.

## Documentation

- [Fresh-PC and fast runner guide](docs/Run%20on%20a%20Fresh%20PC.md)
- [Complete project guide](docs/FM%20School%20-%20Complete%20Project%20Guide.md)
- [JSON API guide](docs/JSON%20API%20Guide.md)
- [Demo accounts and seed](demo/README.md)
- [Styling guide](docs/Styling.md)
- [Feature explanations and manual checks](docs/Feature%20Chunks.md)

- [Concise project lab report](docs/Lab%20Report.md)
