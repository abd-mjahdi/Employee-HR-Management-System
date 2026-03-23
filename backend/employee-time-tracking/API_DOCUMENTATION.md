# API Documentation - Authentication Endpoints

## POST /auth/register
Register a new user account.

**URL:** `http://localhost:8080/auth/register`

**Method:** `POST`

**Authentication Required:** No

**Request Body:**
```json
{
  "username": "ali.omar",
  "email": "ali.omar@company.com",
  "password": "SecurePass123!",
  "firstName": "ali",
  "lastName": "omar",
  "userRole": "EMPLOYEE",
  "departmentId": 1,
  "managerId": 2
}
```

**Success Response (201 CREATED):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "email": "ali.omar@company.com",
  "role": "EMPLOYEE"
}
```

**Error Responses:**

409 CONFLICT - Email already exists:
```json
{
  "success": false,
  "message": "Email already registered",
  "email": null,
  "role": null
}
```

404 NOT FOUND - Department not found:
```json
{
  "success": false,
  "message": "Department not found",
  "email": null,
  "role": null
}
```

400 BAD REQUEST - Weak password:
```json
{
  "success": false,
  "message": "Password must be at least 8 characters",
  "email": null,
  "role": null
}
```

**Example cURL:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ali.omar",
    "email": "ali.omar@company.com",
    "password": "SecurePass123!",
    "firstName": "ali",
    "lastName": "omar",
    "userRole": "EMPLOYEE",
    "departmentId": 1,
    "managerId": 2
  }'
```

---

## POST /auth/login
Authenticate user and receive JWT token.

**URL:** `http://localhost:8080/auth/login`

**Method:** `POST`

**Authentication Required:** No

**Request Body:**
```json
{
  "email": "admin@example.com",
  "password": "admin123"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "admin@example.com",
  "role": "HR_ADMIN"
}
```

**Error Response (401 UNAUTHORIZED):**
```json
{
  "success": false,
  "message": "Invalid credentials",
  "token": null,
  "email": null,
  "role": null
}
```

**Error Response (403 FORBIDDEN) - Account deactivated:**
```json
{
  "success": false,
  "message": "Account is deactivated",
  "token": null,
  "email": null,
  "role": null
}
```

**Example cURL:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin123"
  }'
```

---

## GET /auth/me
Get current authenticated user details.

**URL:** `http://localhost:8080/auth/me`

**Method:** `GET`

**Authentication Required:** Yes (JWT token in Authorization header)

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "firstName": "Admin",
  "lastName": "User",
  "role": "HR_ADMIN",
  "department": {
    "id": 1,
    "departmentName": "Human Resources",
    "departmentCode": "HR",
    "isActive": true
  },
  "isActive": true
}
```

**Error Response (401 UNAUTHORIZED) - Missing or invalid token:**
```
Token expired
```
or
```
Invalid token
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## JWT Token Structure

**Claims in JWT payload:**
```json
{
  "sub": "user@company.com",
  "user_id": 5,
  "role": "MANAGER",
  "exp": 1234567890
}
```

**Token expiration:** Configured in application.properties (default: 24 hours)

**Using the token:**
Include in all authenticated requests as:
```
Authorization: Bearer <your-jwt-token>
```

---

## Testing with Postman

1. **Register a user:** Send POST to /auth/register with user details
2. **Login:** Send POST to /auth/login with email and password
3. **Copy the token** from login response
4. **Set Authorization:** In Postman, select "Bearer Token" type and paste the token
5. **Call protected endpoints:** GET /auth/me or any other authenticated endpoint

---

## Default Admin Account

For initial testing, a default admin account is created on first startup:

**Email:** admin@example.com  
**Password:** admin123  
**Role:** HR_ADMIN

I must Change this password in production!

---

## User Management Endpoints

### GET /users
Get all users with pagination (HR Admin only).

**URL:** `http://localhost:8080/users`

**Method:** `GET`

**Authentication Required:** Yes (HR_ADMIN role)

**Query Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Items per page
- `sort` (optional) - Sort field and direction (e.g., `firstName,asc`)

**Success Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "username": "john.doe",
      "email": "john.doe@company.com",
      "firstName": "John",
      "lastName": "Doe",
      "userRole": "EMPLOYEE",
      "department": {
        "id": 1,
        "departmentName": "Engineering",
        "departmentCode": "ENG",
        "isActive": true
      },
      "isActive": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 50,
  "totalPages": 3
}
```

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/users?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### GET /users/{id}
Get specific user details.

**URL:** `http://localhost:8080/users/{id}`

