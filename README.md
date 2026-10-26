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
- The Student migration was verified; Student creation, listing and editing have been demonstrated. Deletion is implemented and awaits manual confirmation.
- Database-backed accounts, hashed passwords, login/logout and Admin-only Student/account management are implemented in chunk 2A, pending browser verification.
- Teacher/Student personal profiles and account deletion are implemented in chunk 2B.
- Chunk 3 implements applications, invitations, recipient-only acceptance/rejection, association removal and Admin control. The associated Teacher may edit Student name/roll and course marks (0–100). Attendance is out of scope.
- Tailwind compilation is not configured yet.

### Login after chunk 2A

The old generated `user` account is replaced by database accounts. On startup, Liquibase adds the `accounts` table without changing the applied Student changeset. If no Admin exists, the application creates username `admin` and prints `INITIAL ADMIN PASSWORD` once in the Run console. Keep that password locally: it persists across restarts and is stored only as a hash in the database.

Alternatively set `FM_ADMIN_PASSWORD` in the run configuration before the first Admin is created (8–64 characters, at most 72 UTF-8 bytes). Changing this environment variable later does not reset an existing Admin password. Do not commit passwords in the shared run configuration.

Log in at `/login`. Admin Home has exactly two management choices: **Manage Teachers** (`/admin/teachers`) and **Manage Students** (`/admin/students`). Each section lists the relevant logins and profiles, with Add, Edit and Delete actions. Add chooses the role automatically, collects credentials, then opens the personal-profile form. Teacher/Student users complete their own `/me` profile. Only Admin may manage the general Student collection, both HTML and JSON.

Teacher/Student Home links to **My Enrollments** (`/enrollments`). Students apply to Teachers; Teachers invite Students. Only the recipient may accept/reject pending requests. Admin can manage all requests and directly create accepted associations through **Manage Enrollments** inside either management section. Each Student may have only one accepted course; a Teacher may have many Students. Participants may remove an accepted association without deleting either account. Rejected requests may be sent again; duplicate pending/accepted pairs are blocked. The associated Teacher (or Admin) may use **Edit Student** to edit name, roll and marks; the Student sees marks read-only. Name/roll changes do not change the login username or password.

## Run on another PC

Follow [the fresh-PC setup guide](docs/Run%20on%20a%20Fresh%20PC.md).

GitHub stores committed project files. Existing PostgreSQL volume contents do not travel with the repository. Migrations reproduce the schema once implemented; existing records need sample data or a separate export/import procedure.

The Compose credentials are local demonstration values, not production credentials. The database port is bound to localhost.

## Planned permissions

- Admin: full control.
- Teacher: own name/course, invitations and incoming applications, and name/roll/marks of Students accepted into their own course.
- Student: own profile.
- One Teacher teaches one course and may enroll many Students; one Student may have only one accepted course.
- Student applications and Teacher invitations require acceptance by the recipient.
- Removing a course association keeps both accounts.
- Students and Teachers may delete their own accounts.

## Working between home and lab

Optional ready-to-test accounts and course assignments are documented in [demo/README.md](demo/README.md). The opt-in SQL seed recreates five Teachers and fifteen Students on a fresh lab database. Existing database volumes are still not included in Git.

Pull before editing a clean clone. Review, commit and push before leaving either PC. Unpushed commits and unexported database records remain on that PC.
