# SPAT — Open Questions, Implementation Gaps, and Risks
## Client Clarification Requests, Gap Analysis, and Recommended Actions

---

| Document | SPAT Open Questions and Gaps |
|---|---|
| Version | 1.0 |
| Date | October 2026 |
| Status | Requires Client and Team Response |

---

## Summary

This document consolidates all identified implementation gaps, risks, security concerns, open questions requiring client input, and recommendations for the SPAT platform. All findings are based on direct codebase inspection conducted in October 2026.

---

## Part 1: Critical Issues — Immediate Action Required

### ISSUE-001: SMTP Credentials in `.env.example`
**Severity:** 🔴 Critical  
**File:** `c:\SPAT\backend\.env.example`

**Finding:** The `.env.example` file — which is committed to version control — contains what appear to be real SMTP credentials (a Gmail app password) for `intouract.spark@gmail.com`. The `.env.example` file is explicitly designed to be committed to Git and should contain only placeholder values.

**Risk:** Any developer who clones the repository or any third party who accesses the Git history can obtain these credentials and use them for email sending, account enumeration, or spam abuse.

**Recommended Action (Immediate):**
1. Revoke the exposed Gmail App Password immediately.
2. Generate a new App Password.
3. Update `.env.example` to use only placeholder values:
   ```
   MAIL_USERNAME=your_gmail_address@gmail.com
   MAIL_PASSWORD=your_gmail_app_password_here
   ```
4. Ensure the actual credentials are only in `.env` (which is in `.gitignore`).

---

### ISSUE-002: MongoDB Connection String in `.env.example`
**Severity:** 🔴 Critical  
**File:** `c:\SPAT\backend\.env.example`

**Finding:** The `.env.example` also contains what appears to be a real MongoDB Atlas connection string with embedded credentials.

**Risk:** Unauthorized MongoDB access; potential data exposure or deletion.

**Recommended Action (Immediate):**
1. Rotate MongoDB Atlas credentials.
2. Replace with placeholder in `.env.example`.

---

## Part 2: Implementation Gaps

### GAP-001: Submission Withdrawal Endpoint Missing
**Severity:** 🟡 Medium  
**Gap Location:** `SubmissionService.java` (method `withdrawSubmission` defined) vs. `StudentController.java` (no endpoint for it)

**Detail:** The `SubmissionService` interface declares `withdrawSubmission(String studentId, String submissionId)` but no HTTP endpoint exists to invoke it. Students currently cannot withdraw a pending submission via the API.

**Impact:** Students cannot retract erroneous or accidental submissions without mentor intervention.

**Recommendation:**  
Add to `StudentController`:
```java
@DeleteMapping("/submissions/{submissionId}")
public ResponseEntity<ApiResponse<Void>> withdrawSubmission(
    @PathVariable String submissionId,
    @AuthenticationPrincipal UserDetails userDetails) {
    User student = findStudent(userDetails.getUsername());
    submissionService.withdrawSubmission(student.getId(), submissionId);
    return ResponseEntity.ok(ApiResponse.success("Submission withdrawn successfully"));
}
```
**Client Question:** Should withdrawal be allowed only for PENDING submissions, or also for REJECTED ones?

---

### GAP-002: HOD Succession Review Endpoints Missing
**Severity:** 🔴 High  
**Gap Location:** `HodController.java`

**Detail:** The `SuccessionRequest` entity documents that the HOD is the reviewer for MENTOR succession requests. However, `HodController` has no endpoints to:
1. List pending succession requests in the HOD's department.
2. Approve or reject a succession request.

**Impact:** The mentor succession workflow is incomplete. Mentors can submit requests but no reviewer can action them through the API. The frontend may be calling a non-existent endpoint, causing failures.

**Recommendation:** Add two endpoints to `HodController`:
```java
GET  /api/hod/succession-requests        → list PENDING MENTOR succession requests in department
PUT  /api/hod/succession-requests/{id}/approve
PUT  /api/hod/succession-requests/{id}/reject
```

