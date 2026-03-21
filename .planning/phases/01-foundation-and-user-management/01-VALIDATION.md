---
phase: 01
slug: foundation-and-user-management
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-03-21
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Testcontainers 2.0.x |
| **Config file** | `src/test/resources/application-test.yml` |
| **Quick run command** | `mvn test -Dtest="*Test" -DfailIfNoTests=false` |
| **Full suite command** | `mvn verify` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest="*Test"`
- **After every plan wave:** Run `mvn verify`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | AUTH-01 | integration | `mvn test -Dtest=AuthControllerTest#testRegisterSuccess` | ❌ W0 | ⬜ pending |
| 01-01-02 | 01 | 1 | AUTH-01 | unit | `mvn test -Dtest=UserServiceTest#testRegisterDuplicateEmail` | ❌ W0 | ⬜ pending |
| 01-02-01 | 02 | 1 | AUTH-02 | integration | `mvn test -Dtest=AuthControllerTest#testLoginSuccess` | ❌ W0 | ⬜ pending |
| 01-02-02 | 02 | 1 | AUTH-02 | integration | `mvn test -Dtest=AuthControllerTest#testLoginInvalidPassword` | ❌ W0 | ⬜ pending |
| 01-03-01 | 03 | 1 | AUTH-03 | unit | `mvn test -Dtest=JwtTokenProviderTest#testValidateToken` | ❌ W0 | ⬜ pending |
| 01-03-02 | 03 | 1 | AUTH-03 | integration | `mvn test -Dtest=AuthControllerTest#testRefreshToken` | ❌ W0 | ⬜ pending |
| 01-04-01 | 04 | 1 | AUTH-04 | integration | `mvn test -Dtest=AuthControllerTest#testLogout` | ❌ W0 | ⬜ pending |
| 01-05-01 | 05 | 2 | PROF-01 | integration | `mvn test -Dtest=UserControllerTest#testSetDisplayName` | ❌ W0 | ⬜ pending |
| 01-06-01 | 06 | 2 | PROF-02 | integration | `mvn test -Dtest=AvatarControllerTest#testUploadAvatar` | ❌ W0 | ⬜ pending |
| 01-06-02 | 06 | 2 | PROF-02 | unit | `mvn test -Dtest=AvatarServiceTest#testValidateInvalidFile` | ❌ W0 | ⬜ pending |
| 01-07-01 | 07 | 2 | PROF-03 | integration | `mvn test -Dtest=UserControllerTest#testGetOwnProfile` | ❌ W0 | ⬜ pending |
| 01-08-01 | 08 | 2 | PROF-04 | integration | `mvn test -Dtest=UserControllerTest#testGetOtherProfile` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/.../config/TestcontainersConfiguration.java` — Testcontainers config for MySQL and Redis (embedded in single file)
- [ ] `src/test/resources/application-test.yml` — Test configuration
- [ ] `src/test/java/.../controller/AuthControllerTest.java` — AUTH-01, AUTH-02, AUTH-03, AUTH-04
- [ ] `src/test/java/.../controller/UserControllerTest.java` — PROF-01, PROF-03, PROF-04
- [ ] `src/test/java/.../controller/AvatarControllerTest.java` — PROF-02
- [ ] `src/test/java/.../service/UserServiceTest.java` — Unit tests
- [ ] `src/test/java/.../service/AvatarServiceTest.java` — Unit tests
- [ ] `src/test/java/.../security/JwtTokenProviderTest.java` — Unit tests

**Note:** Redis test configuration is embedded within TestcontainersConfiguration.java (includes GenericContainer for Redis 7-alpine). No separate TestRedisConfiguration.java file is needed.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Avatar displays correctly on profile | PROF-02, PROF-03 | Visual verification across browsers | Upload avatar, view profile in Chrome/Firefox/Safari, verify image renders correctly |
| Session persists across browser refresh | AUTH-03 | Requires browser state persistence | Log in, refresh page, verify user remains authenticated |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending