# Frontend MVP — Implementation Task List

Do not skip ahead. Each phase depends on the previous one. Follow the locked decisions below so implementers do not invent a second UI, a second API host, or extra features.

This list is the **authenticated app** (shell, people, time, leave). Login, company-domain lookup, invite accept, and tenant Host rules already exist. Do not rebuild them.

---

## Current frontend (inspected)

Stack: Angular 21 standalone components, reactive forms, `provideHttpClient` + `authInterceptor`, Vitest.

What already works (do **not** redo):

- Marketing page on apex `localhost:4200` (`MarketingHomeComponent`). Log in in the header only.
- `/login` with **no** tenant: company domain field + `.localhost` suffix, then `window.location` to `http://{slug}.localhost:4200/login`.
- `/login` **on** a tenant host: email + password only. Shows slug. No `companyId`.
- `/invite?token=`: accept invite (same login-box layout).
- `/dashboard`: stub session page (email, slug, role, Sign Out). No real data.
- Tenant from hostname (`TenantService`, `parseTenantSlug`). API = same hostname, port `8080` via `TenantService.url(...)`.
- JWT in `localStorage`. Bearer interceptor. 401 → logout. `authGuard` / `guestGuard` / `tenantGuard` / `marketingHomeGuard`.

What is missing (this document):

- App shell (sidebar + top bar + nested routes).
- Shared page/table/form CSS used by the whole app (login styles exist but are local to auth).
- Typed models and HTTP services for users, departments, projects, time entries, leave.
- Real dashboard (`GET /users/me/dashboard`).
- User management, invite-from-HR, team list, profile edit.
- Department and project admin pages (needed for user create and time entry project select).
- Time entry list/create/edit/approve/reject/correction.
- Leave request list/create/cancel/approve/deny, balances, team/HR queues.
- Role-based navigation.

There is **no** reporting UI in this list. Backend `ReportController` exists; do not call it yet.

---

## Locked decisions (do not change without an explicit product decision)

1. **Tenant from Host only.** Keep using `TenantService.url(path)`. Never hardcode `http://localhost:8080`. Never send `companyId`, `tenantId`, or `subdomain` in JSON, query, or path.
2. **No company picker** inside the app. Login and invite stay on the tenant host. Do not add a “switch company” control.
3. **Call the existing backend.** Field names must match the Java DTOs below. Do not invent endpoints, query params, or body fields. If an endpoint is missing (example: no `GET /invitations` list), do not fake a list.
4. **MVP only.** Buttons, forms, HTML tables, filters, empty text. No charts, calendars widgets, drag-and-drop, toasts libraries, skeletons, page transitions, or “smart” dashboards.
5. **Look like the current login page.** White page, black text, system fonts, square corners, solid 1px borders. Copy from `login.component.scss` and `styles.scss`.
6. **Auth pages stay chrome-free.** Marketing, login, and invite stay full-page (no sidebar). The shell wraps only authenticated child routes.
7. **Roles from membership.** `EMPLOYEE`, `MANAGER`, `HR_ADMIN` from `AuthService.currentUser()?.role` (`userRole` on `GET /users/me`). Hide nav links the role cannot use. Guard those routes. A 403 from the API is an error banner, not a crash.
8. **Reactive forms + Angular control flow.** Same as login: `formGroup`, `@if`, standalone `*.component.ts` / `*.html` / `*.scss`. No Angular Material, PrimeNG, Bootstrap, Tailwind, or icon fonts.
9. **No mock data.** Every list is an HTTP call. Loading = disable the submit button or show the word `Loading.` Empty = one sentence (`No time entries.`).

### UI rules (mandatory for every page)

Implementers who ignore this section have failed the task.