---

### GAP-003: College Admin HOD Approval Workflow Missing
**Severity:** 🔴 High  
**Gap Location:** `CollegeAdminController.java`, frontend: `CollegeAdminHODRequests.jsx`

**Detail:** The frontend has a dedicated page at `/dashboard/college-admin/hod-requests` for HOD-related approvals, but `CollegeAdminController` exposes no endpoint for:
1. Listing pending HOD registrations or HOD succession requests.
2. Approving or rejecting them.

**Impact:** College Admins cannot approve HOD registrations or HOD succession requests through the UI.

**Open Question for Client:** Is the HOD approval flow: (a) College Admin approves HOD self-registration directly, or (b) HOD succeeds via a succession request that the College Admin reviews? The codebase suggests (b) based on `SuccessionRequest.outgoingRole = HOD`.

**Recommendation:** Add to `CollegeAdminController`:
```java
GET  /api/college-admin/hod-requests          → list pending HOD succession requests
PUT  /api/college-admin/hod-requests/{id}/approve
PUT  /api/college-admin/hod-requests/{id}/reject
```

---

### GAP-004: Mentor Student Approvals Endpoint Missing
**Severity:** 🟡 Medium  
**Gap Location:** `MentorController.java`, frontend: `MentorStudentApprovals.jsx`

**Detail:** The frontend has a page at `/dashboard/mentor/student-approvals` (named `MentorStudentApprovals` in AppRouter), but `MentorController` has no endpoint for listing or approving pending student registrations under the mentor.

