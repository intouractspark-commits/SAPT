# SPAT — Workflows and Architecture
## Detailed Workflows, Sequence Diagrams, and Architecture Reference

---

| Document | SPAT Workflows and Architecture |
|---|---|
| Version | 1.0 |
| Date | October 2026 |

---

## Section 1: System Architecture Diagrams

### 1.1 Cloud Deployment Architecture

```mermaid
graph TB
    subgraph "User Devices"
        U1["Student Browser"]
        U2["Mentor Browser"]
        U3["HOD/Admin Browser"]
    end

    subgraph "Vercel CDN (Frontend)"
        FE["React 19 SPA<br/>Built with Vite<br/>Styled with Tailwind CSS<br/>vercel.json SPA rewrites"]
    end

    subgraph "Render (Backend)"
        BE["Spring Boot 3.2.5<br/>Docker Container<br/>JRE 17 Alpine<br/>Port 10000<br/>-Xmx400m"]
        SV["Spring Security<br/>JWT Auth Filter<br/>CORS (configured origins)"]
    end

    subgraph "Supabase (Primary DB)"
        PG["PostgreSQL<br/>SSL Required<br/>Tables: users, colleges,<br/>departments, submissions,<br/>daily_logs, notifications,<br/>succession_requests, etc."]
    end

    subgraph "MongoDB Atlas (Secondary DB)"
        MG["MongoDB Cluster<br/>Collections:<br/>submission_files (blobs)<br/>sapt_logs (audit trail)"]
    end

    subgraph "Google Workspace"
        SM["Gmail SMTP<br/>Port 587 (TLS)<br/>OTP Emails<br/>Notification Emails"]
    end

    U1 & U2 & U3 -->|HTTPS| FE
    FE -->|HTTPS + JWT Bearer| BE
    BE --> SV
    BE -->|Spring Data JPA / JDBC| PG
    BE -->|Spring Data MongoDB| MG
    BE -->|Spring Mail (Async)| SM
```

### 1.2 Backend Internal Architecture

```mermaid
graph TB
    subgraph "HTTP Request Handling"
        RC["HTTP Request"] --> JF["JwtAuthFilter<br/>(extract + validate token)"]
        JF --> SCH["SecurityContextHolder<br/>(set Authentication)"]
        SCH --> CTRL["Role Controller<br/>(@PreAuthorize)"]
    end

    subgraph "Modules"
        CTRL --> AUTH["auth/<br/>AuthController<br/>AuthService"]
        CTRL --> SA["systemadmin/<br/>SystemAdminController<br/>SystemAdminService"]
        CTRL --> CA["collegeadmin/<br/>CollegeAdminController<br/>CollegeAdminService"]
        CTRL --> HOD["hod/<br/>HodController<br/>HodService"]
        CTRL --> MT["mentor/<br/>MentorController<br/>MentorService"]
        CTRL --> ST["student/<br/>StudentController<br/>StudentService"]
        CTRL --> SUB["submission/<br/>SubmissionController<br/>SubmissionService"]
        CTRL --> NT["notification/<br/>NotificationController<br/>NotificationService"]
    end

    subgraph "Common Infrastructure"
        AUTH & SA & CA & HOD & MT & ST & SUB --> CMN["common/<br/>ApiResponse<br/>SaptException<br/>GlobalExceptionHandler<br/>AppConstants"]
        AUTH & SA & CA & HOD & MT & ST & SUB --> NT
    end

    subgraph "Data Layer"
        AUTH --> JPA["Spring Data JPA<br/>UserRepository<br/>SessionRepository"]
        SA --> JPA
        CA --> JPA
        HOD --> JPA
        MT --> JPA & MNG["Spring Data MongoDB<br/>MongoSubmissionFile"]
        ST --> JPA & MNG
        SUB --> JPA & MNG
        NT --> JPA
    end
```

### 1.3 Frontend Architecture