- No emojis anywhere (templates, CSS content, titles, empty states, buttons).
- No gradient backgrounds. No background images.
- No dashed or dotted borders. Solid 1px only (`#111111` or `#eeeeee` / `#dddddd` for inner row lines).
- No rounded corners. `border-radius: 0` on buttons, inputs, selects, textareas, tables, sidebar, banners.
- No CSS animations, transitions, transforms, or keyframes. Hover may swap background/text color instantly (login already does this).
- Colors: page `#ffffff`, text `#111111`, muted `#555555` / `#666666`, hover `#eeeeee` / `#333333`, error banner background `#f8f8f8`. Do not introduce a palette.
- Font: `system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif` (already on `body`).
- Inputs/selects: full width in forms, padding like login (`0.6rem 0.75rem`), black border, white background, thicker border on focus (`border-width: 2px`), no outline glow.
- Primary button: black background, white text, 1px black border (login `.btn-submit`). Secondary: white background, black text, 1px black border (dashboard Sign Out).
- Do not hardcode API hosts, demo user tables, fake JWT, or `companyId`.

### Basic layout (what to build)

```
+------------------+----------------------------------------+
| Company slug     |  {slug}     First Last (ROLE)  Sign Out|
|                  |----------------------------------------|
| Home             |  Page title                            |
| Time             |                                        |
| Leave            |  Filters / primary button              |
| Profile          |                                        |
| --- manager+ --- |  Table or form                         |
| Team             |                                        |
| Time approvals   |  Error banner if the API failed        |
| Leave approvals  |                                        |
| --- HR only ---- |                                        |
| People           |                                        |
| Invite           |                                        |
| Departments      |                                        |
| Projects         |                                        |
| All leave        |                                        |
+------------------+----------------------------------------+
```

- Left sidebar: about `220px`, `border-right: 1px solid #111111`, white background, stacked text links. Active route: black background, white text. No icons.
- Top bar: `border-bottom: 1px solid #111111`, company slug on the left of the content (or repeat slug in the sidebar header), user email or `firstName lastName` and role, Sign Out on the right.
- Main: padding `1.5rem 2rem`. Page title as `h1`. One primary action on the right of the title when needed (`New time entry`, `Add person`).
- Tables: native `<table>`. Header row bold. Cells padded. Outer table border 1px solid `#111111`. No zebra gradients; a single `#eeeeee` row bottom border is enough.
- Forms: stack of labeled fields, same as login. Selects for department, project, leave type, role, status. `input type="date"` and `type="time"` (no date-picker library). Optional extra rows for breaks: Add break / Remove, two time inputs + unpaid checkbox.
- Confirm destructive actions with `window.confirm` (deactivate, delete pending entry). Approve can be immediate. Reject/deny needs a required reason field (prompt or a small form on the same page).

### Pages the agent must create (authenticated)

| Route | Page | Who |
| --- | --- | --- |
| `/dashboard` | Home (real dashboard) | all |
| `/profile` | Edit first/last name | all |
| `/time` | My time entries | all |
| `/time/new` | Create time entry | all |
| `/time/:id/edit` | Edit pending time entry | all |
| `/time/approvals` | Pending time approvals | MANAGER, HR_ADMIN |
| `/time/team` | Team time entries | MANAGER, HR_ADMIN |
| `/leave` | My leave (balances + requests) | all |
| `/leave/new` | Create leave request | all |
| `/leave/approvals` | Pending leave + cancellation queue | MANAGER, HR_ADMIN |
| `/leave/team` | Team leave | MANAGER, HR_ADMIN |
| `/leave/all` | All leave (filters) | HR_ADMIN |
| `/people` | People list + search | HR_ADMIN |
| `/people/new` | Create person | HR_ADMIN |
| `/people/:id` | Person detail / edit / activate | HR_ADMIN |
| `/people/invite` | Create invitation | HR_ADMIN |
| `/team` | Direct reports | MANAGER, HR_ADMIN |
| `/departments` | Department list + create/edit | HR_ADMIN |
| `/projects` | Project list + create/edit | HR_ADMIN |

Login `/login`, invite `/invite`, marketing `/` stay as they are. After login, `guestGuard` may keep redirecting to `/dashboard`.

