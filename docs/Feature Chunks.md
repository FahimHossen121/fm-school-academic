# Feature chunks and manual checks

We now work one feature chunk at a time: code and explanation first, student testing and understanding next. Do not treat a chunk as verified until its checks pass.

**Latest scope, 26 October 2026:** one accepted course per Student; many Students per Teacher. The accepted Student's Teacher may edit the Student's name, roll and marks (0–100). Login username/password are separate and unchanged by profile edits. Students can read their own marks but cannot change them. Attendance remains excluded. This supersedes the earlier many-to-many and no-marks design notes below.

## Roadmap

1. Finish Student HTML editing and deletion.
2. Persist accounts; implement login, logout, password hashing and Admin/Teacher/Student permissions.
3. Add each Teacher's course and Student/Teacher enrollment requests with recipient acceptance.
4. Complete the remaining REST endpoints and validation/error contracts. Attendance is excluded; marks were reintroduced for accepted enrollments.
5. Complete JSON CRUD, compiled Tailwind styling, application Docker packaging, tests and fresh-PC documentation.

**API chunk completed in code:** Student/Teacher JSON CRUD, own-profile API, session/logout, CSRF endpoint, and enrollment/marks API now reuse the existing business and ownership rules. See [JSON API Guide.md](JSON%20API%20Guide.md). API responses use DTOs, and errors use JSON status envelopes. Account + profile creation is transactional. Verification: 53 isolated tests and 17 live API checks with disposable records passed. Application Docker packaging and final fresh-PC QA remain. Compiled Tailwind styling is now implemented; see the styling checkpoint below.

Role and ownership restrictions belong in the backend, including requests submitted directly rather than through the UI. They are not implemented in chunk 1.

## Chunk 1: Student editing and deletion

### Responsibilities

- `StudentPageController`: GET loads a Student into `StudentForm`; POST validates updates or requests deletion. Successful writes redirect to the list. Missing IDs produce 404 through the service.
- `StudentService`: update loads the existing entity, changes name/roll and saves it without changing the ID. Delete loads the target and deletes it. `@Transactional` makes each operation a database transaction; exceptions roll it back. JPA can also flush changes to a managed entity at commit, but the explicit `save()` keeps the persistence step visible while learning.
- `student-edit.html`: renders the populated form and validation errors; uses the Student ID in the form action. Thymeleaf integrates the POST form with Spring Security's CSRF protection.
- `students.html`: provides Add, Edit and Delete actions. Delete is POST and has no confirmation popup as requested.

The form DTO contains no ID. The path identifies which Student to update. This is not an authorization check: chunk 2 must restrict who may target that ID.

### Student's manual checks

1. Restart FM School from IntelliJ; open `/students` and sign in with the current default development account if needed.
2. Create a disposable Student using an English-letter name and digit-only roll.
3. Click Edit: confirm existing values are filled in.
4. Change the name/roll and save: confirm the row changes and its ID stays the same. Check `/api/students` for the same change.
5. Edit with roll `abc`: confirm an error appears, the input remains and the database value stays unchanged. Also test blank name and a name containing digits.
6. Refresh the redirected list: confirm no duplicate record was created.
7. Delete the disposable Student: confirm it disappears from the HTML list and JSON list.
8. Open `/students/999999999/edit` for an ID absent from the database: expect 404.

Only a Java compile check is performed during coding. Browser interactions and database effects remain for the student to verify. No migration changes are needed for this chunk.

## Chunk 2A: accounts, login and role boundaries

### What changed and why