```mermaid
graph TB
    subgraph "Entry Point"
        IH["index.html"] --> MN["main.jsx<br/>React 19"]
    end

    subgraph "Context Providers"
        MN --> AP["AuthProvider<br/>(AuthContext.jsx)<br/>- user session<br/>- login/logout<br/>- updateSession<br/>- auto-logout on 401"]
        AP --> TP["ThemeProvider<br/>(ThemeContext.jsx)"]
    end

    subgraph "Routing"
        TP --> AR["AppRouter.jsx<br/>BrowserRouter"]
        AR --> PR["ProtectedRoute<br/>(checks role + auth)"]
        AR --> PUB["PublicRoute<br/>(redirects if logged in)"]
    end

    subgraph "Public Pages"
        PUB --> HP["HomePage"]
        PUB --> LP["LoginPage + AdminLoginPage"]
        PUB --> RP["RegisterPage"]
    end

    subgraph "Protected Dashboards"
        PR --> DL["RoleDashboardLayout<br/>(Sidebar + Navbar)"]
        DL --> SAP["SystemAdmin Pages<br/>- Dashboard<br/>- Colleges<br/>- Add Admin<br/>- College Admins<br/>- Profile"]
        DL --> CAP["CollegeAdmin Pages<br/>- Dashboard<br/>- Departments<br/>- HOD Requests<br/>- Profile"]
        DL --> HP2["HOD Pages<br/>- Dashboard<br/>- Mentors<br/>- Approvals<br/>- Profile"]
        DL --> MP["Mentor Pages<br/>- Dashboard<br/>- Students<br/>- Submissions<br/>- Logs<br/>- Student Approvals<br/>- Profile"]
        DL --> SP["Student Pages<br/>- Metrics (Dashboard)<br/>- Submission<br/>- Logs<br/>- Reviews<br/>- Profile"]
    end
```

### 1.4 Entity Relationship Diagram (Abridged)

```mermaid
erDiagram
    colleges {
        char36 id PK
        varchar name UK
        varchar state
        enum status "ACTIVE|INACTIVE|SUSPENDED"
        date contract_start
        date contract_end
    }

    departments {
        char36 id PK
        char36 college_id FK
        varchar name
    }

    users {
        char36 id PK
        enum role "SYSTEM_ADMIN|COLLEGE_ADMIN|HOD|MENTOR|STUDENT"
        varchar email UK
        enum status "PENDING|APPROVED|REJECTED"
        char36 college_id FK
        char36 department_id FK
        char36 mentor_id FK "self-ref"
        char36 hod_id FK "self-ref"
        varchar roll_no
    }

    submissions {
        char36 id PK
        char36 student_id FK
        char36 mentor_id FK
        enum status "PENDING|APPROVED|REJECTED"
        int suggested_credits
        int awarded_credits
        int credit_penalty
        bool is_resubmission
        char36 parent_submission_id FK "self-ref"
    }

    student_credit_snapshots {
        char36 id PK
        char36 student_id UK
        int total_credits
        int stars
        varchar badge
    }

    daily_logs {
        char36 id PK
        char36 student_id FK
        varchar review_status
        char36 remarked_by FK
    }

    activity_categories {
        char36 id PK
        varchar name UK
        bool is_custom
    }

    activity_sub_types {
        char36 id PK
        char36 category_id FK
        varchar label
        int points
    }

    notifications {
        char36 id PK
        char36 recipient_id FK
        enum type
        bool is_read
    }

    succession_requests {
        char36 id PK
        char36 outgoing_user_id FK
        varchar status "PENDING|APPROVED|REJECTED|CANCELLED"
        enum outgoing_role "MENTOR|HOD"
    }

    star_thresholds {
        int id PK
        int stars
        int min_credits
        varchar badge_name
    }

    colleges ||--o{ departments : "has"
    colleges ||--o{ users : "members"
    departments ||--o{ users : "assigned"
    users ||--o{ submissions : "submits as student"
    users ||--o{ submissions : "reviews as mentor"
    users ||--|| student_credit_snapshots : "has snapshot"
    users ||--o{ daily_logs : "writes"
    users ||--o{ notifications : "receives"
    submissions ||--o{ submissions : "resubmission of"
    activity_categories ||--o{ activity_sub_types : "has"
    activity_sub_types ||--o{ submissions : "classifies"
```

---

## Section 2: Detailed Workflow Diagrams