Do **not** create: report pages, audit log page, leave-type admin, invitation inbox, company settings, notifications, forgot-password, charts.

### Backend contracts (use these fields)

Jackson JSON: camelCase. Dates `"yyyy-MM-dd"`. Times `"HH:mm:ss"` (send `"09:00:00"` from `type="time"`). Enums as strings.

Spring `Page`: `{ content, totalElements, totalPages, number, size, first, last }`.

**Users**

- `GET /users` HR, pageable (`page`, `size`, `sort`)
- `GET /users/{id}`
- `GET /users/me`
- `GET /users/team` MANAGER, HR_ADMIN
- `POST /users` HR body: `username`, `email`, `firstName`, `lastName`, `userRole`, `departmentId`, `managerMembershipId` (optional). Response: `{ userResponseDto, temporaryPass }`
- `PUT /users/{id}` HR body: `username`, `email`, `firstName`, `lastName`, `userRole`, `departmentId`, `managerMembershipId`
- `PATCH /users/{id}/deactivate` HR
- `PATCH /users/{id}/activate` HR
- `GET /users/search` HR query: `departmentId`, `role`, `active`, `name`
- `PATCH /users/me/profile` body: `firstName`, `lastName`
- User payload: `id`, `username`, `email`, `firstName`, `lastName`, `userRole`, `departmentId`, `managerId`, `isActive`

**Invitations** (create only; no list API)

- `POST /invitations` HR body: `email`, `role`, `departmentId`, `managerMembershipId`. Response: `id`, `email`, `role`, `expiresAt`, `token`
- Invite link to show once: `{window.location.origin}/invite?token={token}` (do not hardcode host or port)

**Departments**

- `GET /departments` — `id`, `departmentName`, `departmentCode`, `isActive`
- `POST /departments` HR: `departmentName`, `departmentCode`
- `PUT /departments/{id}` HR: `departmentName`, `departmentCode`, `isActive`

**Projects**

- `GET /projects/active` — `id`, `projectName`, `projectCode`, `description`, `isActive`
- `POST /projects` HR: `projectName`, `projectCode`, `description`
- `PUT /projects/{id}` HR: same + `isActive`
- There is no “list all including inactive”. HR table uses `/projects/active`.

**Dashboard**

- `GET /users/me/dashboard` — `user`, `leaveBalances`, `upcomingLeave`, `recentLeaveRequests`, `recentTimeEntries`, `stats`
- `stats`: `hoursThisWeek`, `hoursThisMonth`, `pendingTimeEntriesCount`, `pendingLeaveRequestsCount`, `pendingTimeApprovalsCount`, `pendingLeaveApprovalsCount`, `teamMembersOnLeaveToday`, `totalActiveEmployees` (nulls for roles that do not get them)

**Time entries** — status: `PENDING`, `APPROVED`, `DENIED`, `CANCELLED`, `PENDING_CORRECTION`, `CANCELLATION_PENDING`

- `POST /time-entries` body: `entryDate`, `clockInTime`, `clockOutTime`, `projectId`, `description`, `breaks[]` (`breakStart`, `breakEnd`, `isUnpaid`)
- `GET /time-entries/me` query: `status`, `startDate`, `endDate`
- `GET /time-entries/stats/me` — `totalHoursThisWeek`, `averageHoursPerDayThisMonth`, `topProjectCodeThisMonth`, `topProjectHoursThisMonth`
- `PUT /time-entries/{id}` same body as create (pending only)
- `DELETE /time-entries/{id}` pending only
- `POST /time-entries/{id}/breaks` / `GET .../breaks` / `DELETE .../breaks/{breakId}`
- `POST /time-entries/{id}/correction-request` body: `explanation`
- `GET /time-entries/team` MANAGER/HR query: `status`, `startDate`, `endDate`, `name`
- `GET /time-entries/pending-approval`
- `POST /time-entries/{id}/approve`
- `POST /time-entries/{id}/reject` body: `reason`
- `POST /time-entries/{id}/correction-approve` / `correction-deny`
- `GET /time-entries/summary` query: `startDate`, `endDate`, `userId` optional — `totalHours`, `byDate[]`, `byProject[]`, `byEmployee[]` each `{ key, totalHours }`
- Time row: `id`, `userId`, `userFirstName`, `userLastName`, `entryDate`, `clockInTime`, `clockOutTime`, `totalHours`, `projectId`, `projectName`, `projectCode`, `description`, `rejectionReason`, `correctionReason`, `status`, `breaks`

