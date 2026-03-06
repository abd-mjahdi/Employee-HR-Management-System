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