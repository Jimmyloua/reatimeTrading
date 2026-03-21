# Phase 1: Foundation and User Management - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-21
**Phase:** 01-foundation-and-user-management
**Areas discussed:** Registration flow, Profile setup timing, Avatar upload

---

## Registration Flow

| Option | Description | Selected |
|--------|-------------|----------|
| Immediate access after registration | Simplest onboarding. Users can browse and create listings immediately. Email verification can be added later if spam becomes an issue. | ✓ |
| Email verification required before first login | Higher friction but verified emails. Prevents fake accounts and ensures valid email for notifications. Requires email service setup now. | |

**User's choice:** Immediate access after registration (Recommended)
**Notes:** User initially asked about Google OAuth, which is out of scope per REQUIREMENTS.md. Redirected to email/password registration.

---

| Option | Description | Selected |
|--------|-------------|----------|
| Just email and password | Simple email/password only. Minimal friction for new users. | ✓ |
| Email, password, and display name | Display name required during registration. Adds one step but ensures profiles are complete from day one. | |

**User's choice:** Just email and password (Recommended)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Minimum 8 characters, no other rules | Standard security. Minimum 8 characters, no complexity rules. Lower friction for users. | ✓ |
| Complex password rules (uppercase, lowercase, number, special) | Stronger passwords. Minimum 8 chars with uppercase, lowercase, number, special char. Better security but more friction. | |

**User's choice:** Minimum 8 characters, no other rules (Recommended)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Generic error: "Email already registered" | Standard pattern. Generic error prevents email enumeration. Simpler to implement. | ✓ |
| Specific error with login link suggestion | User-friendly. Shows login link. Slightly more complex but better UX. | |

**User's choice:** Generic error: "Email already registered" (Recommended)

---

## Profile Setup Timing

| Option | Description | Selected |
|--------|-------------|----------|
| After registration, at user's choice | Cleanest onboarding. Users explore the platform first, then complete profile when ready. Matches the "just email/password" registration you chose. | ✓ |
| Immediately after registration (required before first action) | Guided onboarding. Forces profile setup before first use. Ensures complete profiles but adds friction. | |

**User's choice:** After registration, at user's choice (Recommended)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Profile required only for selling items | Maximum flexibility. Users can browse without profile. Must set display name when creating first listing (seller identity required). | |
| Profile required for any interaction (listing or chat) | Consistent experience. All users have display name before any interaction. Adds step before messaging or listing. | ✓ |

**User's choice:** Profile required for any interaction (listing or chat)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Inline profile setup prompt | Smooth UX. When users try to create listing or start chat, show profile setup form inline or modal. No separate "complete profile" page. | ✓ |
| Redirect to profile setup page | Clear flow. Redirect to dedicated profile setup page. More explicit but less seamless. | |

**User's choice:** Inline profile setup prompt (Recommended)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Use placeholder: "New User" | Consistent defaults. All new users have "New User" as display name until changed. Placeholder appears on profile. | ✓ |
| Empty display name with prompt | Clear incomplete state. No display name shows, "Set your name" prompt visible. More obvious that profile is incomplete. | |

**User's choice:** Use placeholder: "New User" (Recommended)

---

## Avatar Upload

| Option | Description | Selected |
|--------|-------------|----------|
| S3-compatible cloud storage | Production-ready. Use AWS S3 or compatible storage (MinIO, DigitalOcean Spaces). Scalable, CDN integration possible. | |
| Local filesystem (development only) | Simplest setup. Store files in local filesystem. Works for single-server deployment. Migration to cloud needed for scaling. | ✓ |

**User's choice:** Local filesystem (development only)

---

| Option | Description | Selected |
|--------|-------------|----------|
| 5 MB maximum | Reasonable size for profile photos. Allows high-quality images. Server validates and rejects oversize files. | ✓ |
| 10 MB maximum | Larger files for high-res photos. More storage needed. Users may have large phone photos. | |

**User's choice:** 5 MB maximum (Recommended)

---

| Option | Description | Selected |
|--------|-------------|----------|
| JPEG, PNG, WebP | Covers all common formats. Users can upload JPEGs, PNGs (transparency), and WebP (modern). Flexible. | ✓ |
| JPEG and PNG only | Most compatible. JPEG for photos, PNG for graphics. WebP support optional. Broader device compatibility. | |

**User's choice:** JPEG, PNG, WebP (Recommended)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Replace only | Flexible UX. Users can replace avatar anytime. Older avatar deleted automatically to save space. | ✓ |
| Allow multiple avatars with selection | Gallery option. Users can choose from previously uploaded avatars. More storage used, more complex UI. | |

**User's choice:** Replace only (Recommended)

---

## Claude's Discretion

- Email format validation approach
- Password hashing algorithm (Spring Security default recommended)
- JWT token expiration time and refresh strategy
- Default avatar image for users who don't upload one
- Avatar image dimensions and resizing behavior
- Session timeout policies

## Deferred Ideas

None - discussion stayed within phase scope.