Do not wire `GET /time-entries/export` in this MVP.

**Leave**

- `GET /leave-types` — `id`, `typeName`, `description`, `isActive`
- `GET /leave-balances/me` — `id`, `userId`, `leaveTypeId`, `leaveTypeName`, `year`, `currentBalance`, `lastAccrualDate`
- `GET /leave-balances/user/{userId}` MANAGER/HR
- `POST /leave-requests` body: `leaveTypeId`, `startDate`, `endDate`, `reason`
- `GET /leave-requests/me`
- `GET /leave-requests/{id}`
- `POST /leave-requests/{id}/cancel` optional body: `reason`
- `GET /leave-requests/pending` MANAGER/HR
- `GET /leave-requests/cancellation-pending` MANAGER/HR
- `GET /leave-requests/team` query: `status`, `startDate`, `endDate`
- `GET /leave-requests` HR pageable query: `userId`, `status`, `startDate`, `endDate`
- `POST /leave-requests/{id}/approve` optional `notes`
- `POST /leave-requests/{id}/deny` body: `reason`
- `POST /leave-requests/{id}/cancel-approve` optional `notes`
- `POST /leave-requests/{id}/cancel-deny` body: `reason`
- Leave row: `id`, `userId`, `leaveTypeId`, `leaveTypeName`, `startDate`, `endDate`, `totalDays`, `reason`, `status`, `managerNotes`, `cancellationReason`
- Review row adds `employeeName` instead of relying on `userId` alone

Do not add leave-type or leave-policy admin. Those APIs are not public for write.

---

## How to run a group

Copy one group into the agent (effort line + the numbered tasks). Do not merge two groups in one shot unless they are both `low`. Do not start a later phase before the earlier phase’s groups are done.

---

## Phase 0 — Shared CSS and small auth helpers


### Group 0.1 — Global classes from the login look

effort: low

1. Move the reusable login look into `frontend/src/styles.scss` as global classes (do not restyle the login page into something new). Add: `.error-banner`, `.form-field` (label + input/select/textarea), `.btn-primary`, `.btn-secondary`, `.btn-link`, `.data-table`, `.page-header` (title row with optional action), `.muted`. Keep `border-radius: 0`, solid borders, no gradients, no transitions.
2. Keep `login.component.scss` working. Either import the same rules or leave login as-is if it already matches. Do not add rounded corners or new colors while “cleaning up”.


### Group 0.2 — Role helper and API error text

effort: low

3. Add `AuthService.hasAnyRole(roles: UserRole[])`. Keep `hasRole`. Use `currentUser()?.role`.
4. Add a small helper (for example `core/http/api-error.ts`) that reads `HttpErrorResponse` the same way login does (`error.message`, 401/403/0). Feature pages will use this for banners. Do not change interceptor logout behavior.


---

## Phase 1 — App shell (sidebar, top bar, routes)


### Group 1.1 — Layout component

effort: medium

5. Create `features/layout/app-layout.component.ts` / `.html` / `.scss`: left sidebar, top bar, `<router-outlet>` for children. Sidebar header = tenant slug (`TenantService.slug`). Top bar = `firstName lastName` or email, role, Sign Out calling `AuthService.logout()`.
6. Style the shell with the locked UI rules: sidebar `border-right: 1px solid #111111`, top bar `border-bottom: 1px solid #111111`, square links, active link black background white text. Full height. No icons, no collapse animation.


