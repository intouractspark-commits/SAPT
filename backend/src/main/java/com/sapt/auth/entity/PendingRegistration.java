package com.sapt.auth.entity;

import com.sapt.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ============================================================
 * PendingRegistration – Temporary registration holding area
 * ============================================================
 * Stores registration data BEFORE email OTP is verified.
 * Once the user verifies their email, data is moved to the
 * `users` table and this record is deleted.
 *
 * This ensures the `users` table ONLY contains email-verified
 * accounts.
 *
 * Table: pending_registrations
 * ============================================================
 */
@Entity
@Table(
    name = "pending_registrations",
    indexes = {
        @Index(name = "idx_pending_email", columnList = "email", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "college_id", columnDefinition = "CHAR(36)")
    private String collegeId;

    @Column(name = "college_name", length = 255)
    private String collegeName;

    @Column(name = "department_id", columnDefinition = "CHAR(36)")
    private String departmentId;

    @Column(name = "department_name", length = 255)
    private String departmentName;

    @Column(name = "mentor_id", columnDefinition = "CHAR(36)")
    private String mentorId;

    @Column(name = "mentor_name", length = 255)
    private String mentorName;

    @Column(name = "id_card_url", columnDefinition = "TEXT")
    private String idCardUrl;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
