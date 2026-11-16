# SPAT — Feature Catalogue
## Detailed Feature Inventory with Implementation Status

---

| Document | SPAT Feature Catalogue |
|---|---|
| Version | 1.0 |
| Date | October 2026 |
| Status | Draft |

---

## Legend

| Status Label | Meaning |
|---|---|
| ✅ Implemented | Verified working code with controller, service, and entity in codebase |
| 🔶 Partially Implemented | Code structure exists but missing key endpoints or logic |
| 📋 Planned | Documented intent (e.g., TODO comment, service interface method) but no controller endpoint |
| ❌ Gap | Expected by frontend/UX but no backend implementation found |

---

## Module 1: Authentication & User Management

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-001 | Email + Password Login | User authenticates with email, password, and role selection; receives JWT token | All | ✅ Implemented |
| F-002 | Multi-Role Login Routing | Two separate login pages: `/login` (HOD, Mentor, Student) and `/admin-login` (System Admin, College Admin) | All | ✅ Implemented |
| F-003 | User Self-Registration | Student, Mentor, or HOD registers with: name, email, password, role, college, department, mentor (for students) | STUDENT, MENTOR, HOD | ✅ Implemented |
| F-004 | OTP Email Verification | OTP sent to registered email; user must verify before account activates | All | ✅ Implemented |
| F-005 | Resend OTP | User may request a new OTP if the previous expired | All | ✅ Implemented |
| F-006 | OTP-Based Password Reset | User receives OTP → verifies → sets new password | All | ✅ Implemented |
| F-007 | JWT Token Issuance | JWT token issued on login; includes role claim; 24-hour expiry by default | All | ✅ Implemented |
| F-008 | Token Auto-Refresh on Profile Update | New JWT issued when profile is updated to reflect changed claims | All | ✅ Implemented |
| F-009 | Session Persistence | User session stored in `localStorage`; restored on page reload if token not expired | All | ✅ Implemented |
| F-010 | Auto-Logout on 401/403 | Global event listener detects session expiry; auto-redirects to appropriate login page | All | ✅ Implemented |
| F-011 | Account Status Control | Users are PENDING by default; supervisors approve them to APPROVED; REJECTED blocks login | All | ✅ Implemented |
| F-012 | Soft-Delete (is_active flag) | User accounts can be deactivated without data deletion | All | ✅ Implemented |
| F-013 | College Listing for Registration | Public endpoint returns list of registered colleges for registration dropdown | Public | ✅ Implemented |
| F-014 | Department Listing for Registration | Public endpoint returns departments for a given college name | Public | ✅ Implemented |
| F-015 | Mentor Listing for Registration | Public endpoint returns available mentors in a college+department for student assignment | Public | ✅ Implemented |
| F-016 | User Status Management | Admin can update any user's status (PENDING/APPROVED/REJECTED) | SYSTEM_ADMIN, COLLEGE_ADMIN | ✅ Implemented |
| F-017 | Profile Update (All Roles) | User can update: name, phone, position, avatar URL, ID card URL | All | ✅ Implemented |

---

## Module 2: System Administration

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-020 | System Admin Dashboard | KPI stats: total colleges, total users, total submissions, system-wide analytics | SYSTEM_ADMIN | ✅ Implemented |
| F-021 | College Onboarding | Create a new college with: name, state, address, official email, chairman name, contract start/end | SYSTEM_ADMIN | ✅ Implemented |
| F-022 | College Admin Account Creation | Create College Admin account simultaneously with college onboarding | SYSTEM_ADMIN | ✅ Implemented |
| F-023 | College Directory | Paginated list of all registered colleges with admin details and status | SYSTEM_ADMIN | ✅ Implemented |
| F-024 | College Detail View | View all details of a specific college including contract and user counts | SYSTEM_ADMIN | ✅ Implemented |
| F-025 | College Status Management | Set college status: ACTIVE, INACTIVE, SUSPENDED | SYSTEM_ADMIN | ✅ Implemented |
| F-026 | College Admin Directory | List all College Admin accounts across all colleges | SYSTEM_ADMIN | ✅ Implemented |
| F-027 | System Admin Profile | View and update System Admin's own profile | SYSTEM_ADMIN | ✅ Implemented |
| F-028 | Star Threshold Management | Edit credit thresholds for star/badge levels | SYSTEM_ADMIN | ❌ Gap — no API endpoint found |