**Method:** `GET`

**Authentication Required:** Yes

**Authorization:** User can view their own profile, managers can view direct reports, HR admins can view anyone

**URL Parameters:**
- `id` (required) - User ID

**Success Response (200 OK):**
```json
{
  "id": 5,
  "username": "alice.dev",
  "email": "alice@company.com",
  "firstName": "Alice",
  "lastName": "Brown",
  "userRole": "EMPLOYEE",
  "department": {
    "id": 1,
    "departmentName": "Engineering",
    "departmentCode": "ENG",
    "isActive": true
  },
  "isActive": true
}
```

**Error Response (403 FORBIDDEN):**
```
You cannot access this resource
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/users/5 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### GET /users/me
Get current authenticated user profile.

**URL:** `http://localhost:8080/users/me`

**Method:** `GET`

**Authentication Required:** Yes

**Success Response (200 OK):**
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "firstName": "Admin",
  "lastName": "User",
  "userRole": "HR_ADMIN",
  "department": {
    "id": 4,
    "departmentName": "Human Resources",
    "departmentCode": "HR",
    "isActive": true
  },
  "isActive": true
}
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### GET /users/team
Get team members (manager's direct reports).

**URL:** `http://localhost:8080/users/team`

**Method:** `GET`

**Authentication Required:** Yes (MANAGER or HR_ADMIN role)

**Success Response (200 OK):**
```json
[
  {
    "id": 4,
    "username": "alice.dev",
    "email": "alice@company.com",
    "firstName": "Alice",
    "lastName": "Brown",
    "userRole": "EMPLOYEE",
    "department": {
      "id": 1,
      "departmentName": "Engineering",
      "departmentCode": "ENG",
      "isActive": true
    },
    "isActive": true
  },
  {
    "id": 5,
    "username": "bob.dev",
    "email": "bob@company.com",
    "firstName": "Bob",
    "lastName": "Wilson",
    "userRole": "EMPLOYEE",
    "department": {
      "id": 1,
      "departmentName": "Engineering",
      "departmentCode": "ENG",
      "isActive": true
    },
    "isActive": true
  }
]
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/users/team \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### POST /users
Create a new user (HR Admin only).

**URL:** `http://localhost:8080/users`

**Method:** `POST`

**Authentication Required:** Yes (HR_ADMIN role)

**Request Body:**
```json
{
  "username": "jane.smith",
  "email": "jane.smith@company.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "userRole": "EMPLOYEE",
  "departmentId": 2,
  "managerId": 3
}
```

**Success Response (201 CREATED):**
```json
{
  "user": {
    "id": 8,
    "username": "jane.smith",
    "email": "jane.smith@company.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "userRole": "EMPLOYEE",
    "department": {
      "id": 2,
      "departmentName": "Sales",
      "departmentCode": "SALES",
      "isActive": true
    },
    "isActive": true
  },
  "temporaryPassword": "a3f9d2c8e1b4"
}
```

**Note:** The temporary password is only shown once. Provide it to the new employee.

**Error Responses:**

409 CONFLICT - Email already exists:
```json
{
  "success": false,
  "message": "user already exists with that email"
}
```

409 CONFLICT - Username taken:
```json
{
  "success": false,
  "message": "username unavailable"
}
```

404 NOT FOUND - Department not found:
```json
{
  "success": false,
  "message": "Department not found"
}
```

**Example cURL:**
```bash
curl -X POST http://localhost:8080/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane.smith",
    "email": "jane.smith@company.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "userRole": "EMPLOYEE",
    "departmentId": 2,
    "managerId": 3
  }'
```

---

### PUT /users/{id}
Update user information (HR Admin only).

**URL:** `http://localhost:8080/users/{id}`

**Method:** `PUT`

**Authentication Required:** Yes (HR_ADMIN role)

**URL Parameters:**
- `id` (required) - User ID

**Request Body (all fields optional for partial update):**
```json
{
  "username": "john.doe.updated",
  "email": "john.updated@company.com",
  "firstName": "John",
  "lastName": "Doe",
  "userRole": "MANAGER",
  "departmentId": 2,
  "isActive": true
}
```

**Success Response (200 OK):**
```json
{
  "id": 5,
  "username": "john.doe.updated",
  "email": "john.updated@company.com",
  "firstName": "John",
  "lastName": "Doe",
  "userRole": "MANAGER",
  "department": {
    "id": 2,
    "departmentName": "Sales",
    "departmentCode": "SALES",
    "isActive": true
  },
  "isActive": true
}
```

**Error Response (409 CONFLICT) - Duplicate email:**
```json
{
  "success": false,
  "message": "Email already in use"
}
```

**Example cURL:**
```bash
curl -X PUT http://localhost:8080/users/5 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe Updated",
    "userRole": "MANAGER"
  }'
```

---

### PATCH /users/me/profile
Update own profile (non-sensitive fields only).

**URL:** `http://localhost:8080/users/me/profile`

**Method:** `PATCH`

**Authentication Required:** Yes (EMPLOYEE or MANAGER role)

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Updated"
}
```

**Success Response (204 NO CONTENT):**
No response body.

**Note:** Users can only update firstName and lastName. Cannot change email, username, role, department, or manager.

**Example cURL:**
```bash
curl -X PATCH http://localhost:8080/users/me/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Updated"
  }'