### Group 1.2 — Nav by role

effort: medium

7. Build the sidebar links from a single list in the layout component (label + route + roles). Employee: Home `/dashboard`, Time `/time`, Leave `/leave`, Profile `/profile`. Manager also: Team `/team`, Time approvals `/time/approvals`, Leave approvals `/leave/approvals`. HR also: People `/people`, Invite `/people/invite`, Departments `/departments`, Projects `/projects`, All leave `/leave/all`. Hide links the current role cannot see.
8. Mark the current route as active with `routerLinkActive` (exact where needed so `/time` is not active on `/time/new` unless you intend that).


### Group 1.3 — Nested routes and role guard

effort: medium

9. Add `roleGuard` in `core/guards/role.guard.ts`. Route `data.roles` is `UserRole[]`. If the user lacks a role, redirect to `/dashboard`. Still require `tenantGuard` + `authGuard` on the layout.
10. Change `app.routes.ts`: authenticated area is the layout component with children for every page route in the table above. For this group, child routes may still point at empty placeholder components **or** the existing dashboard stub. Public routes (`''`, `login`, `invite`) stay outside the layout.
11. Point `guestGuard` success redirect at `/dashboard` (already). Remove the old dashboard header/sign-out from `DashboardLandingComponent` once the layout owns Sign Out so it is not duplicated. Keep a short placeholder body until Phase 4.


---

## Phase 2 — TypeScript models (no UI)


### Group 2.1 — People and org models

effort: medium

12. Add `core/models` files matching backend DTOs (separate files is fine): `UserResponse`, `UserCreatedResponse`, `CreateUserRequest`, `UserUpdateRequest`, `UserWriteRequest`, `InvitationCreated`, `CreateInvitationRequest`, `Department`, `CreateDepartmentRequest`, `UpdateDepartmentRequest`, `Project`, `CreateProjectRequest`, `UpdateProjectRequest`, `SpringPage<T>`.
13. Reuse existing `UserRole`. Map `userRole` / `isActive` as the API sends them. Do not add `companyId` to any interface.


### Group 2.2 — Time, leave, dashboard models

effort: medium

14. Add time models: `TimeEntry`, `TimeEntryBreak`, `CreateTimeEntryRequest`, `CreateTimeEntryBreakRequest`, `TimeEntryPersonalStats`, `TimeEntrySummary`, `TimeSummaryItem`, `CorrectionRequest`, `TimeEntryRejection`. Status union: `PENDING | APPROVED | DENIED | CANCELLED | PENDING_CORRECTION | CANCELLATION_PENDING`.
15. Add leave models: `LeaveType`, `LeaveBalance`, `LeaveRequest`, `LeaveRequestReview`, `CreateLeaveRequest`, `LeaveDenyRequest`, `LeaveCancelRequest`, `LeaveApprovalNotes`.
16. Add dashboard models: `UserDashboard`, `DashboardStats`.


---

## Phase 3 — HTTP services (no pages yet)


### Group 3.1 — User and invitation services

effort: medium

17. Create `core/services/user.service.ts`. All URLs via `TenantService.url`. Methods: `getPage`, `getById`, `getMe`, `getTeam`, `search`, `create`, `update`, `deactivate`, `activate`, `updateProfile`. Typed. No `companyId`.
18. Create `core/services/invitation.service.ts` with `create` only (`POST /invitations`). Do not add list/revoke UI later unless a GET exists (it does not).


### Group 3.2 — Department and project services

effort: low

19. Create `core/services/department.service.ts`: `list`, `getById`, `create`, `update`.
20. Create `core/services/project.service.ts`: `listActive`, `getById`, `create`, `update`.


### Group 3.3 — Time entry service

effort: medium

