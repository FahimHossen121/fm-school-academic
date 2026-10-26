# Lab Report: REST API, Spring Boot and Docker

## 1. Objectives

- To understand REST APIs and HTTP request–response communication.
- To implement a database-connected application using Spring Boot and MVC.
- To apply authentication, role-based access control and record ownership checks.
- To use Docker Compose for a reproducible PostgreSQL environment.

## 2. Introduction

FM School is a school-management application developed using Spring Boot and
PostgreSQL. It manages Admin, Teacher and Student accounts, personal profiles,
course enrollments and marks. The project combines Thymeleaf web pages with JSON
API endpoints. Spring Security controls access, while Docker runs the PostgreSQL
database in a container.

## 3. REST API, Spring Boot and Docker

### 3.1 REST API

REST is an architectural approach for building web services around resources.
Clients send HTTP requests to endpoints, and the server processes them and returns
a response. FM School exposes resources such as `/api/students`, `/api/teachers`
and `/api/enrollments`, using JSON for API request and response data.

| Method | Purpose | FM School example |
|---|---|---|
| GET | Retrieve data | `GET /api/students` |
| POST | Create a resource | `POST /api/students` |
| PUT | Update data | `PUT /api/students/{id}` |
| DELETE | Remove a resource | `DELETE /api/students/{id}` |

These Student collection operations are restricted to Admin. Teachers use the
scoped endpoint `/api/enrollments/{id}/student` to update a Student associated
with their accepted course.

HTTP status codes communicate the result: `200` indicates success, `201` resource
creation, `400` invalid input, `401` missing authentication, `403` forbidden access,
`404` a missing resource and `409` a conflict such as a duplicate username.
DTOs define permitted input and response fields, keeping password hashes out of
API responses. State-changing requests also require a valid CSRF token.

### 3.2 Spring Boot

**Project setup:** The Maven-based project was generated through Spring
Initializr and opened in IntelliJ IDEA. Its dependencies include Spring Web MVC,
Spring Data JPA, PostgreSQL Driver, Thymeleaf, Spring Security, Lombok, DevTools
and Docker Compose Support. Validation and Liquibase support were also added.

**MVC and layered structure:** Entities such as `Student`, `Teacher` and
`Enrollment` represent stored data. Thymeleaf templates display forms and tables,
while controllers receive requests and select responses. Services contain
business rules, and repositories perform database operations. For example,
`ProfilePageController` handles the profile page and delegates updates to
`ProfileService`. Compiled Tailwind CSS styles the Thymeleaf pages.

**Spring Data JPA:** Entities use annotations such as `@Entity`, `@Id` and
`@GeneratedValue`. Repository interfaces extend `JpaRepository`, providing
operations such as `save()`, `findAll()` and `findById()`. Liquibase creates and
updates the schema through versioned migrations, while Hibernate validates it.

**Spring Security:** Authentication verifies login credentials, while
authorization checks roles and ownership. Admin can manage all Teacher and
Student records, including marks. Teachers can edit only Students in their
accepted enrollments. Students can manage their own profiles and view their
marks. Passwords are stored as bcrypt hashes.

**Enrollment workflow:** A Student may apply to a Teacher, or a Teacher may invite
a Student. The recipient must accept before Teacher editing permission is granted.
Each Student may have only one accepted course, while a Teacher may have many
Students. Editing a Student's name or roll does not change their login credentials.

**PostgreSQL integration:** The database stores accounts, profiles and enrollment
records. Java connects through the PostgreSQL JDBC driver. Spring Boot's Docker
Compose support obtains connection details from `compose.yaml`; the browser
communicates with Spring Boot rather than directly with PostgreSQL.

### 3.3 Docker

Docker runs software in isolated containers. An image supplies the files needed
to create a container; a container is an instance of that image. FM School uses
the pinned `postgres:18.6` image, with database `fm_school_db` and a named volume
for persistent data.

The database can be started and inspected using:

```powershell
docker compose up -d postgres
docker compose ps
docker compose exec postgres psql -U myuser -d fm_school_db -c "SELECT id, name, roll FROM students;"
```

PostgreSQL is published locally on port `5432`, while Spring Boot serves the
application on port `8080`. In the current project, only PostgreSQL runs in
Docker; the Java application runs on Windows through IntelliJ or Maven. An
application Dockerfile has not been added. The included `run-demo.ps1` automates
local startup and loading of demonstration accounts and enrollments.

## 4. Discussion

FM School connects HTTP communication, MVC, persistence and security in one
application. HTML and JSON controllers reuse service-level rules, so direct API
requests cannot bypass role or ownership restrictions. Enrollment acceptance
provides a concrete example of authorization depending on both role and stored
relationships. Docker Compose and Liquibase make database setup reproducible,
while the demo seed supplies records on another PC without transferring a local
database volume.

## 5. Conclusion

The project demonstrated a database-backed school-management application using
Spring Boot, REST-style JSON APIs, Thymeleaf and Spring Security. Docker provided
the PostgreSQL runtime, and Liquibase managed schema changes. Together, these
components supported account management, controlled enrollments and scoped
Teacher access to Student information.

## 6. References

1. [FM School project repository](https://github.com/FahimHossen121/fm-school-academic)
2. [Spring Boot documentation](https://docs.spring.io/spring-boot/)
3. [Docker documentation](https://docs.docker.com/get-started/)
4. [Spring Initializr](https://start.spring.io/)
