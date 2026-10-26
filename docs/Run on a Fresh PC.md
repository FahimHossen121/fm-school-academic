# FM School: run on a fresh Windows PC

> **26 October 2026 update — chunk 2A:** Student migration, creation/listing and editing have since been demonstrated. Account security is now implemented and awaits manual startup verification. Old stopping-point notes below describe earlier checkpoints. The old default `user` login is replaced: use `admin` with the `INITIAL ADMIN PASSWORD` printed once on first account creation, or the `FM_ADMIN_PASSWORD` you set before that creation. Passwords persist and are not regenerated on each run. See the README and Feature Chunks guide for current checks. Teacher/Student own-profile access is the next chunk. The repository is `https://github.com/FahimHossen121/fm-school-academic.git`; use this actual URL in clone commands instead of the older placeholder.

This guide explains how to download the project from GitHub, prepare a Windows lab PC, and run the same application there. It records the current setup on 9 October 2026. Replace `YOUR_USERNAME` in example URLs with the repository owner's GitHub username.

The project is still being built. The working endpoint is `/api/hello`. Student CRUD, custom accounts/roles, Tailwind compilation, and the first verified Liquibase migration are not complete. Downloading the source does not complete those features.

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
| Node.js and npm | Compile Tailwind CSS later | Not currently needed for `/api/hello`; document exact supported version when Tailwind is added |

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

### Liquibase checkpoint

The dependency was synced, but the first migration has not been executed successfully. During the initial Git review, the draft was found at `src/main/db/changelog/db.changelog-master.yaml`, outside the resources directory. It has YAML spacing/indentation issues and uses `di` instead of `id` for the primary-key column. These are unfinished learning steps; they have not been silently corrected in this Git setup commit. Before calling this guide fully verified, complete the guided migration step and commit its corrected files.

The planned changelog is `src/main/resources/db/changelog/db.changelog-master.yaml`. Liquibase should create the `students` table and record the migration; Hibernate should validate entity-to-table mapping. If the application reports a missing changelog or table, this checkpoint is unfinished. Do not switch back to Hibernate `update` simply to hide the migration error.

A fresh lab volume starts with a fresh database. Migrations reproduce schema, not the development PC's Student records. Sample data or a deliberate database export/import will be added if demonstrations require existing records. No seed/export workflow is currently implemented.

## 10. Open and verify the working endpoint

Open `http://localhost:8080/api/hello` in the lab PC's browser.

Until custom security is implemented, sign in with username `user` and the generated development password from **that run's console**. The development-PC screenshot password is not the lab password.

Expect a JSON response with a message such as `Hello from FM School`. This verifies the controller endpoint, not Student CRUD. `/` currently has no homepage mapping and can legitimately return 404 after authentication.

If `/api/hello` returns 404 after editing the controller, save, stop and rerun the Java application, then try again. Verify the package and mapping rather than assuming the database is responsible for every HTTP error.

## 11. Tailwind checkpoint for later

Node.js is chosen for compiling Tailwind, while Java remains the server. Tailwind configuration and npm scripts do not exist yet. Do not run a guessed build command.

When implemented, this guide must be updated with the exact Node version, `npm ci`, the actual CSS build script, and the generated CSS path. Commit the lockfile and input/configuration; exclude `node_modules`. We will decide whether to also commit compiled CSS or generate it during setup/build.

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
- `/api/hello` returns JSON after login.
- First migration applies successfully once implemented; database table inspected.
- Exact Tailwind build succeeds once implemented.
- A test modification can be pushed and downloaded on the other PC.

**Verification status:** this guide has been authored, but no fresh lab-PC run has yet been observed. We must test the procedure on a fresh clone before claiming portability is proven.