21. Create `core/services/time-entry.service.ts` for the MVP methods in the contract (create, me, stats/me, update, delete, breaks, correction-request, team, pending-approval, approve, reject, correction-approve, correction-deny, summary). Skip export.
22. Pass date/status query params only when set. Do not send empty strings as filters.


### Group 3.4 — Leave services

effort: medium

23. Create `core/services/leave-type.service.ts` (`list`) and `core/services/leave-balance.service.ts` (`getMe`, `getForUser`).
24. Create `core/services/leave-request.service.ts` for create, me, getById, listAll (HR page), pending, cancellationPending, team, approve, deny, cancel, cancelApprove, cancelDeny.


### Group 3.5 — Dashboard service

effort: low

25. Create `core/services/dashboard.service.ts` with `getMine()` → `GET /users/me/dashboard`.


---

## Phase 4 — Home dashboard


### Group 4.1 — Real home page

effort: medium

26. Replace the dashboard stub body. Load `DashboardService.getMine()` on init. Show an error banner on failure.
27. Render `stats` as a simple list or a grid of labeled numbers (hours this week/month, pending counts). Show manager/HR stats only when the value is not null. No charts.
28. Three tables or stacked lists: recent time entries (date, hours, project, status), recent leave requests (type, dates, status), upcoming leave (type, dates). Link “Time” / “Leave” in the sidebar is enough; optional text links to `/time` and `/leave`. Empty sentence if a list is empty.


---

## Phase 5 — Profile


### Group 5.1 — Profile form

effort: low

29. Create `features/profile/profile.component.ts` / `.html` / `.scss`. Prefill first/last from `GET /users/me` or `AuthService.currentUser()`. Fields: first name, last name. Submit `PATCH /users/me/profile`. Success: short text `Saved.` and refresh `AuthService.fetchCurrentUser()`. Errors in `.error-banner`.
30. Register `/profile` on the layout. Show email and role as read-only text (not inputs).


---

## Phase 6 — Departments (HR)


### Group 6.1 — List and create

effort: medium

31. Create `features/departments/departments.component.ts` / `.html` / `.scss`. Table: name, code, active. Load `GET /departments`.
32. Same page: form to create (`departmentName`, `departmentCode`) and `POST /departments`. Reload the table. HR route `/departments` + `roleGuard` HR_ADMIN.


### Group 6.2 — Edit department

effort: low

33. Selecting a row (or an Edit button) fills the same form. Save calls `PUT /departments/{id}` including `isActive` (checkbox). Cancel clears the form back to create mode.


---

## Phase 7 — Projects (HR)


### Group 7.1 — List, create, edit

effort: medium

34. Create `features/projects/projects.component.ts` / `.html` / `.scss`. Table from `GET /projects/active`: name, code, description.
35. Same page create form: name, code, description. `POST /projects`. Edit via `PUT /projects/{id}` with `isActive`. Route `/projects`, HR only.


---

## Phase 8 — User management (HR) and team


### Group 8.1 — People list

effort: medium

36. Create `features/users/users-list.component.ts` / `.html` / `.scss`. Load `GET /users` with `page` and `size` (20). Table: name, email, role, department id (or name if you also loaded departments), active. Prev/Next when `totalPages > 1`.
37. Filters that call `GET /users/search`: name text, department select (`GET /departments`), role select (`EMPLOYEE` / `MANAGER` / `HR_ADMIN`), active select. Clear filters returns to the paged list. Route `/people`. Row click or Edit goes to `/people/:id`. Button `Add person` → `/people/new`.


### Group 8.2 — Create person

effort: medium

38. Create `features/users/user-create.component.ts` form: username, email, firstName, lastName, userRole select, departmentId select, managerMembershipId optional number or select. Manager select: `GET /users/search?role=MANAGER` (and/or HR if you need supervisors for managers). Do not send `companyId`.
39. On `201`, show `temporaryPass` once in a bordered box (copy as text, no extra library) and a link to `/people/:id`. If the API omits a temp password (existing global user), show the created user without a password box.