---

## Module 3: College Administration

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-030 | College Admin Dashboard | Analytics: user/dept counts, submission breakdown (approved/pending/rejected), top students, top mentors, dept charts, monthly trends, placement readiness | COLLEGE_ADMIN | ✅ Implemented |
| F-031 | Department Overview | All departments with: HOD info, mentor count, student count, activity statistics | COLLEGE_ADMIN | ✅ Implemented |
| F-032 | User Directory by Role | Filter users in college by role: HOD, MENTOR, STUDENT | COLLEGE_ADMIN | ✅ Implemented |
| F-033 | College Profile Update | Update college's registered address and official email | COLLEGE_ADMIN | ✅ Implemented |
| F-034 | College Admin Profile | View and update own profile | COLLEGE_ADMIN | ✅ Implemented |
| F-035 | HOD Request / Approval Workflow | Frontend page (`hod-requests`) for approving HOD succession/registration requests | COLLEGE_ADMIN | ❌ Gap — no backend endpoint in CollegeAdminController |

---

## Module 4: HOD Dashboard

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-040 | HOD Department Dashboard | Analytics: mentor/student totals, placement factor tallies, factor bar chart, monthly skill trend area chart, top 3 students, top 3 mentors | HOD | ✅ Implemented |
| F-041 | Approved Mentor List | View all APPROVED mentors in HOD's department | HOD | ✅ Implemented |
| F-042 | Pending Mentor Approvals | View and action PENDING mentor registrations in HOD's department | HOD | 🔶 Partially Implemented — GET endpoint exists; status-update endpoint is in `/api/auth/users/{userId}/status` |
| F-043 | HOD Profile | View and update own profile | HOD | ✅ Implemented |
| F-044 | Succession Request Review | Review and approve/reject MENTOR succession requests | HOD | ❌ Gap — no endpoint in HodController |

---

## Module 5: Mentor Management

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-050 | Mentor Dashboard | Stats: total students, total credits awarded, approve/reject/pending counts, success rate %, top students by credits, factor counts (by category), monthly skill trend, recent pending submissions | MENTOR | ✅ Implemented |
| F-051 | Assigned Students List | View all students assigned to this mentor with credit summaries | MENTOR | ✅ Implemented |
| F-052 | Student Submission View | View all submissions for a specific assigned student | MENTOR | ✅ Implemented |
| F-053 | Pending Submission Queue | Chronological list of all PENDING submissions across assigned students | MENTOR | ✅ Implemented |
| F-054 | Approve Submission | Mark submission APPROVED; set awarded credits (defaults to suggested credits) | MENTOR | ✅ Implemented |
| F-055 | Reject Submission with Penalty | Mark submission REJECTED; apply 10% credit penalty; store review comment | MENTOR | ✅ Implemented |
| F-056 | Daily Log View | View all daily log entries from assigned students | MENTOR | ✅ Implemented |
| F-057 | Daily Log Remark | Add a remark and review status (approved/pending) on any student daily log | MENTOR | ✅ Implemented |
| F-058 | Mentor Succession Request | Submit succession request: provide incoming candidate's name, email, phone, password | MENTOR | ✅ Implemented |
| F-059 | Cancel Succession Request | Cancel a pending succession request | MENTOR | ✅ Implemented |
| F-060 | Mentor Profile | View and update own profile | MENTOR | ✅ Implemented |
| F-061 | Student Approvals | Approve/reject pending student registrations under mentor | MENTOR | ❌ Gap — frontend route exists; no backend endpoint in MentorController |

---

