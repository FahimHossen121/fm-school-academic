# FM School JSON API

The REST API uses the same database and permission rules as the Thymeleaf pages. HTML is for browser users; JSON is for API clients. DTOs define response fields and exclude password hashes. No new database migration is needed for this API chunk.

## Authentication and CSRF

- Browser users may sign in at `/login`, then use their session to open JSON GET endpoints in the same browser.
- API clients may use HTTP Basic with an existing username/password. Use HTTPS outside local development.
- Every request needs authentication. Admin-only collections remain forbidden to Teachers/Students even when URLs or JSON IDs are changed.
- POST, PUT and DELETE require a valid CSRF token and its matching session cookie. GET `/api/csrf` first; use the returned `headerName` and `token` on writes, preserving cookies. This also applies to Basic-authenticated clients because browsers may automatically send credentials.
- API authentication failures return JSON 401 rather than redirecting to an HTML login form. Forbidden operations or invalid/missing CSRF return JSON 403.

The application uses two ordered Spring Security filter chains: `/api/**` gets JSON error handling; ordinary pages keep form-login redirects. Both retain CSRF and backend method authorization.

## Endpoints

### Admin Student management

| Method | URL | Success |
|---|---|---|
| GET | `/api/students` | 200, list of Student DTOs |
| GET | `/api/students/{id}` | 200, one Student DTO |
| POST | `/api/students` | 201, created DTO and Location header |
| PUT | `/api/students/{id}` | 200, updated DTO |
| DELETE | `/api/students/{id}` | 204, no body |

Create a login and Student profile atomically:

```json
{
  "username": "apiDemoStudent",
  "password": "DemoPass123",
  "profile": {
    "name": "Api Student",
    "roll": "0098765"
  }
}
```

Update name/roll (never login credentials):

```json
{"name":"Api Student Updated","roll":"0098765"}
```

A response contains `id`, `accountId`, `username`, `name`, `roll`. `id` is the Student record ID, not the Account ID. Some legacy Students have no account. Deleting a linked Student through this API deletes the login/profile and dependent enrollment rows; it does not delete Teachers.

### Admin Teacher management

| Method | URL | Success |
|---|---|---|
| GET | `/api/teachers` | 200, list |
| GET | `/api/teachers/{id}` | 200, Teacher DTO |
| POST | `/api/teachers` | 201 with Location |
| PUT | `/api/teachers/{id}` | 200, updated DTO |
| DELETE | `/api/teachers/{id}` | 204 |

Create body:

```json
{
  "username": "apiDemoTeacher",
  "password": "DemoPass123",
  "profile": {
    "name": "Api Teacher",
    "course": "SEPM"
  }
}
```

Update body:

```json
{"name":"Api Teacher Updated","course":"SEPM"}
```

Teacher responses contain `id`, `accountId`, `username`, `name`, `course`. The endpoint fixes the new account's role to TEACHER or STUDENT; input cannot create an Admin or promote another role. Existing duplicate usernames produce 409. Invalid profile creation rolls back the account creation too.

### Personal identity and profiles

| Method | URL | Permission / result |
|---|---|---|
| GET | `/api/session` | Any login: username and roles |
| GET | `/api/csrf` | Any login: token header name and token |
| GET | `/api/me` | Teacher/Student: own profile only |
| PUT | `/api/me` | Teacher/Student: edit own profile |
| DELETE | `/api/me` | Teacher/Student: delete own login/profile, invalidate session; 204 |
| POST | `/api/logout` | Log out current session; 204 |

Use a flat profile body for PUT `/api/me`: Student uses `name` and `roll`; Teacher uses `name` and `course`. This route derives identity from authentication and accepts no target account ID. It cannot update marks. The single Admin does not have a Teacher/Student personal profile and cannot self-delete through `/api/me`; its identity is available at `/api/session`. Creating/deleting another Admin is deliberately not exposed, preserving the agreed single-Admin model.

### Enrollment and Teacher authority