- `002-create-accounts` adds an accounts table. Changeset 001 is preserved because it was already applied. No existing Student records are altered.
- `Account` contains username, password hash and role. An account is a login identity; linking it to the Student/Teacher profile is the next chunk.
- `SecurityConfig` supplies database-backed `UserDetailsService`, a delegating password encoder (bcrypt for new passwords), and HTTP access rules. Spring Security verifies submitted passwords against hashes. Session form login fits Thymeleaf; HTTP Basic is also available for API testing. CSRF protection remains enabled, including for writes made by API clients.
- `/students`, `/api/students` and `/admin` paths, including descendants, require ADMIN. `StudentService` and `AccountService` also enforce Admin authority at the method level. Hiding a button is not the permission check.
- `AdminBootstrap` creates one initial Admin only when none exists. Username is `admin`. With no `FM_ADMIN_PASSWORD` environment variable it generates and prints a random password once. It stores only the hash. Retain this local password; restarting does not generate a replacement. A configured password is used only for initial creation, never for resetting an existing account.
- Admin may create Teacher and Student logins. Duplicate usernames, invalid forms, unsupported roles and overlong bcrypt inputs are rejected. The create form does not let anyone create a second Admin. Passwords are neither displayed in the account list nor repopulated into the form after errors.
- `/` is a signed-in home page with logout. Teachers/Students currently see a placeholder for the personal-record workflow. Their profile CRUD and account deletion are still required in chunk 2B.
- Error responses no longer include stack traces or exception messages by default. A 404/403 status remains meaningful without exposing Java internals.

### Manual test

1. Restart FM School. Find the initial Admin password in the Run console (`INITIAL ADMIN PASSWORD`), or use the configured `FM_ADMIN_PASSWORD` if this is the first creation.
2. Open `/login` and log in as `admin`. The old `user` login no longer exists.
3. Open Manage Students. Confirm existing list/create/edit functionality still works as Admin.
4. Open Manage Accounts and create one TEACHER and one STUDENT login with passwords of 8–64 characters. Try a duplicate username: expect a readable form error.
5. Log out using the home page button. Try a wrong password, then the correct Teacher login.
6. As Teacher, manually open `/students`, `/students/new`, a Student edit URL, `/api/students`, and `/admin/accounts`: expect 403. Repeat with Student. These are collection-management routes, not their future own-record routes.
7. As either non-Admin account, the home page must work and logout must return to login. The placeholder is expected for this chunk.
8. Restart and log in with the same Admin credentials: the stored account must remain usable. Check `SELECT username, role, password_hash FROM accounts;` locally if desired: values should be hashes starting with `{bcrypt}`, not the original passwords. Do not publish screenshots of credentials or hashes.

Automated access-rule tests use the real security filter chain with a mocked account repository and isolated test routes. They do not apply migrations, start Docker, or verify browser template rendering. Manual startup verifies those integration points.

## Chunk 2B: personal profiles and account deletion

### Responsibilities and choices

- Migration 003 adds a nullable unique `students.account_id` foreign key and a `teachers` table. Teacher holds one name and one course. Previously applied changesets are preserved. Existing standalone Students remain as legacy records; new profiles are linked to their login automatically.
- An Account identifies who can log in. Its linked Student or Teacher identifies the personal record. IDs in the two tables need not be the same.
- `ProfileService` obtains the current username from Spring Security, then loads that Account and its own record. The form has no record-ID selection; updates resolve the profile from the authenticated login. Admin-only methods accept account IDs and check method permissions.
- `ProfilePageController` uses `/me` for personal read/create/update and `/me/delete` for self-deletion. The same profile template is used by Admin through `/admin/accounts/{id}/profile`, with role-specific name/roll or name/course fields.
- First save creates a profile if absent and links it automatically to the selected login. Later saves update that same profile. Manual linking by Student ID was removed at the user's request.
- Self-deletion removes the linked personal record and login, then invalidates the session. Other accounts remain. Admin can delete Teacher/Student accounts; the single Admin is protected from deletion. There is no confirmation popup, as requested.
- The new account link is excluded from Student JSON, so the list API cannot expose account password hashes through entity relationships.
- Teacher access to associated Students and enrollment acceptance follow in chunk 3. Teacher still cannot use Admin's general Student CRUD URLs. Marks and attendance have since been removed from scope.

### Manual checks

