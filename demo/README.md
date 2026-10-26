# Optional local demo accounts

This seed is for local lab demonstrations. It is not run automatically during application startup.

Every demo password equals its username. Passwords are stored as bcrypt hashes in the SQL file and database. The credentials are deliberately public demo values: do not use them on a public deployment or for real records.

| Teacher name / username | Password | Course | Accepted Students |
|---|---|---|---|
| teacher2 | teacher2 | Demo Course 2 | student2, student3, student4 |
| teacher3 | teacher3 | Demo Course 3 | student5, student6, student7 |
| teacher4 | teacher4 | Demo Course 4 | student8, student9, student10 |
| teacher5 | teacher5 | Demo Course 5 | student11, student12, student13 |
| teacher6 | teacher6 | Demo Course 6 | student14, student15, student16 |

Student names and usernames are exactly `student2` through `student16`; each password matches its username. Rolls are `9000002` through `9000016`. These 15 associations are ACCEPTED and marked as created by ADMIN. Marks start blank for testing.

## Create this data on the lab PC

1. Clone the project, start Docker, and run FM School so Liquibase applies changesets 001–005.
2. In a PowerShell terminal in the project root, run:

```powershell
Get-Content -LiteralPath .\demo\seed-demo-data.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U myuser -d fm_school_db
```

3. Open `/login` and log in as one of the demo users.
4. A Teacher can open My Enrollments and edit only the three assigned Students' name, roll and marks. A Student sees their one accepted course and read-only marks.

The seed adds data in a single transaction. It does not delete data, change the existing Admin, or reset existing users' passwords/profiles. Re-running skips existing usernames/profiles and Teacher–Student pairs. If an existing demo Student already belongs to another accepted course, the unique rule aborts the transaction instead of silently moving them. Do not re-run it to undo tests: it preserves changed profiles and marks, and may recreate associations that were removed.

Name validation keeps the letters/spaces rule for ordinary names but permits `studentN`/`teacherN` demo labels so these seeded profiles remain editable. This is a deliberate demo exception.
