# Notifications UI — Implementation Task List

Do not skip ahead. Each phase depends on the previous one. Follow the locked decisions so implementers do not invent HTTP polling, SockJS, email, or a second API host.

This list is **frontend only** (`frontend/`). Backend REST + STOMP `/ws` already exist (finish the backend task list first, including Group 3.1). Do not change Java. Do not rebuild login, people, time, leave, or reports pages.

---

## Current frontend (inspected)

Stack: Angular 21 standalone components, `provideHttpClient` + auth interceptor, Vitest. **No `@stomp/stompjs` yet.**

What already works (reuse, do **not** redo):

- Tenant from hostname (`TenantService`, `parseTenantSlug`). API = same hostname, port `8080` via `TenantService.url(...)`. `apiBaseUrl` is `http://{tenant}.localhost:8080` in local dev (`tenant.util.ts`).
- JWT in `localStorage`, Bearer interceptor, 401 → logout. `authGuard` / `guestGuard` / `tenantGuard` / `roleGuard`. `AuthService.hasRole` / `hasAnyRole`. Token key already used by `AuthService` — reuse it for the websocket query; do not invent a second token store.
- Global look in `styles.scss`: `.error-banner`, `.form-field`, `.btn-primary`, `.btn-secondary`, `.data-table`, `.page-header`, `.muted`. Square corners, solid 1px borders, no gradients.
- `AppLayoutComponent` (sidebar + top bar + Sign Out) in `features/layout/`. Nav list is `navItems` in that component (`ALL_ROLES` / `MANAGER_PLUS` / `HR_ONLY`). Feature pages already sit under the layout in `app.routes.ts`.
- `apiErrorMessage` in `core/http/api-error.ts` (login-oriented 403 text — **do not use it blindly** for notifications; prefer `error.error.message` like reports).

What is missing (this document):

- No notification models, no HTTP `NotificationService`, no STOMP client, no `/notifications` route, no inbox page, no unread badge.

---

## Locked decisions (do not change without an explicit product decision)

1. **Frontend only.** Do not edit `backend/`. Do not add Java endpoints. If a field is not on the DTO below, do not display it.
2. **Tenant from Host only.** HTTP uses `TenantService.url('/notifications...')`. Websocket uses the **same tenant host**, `http` → `ws` (and `https` → `wss`), path `/ws`. Never hardcode `localhost:8080`. Never send `companyId`, `tenantId`, or `subdomain`.
3. **STOMP over native WebSocket — no polling.** Add npm `@stomp/stompjs` only (no `sockjs-client`). Connect while `AppLayoutComponent` is alive. Subscribe to `/user/queue/notifications`. Disconnect on layout destroy and on logout. **No `setInterval` unread-count poll.**
4. **Handshake auth:** `wsUrl + '?token=' + encodeURIComponent(jwt)`. Do not put the JWT in logs or in the URL bar of feature routes. STOMP `connectHeaders` may repeat `Authorization: Bearer {jwt}` if easy; query `token` is mandatory (backend handshake interceptor).
5. **Live update:** on each STOMP `NotificationDto`, increment the unread badge by 1 and, if the inbox page is showing, prepend the row (skip duplicate `id`). Initial badge and inbox table still load via **HTTP once** (`unreadCount()` / `list()`). After mark-read / mark-all, update from HTTP (or decrement locally); do not wait for a STOMP mark-read frame (there is none).
6. **Reconnect:** `@stomp/stompjs` built-in reconnect (e.g. 5s). Stop reconnecting after logout. Ignore STOMP errors in the layout (no error-banner spam). Inbox page still shows HTTP errors on list/mark-read.
7. **All authenticated roles** see Notifications. Use `ALL_ROLES` on the nav item. No `roleGuard` on the inbox route.
8. **Reuse shell and CSS.** Native `<table class="data-table">`. Empty = one sentence. Loading = `Loading.` Unread rows may use `font-weight: 700`; no color badges, pills, icons, or emoji.
9. **Do not add** a dropdown panel or bell icon. Top bar: **text link** to `/notifications` plus count, e.g. `Notifications (3)` or `Notifications` when 0.
10. **Click = mark read, then navigate.** `LEAVE_REQUEST` → `/leave`. `TIME_ENTRY` → `/time`. Do not deep-link to `resourceId`. Do not open a modal.
11. **HTTP errors:** prefer `error.error.message`. Do not map every 403 to the login “account deactivated” string. 401 still goes through the interceptor.
12. **Do not add** Vitest specs unless you already have a one-file pattern you can copy.

### UI rules (mandatory)

Same as the rest of the app: no emojis, no gradients, no dashed borders, `border-radius: 0`, no transitions/animations, colors `#ffffff` / `#111111` / muted `#555555`, error banner `#f8f8f8`. Primary button black, secondary white with black border.

### Backend contracts (camelCase JSON)

Do not send `companyId`. Datetimes are ISO-like strings from Jackson (show with `DatePipe` or the raw string; do not add a timezone library).

**List** — `GET /notifications?unreadOnly`

- Omit `unreadOnly` for all. Send `unreadOnly=true` only if you add an “Unread only” checkbox (optional; default is all).
- Body: **array** of `id`, `type`, `title`, `body`, `resourceType` (`LEAVE_REQUEST` | `TIME_ENTRY`), `resourceId`, `readAt` (string or null), `createdAt` (string)