1. Restart FM School and confirm migration 003 succeeds. Existing account passwords remain unchanged.
2. Log in as `teacher1`; click Home -> My Profile. Fill Name `Teacher One`, Course `SEPM`; click Save Profile. Expect Profile saved. Refresh and confirm values remain.
3. Log out from Home and log in as `student1`. Open My Profile; fill Name `Student One`, Roll `0012345`; save. Leading zeros must remain. Try a text roll and confirm validation prevents saving.
4. Log in as Admin; open Manage Accounts -> Manage Profile beside teacher1/student1. Confirm and edit their saved details.
5. Confirm the managed profile form contains only Name and Roll for a Student; there is no manual Student-ID linking field.
6. As Teacher/Student, manually request `/admin/accounts/1/profile` or another managed-account URL: expect 403. `/students` remains Admin-only. `/me?id=999` still resolves the logged-in user's own record.
7. Create a disposable Student login through Admin and complete its profile. As that test user, click Delete Account on My Profile: expect logout; the same credentials must no longer log in. The linked Student disappears while unrelated accounts and Students remain.

Verification during coding: Java compilation and 14 isolated security/ownership tests passed. Tests cover role denial, CSRF, self-update ignoring foreign Student IDs, self-deletion identity, Teacher course targeting, blocking takeover of assigned Students, and protecting the single Admin. Browser rendering, migration execution and database behavior remain manual checkpoints.

## Admin navigation consolidation

The Admin home now has strictly **Manage Teachers** and **Manage Students**. There is no separate Manage Accounts navigation. Earlier instructions mentioning the accounts list are superseded by these category pages.

- `/admin/teachers` lists Teacher username, name and course.
- `/admin/students` lists Student username, name and roll. Legacy standalone Student records appear once with `No login linked`; linked Students appear once with their login.
- Each page provides its role-specific Add action. The credentials step has no role selector: the route fixes the role on the server, then opens the personal-profile step.
- Edit and Delete actions manage the linked login/profile together. Standalone records retain their existing edit/delete routes. Saving a managed profile returns to the correct category.
- `/students` redirects to the unified Student management page. The old `/admin/accounts` landing page redirects Home. Internal account-ID profile routes remain protected and are implementation details, not another management menu.

Verification: compilation and 14 security/ownership tests passed. Live browser checks confirmed both category pages, correct listed data, the Add Student credentials page, and exactly two Admin management links. No records were deleted or created during these navigation checks.

## Chunk 3: enrollment requests and accepted associations

**Scope correction:** marks and attendance are not needed and will not be added. Teacher management of Students means invitations, applications, accepted-course membership and removal of that membership; it does not permit editing a Student's personal name/roll.

### What each file does

- `Enrollment` is the explicit join entity for the many-to-many relationship. Each row refers to one Teacher and one Student, stores PENDING/ACCEPTED/REJECTED, and records whether STUDENT, TEACHER or ADMIN initiated it. A Teacher still has just one course, stored on the Teacher record.
- Liquibase changeset 004 adds this table, a unique Teacher/Student pair constraint, and cascading foreign keys. Existing migrations and personal records are preserved. Deleting a person cleans up their associated join rows; deleting a join row does not delete either person.
- `EnrollmentRepository` queries only the logged-in account's participation unless Admin asks for all. Write operations lock an existing request row so concurrent accept/reject operations cannot both replace its pending state.
- `EnrollmentService` derives the current account from the security context. Student applies using their own Student record; Teacher invites using their own Teacher record. A supplied target ID chooses the other participant, never the sender identity.
- `respond` checks recipient identity before updating a pending request. A Student application requires the target Teacher's response; a Teacher invitation requires the target Student's response. Unrelated users and senders get 403. Admin may act on any request.
- A second request for an already pending pair returns a readable conflict message. An opposite-direction request never silently accepts the first request. An accepted pair cannot be duplicated. Rejected pairs can request again by reusing the same row.
- `remove` permits either participant to remove ACCEPTED membership. Sender cancellation of PENDING requests remains unavailable, as agreed; Admin may remove any state.
- `getBoard` creates safe view DTOs. Invitation choices show Student names only. A Teacher sees a Student's roll on an ACCEPTED association, not a pending request. Each non-Admin board contains only the account's own participation.
- `EnrollmentPageController` accepts POST actions and redirects with readable success/conflict messages. CSRF remains active on every form. Permission failures remain 403 and missing records 404.
- `enrollments.html` displays role-specific application/invitation forms, statuses, recipient-only buttons and accepted memberships. No marks/attendance fields exist.

### Manual test A: Student applies; Teacher accepts