### Workflow 1: Complete Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as React App
    participant BE as Spring Boot API
    participant DB as PostgreSQL
    participant MAIL as Gmail SMTP

    Note over U,MAIL: === REGISTRATION ===
    U->>FE: Fill registration form
    FE->>BE: POST /api/auth/register {name, email, password, role, college, department}
    BE->>DB: Check if email already exists
    alt Email exists
        BE-->>FE: 409 Conflict "Email already registered"
    else Email is new
        BE->>DB: INSERT users {status=PENDING, otp_code, otp_expires_at}
        BE->>+MAIL: Send OTP email (async)
        MAIL-->>-BE: Email queued
        BE-->>FE: 200 OK "Check email for OTP"
    end

    Note over U,MAIL: === OTP VERIFICATION ===
    U->>FE: Enter OTP from email
    FE->>BE: POST /api/auth/otp/verify {email, otp, purpose=EMAIL_VERIFICATION}
    BE->>DB: SELECT user WHERE email = ?
    alt OTP invalid or expired
        BE-->>FE: 400 "Invalid or expired OTP"
    else OTP valid
        BE->>DB: UPDATE users SET otp_code=null, otp_expires_at=null
        BE-->>FE: 200 OK "Email verified"
    end

    Note over U,MAIL: === LOGIN ===
    U->>FE: Enter email + password + role
    FE->>BE: POST /api/auth/login {email, password, role}
    BE->>DB: SELECT user WHERE email = ?
    BE->>BE: BCrypt verify password
    alt Wrong credentials or wrong role
        BE-->>FE: 401 Unauthorized
    else Status = PENDING
        BE-->>FE: 403 "Account pending approval"
    else Status = REJECTED
        BE-->>FE: 403 "Account rejected"
    else Status = APPROVED
        BE->>DB: UPDATE last_login_at
        BE->>BE: Generate JWT (24h, role claim)
        BE-->>FE: 200 LoginResponse {token, role, name, college, department, ...}
        FE->>FE: Store JWT in localStorage
        FE->>U: Redirect to /dashboard/{role}
    end
```

### Workflow 2: Complete Submission Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING : Student submits activity
    PENDING --> APPROVED : Mentor approves (sets awarded_credits)
    PENDING --> REJECTED : Mentor rejects (credit_penalty = ceil(suggested × 10%))
    REJECTED --> PENDING : Student resubmits (new submission, is_resubmission=true)
    APPROVED --> [*] : Credits added to StudentCreditSnapshot
    REJECTED --> [*] : Penalty deducted from StudentCreditSnapshot
```

```mermaid
sequenceDiagram
    participant S as Student
    participant FE as React App
    participant BE as Spring Boot API
    participant PG as PostgreSQL
    participant MG as MongoDB
    participant N as Notification System

    Note over S,N: === STUDENT CREATES SUBMISSION ===
    S->>FE: Open /dashboard/student/submission
    FE->>BE: GET /api/student/categories
    BE->>PG: SELECT activity_categories + activity_sub_types WHERE is_active=true
    BE-->>FE: [{category, subTypes: [{label, points}]}]
    S->>FE: Select category, sub-type, fill title/description/date, attach file
    FE->>BE: POST /api/student/submissions {categoryId, subTypeId, title, description, activityDate, files[]}
    BE->>PG: INSERT submissions {status=PENDING, suggestedCredits=subType.points}
    BE->>MG: Store file as MongoSubmissionFile document
    BE->>PG: INSERT notifications {recipient=mentor, type=SUBMISSION_CREATED}
    BE-->>FE: SubmissionResponse {id, status:"pending", suggestedCredits:N}
    FE->>S: "Submission uploaded successfully"

    Note over S,N: === MENTOR REVIEWS ===
    FE->>BE: GET /api/mentor/submissions/pending (Mentor polls)
    BE-->>FE: [SubmissionResponse, ...]
    
    alt Mentor Approves
        FE->>BE: PUT /api/mentor/submissions/{id}/approve {credits: N, review: "text"}
        BE->>PG: UPDATE submissions SET status=APPROVED, awarded_credits=N, reviewed_at=now
        BE->>PG: UPDATE student_credit_snapshots (totalCredits += N, totalApproved++, recalc stars)
        BE->>PG: INSERT notifications {recipient=student, type=SUBMISSION_APPROVED}
        BE-->>FE: 200 OK
    else Mentor Rejects
        FE->>BE: PUT /api/mentor/submissions/{id}/reject {review: "reason"}
        BE->>BE: penalty = ceil(suggestedCredits × 0.10)
        BE->>PG: UPDATE submissions SET status=REJECTED, credit_penalty=penalty
        BE->>PG: UPDATE student_credit_snapshots (totalCredits -= penalty, totalRejected++)
        BE->>PG: INSERT notifications {recipient=student, type=SUBMISSION_REJECTED}
        BE-->>FE: 200 OK
    end

    Note over S,N: === STUDENT RESUBMITS (after rejection) ===
    S->>FE: View rejection → decide to resubmit
    FE->>BE: POST /api/student/submissions {is_resubmission=true, parent_submission_id=originalId, ...}
    BE->>PG: INSERT new submission {is_resubmission=true, parent_submission_id=...}
    BE-->>FE: New SubmissionResponse (status=PENDING)
```

### Workflow 3: Password Reset Flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant EM as Email

    U->>FE: Click "Forgot Password"
    FE->>BE: POST /api/auth/otp/send {email, purpose=PASSWORD_RESET}
    BE->>DB: Look up user by email
    BE->>DB: UPDATE users SET otp_code=randomCode, otp_expires_at=now+15min
    BE->>EM: Send password reset OTP email
    BE-->>FE: 200 "OTP sent"
    
    U->>FE: Enter OTP
    FE->>BE: POST /api/auth/otp/verify {email, otp, purpose=PASSWORD_RESET}
    BE->>DB: Validate OTP and expiry
    BE-->>FE: 200 "OTP verified"

    U->>FE: Enter new password
    FE->>BE: POST /api/auth/password/reset {email, otp, newPassword}
    BE->>DB: BCrypt hash newPassword
    BE->>DB: UPDATE users SET password_hash=newHash, otp_code=null
    BE-->>FE: 200 "Password reset. Please log in."
    FE->>U: Redirect to login page
```

### Workflow 4: Mentor Succession Handover

```mermaid
sequenceDiagram
    participant M as Outgoing Mentor
    participant HOD as HOD
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL

    M->>FE: Open Succession form in Mentor Profile
    M->>FE: Fill: candidateName, candidateEmail, candidatePhone, tempPassword
    FE->>BE: POST /api/mentor/succession {candidateName, email, phone, password}
    BE->>DB: BCrypt hash candidate password
    BE->>DB: INSERT succession_requests {outgoingUserId=mentor.id, status=PENDING, outgoingRole=MENTOR}
    BE-->>FE: 200 "Succession request submitted"

    Note over HOD,DB: HOD reviews (endpoint gap — TBC)
    HOD->>BE: [TBC endpoint] GET pending succession requests in department
    BE-->>HOD: [list of PENDING succession requests]

    HOD->>BE: [TBC endpoint] PUT approve succession/{id}
    BE->>DB: UPDATE succession_requests SET status=APPROVED, reviewedBy=hodId, reviewedAt=now
    BE->>DB: UPDATE users SET name=candidate.name, email=candidate.email, password_hash=candidate.hash
    Note over DB: Outgoing mentor's account is now the incoming faculty's account

    alt Mentor wants to cancel before decision
        M->>BE: DELETE /api/mentor/succession
        BE->>DB: UPDATE succession_requests SET status=CANCELLED
        BE-->>M: 200 "Succession request cancelled"
    end
```

### Workflow 5: HOD Approves a Mentor Registration

```mermaid
sequenceDiagram
    participant MT as New Mentor
    participant HOD as HOD
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL

    MT->>FE: Navigate to /register
    MT->>FE: Fill: name, email, password, role=MENTOR, college, department
    FE->>BE: POST /api/auth/register
    BE->>DB: INSERT users {role=MENTOR, status=PENDING}
    BE->>DB: Send OTP → mentor verifies email

    HOD->>FE: Navigate to /dashboard/hod/approvals
    FE->>BE: GET /api/hod/mentor-approvals
    BE->>DB: SELECT users WHERE role=MENTOR AND status=PENDING AND department_id=hod.departmentId
    BE-->>FE: [List of pending mentors]

    HOD->>FE: Click Approve for a mentor
    FE->>BE: POST /api/auth/users/{mentorId}/status?status=APPROVED
    BE->>DB: UPDATE users SET status=APPROVED WHERE id=mentorId
    BE-->>FE: 200 "User status updated"
    FE->>HOD: Mentor removed from pending list

    Note over MT: Mentor can now log in and access the mentor dashboard