### Group 8.3 — Person detail

effort: medium

40. Create `features/users/user-detail.component.ts`. Load `GET /users/{id}`. Edit form same fields as create (no password). Save `PUT /users/{id}`.
41. Buttons: Deactivate (`PATCH .../deactivate`) and Activate (`PATCH .../activate`) with `window.confirm`. Hide the one that does not apply. Show API errors on the banner. Route `/people/:id`.


### Group 8.4 — Invite person

effort: medium

42. Create `features/users/invite-create.component.ts`. Form: email, role, departmentId, optional managerMembershipId. `POST /invitations`.
43. On success, show `token` once and the full accept URL `{origin}/invite?token=...`. Short note: the invitee must open it on this company host. Route `/people/invite`. No invitation table (no list API).


### Group 8.5 — Team page

effort: low

44. Create `features/users/team.component.ts`. `GET /users/team`. Table: name, email, role. Route `/team` for MANAGER and HR_ADMIN. Optional link on a name is not required (no employee file for managers unless HR).


---

## Phase 9 — Time entries (everyone)


### Group 9.1 — My time list

effort: medium

45. Create `features/time/time-list.component.ts`. `GET /time-entries/me` plus filters: status select, startDate, endDate. Table: date, clock in, clock out, hours, project, status. Button `New time entry` → `/time/new`. Edit link only when status is `PENDING` → `/time/:id/edit`.
46. On the same page, load `GET /time-entries/stats/me` and show the four numbers as labeled text above the table.


### Group 9.2 — Create time entry

effort: medium

47. Create `features/time/time-form.component.ts` for `/time/new`. Fields: entryDate, clockInTime, clockOutTime, projectId select from `GET /projects/active`, description textarea. Optional breaks: list of start/end/`isUnpaid`; Add break / Remove break. Submit `POST /time-entries`. Times must include seconds in JSON (`HH:mm:ss`).
48. On success, go to `/time`. Show validation and API errors on the banner. Do not submit `companyId`.


### Group 9.3 — Edit and delete pending

effort: medium

49. Reuse the form component for `/time/:id/edit`. Load the entry from `GET /time-entries/me` (find by id) or from the list navigation state; if missing, show `Time entry not found.` Prefill including breaks. Submit `PUT /time-entries/{id}`.
50. Delete button on edit (and optional on the list for pending rows) calls `DELETE /time-entries/{id}` after `window.confirm`. Then go to `/time`.


### Group 9.4 — Correction request

effort: low

51. On my list, for `APPROVED` rows, a button `Request correction` with a required explanation field (inline or tiny form). `POST /time-entries/{id}/correction-request` body `{ explanation }`. Reload the list. No other statuses.


---

## Phase 10 — Time entries (manager / HR)


### Group 10.1 — Approval queue

effort: medium

52. Create `features/time/time-approvals.component.ts`. `GET /time-entries/pending-approval`. Table: employee name, date, hours, project, status.
53. Approve button → `POST /time-entries/{id}/approve`. Reject: required reason then `POST /time-entries/{id}/reject` `{ reason }`. Reload the queue. Route `/time/approvals`.


### Group 10.2 — Team time and summary

effort: medium

54. Create `features/time/time-team.component.ts`. `GET /time-entries/team` with optional status, startDate, endDate, name. Same columns as my list plus employee name. Route `/time/team`.
55. Same page: date range + optional userId number, button `Summary` → `GET /time-entries/summary`. Show `totalHours` and three simple tables (`byDate`, `byProject`, `byEmployee`) with `key` and `totalHours`. This is a summary on the team page, not a reports section.


### Group 10.3 — Correction approve / deny

effort: low

56. On the approvals page (or team list), for `PENDING_CORRECTION` rows, buttons Approve correction / Deny correction calling `correction-approve` and `correction-deny`. Reload.


---

## Phase 11 — Leave (everyone)


### Group 11.1 — My leave page

effort: medium