```

---

### PATCH /users/{id}/deactivate
Deactivate a user account (HR Admin only).

**URL:** `http://localhost:8080/users/{id}/deactivate`

**Method:** `PATCH`

**Authentication Required:** Yes (HR_ADMIN role)

**URL Parameters:**
- `id` (required) - User ID

**Success Response (204 NO CONTENT):**
No response body.

**Error Response (403 FORBIDDEN) - Account already deactivated:**
```json
{
  "success": false,
  "message": "User already deactivated"
}
```

**Example cURL:**
```bash
curl -X PATCH http://localhost:8080/users/5/deactivate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### PATCH /users/{id}/activate
Reactivate a deactivated user account (HR Admin only).

**URL:** `http://localhost:8080/users/{id}/activate`

**Method:** `PATCH`

**Authentication Required:** Yes (HR_ADMIN role)

**URL Parameters:**
- `id` (required) - User ID

**Success Response (204 NO CONTENT):**
No response body.

**Error Response (403 FORBIDDEN) - Account already active:**
```json
{
  "success": false,
  "message": "User already active"
}
```

**Example cURL:**
```bash
curl -X PATCH http://localhost:8080/users/5/activate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### GET /users/search
Search users with filters (HR Admin only).

**URL:** `http://localhost:8080/users/search`

**Method:** `GET`

**Authentication Required:** Yes (HR_ADMIN role)

**Query Parameters (all optional):**
- `departmentId` - Filter by department ID
- `role` - Filter by role (EMPLOYEE, MANAGER, HR_ADMIN)
- `active` - Filter by active status (true/false)
- `name` - Search by first or last name (partial match)

**Success Response (200 OK):**
```json
[
  {
    "id": 4,
    "username": "alice.dev",
    "email": "alice@company.com",
    "firstName": "Alice",
    "lastName": "Brown",
    "userRole": "EMPLOYEE",
    "department": {
      "id": 1,
      "departmentName": "Engineering",
      "departmentCode": "ENG",
      "isActive": true
    },
    "isActive": true
  }
]
```

**Example cURL:**
```bash
# Search by department
curl -X GET "http://localhost:8080/users/search?departmentId=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Search by role and active status
curl -X GET "http://localhost:8080/users/search?role=EMPLOYEE&active=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Search by name
curl -X GET "http://localhost:8080/users/search?name=Alice" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Combine filters
curl -X GET "http://localhost:8080/users/search?departmentId=1&role=EMPLOYEE&active=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Department Endpoints

### GET /departments
Get all departments.

**URL:** `http://localhost:8080/departments`

**Method:** `GET`

**Authentication Required:** Yes

**Success Response (200 OK):**
```json
[
  {
    "id": 1,
    "departmentName": "Engineering",
    "departmentCode": "ENG",
    "isActive": true
  },
  {
    "id": 2,
    "departmentName": "Sales",
    "departmentCode": "SALES",
    "isActive": true
  }
]
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/departments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### GET /departments/{id}
Get specific department details.

**URL:** `http://localhost:8080/departments/{id}`

**Method:** `GET`

**Authentication Required:** Yes

**URL Parameters:**
- `id` (required) - Department ID

**Success Response (200 OK):**
```json
{
  "id": 1,
  "departmentName": "Engineering",
  "departmentCode": "ENG",
  "isActive": true
}
```

**Error Response (404 NOT FOUND):**
```json
{
  "success": false,
  "message": "Department not found"
}
```

**Example cURL:**
```bash
curl -X GET http://localhost:8080/departments/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---