**Unread count** — `GET /notifications/unread-count`

- Body: `{ count: number }`

**Mark one read** — `POST /notifications/{id}/read` — 204 empty.

**Mark all read** — `POST /notifications/read-all` — 204 empty.

**STOMP** — `ws://{tenant-host}:8080/ws?token={jwt}` then subscribe `/user/queue/notifications`. Payload = same object as a list item (`NotificationDto`).

`type` values (display `title` / `body` from the API; do not re-translate): `LEAVE_SUBMITTED`, `LEAVE_APPROVED`, `LEAVE_DENIED`, `LEAVE_CANCELLED`, `LEAVE_CANCELLATION_REQUESTED`, `LEAVE_CANCELLATION_DENIED`, `TIME_APPROVED`, `TIME_REJECTED`, `TIME_CORRECTION_REQUESTED`, `TIME_CORRECTION_APPROVED`, `TIME_CORRECTION_DENIED`.

---

## How to run a group

Copy **one group** into the agent (effort line + every numbered task in that group). These groups are sized for a **high** or **extra-high** model: one group = one shot. Do not split a group across chats. Do not start a later phase before the earlier one is done.

Run this list **after** the backend notification groups (including websocket Group 3.1) are done.

---

## Phase 0 — Types and HTTP


### Group 0.1 — Models and HTTP `NotificationService`

effort: high

1. Add `core/models/notification.model.ts` with `NotificationResourceType`, `NotificationType` (union of the strings above), `Notification` (`id`, `type`, `title`, `body`, `resourceType`, `resourceId`, `readAt: string | null`, `createdAt`), and `UnreadCount` (`count: number`). No `companyId`. No `recipientUserId`.
2. Add `core/services/notification.service.ts`. All REST URLs via `TenantService.url`. Methods: `list(unreadOnly?: boolean)` (omit the query when not true), `unreadCount()`, `markRead(id)`, `markAllRead()`. POSTs must tolerate 204. Do not send empty `unreadOnly=`. Add `wsUrl(token: string)` that takes `TenantService` `apiBaseUrl`, swaps `http`→`ws` / `https`→`wss`, and returns `{wsBase}/ws?token={encoded jwt}`. Do not add STOMP yet.

---

## Phase 1 — Inbox page, route, top-bar badge


### Group 1.1 — Page, nav, mark-read, navigation

effort: extra-high

3. Add `features/notifications/notification-inbox.component.ts` / `.html` / `.scss` as a child of `AppLayoutComponent` in `app.routes.ts` at `/notifications` (`tenantGuard` + `authGuard` already on the layout — **no** `roleGuard`). Page title `Notifications`. Table columns: when (`createdAt`), title, body, type. Unread rows bold. Empty: `No notifications.` Error banner on failure. Button `Mark all read` (`.btn-secondary`) disabled while loading or when there are no unread rows. Load list **once** via HTTP on init.
4. Sidebar: add `{ label: 'Notifications', route: '/notifications', roles: ALL_ROLES, exact: true }` to `navItems` in `app-layout.component.ts` (near Home is fine). Top bar: text link to `/notifications` showing `Notifications` or `Notifications (n)` from unread count. Load count **once** on layout init via HTTP. Do not use a dropdown, bell icon, or `setInterval`.
5. Row click (or a text `Open` control per row): `markRead(id)` if `readAt` is null, then `Router.navigate` to `/leave` when `resourceType` is `LEAVE_REQUEST`, else `/time`. Refresh the table and unread count after mark-one and mark-all. If mark-read returns 404, show a banner; do not crash.

---

## Phase 2 — STOMP client (no polling)


### Group 2.1 — Connect, subscribe, live badge, reconnect

effort: extra-high

6. `npm install @stomp/stompjs` (no sockjs). Add `core/services/notification-socket.service.ts` using `Client` from `@stomp/stompjs`. `brokerURL` = `NotificationService.wsUrl(jwt)`. Subscribe `/user/queue/notifications`. Parse body as `Notification`. Expose a stream (Subject/Observable) of incoming items. `activate()` when the layout is created and a token exists; `deactivate()` on destroy and when `AuthService` logs out. Enable reconnect delay ~5000ms. Do not poll HTTP.
7. In `AppLayoutComponent`, subscribe to the socket stream: increment unread count (do not go below the idea of “this is a new unread row”; skip if `id` already counted). Inbox component: if it is alive, prepend the dto unless that `id` is already in the table. After mark-all, set badge to 0 immediately.
8. Grep `frontend/src` for notification `setInterval`, `localhost:8080`, `companyId`, `tenantId`, `sockjs`. Remove any. All REST through `TenantService.url`. Grep new notification files for `border-radius`, `gradient`, `dashed`, `transition`, `animation`, emoji, canvas/charts. None. Corners square.

---

## Execution order (summary)

`0 models+HTTP` → `1 inbox+nav+mark-read` → `2 STOMP (no poll)`.

Do not build the page before `NotificationService` exists. Do not add HTTP polling. Do not add SockJS. Do not redesign login.

Suggested one-shot size: **one Group** per run. `high` and `extra-high` are still one run (the whole group).