1. Ensure teacher1 and student1 have completed their personal profiles.
2. Restart FM School. Log out of Admin, then log in as student1.
3. Home -> My Enrollments. Choose Teacher One / SEPM under Apply to a Teacher; click Apply.
4. Expect PENDING and a success message. The sender must have no Accept/Reject/Remove buttons.
5. Try Apply again for the same Teacher: expect a pending-request message, not a second row.
6. Home -> Log out. Log in as teacher1 -> My Enrollments. Find the Student's PENDING application; click Accept.
7. Expect ACCEPTED, the Student's roll visible, and a Remove button. Log in as student1 again and confirm the same accepted association appears.
8. Click Remove as one participant. Confirm the association disappears, while both logins and personal profiles remain.

### Manual test B: Teacher invites; Student accepts/rejects

1. After removing the earlier association, log in as teacher1 -> My Enrollments.
2. Under Invite a Student, choose Student One and click Invite. Expect PENDING, with no Accept/Reject for the sender.
3. Log in as student1 -> My Enrollments. Click Reject. Expect REJECTED, with no accepted membership.
4. Log in as teacher1 and invite that Student again. The same rejected pair becomes PENDING without an extra association row.
5. Log in as student1 and Accept. Expect ACCEPTED on both accounts.

### Manual test C: Admin and isolation

1. As Admin: Home -> Manage Teachers (or Manage Students) -> Manage Enrollments.
2. Select a Teacher and Student and click Associate: Admin directly sets ACCEPTED without recipient approval.
3. Admin can view all pairs, respond to pending requests and remove associations in any state. These actions do not change people's roles.
4. With a second Teacher/Student, confirm they cannot see another pair on My Enrollments. A direct POST for another pair must return 403 even with a valid CSRF token.
5. Confirm one Teacher may enroll multiple Students, but a Student already accepted into one course cannot be accepted into another. Pending associations are not memberships.

### Verification performed during coding

- Compilation and 30 isolated tests passed (16 enrollment workflow tests, 6 profile ownership tests, 8 security rules tests).
- Tests cover both request directions, recipient-only acceptance/rejection, unrelated-account denial, duplicates, pending cancellation denial, retry after rejection, many-to-many requests, role boundaries, Admin direct association, and membership removal without account deletion.
- Live database inspection confirmed changeset 004, unique pair constraint, and both cascading foreign keys.
- The live Admin enrollment page rendered with existing Teacher/Student choices. Browser submissions across both users remain the student's manual test; no enrollment was created/deleted during the read-only page check.

## Chunk 3 revision: one course and Teacher editing

- Changeset 005 adds nullable `enrollments.marks`, a database check for 0–100, and a PostgreSQL partial unique index on Student ID for ACCEPTED rows. Pending requests can exist for different Teachers, but only one may become an accepted membership. No existing records were discarded; no conflicting accepted memberships were found before migration.
- Applying, inviting, accepting and Admin association check whether another course already owns that accepted membership. Students must leave it before switching. Available invitation choices exclude enrolled Students.
- Acceptance transactions lock the Student row before the enrollment row. This serializes competing acceptances for the same Student. The unique database index provides an additional guarantee even for writes outside the application.
- `/enrollments/{id}/student/edit` displays name, roll and marks for an accepted association. `/enrollments/{id}/student` saves them. Only the associated Teacher or Admin may access either operation; pending/rejected relationships and unrelated Teachers are rejected with 403. The Student cannot invoke either edit method.
- `EnrolledStudentForm` validates name, digit-only roll and optional marks (0–100, at most two decimals). Blank marks mean not graded. The Student personal-profile form has no marks field; submitted unknown fields cannot change marks.
- Profile edits modify `students.name` and `students.roll`; marks belong to the enrollment row. They do not modify `accounts.username`, `accounts.password_hash` or `accounts.role`. Therefore the Student continues logging in with exactly the same credentials after being edited. Removing a course association removes its course marks but keeps the Student profile and login.

### Manual verification