```

### Workflow 6: Student Earns Star Badge

```mermaid
flowchart TD
    A["Student submits activity"] --> B["Mentor approves: awarded_credits = N"]
    B --> C["SubmissionService.reviewSubmission()"]
    C --> D["UPDATE submissions: status=APPROVED, awarded_credits=N"]
    D --> E["Recalculate StudentCreditSnapshot"]
    E --> F["totalCredits += N; totalApproved++"]
    F --> G["Query star_thresholds WHERE min_credits <= totalCredits ORDER BY stars DESC LIMIT 1"]
    G --> H{"New star level?"}
    H --> |"Yes"| I["UPDATE snapshot: stars=newLevel, badge=badgeName"]
    H --> |"No change"| J["Keep current star level"]
    I --> K["Student dashboard shows new badge 🏅"]

    subgraph "Star Thresholds (seeded)"
        T1["0 stars: < 100 credits → Beginner"]
        T2["1 star: 100+ credits → Bronze"]
        T3["2 stars: 250+ credits → Silver"]
        T4["3 stars: 500+ credits → Gold"]
        T5["4 stars: 1000+ credits → Platinum"]
        T6["5 stars: 2000+ credits → Diamond"]
    end
```

### Workflow 7: System Admin Onboards a College

```mermaid
sequenceDiagram
    participant SA as System Admin
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant MAIL as Gmail SMTP

    SA->>FE: Navigate to /dashboard/system-admin/add-admin
    FE->>SA: Display onboarding form
    SA->>FE: Fill: collegeName, state, address, contractStart, contractEnd, adminName, adminEmail, adminPassword
    FE->>BE: POST /api/system-admin/college-admin {all fields}
    
    BE->>DB: Check if college name already exists
    alt College exists
        BE-->>FE: 409 Conflict
    else New college
        BE->>DB: INSERT colleges {status=ACTIVE, contract dates}
        BE->>DB: INSERT users {role=COLLEGE_ADMIN, status=APPROVED, college_id=new_college_id}
        BE->>MAIL: Send welcome email to new College Admin (TBC)
        BE-->>FE: 200 OK "College admin created successfully"
        FE->>SA: Success confirmation
    end

    Note over SA,DB: College Admin can immediately log in to /admin-login
```

---

## Section 3: Data Flow Reference

### 3.1 Frontend → Backend Data Flow

```
Browser Request
    ↓
React Component (e.g., MentorDashboard.jsx)
    ↓
API call via utils/api.js (adds JWT Authorization header)
    ↓
Spring Boot JwtAuthFilter
    ├── Extract token from "Authorization: Bearer {token}"
    ├── Validate signature and expiry (JwtUtil)
    └── Set SecurityContext (email as username, ROLE_X as authority)
    ↓
Controller method (e.g., MentorController.getDashboardStats)
    ├── @PreAuthorize("hasRole('MENTOR')") — enforced
    └── @AuthenticationPrincipal UserDetails → userDetails.getUsername() = email
    ↓
Service layer (e.g., MentorService.getDashboardStats)
    ├── Resolve email → User entity via UserRepository
    └── Business logic + database queries
    ↓
Repository layer (Spring Data JPA / MongoDB)
    ↓
PostgreSQL (Supabase) or MongoDB (Atlas)
    ↓
Response wrapped in ApiResponse<T> { success, message, data, timestamp }
    ↓