57. Create `features/leave/leave-list.component.ts`. Load `GET /leave-balances/me` and `GET /leave-requests/me`.
58. Show balances as a table: type name, year, current balance. Show requests: type, start, end, days, status, reason. Button `New leave request` → `/leave/new`. Route `/leave`.


### Group 11.2 — Create leave request

effort: medium

59. Create `features/leave/leave-form.component.ts`. Fields: leaveTypeId select from `GET /leave-types`, startDate, endDate, reason. `POST /leave-requests`. On success go to `/leave`.
60. Keep dates as `yyyy-MM-dd`. Do not add half-day widgets or file uploads.


### Group 11.3 — Cancel leave

effort: low

61. On my list, for rows that are still cancellable (`PENDING` or `APPROVED` — if the API errors, show the message). Button Cancel with optional reason → `POST /leave-requests/{id}/cancel`. Reload.


---

## Phase 12 — Leave (manager / HR)


### Group 12.1 — Leave approvals

effort: medium

62. Create `features/leave/leave-approvals.component.ts`. Load `GET /leave-requests/pending` and `GET /leave-requests/cancellation-pending`. Two tables: employee, type, dates, days, reason / cancellationReason.
63. Pending: Approve (`POST .../approve`, notes optional) and Deny (required reason). Cancellation: Cancel approve / Cancel deny (deny requires reason). Reload both tables. Route `/leave/approvals`.


### Group 12.2 — Team leave

effort: medium

64. Create `features/leave/leave-team.component.ts`. `GET /leave-requests/team` with optional status, startDate, endDate. Table like the review DTO. Route `/leave/team`. Optional: view balances `GET /leave-balances/user/{userId}` under a selected row (simple nested table). Skip if it bloats the page.


### Group 12.3 — HR all leave

effort: medium

65. Create `features/leave/leave-all.component.ts`. `GET /leave-requests` with pageable + filters `userId`, `status`, `startDate`, `endDate`. Table of review rows. Prev/Next. Route `/leave/all`, HR only.
66. No CSV, no charts, no calendar heatmap.


---

## Phase 13 — Reporting

(empty)

No reporting UI in this MVP. Do not add `/reports` routes, do not call `ReportController`, do not add charts, payroll screens, compliance screens, or export buttons.

Leave this phase blank until a later task list.

---

## Phase 14 — Fit and finish (MVP definition of done)


### Group 14.1 — Empty, forbidden, nav

effort: medium

67. Every list page has a one-line empty state when `length === 0` (plain text, no illustration). Every mutating page has `.error-banner` on HTTP failure.
68. Confirm `roleGuard` on HR and manager routes. Confirm sidebar hides links the role cannot use. Employee who types `/people` lands on `/dashboard`.
69. Confirm Sign Out is only in the layout top bar (not duplicated on inner pages). Confirm login/invite still have no sidebar.


### Group 14.2 — Tenant and style audit

effort: low

70. Grep the frontend for `localhost:8080`, `companyId`, and `tenantId`. Remove any that send tenant as a client field. All HTTP must go through `TenantService.url`.
71. Grep for `border-radius`, `gradient`, `dashed`, `emoji`, `transition`, `animation`. Remove them from new code. Corners stay square. Backgrounds stay flat white.


---

## Execution order (summary)

`0 shared CSS/helpers` → `1 app shell` → `2 models` → `3 HTTP services` → `4 dashboard` → `5 profile` → `6 departments` → `7 projects` → `8 people/invite/team` → `9 my time` → `10 time approvals` → `11 my leave` → `12 leave approvals` → `13 reporting skipped` → `14 audit`.

Do not build people forms before department list exists (dropdown). Do not build time forms before project list exists (dropdown). Do not build leave forms before leave-types service exists. Do not start reporting. Do not redesign login.

Suggested one-shot size: one **Group** per agent run at the listed effort. `low` is one run. `medium` is one run. Do not hand an agent a whole phase unless every group in it is `low`.
