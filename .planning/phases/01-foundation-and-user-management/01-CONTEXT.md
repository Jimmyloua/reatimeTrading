# Phase 1: Foundation and User Management - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can create accounts, authenticate securely, and manage their identity on the platform. This phase delivers user registration, authentication with JWT-based sessions, and basic profile management (display name and avatar). All marketplace features (listings, chat, transactions) are separate phases.

</domain>

<decisions>
## Implementation Decisions

### Registration Flow
- **D-01:** Users receive immediate access after registration (no email verification required for v1)
- **D-02:** Registration collects only email and password (display name set later in profile)
- **D-03:** Password requirements: minimum 8 characters, no complexity rules
- **D-04:** Duplicate email registration returns generic error "Email already registered" (prevents email enumeration)

### Profile Setup Timing
- **D-05:** Profile setup is optional after registration - users choose when to complete it
- **D-06:** Profile (display name) required before any interaction: creating listings or starting chats
- **D-07:** When users attempt an action requiring profile, show inline profile setup prompt (modal or form)
- **D-08:** Users without a display name show "New User" placeholder until they set one

### Avatar Upload
- **D-09:** Avatar images stored on local filesystem for v1 (development setup - migration to S3 planned for production)
- **D-10:** Maximum file size: 5 MB for avatar uploads
- **D-11:** Supported formats: JPEG, PNG, WebP
- **D-12:** Users can only replace current avatar (no multiple avatar gallery)

### Claude's Discretion
- Email format validation approach
- Password hashing algorithm (Spring Security default recommended)
- JWT token expiration time and refresh strategy
- Default avatar image for users who don't upload one
- Avatar image dimensions and resizing behavior
- Session timeout policies

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

No external specs or ADRs defined yet for this phase. Requirements are fully captured in:
- `.planning/REQUIREMENTS.md` — AUTH-01 to AUTH-04, PROF-01 to PROF-04
- `.planning/ROADMAP.md` — Phase 1 details and success criteria
- `.planning/PROJECT.md` — Tech stack mandates (Spring Boot 3.5.x, JDK 21, MySQL 8, Redis 7)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
None - this is a greenfield project with no existing codebase.

### Established Patterns
None - patterns will be established in this phase.

### Integration Points
- MySQL database will store user accounts and profiles
- Redis will handle session storage for JWT tokens
- Spring Security 7 will provide authentication/authorization framework
- Spring Session will manage distributed sessions

This phase establishes the foundation that all subsequent phases build upon.

</code_context>

<specifics>
## Specific Ideas

- OAuth login (Google) explicitly out of scope for v1 per REQUIREMENTS.md - can be added in future version
- Email verification can be added later if spam/fake accounts become an issue
- Local filesystem storage for avatars is development-focused - production will need S3-compatible storage for scaling

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope.

</deferred>

---

*Phase: 01-foundation-and-user-management*
*Context gathered: 2026-03-21*