**Impact:** Mentors cannot approve new student registrations through the designated page. (Currently, student approval may flow through `/api/auth/users/{userId}/status`, but it's not role-restricted to mentors.)

**Open Question for Client:** Is this page intended for:
  (a) Mentors approving students assigned to them (i.e., approving a PENDING student account), or  
  (b) Mentors accepting/rejecting a student's request to be assigned to them?

**Recommendation:** Clarify the workflow, then add a dedicated endpoint:
```java
GET  /api/mentor/student-approvals        → list PENDING students with mentor_id = this mentor
POST /api/mentor/student-approvals/{id}/approve
POST /api/mentor/student-approvals/{id}/reject
```

---

### GAP-005: Star Threshold Management API Missing
**Severity:** 🟢 Low  
**Gap Location:** `SystemAdminController.java`

**Detail:** The `star_thresholds` table is pre-seeded and the entity documentation explicitly states "System Admins can update min_credits values without code changes." However, no API endpoint exists to list or update thresholds.

**Impact:** Any threshold change currently requires direct database access. This is a security and operational risk in a managed SaaS environment.

**Recommendation:** Add to `SystemAdminController`:
```java
GET  /api/system-admin/star-thresholds
PUT  /api/system-admin/star-thresholds/{id}   {minCredits: 150}
```

---

### GAP-006: Submission Pagination — Workaround in Use
**Severity:** 🟡 Medium  
**Gap Location:** `StudentController.java` line 126

**Detail:**
```java
@RequestParam(defaultValue = "1000") int size
```
The student submission listing uses a default page size of 1000, effectively bypassing pagination. This will cause performance and memory problems as submission volume grows.

**Impact:** Large submission histories will result in slow API responses and high memory consumption.

**Recommendation:** Implement proper pagination on the frontend (page controls) and reduce default size to 20–50. Add filter parameters (status, date range, category) to reduce data transfer.

---

### GAP-007: No Automated Test Coverage
**Severity:** 🔴 High  
**Gap Location:** `backend/src/test/`

**Detail:** The `src/test` directory exists (standard Maven structure) but no test implementation files were found during inspection. This means:
- No unit tests for services or controllers.
- No integration tests for API endpoints.
- No database migration or seed tests.

**Impact:** High regression risk during development. Any future change can silently break existing functionality with no safety net.

**Recommendation (High Priority for Production Launch):**
1. Implement unit tests for all service classes using JUnit 5 + Mockito.
2. Implement integration tests for all controllers using `@SpringBootTest` + `MockMvc`.
3. Add frontend component tests using React Testing Library.
4. Set up a CI pipeline (GitHub Actions recommended) to run tests on every push/PR.

---

### GAP-008: ReviewController Purpose Unclear
**Severity:** 🟡 Medium  
**Gap Location:** `ReviewController.java`, `ReviewService.java`

**Detail:** The `review` package exposes two **publicly accessible** (no authentication required — per `SecurityConfig.java`) endpoints:
- `GET /api/reviews` — list all reviews
- `POST /api/reviews` — create a review

The `Review.java` entity (MongoDB?) appears separate from the submission review workflow. This appears to be a testimonials or platform ratings module.

**Risk:** The public POST endpoint allows unauthenticated users to create review records. If this data is displayed publicly, it could be abused for spam or inappropriate content.

**Open Question for Client:** What is the intended purpose of `/api/reviews`? Options:
  (a) Public testimonials about the SPAT platform (for marketing landing page).
  (b) Student feedback on the platform.
  (c) Something else entirely.

**Recommendation:** If this is a testimonials module, add at minimum a basic CAPTCHA or rate limit. If authentication is intended, move it behind the JWT filter.

---

### GAP-009: SubmissionController Stub
**Severity:** 🟢 Low  
**Gap Location:** `SubmissionController.java`

**Detail:** `SubmissionController` contains a TODO comment ("Add endpoints for CRUD and review workflow") and only implements the file-download endpoint. The actual submission creation and review are correctly handled through `StudentController` and `MentorController` respectively. The standalone `SubmissionController` is architecturally redundant in its current state.

**Recommendation:** Either:
  (a) Remove the SubmissionController's TODO comment and document that it serves only the file-download use case, or  
  (b) Migrate the file-download endpoint into a more appropriate controller (e.g., a general `/api/files` resource controller).

---

## Part 3: Open Questions for Client

| Q ID | Question | Area | Priority | Notes |
|---|---|---|---|---|
| Q-001 | Who approves HOD self-registrations — College Admin directly, or via a succession-style workflow? | Workflow | 🔴 High | Affects GAP-003 resolution |
| Q-002 | Is the `MentorStudentApprovals` page for approving student accounts, or for assigning/accepting student-mentor pairing requests? | Workflow | 🔴 High | Affects GAP-004 resolution |
| Q-003 | Should JWT tokens be moved from `localStorage` to `httpOnly` cookies? This would require backend changes to set/clear cookies. | Security | 🔴 High | Mitigates XSS risk |
| Q-004 | What is the intended purpose of `GET /api/reviews` and `POST /api/reviews`? Are these platform testimonials? | Feature | 🟡 Medium | Affects public access decision |
| Q-005 | What is the data retention policy for submissions, daily logs, and notifications? (e.g., keep for 5 years, archive after graduation) | Compliance | 🔴 High | Required for regulatory compliance |
| Q-006 | Should submission withdrawal be available to students? Under what conditions? (PENDING only, or also REJECTED?) | Feature | 🟡 Medium | Affects GAP-001 resolution |
| Q-007 | Are there performance SLAs (response time, uptime guarantees) in the client contract? | Operations | 🔴 High | Needed for infrastructure planning |
| Q-008 | What are the backup and disaster recovery requirements for the Supabase PostgreSQL and MongoDB Atlas databases? | Operations | 🔴 High | Must be documented in SLA |
| Q-009 | Should star thresholds (Bronze, Silver, etc.) be configurable per-college or remain system-wide? | Feature | 🟢 Low | Affects GAP-005 resolution |
| Q-010 | Is NAAC accreditation report generation or AICTE report generation required? | Feature | 🟡 Medium | Major feature if yes — significant dev effort |
| Q-011 | Are departments pre-seeded during college onboarding, or is there a post-onboarding department management feature needed? | Feature | 🔴 High | No department create/edit endpoint found |
| Q-012 | Should the public file download endpoint (`/api/submission/files/download`) require authentication? | Security | 🟡 Medium | Currently unauthenticated by SecurityConfig |
| Q-013 | Are there plans for a mobile application (iOS/Android) within the next 12 months? | Roadmap | 🟡 Medium | Affects API design decisions (versioning, etc.) |
| Q-014 | Will the platform need to support multiple languages (Tamil, Hindi, etc.)? | Feature | 🟡 Medium | Affects UI and backend string handling |

---

## Part 4: Security Findings

| Security ID | Finding | Severity | Recommendation |
|---|---|---|---|
| SEC-001 | SMTP credentials in `.env.example` | 🔴 Critical | Rotate immediately; use placeholders only |
| SEC-002 | MongoDB URI with credentials in `.env.example` | 🔴 Critical | Rotate immediately; use placeholders only |
| SEC-003 | JWT stored in `localStorage` (XSS-vulnerable) | 🟡 Medium | Consider httpOnly cookies for enhanced security |
| SEC-004 | Public file download endpoint (no auth) | 🟡 Medium | Confirm if intentional; add auth if files contain PII |
| SEC-005 | Public reviews endpoint allows unauthenticated POST | 🟡 Medium | Add rate limiting or move behind auth |
| SEC-006 | No Content Security Policy (CSP) headers confirmed | 🟢 Low | Add CSP headers to mitigate XSS risk |
| SEC-007 | No automated dependency vulnerability scanning | 🟢 Low | Add `mvn dependency:check` or Snyk to CI pipeline |

---

## Part 5: Recommended Priority Actions

### 🔴 Immediate (Before Production Launch)

1. **Rotate SMTP and MongoDB credentials** exposed in `.env.example` (ISSUE-001, ISSUE-002).
2. **Implement HOD succession review endpoints** (GAP-002).
3. **Implement College Admin HOD approval endpoints** (GAP-003).
4. **Begin automated test implementation** — at minimum, critical path tests for auth, submission creation, and review workflow (GAP-007).
5. **Clarify and document the HOD registration workflow** with the client (Q-001).

### 🟡 Short-Term (Within 4–6 Weeks)

6. **Add submission withdrawal endpoint** (GAP-001).
7. **Implement Mentor Student Approvals** once workflow is clarified (GAP-004).
8. **Fix submission pagination** to use proper page sizes (GAP-006).
9. **Add Star Threshold Management API** for System Admin (GAP-005).
10. **Clarify and secure the `/api/reviews` module** (GAP-008).
11. **Add department creation/management endpoints** for College Admin (Q-011).

### 🟢 Medium-Term (1–3 Months)

12. **Evaluate JWT storage security** — consider httpOnly cookies (SEC-003).
13. **Set up CI/CD pipeline** (GitHub Actions) with test automation.
14. **Implement PDF/Excel report export** for regulatory compliance (F-127).
15. **Add proper API versioning** (`/api/v1/`) in preparation for mobile clients.
16. **Add CSP and other security headers** to the frontend deployment.

---

## Part 6: Technical Debt Register

| TD ID | Item | Impact | Effort |
|---|---|---|---|
| TD-001 | SubmissionController stub with TODO | Low | Low |
| TD-002 | `size=1000` pagination workaround | Medium (performance) | Low |
| TD-003 | `@AuthenticationPrincipal` returns email; requires extra DB lookup in every request | Medium (performance) | Medium |
| TD-004 | Denormalized college/department/mentor names on User entity — must be kept in sync manually | Medium (data consistency) | Medium |
| TD-005 | No API versioning (`/api` vs. `/api/v1`) | Low (now), High (later) | Medium |
| TD-006 | No automated tests | High (regression risk) | High |
| TD-007 | Frontend README is the default Vite template, not project documentation | Low | Low |
