package com.sapt.auth.service;

import com.sapt.auth.dto.AuthDtos;
import com.sapt.auth.dto.LoginRequest;
import com.sapt.auth.dto.LoginResponse;
import com.sapt.auth.dto.RegisterRequest;
import com.sapt.auth.entity.PendingRegistration;
import com.sapt.auth.entity.User;
import com.sapt.auth.repository.PendingRegistrationRepository;
import com.sapt.auth.repository.UserRepository;
import com.sapt.common.enums.UserStatus;
import com.sapt.common.exception.SaptException;
import com.sapt.common.utils.CommonUtils;
import com.sapt.notification.NotificationService;
import com.sapt.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * AuthServiceImpl â€” Authentication Service Implementation
 * ============================================================
 * Implements all authentication operations against the
 * unified `users` table (no separate auth_users or otp_tokens).
 *
 * OTP Flow:
 *   OTP is stored directly on the User entity (otp_code, otp_expires_at).
 *   After successful verification the OTP fields are nulled out.
 *   A null otp_code signals "email verified".
 *
 * Login gate (all three must pass):
 *   1. Password correct        â†’ Spring Security authentication
 *   2. otpCode == null         â†’ email was verified
 *   3. status == APPROVED      â†’ approved by supervisor / admin
 *
 * OTP validity : 10 minutes
 * OTP length   : 6 digits
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int    OTP_LENGTH         = 6;
    private static final int    OTP_EXPIRY_MINUTES = 10;
    private static final String PURPOSE_EMAIL      = "EMAIL_VERIFICATION";
    private static final String PURPOSE_RESET      = "PASSWORD_RESET";

    private final UserRepository                userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final JwtUtil                       jwtUtil;
    private final PasswordEncoder               passwordEncoder;
    private final AuthenticationManager         authenticationManager;
    private final NotificationService           notificationService;
    private final com.sapt.collegeadmin.repository.CollegeRepository collegeRepository;
    private final com.sapt.collegeadmin.repository.DepartmentRepository departmentRepository;
    private final com.sapt.config.DataSyncService dataSyncService;

    // ============================================================
    // LOGIN
    // ============================================================

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Login attempt for: {}", email);

        // 1. Verify credentials via Spring Security
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for: {}", email);
            throw SaptException.unauthorized("Invalid email or password");
        }

        // 2. Load user entity from DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> SaptException.notFound("User not found"));

        // 3. Role must match what the login page selected
        if (!user.getRole().equals(request.getRole())) {
            log.warn("Role mismatch for {} â€” requested: {}, actual: {}",
                    email, request.getRole(), user.getRole());
            throw SaptException.unauthorized("Your role does not match the selected login portal");
        }

        // 4. Email must be verified (otp_code is nulled after successful OTP verification)
        if (user.getOtpCode() != null) {
            log.warn("Unverified email login attempt: {}", email);
            throw SaptException.badRequest("Please verify your email before logging in");
        }

        // 5. Account must be approved (by mentor / admin / system)
        if (user.getStatus() != UserStatus.APPROVED) {
            log.warn("Non-approved login attempt for: {} (status={})", email, user.getStatus());
            throw SaptException.badRequest(
                    user.getStatus() == UserStatus.PENDING
                            ? "Your account is pending approval. Please wait."
                            : "Your account registration was rejected."
            );
        }

        // 6. Generate JWT from Spring Security principal
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        // 7. Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login successful: {} (role={})", email, user.getRole());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs())
                // â”€â”€â”€ Core Identity â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                // â”€â”€â”€ Profile â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                .college(user.getCollegeName())       // user.college in frontend
                .department(user.getDepartmentName()) // user.department in frontend
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                // â”€â”€â”€ UUID References â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                .collegeId(user.getCollegeId())
                .departmentId(user.getDepartmentId())
                // â”€â”€â”€ Role-Specific (null when not applicable) â”€
                .mentorId(user.getMentorId())
                .mentorName(user.getMentorName())
                .hodId(user.getHodId())
                .rollNo(user.getRollNo())
                .createdAt(user.getCreatedAt())
                .build();

    }

    // ============================================================
    // REGISTER
    // ============================================================

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Registration attempt: {} (role={})", email, request.getRole());

        // 1. Block if a fully-verified account already exists in users table
        userRepository.findByEmail(email).ifPresent(existing -> {
            log.warn("Registration failed - email already exists and is verified: {}", email);
            throw SaptException.conflict("An account with this email already exists");
        });

        // 2. Look up and populate database reference fields
        String collegeId = null;
        String collegeName = null;
        if (request.getCollege() != null && !request.getCollege().isBlank()) {
            com.sapt.collegeadmin.entity.College college = collegeRepository.findByName(request.getCollege().trim())
                    .orElseThrow(() -> SaptException.notFound("College not found in database: " + request.getCollege()));
            collegeId = college.getId();
            collegeName = college.getName();
        }

        String departmentId = null;
        String departmentName = null;
        if (collegeId != null && request.getDepartment() != null && !request.getDepartment().isBlank()) {
            List<com.sapt.collegeadmin.entity.Department> depts = departmentRepository.findByCollegeId(collegeId);
            com.sapt.collegeadmin.entity.Department dept = depts.stream()
                    .filter(d -> d.getName().equalsIgnoreCase(request.getDepartment().trim()))
                    .findFirst()
                    .orElse(null);

            // Auto-create department if "Other" custom option was used
            if (dept == null) {
                dept = com.sapt.collegeadmin.entity.Department.builder()
                        .collegeId(collegeId)
                        .name(request.getDepartment().trim())
                        .isActive(true)
                        .build();
                dept = departmentRepository.save(dept);
            }
            departmentId = dept.getId();
            departmentName = dept.getName();
        }

        String mentorId = null;
        String mentorName = null;
        if (com.sapt.common.enums.UserRole.STUDENT.equals(request.getRole())) {
            if (request.getMentorName() == null || request.getMentorName().isBlank()) {
                throw SaptException.badRequest("Please select a registered mentor to complete registration");
            }
            List<User> mentors = userRepository.findByRole(com.sapt.common.enums.UserRole.MENTOR);
            final String targetCollegeId = collegeId;
            User mentor = mentors.stream()
                    .filter(m -> m.getName().equalsIgnoreCase(request.getMentorName().trim())
                            && m.getCollegeId() != null
                            && m.getCollegeId().equals(targetCollegeId))
                    .findFirst()
                    .orElse(null);
            if (mentor != null) {
                mentorId = mentor.getId();
                mentorName = mentor.getName();
            } else {
                throw SaptException.badRequest("Selected mentor was not found or is not registered with your college");
            }
        }

        // 3. Generate OTP
        String otp = CommonUtils.generateOtp(OTP_LENGTH);

        // 4. Store only a small flag for the ID card (base64 can be 5-10 MB)
        String idCardFlag = (request.getIdCardUrl() != null && !request.getIdCardUrl().isBlank())
                ? "uploaded"
                : null;

        // 5. Save to pending_registrations (NOT to users table yet).
        //    If a pending record already exists for this email (re-registration without
        //    verifying the first OTP), update it in place so the unique constraint holds.
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElse(PendingRegistration.builder().email(email).build());

        pending.setName(request.getFullName());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setRole(request.getRole());
        pending.setCollegeId(collegeId);
        pending.setCollegeName(collegeName);
        pending.setDepartmentId(departmentId);
        pending.setDepartmentName(departmentName);
        pending.setMentorId(mentorId);
        pending.setMentorName(mentorName);
        pending.setIdCardUrl(idCardFlag);
        pending.setOtpCode(otp);
        pending.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));

        pendingRegistrationRepository.save(pending);
        log.info("Pending registration saved for: {} (role={}). Awaiting email verification.", email, request.getRole());

        // 6. Send OTP email asynchronously
        notificationService.sendEmailVerificationOtp(email, request.getFullName(), otp);
    }

    // ============================================================
    // SEND / RESEND OTP
    // ============================================================

    @Override
    @Transactional
    public void sendOtp(AuthDtos.OtpRequest request) {
        String email   = request.getEmail().trim().toLowerCase();
        String purpose = request.getPurpose().trim().toUpperCase();
        log.info("OTP request for: {}, purpose: {}", email, purpose);

        if (!PURPOSE_EMAIL.equals(purpose) && !PURPOSE_RESET.equals(purpose)) {
            throw SaptException.badRequest("Invalid OTP purpose. Use EMAIL_VERIFICATION or PASSWORD_RESET");
        }

        if (PURPOSE_EMAIL.equals(purpose)) {
            // For email verification resend: look in pending_registrations first
            Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByEmail(email);
            if (pendingOpt.isPresent()) {
                PendingRegistration pending = pendingOpt.get();
                String otp = CommonUtils.generateOtp(OTP_LENGTH);
                pending.setOtpCode(otp);
                pending.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
                pendingRegistrationRepository.save(pending);
                notificationService.sendEmailVerificationOtp(email, pending.getName(), otp);
                log.info("Resent email verification OTP to pending registration: {}", email);
                return;
            }
            // Fall through: no pending record means account is already verified
            throw SaptException.notFound("No pending registration found for this email. Please register first.");
        }

        // For password reset: user must exist in users table
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> SaptException.notFound("No account found with this email"));

        String otp = refreshOtp(user);
        notificationService.sendPasswordResetOtp(email, otp);
        log.info("OTP sent to {} for purpose: {}", email, purpose);
    }

    // ============================================================
    // VERIFY OTP
    // ============================================================

    @Override
    @Transactional
    public void verifyOtp(AuthDtos.OtpVerifyRequest request) {
        String email   = request.getEmail().trim().toLowerCase();
        String otp     = request.getOtp().trim();
        String purpose = request.getPurpose() != null ? request.getPurpose().trim().toUpperCase() : PURPOSE_EMAIL;
        log.info("OTP verification for: {}, purpose: {}", email, purpose);

        if (PURPOSE_EMAIL.equals(purpose)) {
            // Look in pending_registrations table
            PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                    .orElseThrow(() -> SaptException.notFound("No pending registration found. Please register first."));

            // Validate OTP
            if (pending.getOtpCode() == null) {
                throw SaptException.badRequest("No OTP found. Please request a new one.");
            }
            if (!pending.getOtpCode().equals(otp)) {
                log.warn("Wrong OTP entered for pending registration: {}", email);
                throw SaptException.badRequest("Invalid OTP. Please check and try again.");
            }
            if (LocalDateTime.now().isAfter(pending.getOtpExpiresAt())) {
                log.warn("Expired OTP used for pending registration: {}", email);
                throw SaptException.badRequest("OTP has expired. Please request a new one.");
            }

            // OTP is valid - now move from pending_registrations to users table
            User newUser = User.builder()
                    .name(pending.getName())
                    .email(pending.getEmail())
                    .passwordHash(pending.getPasswordHash())
                    .role(pending.getRole())
                    .status(UserStatus.PENDING)
                    .isActive(true)
                    .otpCode(null)          // null = email verified
                    .otpExpiresAt(null)
                    .collegeId(pending.getCollegeId())
                    .collegeName(pending.getCollegeName())
                    .departmentId(pending.getDepartmentId())
                    .departmentName(pending.getDepartmentName())
                    .mentorId(pending.getMentorId())
                    .mentorName(pending.getMentorName())
                    .idCardUrl(pending.getIdCardUrl())
                    .build();

            userRepository.save(newUser);
            pendingRegistrationRepository.deleteByEmail(email);
            log.info("Email verified - user moved from pending to users table: {} (role={})", email, pending.getRole());

        } else {
            // PASSWORD_RESET: user is in users table
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> SaptException.notFound("User not found"));
            validateOtp(user, otp);
            user.setOtpCode(null);
            user.setOtpExpiresAt(null);
            userRepository.save(user);
            log.info("OTP verified for password reset: {}", email);
        }
    }

    // RESET PASSWORD
    // ============================================================

    @Override
    @Transactional
    public void resetPassword(AuthDtos.PasswordResetRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp   = request.getOtp().trim();
        log.info("Password reset attempt for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> SaptException.notFound("User not found"));

        // Validate OTP
        validateOtp(user, otp);

        // Update password and clear OTP
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        log.info("Password reset successfully for: {}", email);
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    @Override
    public void logout(String token) {
        // JWT is stateless â€” client must delete the token.
        // For token blacklisting (e.g., Redis), add the token here.
        // Session rows in the `sessions` table can be revoked here if needed.
        log.info("Logout called â€” client must discard the JWT.");
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    /**
     * Generates a new OTP, saves it on the user entity, and returns the OTP value.
     * Replaces any previous unexpired OTP.
     */
    private String refreshOtp(User user) {
        String otp = CommonUtils.generateOtp(OTP_LENGTH);
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepository.save(user);
        log.debug("OTP refreshed for: {}", user.getEmail());
        return otp;
    }

    /**
     * Validates the OTP stored on the user entity.
     * Throws a SaptException if the OTP is null, wrong, or expired.
     */
    private void validateOtp(User user, String submittedOtp) {
        if (user.getOtpCode() == null) {
            throw SaptException.badRequest("No OTP found. Please request a new one.");
        }
        if (!user.getOtpCode().equals(submittedOtp)) {
            log.warn("Wrong OTP entered for: {}", user.getEmail());
            throw SaptException.badRequest("Invalid OTP. Please check and try again.");
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiresAt())) {
            log.warn("Expired OTP used for: {}", user.getEmail());
            throw SaptException.badRequest("OTP has expired. Please request a new one.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<AuthDtos.CollegeDto> getAllColleges() {
        return collegeRepository.findByStatus(com.sapt.common.enums.CollegeStatus.ACTIVE).stream()
                .map(c -> new AuthDtos.CollegeDto(c.getId(), c.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> getDepartmentsByCollegeName(String collegeName) {
        java.util.Optional<com.sapt.collegeadmin.entity.College> collegeOpt = collegeRepository.findByName(collegeName.trim());
        if (collegeOpt.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return departmentRepository.findByCollegeIdAndIsActive(collegeOpt.get().getId(), true).stream()
                .map(com.sapt.collegeadmin.entity.Department::getName)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<AuthDtos.MentorDto> getMentors(String collegeName, String departmentName) {
        java.util.Optional<com.sapt.collegeadmin.entity.College> collegeOpt = collegeRepository.findByName(collegeName.trim());
        if (collegeOpt.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String collegeId = collegeOpt.get().getId();
        List<User> mentors = userRepository.findByRole(com.sapt.common.enums.UserRole.MENTOR);
        return mentors.stream()
                .filter(m -> m.getCollegeId() != null && m.getCollegeId().equals(collegeId)
                        && m.getDepartmentName() != null && m.getDepartmentName().equalsIgnoreCase(departmentName.trim())
                        && m.getStatus() == com.sapt.common.enums.UserStatus.APPROVED)
                .map(m -> new AuthDtos.MentorDto(m.getId(), m.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.sapt.auth.entity.User> getUsersByCollegeAndRole(String collegeName, com.sapt.common.enums.UserRole role) {
        return userRepository.findByCollegeNameAndRole(collegeName.trim(), role).stream()
                .filter(u -> u.getOtpCode() == null) // Only show email-verified accounts
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.sapt.auth.entity.User> getUsersByMentorId(String mentorId) {
        return userRepository.findByMentorId(mentorId).stream()
                .filter(u -> u.getOtpCode() == null) // Only show email-verified accounts
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public void updateUserStatus(String userId, com.sapt.common.enums.UserStatus status) {
        com.sapt.auth.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> SaptException.notFound("User not found"));
        user.setStatus(status);
        userRepository.save(user);
        log.info("User status updated for user {}: {}", userId, status);

        // Keep role-specific mirror tables in sync
        dataSyncService.syncUserIfApproved(user);
    }

    @Override
    @Transactional
    public com.sapt.auth.entity.User updateProfile(String userId, com.sapt.auth.dto.ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> SaptException.notFound("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null) user.setEmail(request.getEmail().trim().toLowerCase());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        
        // Handle avatar / profileImage (save to avatarUrl)
        if (request.getAvatar() != null) {
            user.setAvatarUrl(request.getAvatar());
        } else if (request.getProfileImage() != null) {
            user.setAvatarUrl(request.getProfileImage());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw SaptException.badRequest("Current password is required to change password.");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw SaptException.badRequest("Current password is incorrect.");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getMentorId() != null) user.setMentorId(request.getMentorId());
        if (request.getMentorName() != null) user.setMentorName(request.getMentorName());
        if (request.getRollNo() != null) user.setRollNo(request.getRollNo().trim());

        log.info("User profile updated for user {}: {}", userId, user.getEmail());
        return userRepository.save(user);
    }
}