React component renders data
```

### 3.2 API Response Format

All API responses use a consistent wrapper:

```json
{
  "success": true,
  "message": "Dashboard stats fetched successfully",
  "data": { ... },
  "timestamp": "2026-10-27T22:53:12"
}
```

Error responses:
```json
{
  "success": false,
  "message": "Invalid OTP or OTP has expired",
  "data": null,
  "timestamp": "2026-10-27T22:53:12"
}
```

### 3.3 JWT Token Structure

```json
Header: { "alg": "HS256", "typ": "JWT" }
Payload: {
  "sub": "user@example.com",   ← email (used as username in Spring Security)
  "role": "ROLE_MENTOR",
  "iat": 1730050000,
  "exp": 1730136400            ← iat + 86400 seconds (24 hours)
}
Signature: HMAC-SHA256(base64url(header) + "." + base64url(payload), JWT_SECRET)
```

### 3.4 Authentication Principal Resolution

> **Important architectural note:** `UserDetails.getUsername()` in SAPT returns the user's **email address** (not UUID). Controllers that need the user's UUID must look up the User entity via `UserRepository.findByEmail(email)`. This pattern is used across all controllers.

```java
// Pattern used across all controllers:
String email = userDetails.getUsername();       // email
User user = userRepository.findByEmail(email);  // resolve to User entity
String userId = user.getId();                   // get UUID
```

---

## Section 4: Environment and Configuration Reference

### 4.1 Backend Profiles

| Profile | File | Use |
|---|---|---|
| default | `application.properties` | Common config, reads from .env |
| dev | `application-dev.properties` | Development overrides |
| prod | `application-prod.properties` | Production overrides |

### 4.2 Application Configuration Key Points

- **Context path:** `/api` (all endpoints prefixed with `/api/`)
- **CORS:** Origins from `ALLOWED_ORIGINS` env var
- **JPA:** Auto-DDL configured for schema creation/update
- **MongoDB:** Configured for `sapt_logs` database
- **Async:** `@Async` used for email sending (non-blocking)
- **Logging:** SLF4J + Logback; log files stored in `backend/logs/`

### 4.3 Frontend Route Map

| Route | Component | Access Control |
|---|---|---|
| `/` | HomePage | Public |
| `/login` | LoginPage | Public (redirects if logged in) |
| `/admin-login` | AdminLoginPage | Public (redirects if logged in) |
| `/register` | RegisterPage | Public (redirects if logged in) |
| `/dashboard/system-admin` | SystemAdminAnalyticsDashboard | SYSTEM_ADMIN only |
| `/dashboard/system-admin/colleges` | SystemAdminColleges | SYSTEM_ADMIN only |
| `/dashboard/system-admin/add-admin` | SystemAdminAddAdmin | SYSTEM_ADMIN only |
| `/dashboard/system-admin/college-admins` | SystemAdminCollegeAdmins | SYSTEM_ADMIN only |
| `/dashboard/system-admin/profile` | SystemAdminProfile | SYSTEM_ADMIN only |
| `/dashboard/college-admin` | CollegeAdminDashboard | COLLEGE_ADMIN only |
| `/dashboard/college-admin/departments` | CollegeAdminDepartments | COLLEGE_ADMIN only |
| `/dashboard/college-admin/hod-requests` | CollegeAdminHODRequests | COLLEGE_ADMIN only |
| `/dashboard/college-admin/profile` | CollegeAdminProfile | COLLEGE_ADMIN only |
| `/dashboard/hod` | HODDashboard | HOD only |
| `/dashboard/hod/mentors` | HODMentors | HOD only |
| `/dashboard/hod/approvals` | HODMentorApprovals | HOD only |
| `/dashboard/hod/profile` | HODProfile | HOD only |
| `/dashboard/mentor` | MentorDashboard | MENTOR only |
| `/dashboard/mentor/students` | MentorStudents | MENTOR only |
| `/dashboard/mentor/submissions` | MentorSubmissions | MENTOR only |
| `/dashboard/mentor/logs` | MentorLogs | MENTOR only |
| `/dashboard/mentor/student-approvals` | MentorStudentApprovals | MENTOR only |
| `/dashboard/mentor/profile` | MentorProfile | MENTOR only |
| `/dashboard/student` | StudentMetrics | STUDENT only |
| `/dashboard/student/submission` | StudentSubmission | STUDENT only |
| `/dashboard/student/logs` | StudentLogs | STUDENT only |
| `/dashboard/student/reviews` | StudentReviews | STUDENT only |
| `/dashboard/student/profile` | StudentProfile | STUDENT only |
| `*` (catch-all) | Redirect to `/` | — |
