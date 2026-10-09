# FM School

A school management project for SEPM lab, developed step by step to learn Spring Boot.

## Stack

- Java 25
- Spring Boot 4.1.1
- Maven
- PostgreSQL 18.6 through Docker Compose
- Spring Data JPA
- Spring Security
- Thymeleaf
- Lombok
- Liquibase
- Compiled Tailwind CSS — planned

## Current progress

- Spring Boot startup and PostgreSQL connectivity were demonstrated on the development PC before adding Liquibase.
- PostgreSQL runs in Docker with a named volume for persistent storage.
- `/api/hello` returned JSON in the demonstrated run.
- The basic Student entity and repository are written.
- The Liquibase dependency is added, but its first migration is unfinished and not verified.
- Student CRUD, custom accounts and role permissions are not implemented yet.
- Tailwind compilation is not configured yet.

### Known migration issue

The current draft changelog is in `src/main/db/changelog/db.changelog-master.yaml`, outside the resources directory. It also needs YAML formatting corrections and the column name `di` corrected to `id`. The intended location is `src/main/resources/db/changelog/db.changelog-master.yaml`.

This initial commit preserves the learning checkpoint. A fresh startup with the Liquibase dependency is not yet verified and may fail until that checkpoint is completed.

## Run on another PC

Follow [the fresh-PC setup guide](docs/Run%20on%20a%20Fresh%20PC.md).

GitHub stores committed project files. Existing PostgreSQL volume contents do not travel with the repository. Migrations reproduce the schema once implemented; existing records need sample data or a separate export/import procedure.

The Compose credentials are local demonstration values, not production credentials. The database port is bound to localhost.

## Planned permissions

- Admin: full control.
- Teacher: own profile and course-specific marks/attendance for accepted students.
- Student: own profile.
- One Teacher teaches one course; Students and Teachers have a many-to-many relationship.
- Student applications and Teacher invitations require acceptance by the recipient.
- Removing a course association keeps both accounts.
- Students and Teachers may delete their own accounts.

## Working between home and lab

Pull before editing a clean clone. Review, commit and push before leaving either PC. Unpushed commits and unexported database records remain on that PC.