1. As the accepted Teacher, My Enrollments -> Edit Student. Confirm the unchanged login username is shown as informational text, not an editable input.
2. Change name or roll and set marks to 85.50; Save Student. Confirm the enrollment table shows the updated data and marks.
3. Log out and log in as the Student using the same original username/password. My Profile should show the changed name/roll; My Enrollments should show 85.50 with no Edit Student action.
4. Try marks above 100, below 0 and more than two decimal places: expect validation errors and no saved invalid value.
5. A different Teacher must be unable to open or POST the accepted Student's edit URL. A pending request must not provide edit authority.
6. A second Teacher cannot enroll the already accepted Student. Remove the first accepted association before switching; then only the newly accepted Teacher has edit authority.

Verification: compilation and 38 security, ownership and workflow tests pass. Tests include credential preservation during edits, unrelated/pending Teacher denial, Student marks-write denial, invalid marks, and single-course enforcement for Student, Teacher and Admin actions. Live database inspection confirmed migration 005 and the unique ACCEPTED index. The existing Teacher's accepted enrollment displayed Edit Student and the form rendered correctly in the browser. The student's manual save/login checks remain to be performed.

## Public signup and profile-first login

- `/signup` lets an anonymous visitor choose Student or Teacher. The matching `/signup/student` or `/signup/teacher` form collects username, password and name, plus Student roll or Teacher course.
- `SignupForm` carries only permitted signup input; it contains no role, account ID or password-hash field. The controller explicitly allowlists binding fields. Changing a POST body to include `role=ADMIN` cannot create an Admin.
- `SignupService` permits anonymous registration only. The Student endpoint fixes STUDENT and creates a Student profile; the Teacher endpoint fixes TEACHER and creates a Teacher profile. Both operations hash the password and create account/profile in a single transaction. An invalid or duplicate submission creates neither a new login nor a new profile.
- No email, institutional identity or other verification is required, following the agreed simplified workflow. Usernames must be unique; names and rolls retain their existing validation. Teacher course and Student roll are required.
- Registration redirects to `/login?registered` with a success message. It does not silently sign the user in. The user then enters their chosen credentials.
- `SecurityConfig.roleLoginSuccessHandler` sends Students/Teachers to `/me` and Admin to `/`. It deliberately clears/overrides saved request destinations, so a Student who originally tried an Admin URL still lands on their profile after signing in. Spring's normal session fixation protection stays active.
- The personal profile page now has My Enrollments and Log out links, so Home is not required as an intermediate step.
- Public signup retains CSRF protection. Passwords are blanked on redisplayed error forms and are never rendered into password input values.

### Manual test

1. Log out. On the login page click Sign up as a Student.
2. Enter a new username, name, password (8–64 characters) and digit-only roll. Click Create Account.
3. Expect the login page with Account created. Sign in using those credentials.
4. Expect My Profile immediately, with the entered name and roll. Click My Enrollments to continue the enrollment workflow.
5. Log out and repeat Sign up as a Teacher with a name and course. After signing in expect My Profile with the course already filled in.
6. Try an already-used username and an invalid/short password: errors must be readable; the password must be blank when the form is redisplayed.
7. Log in as Admin: expect the management Home, still with exactly Manage Teachers and Manage Students as the main choices.

Verification: compilation and 69 isolated tests passed, including 16 new signup/service/web checks. A further 16 live checks confirmed both public forms, real account/profile creation, duplicate validation, fixed roles despite a forged ADMIN field, profile-first login for Student/Teacher and Admin home redirect. Three disposable accounts created during live verification were removed; existing users were preserved. Live browser inspection confirmed the Student signup form and navigation links render.


## Styling checkpoint: compiled Tailwind + Thymeleaf

All 14 existing templates now share a compiled Tailwind stylesheet. Auth pages
use a branded split layout on desktop; signed-in pages share role-aware
navigation, consistent forms, management tables and enrollment badges. Forms
retain their existing actions, DTO bindings, validation and CSRF integration.
No database migrations or permission changes were required. LayoutAdvice only
supplies display information.

Run `npm.cmd ci`, then `npm.cmd run css:build` to rebuild, or `npm.cmd run css:watch`
while editing. Generated CSS is committed so normal lab startup needs no Node
installation. Read [Styling.md](Styling.md) for file responsibilities and manual
checks. CSS compilation, 69 existing Java tests, and 19 live page render checks
passed; signup, teacher profile/enrollments/editing and responsive widths were
reviewed in the browser. Student-led end-to-end testing remains useful.
