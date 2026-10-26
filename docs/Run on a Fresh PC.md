# FM School: run on a fresh Windows PC

> **Fast lab-demo path:** The current project uses database-backed Admin, Teacher and Student accounts. After the first application startup creates the schema, one PowerShell command loads the complete demo dataset. The seeded Admin login is `admin` / `12345678`; demo Teacher and Student passwords equal their usernames.

This guide explains how to download the project from GitHub, prepare a Windows lab PC, and run the same application there. The repository is `https://github.com/FahimHossen121/fm-school-academic.git`.

The fastest route is in the next section. The remaining sections explain the setup in detail and help troubleshoot the lab PC.

## Fast lab-demo setup

Use this path when Docker Desktop, Git, IntelliJ and JDK 25 are already installed.

### One-command runner

The repository includes [`run-demo.ps1`](../run-demo.ps1), a Windows PowerShell runner for the complete local demonstration. It starts PostgreSQL, starts Spring Boot if it is not already running, waits for the application, loads the Admin/Teacher/Student demo records, and opens the login page. It never deletes the Docker volume.

Open PowerShell in the project root and run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-demo.ps1
```

The execution-policy command affects only the current PowerShell window; it does not change the PC permanently. The script starts Spring Boot in a separate process. Its output is saved to `run-demo.log` and `run-demo-error.log`; these are local runtime logs and should not be committed.

The runner uses the Maven Wrapper, so Maven does not need to be installed separately. JDK 25, Docker Desktop and a working Docker engine are still required. The first run may take longer while Maven downloads dependencies and Docker downloads PostgreSQL.

1. Open Docker Desktop and wait until it reports that the engine is running.
2. Clone and enter the project:

```powershell
git clone https://github.com/FahimHossen121/fm-school-academic.git
Set-Location .\fm-school-academic
```

3. Start PostgreSQL:

```powershell
docker compose up -d postgres
```

4. Open this folder in IntelliJ, select JDK 25, allow Maven to finish importing, and run `com.example.fmschool.FmSchoolApplication`.
5. Wait until the Run console reports that the application started successfully. This first startup lets Liquibase create the tables. The Admin generated during startup is not important because the seed sets the demo Admin password.
6. In a second PowerShell terminal opened in the project root, run the complete demo seed:

```powershell
Get-Content -LiteralPath .\demo\seed-demo-data.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U myuser -d fm_school_db
```

7. Open [http://localhost:8080/login](http://localhost:8080/login) and use:

```text
Admin:   admin / 12345678
Teacher: teacher2 / teacher2
Student: student2 / student2
```

The seed creates five Teachers, fifteen Students and fifteen accepted enrollments. It is safe to run for a fresh demo database and does not reset Teacher/Student passwords, profiles or marks. It deliberately ensures the demo Admin password is `12345678`. Read [demo/README.md](../demo/README.md) for all demo accounts.

If the seed command reports that a table does not exist, the application has not completed its first startup yet. Wait for successful startup and run the seed command again. If the Admin login fails, run the seed command against the same project folder and database that the application is using.

## 1. What travels through GitHub?

GitHub stores source files and their committed history. The repository should include `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn`, `src`, `compose.yaml`, and project documentation. Later it should also include Tailwind input files, `package.json`, `package-lock.json`, and the documented CSS build command.

Do not upload `.idea`, `target`, `out`, `node_modules`, local `.env` files, private tokens, or database volume contents. A PostgreSQL volume belongs to the PC where Docker created it. It is not downloaded by cloning GitHub.

Our local handbook contains original screenshots, including temporary generated login passwords. Keep that original local. Before placing it in the public repository, prepare a public copy whose sensitive screenshots are redacted or omitted. Preserve the local originals.

## 2. Install or check the tools

The lab already has Docker and IntelliJ, and software installation is allowed. Check rather than assume that their versions and configuration meet the project's needs.

| Tool | Why it is needed | Current project choice |
|---|---|---|
| Git | Download source and exchange changes | Git for Windows |
| IntelliJ IDEA | Open, build and run Java | Community edition is sufficient for our current steps |
| JDK | Compile and execute Java | Java 25; the development PC uses OpenJDK 25.0.1 |
| Docker Desktop | Run PostgreSQL | Start it and wait until its engine is ready |
| Node.js and npm | Rebuild Tailwind CSS after styling changes | Not needed for a normal demo because compiled CSS is committed |

There is no need to install a separate PostgreSQL server or standalone Maven for the current IDE workflow. Docker runs PostgreSQL; IntelliJ can use bundled Maven. The project also carries a Maven Wrapper, although its IDE import failed on the development PC and bundled Maven succeeded.

Download tools from their official sites: [Git](https://git-scm.com/downloads/win), [IntelliJ](https://www.jetbrains.com/idea/download/), [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/), and [Node.js](https://nodejs.org/en/download). Restart IntelliJ after installations that change PATH so its terminals receive the updated environment.

## 3. Start Docker

Open Docker Desktop. Wait until the engine is running. In a PowerShell terminal, check:

```powershell
docker --version
docker compose version
docker info
```

The first checks the Docker command, the second checks Compose, and the third checks communication with the running engine. If `docker` is not recognized after installation, close and reopen the terminal or IntelliJ. If the engine cannot start, use Docker Desktop's reported WSL/virtualization diagnostic rather than changing the Java project.

## 4. Download the project

Choose a folder in your own lab-PC user account, for example `Documents\SEPM`. Open PowerShell in that folder. Do not clone inside another copy of `fm-school`.

```powershell
git clone https://github.com/YOUR_USERNAME/fm-school.git
```

```powershell
cd fm-school
```

```powershell
git status
```

The working tree should be clean immediately after cloning. Public repositories can be cloned without signing in. Pushing changes requires authentication and write access.

If Git is unavailable, GitHub's **Code -> Download ZIP** can download a snapshot. Extract it and open the folder containing `pom.xml`. A ZIP does not include Git history or support normal `git pull`/`git push`; cloning is preferred for continuing work.

## 5. Open the correct folder in IntelliJ

1. Choose **File -> Open**.
2. Select the downloaded `fm-school` folder containing `pom.xml`, or select `pom.xml` and open it as a project.
3. Trust the repository only after confirming it is your own project.
4. Wait for Maven import and dependency downloads. The first import needs internet access and may take several minutes.

Do not open the containing downloads directory or create a second Java project around the downloaded project.

## 6. Select Java 25

1. Open **File -> Project Structure -> Project**.
2. Set the project SDK to a JDK 25 installation.
3. If absent, use **Add SDK -> Download JDK** and select version 25. A JDK is required, not just a Java runtime.
4. Use the project's language level or SDK default compatible with its `pom.xml`.
5. Ensure the module inherits the project SDK.

The IDE's own runtime can differ from the project SDK. Also, an IDE SDK can work even when `java` is not on PowerShell's PATH. Run through IntelliJ for this guide; configuring shell `JAVA_HOME` is only necessary if you intend to use the Java/Maven Wrapper from the terminal.

## 7. Import Maven and enable Lombok support

1. Open **Settings -> Build, Execution, Deployment -> Build Tools -> Maven**.
2. Select **Bundled Maven** for the Maven home path, matching the successful development-PC import.
3. Under **Importing**, use the project JDK for the importer where that selection is available.
4. Under **Runner**, use the project JDK.
5. Reload the Maven project using the Maven panel or the synchronization prompt.
6. Install/enable IntelliJ's Lombok plugin if the IDE cannot recognize the generated getters/setters. Restart if prompted.
7. If Lombok-generated methods still fail to compile, check **Compiler -> Annotation Processors -> Enable annotation processing**, then rebuild. Do not remove working entity code just to silence an IDE inspection.

The plugin helps the editor understand Lombok. The dependency in `pom.xml` supplies Lombok to the Java build. They have different jobs. The Docker IntelliJ plugin is optional; Docker Desktop and the Compose command are sufficient.

## 8. Start PostgreSQL from this project's Compose file

Open IntelliJ's terminal with **Alt + F12**. Confirm the prompt ends in this downloaded `fm-school` directory.

```powershell
docker compose up -d postgres
```

```powershell
docker compose ps
```

Our recorded configuration uses the `postgres:18.6` image, database `fm_school_db`, and a named volume mounted at `/var/lib/postgresql`. The published port on the development PC is `127.0.0.1:5432:5432`. PostgreSQL 18's storage layout is the reason for this mount location.

The Compose credentials currently discussed are local demonstration values (`myuser` / `secret`). They are not production credentials. Use this database only for lab/demo data and retain the loopback binding. Do not commit real passwords or personal records.

If another service already uses host port 5432, change only the host side to an available port such as `127.0.0.1:5433:5432`. PostgreSQL still listens on port 5432 inside the container. With Spring Boot's Compose support, connection details normally follow the published mapping; update a manually configured datasource URL if one is introduced later.

For a startup failure, read the database log:

```powershell
docker compose logs postgres
```

Do not delete volumes to solve an unexplained failure. `docker compose down -v` deletes the project's persistent database volume. It is not part of normal setup.

## 9. Run Spring Boot

1. Locate `src/main/java/com/example/fmschool/FmSchoolApplication.java`.
2. Open it and click the green Run arrow beside the class or `main` method.
3. Wait for the console to report successful startup, Tomcat on port 8080, and a successful PostgreSQL connection.
4. If startup fails, inspect the first relevant error and its cause; a browser error cannot fix an application that never started.

Only PostgreSQL runs in Docker at this stage. Spring Boot runs as a Java process on the lab PC. A future Dockerfile for the application is part of the assignment but has not been implemented.

### Liquibase and demo-data checkpoint

The current changelog is `src/main/resources/db/changelog/db.changelog-master.yaml`. On the first startup, Liquibase creates the schema and records the applied changesets in the database. Hibernate uses `spring.jpa.hibernate.ddl-auto=validate`, so it checks the schema rather than silently changing it.

A fresh lab volume starts with an empty database. After the application starts successfully, run the seed command from the [fast lab-demo setup](#fast-lab-demo-setup). Migrations create tables; the seed creates demo records.

## 10. Open and verify the application

Open [http://localhost:8080/login](http://localhost:8080/login). The normal demo login is `admin` / `12345678`. The seed also provides `teacher2` / `teacher2` and `student2` / `student2`.

For a quick server check, open `/api/hello` after signing in. The application also exposes the JSON API documented in [JSON API Guide.md](JSON%20API%20Guide.md). The login page and the role-specific home pages are the main demonstration screens.

## 11. Tailwind styling

The compiled CSS is already committed, so Node.js is not required to run the application on the lab PC. Install Node.js only when changing styles or templates that introduce new Tailwind classes. From the project root:

```powershell
npm.cmd ci
npm.cmd run css:build
```

Commit both the editable source under `src/main/resources/styles/` and the generated file under `src/main/resources/static/css/`.

## 12. Continue on two PCs without losing changes

Before leaving home, save files, review changes, commit and push:

```powershell
git status
git diff
git add .
git diff --cached
git commit -m "Describe the work completed"
git push
```

Run commands separately and stop on errors. Review staged content before committing, especially new configuration files. If there are no changes, no commit is needed.

At the lab, for an existing clean clone, download newer commits:

```powershell
git status
git pull --ff-only
```

Do not pull over uncommitted work. Commit or deliberately preserve it first. A failed fast-forward means the histories need review, not a force push.

After lab changes, commit and push before leaving. At home, pull before editing again. An unpushed commit remains only on that PC.

On a shared PC, avoid storing GitHub credentials for later users. Authenticate only when pushing, and sign out/remove saved credentials before leaving if they were retained. Never share a token in a screenshot or terminal transcript.

## 13. Stop for the day

Stop the Java application with IntelliJ's Stop button. Stop the project containers while preserving the named volume:

```powershell
docker compose stop
```

If the lab clears user files or Docker volumes between sessions, local records may disappear. GitHub preserves pushed source history; it is not a backup of unexported database contents.

## 14. Fresh-PC acceptance checklist

- Correct repository cloned into its own folder.
- JDK 25 selected, Maven import successful, editor recognizes Lombok.
- Docker engine reachable; Compose PostgreSQL service starts.
- Java application starts and opens a PostgreSQL connection.
- Liquibase applies changesets 001–005 on first startup.
- The seed command completes without an SQL error.
- `/login` accepts `admin` / `12345678`.
- `/api/hello` returns JSON after login.
- Manage Teachers, Manage Students and My Enrollments open successfully.
- Tailwind CSS is visible from the committed compiled stylesheet.
- A test modification can be pushed and downloaded on the other PC.

**Verification status:** the fast path is based on the committed Compose file, Liquibase changelog and demo seed. Confirm the Docker engine, JDK 25 and Maven import on the actual lab PC before the demonstration.