## Module 6: Student Activity Tracking

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-070 | Student Performance Dashboard | Total credits, star level (0–5), badge name, submission status breakdown (pie/bar charts) | STUDENT | ✅ Implemented |
| F-071 | Activity Category Catalogue | Browse available activity categories and sub-types with default point values | STUDENT | ✅ Implemented |
| F-072 | Activity Submission Form | Create submission: select category, sub-type, enter title, description, activity date, upload file attachments | STUDENT | ✅ Implemented |
| F-073 | Custom Category Creation | Create a custom category/sub-type if none of the predefined categories fit | STUDENT | ✅ Implemented |
| F-074 | Submission History | View all past submissions with status, awarded/suggested credits, review feedback | STUDENT | ✅ Implemented |
| F-075 | Submission Resubmission | Resubmit after rejection; linked to original via `parent_submission_id` | STUDENT | ✅ Implemented (service + entity; frontend-driven) |
| F-076 | Submission Withdrawal | Withdraw a pending submission | STUDENT | 📋 Planned — service interface only; no controller endpoint |
| F-077 | Daily Log Entry | Create log: title, description, optional reference links, log date | STUDENT | ✅ Implemented |
| F-078 | Daily Log Update | Update an existing log entry | STUDENT | ✅ Implemented |
| F-079 | Daily Log History | View all log entries with mentor remarks and review status | STUDENT | ✅ Implemented |
| F-080 | Submission Reviews View | View feedback/reviews from mentor on approved/rejected submissions | STUDENT | ✅ Implemented (via submission history) |
| F-081 | Student Profile | View and update own profile | STUDENT | ✅ Implemented |

---

## Module 7: Credit & Gamification Engine

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-090 | Credit Snapshot | Real-time snapshot of a student's total credits, approved/rejected/pending counts | System | ✅ Implemented |
| F-091 | Suggested Credits from Sub-Type | Default credit value pulled from `ActivitySubType.points` and displayed to student | STUDENT | ✅ Implemented |
| F-092 | Mentor Credit Override | Mentor can set final awarded credits different from the suggested value | MENTOR | ✅ Implemented |
| F-093 | Credit Penalty on Rejection | Rejection deducts ceil(suggestedCredits × 10%) from student's total | System | ✅ Implemented |
| F-094 | Star Level Computation | Total credits mapped to star level (0–5) and badge name via `star_thresholds` table | System | ✅ Implemented |
| F-095 | Badge Display | Badge names: Beginner, Bronze, Silver, Gold, Platinum, Diamond | STUDENT | ✅ Implemented |
| F-096 | Leaderboard / Top Students | Top students ranked by total credits; visible to Mentor, HOD, College Admin | MENTOR, HOD, COLLEGE_ADMIN | ✅ Implemented (within dashboards) |

---

## Module 8: Notification System

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-100 | In-App Notification Inbox | View all notifications ordered newest-first | All | ✅ Implemented |
| F-101 | Unread Notification Count | Bell icon badge showing count of unread notifications | All | ✅ Implemented |
| F-102 | Mark Notification as Read | Mark a single notification read with timestamp | All | ✅ Implemented |
| F-103 | Mark All Read | Batch mark all notifications as read | All | ✅ Implemented |
| F-104 | Email OTP Notification | HTML-formatted OTP email sent via Gmail SMTP | All | ✅ Implemented |
| F-105 | Async Email Delivery | Email notifications sent asynchronously (non-blocking) | System | ✅ Implemented (`@Async`) |
| F-106 | Event-Driven Notifications | Notifications triggered by: submission created, approved, rejected, succession events | System | ✅ Implemented |

---

## Module 9: Succession Management

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-110 | Mentor Succession Request | Mentor submits handover request for incoming faculty | MENTOR | ✅ Implemented |
| F-111 | Cancel Succession Request | Mentor cancels a PENDING succession request | MENTOR | ✅ Implemented |
| F-112 | HOD Review of Mentor Succession | HOD lists and approves/rejects pending mentor succession requests | HOD | ❌ Gap — no endpoint in HodController |
| F-113 | College Admin Review of HOD Succession | College Admin reviews HOD-level succession requests | COLLEGE_ADMIN | ❌ Gap — no endpoint in CollegeAdminController |
| F-114 | Account Credential Transfer on Approval | On approval, outgoing user's credentials are updated to incoming candidate's details | System | ✅ Implemented (per entity design; service layer) |

