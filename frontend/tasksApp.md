# Reporting UI — Implementation Task List

Do not skip ahead. Each phase depends on the previous one. Follow the locked decisions below so implementers do not invent charts, extra report types, or a second API host.

This list is **frontend only** (`frontend/`). The seven `GET /reports/*` endpoints already exist on the backend. Do not change Java. Do not rebuild login, invite, people, time, or leave admin pages.

---

## Current frontend (inspected)

Stack: Angular 21 standalone components, reactive forms, `provideHttpClient` + `authInterceptor`, Vitest.

What already works (reuse, do **not** redo):

- Tenant from hostname (`TenantService`, `parseTenantSlug`). API = same hostname, port `8080` via `TenantService.url(...)`.
- JWT in `localStorage`, Bearer interceptor, 401 → logout. `authGuard` / `guestGuard` / `tenantGuard` / `roleGuard`. `AuthService.hasRole` / `hasAnyRole`.
- Global look in `styles.scss`: `.error-banner`, `.form-field`, `.btn-primary`, `.btn-secondary`, `.data-table`, `.page-header`, `.muted`. Square corners, solid 1px borders, no gradients.
- `AppLayoutComponent` (sidebar + top bar + Sign Out). Nav list is in that component. Feature pages for dashboard, people, time, and leave exist as files.
- `apiErrorMessage` in `core/http/api-error.ts` (login-oriented 403 text — **do not use it blindly** for reports; see locked decision 8).

What is missing (this document):

- No report models, no `ReportService`, no `/reports/*` routes, no Reports nav.
- `app.routes.ts` may still mount `/dashboard` without the layout. Report pages **must** sit under `AppLayoutComponent` so they get the sidebar.
- No payroll CSV download.

There is **no** absence-patterns or overtime-summary UI. Those APIs are gone (404). Do not add them.

---

## Locked decisions (do not change without an explicit product decision)