| Method | URL | Permission / body |
|---|---|---|
| GET | `/api/enrollments` | Own enrollment board; Admin sees all |
| POST | `/api/enrollments/applications` | Student; `{"teacherId":1}`; 201 |
| POST | `/api/enrollments/invitations` | Teacher; `{"studentId":2}`; 201 |
| POST | `/api/enrollments/associations` | Admin; `{"teacherId":1,"studentId":2}`; 201 |
| POST | `/api/enrollments/{id}/accept` | Recipient or Admin; 200 |
| POST | `/api/enrollments/{id}/reject` | Recipient or Admin; 200 |
| DELETE | `/api/enrollments/{id}` | Accepted participant or Admin; 204 |
| GET | `/api/enrollments/{id}/student` | Associated accepted Teacher or Admin |
| PUT | `/api/enrollments/{id}/student` | Associated accepted Teacher or Admin |

Teacher edit body:

```json
{"name":"Student Two","roll":"9000002","marks":85.50}
```

Marks may be null/not graded or 0–100 with up to two decimals. Student users cannot change marks. An unrelated Teacher or a pending request grants no edit permission. The general `/api/students` route stays Admin-only; Teachers use this scoped enrollment route. One Student can have only one ACCEPTED course; a second course's acceptance or direct Admin assignment produces 409 until the previous association is removed.

The board uses directory DTOs: invitation choices reveal Student names, not their rolls; accepted participants can see the course record. No Account entities or password hashes are returned.

## Status and error contracts

- 200: successful read or update (empty collections are `[]`).
- 201: successful creation.
- 204: successful delete/logout with no body.
- 400: invalid JSON, invalid fields, missing role-specific profile information, or invalid identifier format.
- 401: not authenticated or invalid credentials.
- 403: role/ownership/CSRF denial.
- 404: requested record does not exist.
- 409: duplicate username, duplicate/pending association, or single-course conflict.

Validation example:

```json
{
  "status": 400,
  "message": "Validation failed",
  "fields": {"roll": "Roll must contain digits only"}
}
```

No stack traces, SQL errors, password values or hashes are included in these DTO errors. Unknown paths and unsupported HTTP methods may use Spring Boot's standard error envelope with the appropriate status.

## PowerShell example (Windows PowerShell 5 compatible)

Run from a new IntelliJ Terminal while FM School is running. The credential prompt asks for an existing Admin password; it is not stored in source.

```powershell
$credential = Get-Credential -UserName admin -Message 'FM School API credentials'
$pair = '{0}:{1}' -f $credential.UserName, $credential.GetNetworkCredential().Password
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair)) }
$csrf = Invoke-RestMethod -Uri 'http://localhost:8080/api/csrf' -Headers $headers -SessionVariable apiSession
$headers[$csrf.headerName] = $csrf.token

Invoke-RestMethod -Uri 'http://localhost:8080/api/students' -Headers $headers -WebSession $apiSession
```

Create a disposable Student and inspect it:

```powershell
$body = @{
    username = 'apiDemoStudent'
    password = 'DemoPass123'
    profile = @{ name = 'Api Student'; roll = '0098765' }
} | ConvertTo-Json -Depth 3

$created = Invoke-RestMethod -Uri 'http://localhost:8080/api/students' -Method Post -ContentType 'application/json' -Body $body -Headers $headers -WebSession $apiSession
$created
```

Delete only this disposable record after testing:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/students/$($created.id)" -Method Delete -Headers $headers -WebSession $apiSession
```

A repeated creation with the same username returns 409; it does not reset that person's password. Do not use a real person's ID for the deletion example.

## Verification

53 isolated tests pass, including 14 API contract tests. They cover JSON 401/403, field validation, malformed JSON, creation Location headers, 204 deletes, 404/409 errors, DTO password exclusion, own-profile routing and marks validation.

17 live API checks passed against the running application using disposable Teacher/Student records. They verified create/read/update/delete, real database changes, CSRF token use, Teacher collection denial, scoped enrollment data, unrelated Teacher edit denial and Student marks-write denial. Disposable accounts/profiles were removed afterward; existing user records were not modified. Browser/manual verification is still encouraged for learning.