---

## Module 10: Analytics & Reporting

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-120 | System-Wide KPI Stats | Total colleges, users, submissions aggregated across all colleges | SYSTEM_ADMIN | ✅ Implemented |
| F-121 | College Analytics Dashboard | Submission breakdown, user counts, top students/mentors, dept charts, monthly trends, placement readiness | COLLEGE_ADMIN | ✅ Implemented |
| F-122 | Department Analytics Dashboard | Mentor/student counts, factor tallies, skill trend, top performers | HOD | ✅ Implemented |
| F-123 | Mentor Performance Dashboard | Student totals, credit distribution, success rate, top students, monthly skill trend | MENTOR | ✅ Implemented |
| F-124 | Student Performance Dashboard | Own credits, star level, submission breakdown, daily log overview | STUDENT | ✅ Implemented |
| F-125 | Placement Readiness Factors | Activity factor breakdown (hackathons, internships, certifications, etc.) across college/dept | HOD, COLLEGE_ADMIN | ✅ Implemented |
| F-126 | Monthly Skill Trend | Area/line chart showing month-by-month approved activity count | HOD, MENTOR, COLLEGE_ADMIN | ✅ Implemented |
| F-127 | PDF/Excel Report Export | Exportable activity point reports for regulatory submission | All Admin | ❌ Not implemented — Proposed future |

---

## Module 11: File Management

| Feature ID | Feature Name | Description | Target Role | Status |
|---|---|---|---|---|
| F-130 | Submission File Upload | Students attach evidence files (PDF, images, etc.) to submissions; stored in MongoDB | STUDENT | ✅ Implemented |
| F-131 | Submission File Download | Public endpoint to download/view submission files by path | All | ✅ Implemented |
| F-132 | Avatar / Profile Photo Upload | Users upload profile avatars; stored as URL (storage provider TBC) | All | 🔶 Partially Implemented — field exists; upload mechanism TBC |

---

## Feature Status Summary

| Status | Count | Percentage |
|---|---|---|
| ✅ Implemented | 87 | ~79% |
| 🔶 Partially Implemented | 4 | ~4% |
| 📋 Planned | 2 | ~2% |
| ❌ Gap | 8 | ~7% |
| ❌ Not Implemented (Proposed) | 8 | ~7% |

**Overall implementation completeness: approximately 79–83% for core defined features.**

---

## Feature-to-API Mapping

| Feature ID | Primary API Endpoint(s) |
|---|---|
| F-001 | POST /api/auth/login |
| F-003 | POST /api/auth/register |
| F-004 | POST /api/auth/otp/verify |
| F-005 | POST /api/auth/otp/send |
| F-006 | POST /api/auth/password/reset |
| F-020 | GET /api/system-admin/stats |
| F-021 | POST /api/system-admin/college-admin |
| F-023 | GET /api/system-admin/colleges |
| F-025 | PUT /api/system-admin/colleges/{id}/status |
| F-030 | GET /api/college-admin/dashboard |
| F-031 | GET /api/college-admin/departments |
| F-040 | GET /api/hod/dashboard |
| F-042 | GET /api/hod/mentor-approvals |
| F-050 | GET /api/mentor/dashboard |
| F-051 | GET /api/mentor/students |
| F-053 | GET /api/mentor/submissions/pending |
| F-054 | PUT /api/mentor/submissions/{id}/approve |
| F-055 | PUT /api/mentor/submissions/{id}/reject |
| F-057 | PUT /api/mentor/logs/{id}/remark |
| F-058 | POST /api/mentor/succession |
| F-070 | GET /api/student/dashboard/stats |
| F-071 | GET /api/student/categories |
| F-072 | POST /api/student/submissions |
| F-073 | POST /api/student/categories/custom |
| F-074 | GET /api/student/submissions |
| F-077 | POST /api/student/logs |
| F-100 | GET /api/notifications |
| F-101 | GET /api/notifications/unread-count |
| F-102 | PUT /api/notifications/{id}/read |
| F-131 | GET /api/submission/files/download |