1. **Frontend only.** Do not edit `backend/`. Do not add Java endpoints. If a field is not on the DTO below, do not display it and do not invent a query param.
2. **Tenant from Host only.** Every HTTP call uses `TenantService.url('/reports/...')`. Never hardcode `localhost:8080`. Never send `companyId`, `tenantId`, or `subdomain` in JSON, query, or path. If a `companyId` query appears in the browser, ignore it (do not bind it on the Angular `HttpParams`).
3. **Seven reports only**, same paths as the backend. Tables of facts. Totals from the JSON are allowed. No charts, heatmaps, calendars, PDF, Excel libraries, or client-side overtime math.
4. **Roles match the API.** Employee: own employee-time (optional `userId` omitted). Manager: employee-time (optional `userId`), team-leave, leave-balances (no department picker). HR: all seven, including payroll CSV, department-utilization, project-hours, compliance. Hide nav the role cannot use. `roleGuard` on manager/HR routes. A 403 is a banner, not a crash.
5. **Reuse shell and CSS.** Same white page, black text, system fonts, square corners. Native `<table class="data-table">`, `input type="date"`, `.btn-primary` to Load. Empty = one sentence. Loading = disable Load or show `Loading.`
6. **Dates are `yyyy-MM-dd`.** Required `startDate` and `endDate` for range reports. Leave-balances: optional `year`, optional `departmentId` **HR only**. Payroll CSV is the **only** export (`format=csv` exact). Do not add CSV to other reports.
7. **Do not add** audit log, leave-type admin, absence patterns, overtime summary, or a second “analytics” page. Do not call `GET /time-entries/summary` from Reports (that stays on team time).
8. **Report HTTP errors:** prefer `error.error.message` from the API (400 date range, 403 “You cannot access this user's report”, etc.). Do not map every 403 to the login “account deactivated” string. 401 still goes through the interceptor.

### UI rules (mandatory)

Same as the rest of the app: no emojis, no gradients, no dashed borders, `border-radius: 0`, no transitions/animations, colors `#ffffff` / `#111111` / muted `#555555`, error banner `#f8f8f8`. Primary button black, secondary white with black border.

### Pages to create

| Route | Page | Who |
| --- | --- | --- |
| `/reports/employee-time` | One person’s approved hours | all (self; manager/HR may pass `userId`) |
| `/reports/team-leave` | Leave overlapping the range | MANAGER, HR_ADMIN |
| `/reports/leave-balances` | Year balances | MANAGER (own dept, no picker), HR_ADMIN |
| `/reports/payroll` | Regular / OT / total + CSV | HR_ADMIN |
| `/reports/department-utilization` | Hours per department | HR_ADMIN |
| `/reports/project-hours` | Hours per project | HR_ADMIN |
| `/reports/compliance` | Missing leave-balance rows | HR_ADMIN |

Login, invite, marketing stay chrome-free. Do not create `/reports/absence-patterns` or `/reports/overtime-summary`.

### Backend contracts (camelCase JSON)

Jackson dates `"yyyy-MM-dd"`. Do not send `companyId`.

**Employee time** — `GET /reports/employee-time?startDate&endDate&userId` (`userId` optional)

- Query: omit `userId` for self. Manager/HR may set `userId`.
- Body: `userId`, `startDate`, `endDate`, `totalHours`, `averageHoursPerDay`, `daysWithEntries`, `entriesCount`, `dailyHours[]`, `projectBreakdown[]`
- Item: `{ key, totalHours }` (`key` = `YYYY-MM-DD` or project code)

**Team leave** — `GET /reports/team-leave?startDate&endDate`

- Body: `managerId`, `startDate`, `endDate`, `requestsCount`, `totalLeaveDays`, `requests[]`
- Request: `id`, `userId`, `employeeName`, `leaveTypeName`, `startDate`, `endDate`, `totalDays`

**Payroll** — `GET /reports/payroll?startDate&endDate` JSON (default). CSV: **same path** with query `format=csv` (exact lowercase `csv`). `responseType: 'blob'`. Filename `payroll.csv`.

- JSON: `startDate`, `endDate`, `dailyOvertimeThresholdHours`, `weeklyOvertimeThresholdHours`, `totalRegularHours`, `totalOvertimeHours`, `totalHours`, `employees[]`
- Employee: `employeeId`, `name`, `regularHours`, `overtimeHours`, `totalHours`

**Leave balances** — `GET /reports/leave-balances?year&departmentId` (both optional)

- Manager: do **not** send `departmentId` (API forces their department; wrong id → 403).
- HR: omit `departmentId` for company-wide; set it to filter.
- Body: `year`, `departmentId`, `employeesCount`, `balancesCount`, `balances[]`
- Item: `employeeId`, `employeeName`, `departmentId`, `departmentCode`, `departmentName`, `leaveTypeId`, `leaveTypeName`, `year`, `annualAllocation`, `currentBalance`

**Department utilization** — `GET /reports/department-utilization?startDate&endDate` HR

- Body: `startDate`, `endDate`, `totalHours`, `departmentsCount`, `employeesCount`, `departments[]`
- Item: `departmentId`, `departmentCode`, `departmentName`, `totalHours`, `employeesCount`

**Project hours** — `GET /reports/project-hours?startDate&endDate` HR

- Body: `startDate`, `endDate`, `projectsCount`, `employeesCount`, `totalHours`, `projects[]`
- Item: `projectId`, `projectCode`, `projectName`, `totalHours`, `employeesCount`

**Compliance** — `GET /reports/compliance?startDate&endDate` HR (range must be one calendar year or API 400)

- Body: `startDate`, `endDate`, `year`, `entitlementIssuesCount`, `entitlementIssues[]` (list may be capped; count is full)
- Issue: `employeeId`, `employeeName`, `leaveTypeId`, `leaveTypeName`, `year`, `issue`

400 examples: inverted range, range > 366 days, compliance crossing years. Show `message`.

---

## How to run a group

Copy **one group** into the agent (effort line + every numbered task in that group). These groups are sized for a **high** or **extra-high** model: one group = one shot. Do not split a group across chats. Do not start a later phase before the earlier one is done.

---

## Phase 0 — Types and HTTP


### Group 0.1 — Report models and `ReportService`

effort: extra-high

1. Add `core/models/report.model.ts` with interfaces that match the DTOs above (`TimeSummaryItem`, `EmployeeTimeReport`, `TeamLeaveReport`, `TeamLeaveRequestItem`, `PayrollReport`, `PayrollEmployeeHours`, `LeaveBalanceReport`, `LeaveBalanceReportItem`, `DepartmentUtilizationReport`, `DepartmentUtilizationItem`, `ProjectHoursReport`, `ProjectHoursItem`, `ComplianceReport`, `ComplianceEntitlementIssue`). No `companyId`. Numbers as `number` (JSON numbers). Dates as `string`.
2. Add `core/services/report.service.ts`. All URLs via `TenantService.url`. Methods: `employeeTime(start, end, userId?)`, `teamLeave(start, end)`, `payroll(start, end)`, `payrollCsv(start, end)` returning `Blob` / `Observable<Blob>` with `format=csv` only, `leaveBalances(year?, departmentId?)`, `departmentUtilization(start, end)`, `projectHours(start, end)`, `compliance(start, end)`. Omit unset optional params (do not send `userId=` empty, do not send `departmentId` for managers).
3. Payroll CSV request must hit the csv mapping: query param **exactly** `format=csv`. Do not send `format=json` unless you rely on the default (prefer omitting `format` for JSON). Do not parse the blob as JSON.

---

## Phase 1 — Shell, routes, shared filters


### Group 1.1 — Layout, nav, routes, shared date bar

effort: extra-high

4. Ensure report routes (and `/dashboard` if it is still a bare route) are **children of `AppLayoutComponent`**, with `tenantGuard` + `authGuard` on the layout and `roleGuard` + `data.roles` on manager/HR report children. Public `''`, `login`, `invite` stay outside the layout. Do not recreate people/time/leave pages; only wire what you need so reports have the sidebar. If other feature routes are already children, leave them.
5. Add sidebar links (same `navItems` list in `app-layout.component.ts`): **Reports** is not a fake parent — use real routes. All roles: `Employee time` → `/reports/employee-time`. Manager+: `Team leave` → `/reports/team-leave`, `Leave balances` → `/reports/leave-balances`. HR only: `Payroll`, `Department hours`, `Project hours`, `Compliance` → the four HR paths. Hide links the role cannot see. `routerLinkActive` with `exact: true` on these.
6. Shared filter UI used by range reports: `startDate`, `endDate` (`type="date"`), button `Load`. Prefill a sensible default (current month or last 7 days is fine). Reuse `.form-field` / `.page-header`. Put it in `features/reports/` (small shared component **or** duplicated markup if a shared component fights the model — prefer one shared `report-date-filter` component). Employee-time adds optional `userId` number input **only** for MANAGER/HR. Leave-balances uses `year` (number) and HR-only `departmentId` (select from existing `DepartmentService.list()` or a number input if you must; select is better). Payroll page adds `Download CSV` (`.btn-secondary`) that calls `payrollCsv` and saves `payroll.csv` (anchor download or equivalent; no extra library).
7. Placeholder or real components for all seven routes so navigation does not 404. Empty body + title is acceptable until Phase 2/3 fills them **if** the route exists; prefer implementing Load in the same shot for the pages in later groups rather than leaving dead placeholders if you already have the service.

---

## Phase 2 — People-facing reports (employee + manager)


### Group 2.1 — Employee time, team leave, leave balances

effort: extra-high

8. `features/reports/employee-time-report.component.ts` / `.html` / `.scss`. On Load: `employeeTime`. Show error banner on failure (including 403 when viewing another user). Summary line: total hours, average per day, days with entries, entries count. Two tables: daily hours (`key`, `totalHours`), project breakdown (same). Empty: `No approved time entries.` Employee must not send `userId`. Manager/HR: empty userId omits the param.
9. `features/reports/team-leave-report.component.ts`. On Load: `teamLeave`. Show `requestsCount` and `totalLeaveDays`. Table: employee, type, start, end, days. Empty: `No leave in this range.` Route manager + HR.
10. `features/reports/leave-balances-report.component.ts`. On Load: `leaveBalances`. Show year, employees count, balances count. Table: employee, department code/name, leave type, allocation, current balance. Manager: no department control. HR: optional department select; clear = company-wide. Empty: `No leave balances.` 403 banner if the API denies (manager without department).

---

## Phase 3 — HR reports


### Group 3.1 — Payroll, department hours, project hours, compliance

effort: extra-high

11. `features/reports/payroll-report.component.ts`. On Load: JSON payroll. Show range totals (`totalRegularHours`, `totalOvertimeHours`, `totalHours`) and thresholds as labeled text (not editable). Table: employee id, name, regular, overtime, total. Empty: `No employees.` `Download CSV` uses the same dates; on failure show banner. Do not recompute overtime in the browser.
12. `features/reports/department-utilization-report.component.ts`. Table: code, name, total hours, employees count. Show report `totalHours` / `departmentsCount` / `employeesCount`. Empty: `No department hours.`
13. `features/reports/project-hours-report.component.ts`. Table: code, name, total hours, employees count. Show report totals. Empty: `No project hours.`
14. `features/reports/compliance-report.component.ts`. Show `year` and `entitlementIssuesCount`. Table: employee, leave type, year, issue. Empty: `No entitlement issues.` If start/end cross a calendar year, the API 400 message is enough (do not invent a client-only year picker unless you also still send startDate/endDate as required).

All four: HR `roleGuard`, error banner, Load with the shared dates.

---

## Phase 4 — Fit and finish


### Group 4.1 — Authz, tenant, style, gone endpoints

effort: high

15. Confirm employee cannot open HR report URLs (`roleGuard` → `/dashboard`). Confirm manager `/reports/payroll` and `/reports/department-utilization` never call the service after a client-side guard (if they bypass the URL, banner on 403 is required). Confirm employee-time for another user without access shows the API 403 message.
16. Grep `frontend/src` for `localhost:8080`, `companyId`, `tenantId`, `absence-patterns`, `overtime-summary`. Remove any report usage. All report HTTP through `TenantService.url`. Do not add Vitest specs unless you already have a pattern you can copy in one file; do not add frontend specs as a science project.
17. Grep new report files for `border-radius`, `gradient`, `dashed`, `transition`, `animation`, charts (`canvas`, `chart.js`, ngx-charts). None. Corners square. No CSV except payroll.

---

## Execution order (summary)

`0 models+HTTP` → `1 layout/nav/filters` → `2 employee/manager report pages` → `3 HR report pages` → `4 audit`.

Do not build pages before `ReportService` exists. Do not add Charts. Do not restore deleted backend reports. Do not redesign login.

Suggested one-shot size: **one Group** per run. `high` and `extra-high` are still one run (the whole group).
