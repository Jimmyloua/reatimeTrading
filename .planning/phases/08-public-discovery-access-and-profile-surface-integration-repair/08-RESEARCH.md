# Phase 8: public-discovery-access-and-profile-surface-integration-repair - Research

**Researched:** 2026-03-29
**Domain:** Spring Security authorization alignment, public marketplace discovery flows, profile aggregate truthfulness
**Confidence:** HIGH

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DISC-01 | User can browse items by category | Open anonymous `GET /api/listings`, `GET /api/listings/categories`, `GET /api/content/**`; preserve existing category tree and URL-backed browse filters |
| DISC-02 | User can search items by full-text search | Keep `ListingService.searchListings` as the browse backend; make the anonymous `GET /api/listings` path public |
| DISC-03 | User can filter items by price range | Reuse existing browse query params and JPA specification filters once anonymous access is aligned |
| DISC-04 | User can filter items by condition | Reuse existing browse query params and JPA specification filters once anonymous access is aligned |
| DISC-05 | User can filter items by location/distance | Reuse existing browse query params and JPA specification filters once anonymous access is aligned |
| P7-01 | Accessible category disclosure supports preview without committing until selection | Existing frontend disclosure is correct; Phase 8 must restore its backend dependencies for anonymous users |
| P7-02 | Homepage modules, curated collections, and category tiles enter browse with correct URL params | Existing homepage and browse routing are correct; Phase 8 must make content/category/listing APIs reachable anonymously |
| P7-03 | Homepage modules and curated collections are server-driven | Existing content service/contracts are correct; Phase 8 must make content endpoints public |
| PROF-03 | User can view own profile with listing count and join date | Replace hardcoded `listingCount(0L)` with a shared listing-backed aggregate |
| PROF-04 | User can view other users' profiles | Keep `/api/users/{id}` public, make its rating dependencies public, and return truthful listing counts |
</phase_requirements>

## Summary

Phase 8 is a repair phase, not a rebuild. The frontend already exposes anonymous homepage, browse, and public profile routes, and Phase 7 already shipped the URL contracts for category disclosure, collection entry, and homepage modules. The main failure is backend authorization drift: `SecurityConfig` only permits a narrow public subset, so the public UI hits `401` on browse, category, content, and rating requests.

The second failure is profile truthfulness. `UserController` still hardcodes `listingCount(0L)` even though listing-backed counting already exists in `ListingService` for seller detail. Phase 8 should establish one shared profile/listing aggregate source of truth and use it for both self and public profiles. Do not add fallback UI logic to hide these failures; fix the authorization and aggregation contracts directly.

**Primary recommendation:** Keep the existing stack and flows, widen anonymous `GET` access only for the endpoints already used by public surfaces, and extract a shared non-deleted listing count query for profile and seller surfaces.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4.2 | Backend API and security | Already in repo; all Phase 8 backend work is configuration/controller/service level |
| Spring Security | Boot-managed | Anonymous/authenticated route policy | Existing app already centralizes HTTP authorization in `SecurityConfig` |
| React | 19.2.4 | Public homepage, browse, and profile routes | Existing surfaces are already implemented and only need backend parity |
| React Router DOM | 7.1.0 | Public route entry and URL-backed browse flows | Existing route contracts already match Phase 7 requirements |
| TanStack Query | 5.91.3 | Homepage, browse, and profile data fetching | Existing public surfaces already depend on it; no new state layer needed |
| Vitest | 4.1.0 | Frontend regression coverage | Existing tests already cover homepage modules and browse URL behavior |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring Security Test | Boot-managed | MockMvc authorization regression tests | Use for anonymous vs authenticated endpoint coverage |
| Spring Boot Test / MockMvc | Boot-managed | Controller integration tests | Use for public endpoint 200/401 expectations and profile payload truthfulness |
| Axios | 1.13.6 | Shared API client | Relevant because unexpected `401` on public pages currently triggers refresh/logout redirect |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Central `SecurityConfig` matcher alignment | Per-controller `@PreAuthorize("permitAll()")` style changes | Worse fit here; current app already uses centralized HTTP matcher policy and Phase 8 needs one auditable public surface list |
| Shared repository aggregate for listing count | Counting in each controller/service | Reintroduces drift; seller detail and profile surfaces would diverge again |

**Installation:**
```bash
# No new packages required
```

**Version verification:** Phase 8 should use the versions already locked in `backend/pom.xml` and `frontend/package.json`. No dependency upgrade is needed for this repair phase.

## Architecture Patterns

### Recommended Project Structure
```text
backend/src/main/java/com/tradingplatform/
|-- config/          # Security matcher policy
|-- listing/         # Browse/search/category endpoints and listing aggregates
|-- content/         # Homepage and curated collection delivery
|-- transaction/     # Public rating summary/recent-review reads
`-- user/            # Self/public profile payloads

frontend/src/
|-- api/             # Axios clients for browse/content/profile/rating calls
|-- pages/           # HomePage, BrowseListingsPage, UserProfilePage, ProfilePage
|-- components/      # Homepage modules, browse disclosure, profile ratings
`-- tests/           # Anonymous discovery and profile regression coverage
```

### Likely Files To Touch

**Backend**
- `backend/src/main/java/com/tradingplatform/config/SecurityConfig.java`
- `backend/src/main/java/com/tradingplatform/user/UserController.java`
- `backend/src/main/java/com/tradingplatform/listing/service/ListingService.java`
- `backend/src/main/java/com/tradingplatform/listing/repository/ListingRepository.java`
- `backend/src/test/java/com/tradingplatform/controller/UserControllerTest.java`
- `backend/src/test/java/com/tradingplatform/listing/controller/ListingControllerIT.java`
- Add/expand focused controller tests for public content and public rating endpoints

**Frontend**
- `frontend/src/pages/UserProfilePage.tsx`
- `frontend/src/components/profile/ProfileRatingSection.tsx`
- `frontend/src/api/client.ts` is an impact point for verification, even if not edited
- `frontend/src/tests/homepage-modules.test.tsx`
- `frontend/src/tests/browse-category-hover.test.tsx`
- Add a public-profile regression test and an anonymous app-route smoke test

### Pattern 1: Central Anonymous GET Surface
**What:** Keep all public-read authorization rules in `SecurityConfig`, limited to the exact `GET` endpoints already used by anonymous homepage, browse, profile, and rating surfaces.
**When to use:** Any route that must be reachable without login from `App.tsx`.
**Example:**
```java
// Source: repo + Spring Security authorizeHttpRequests docs
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/listings").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/listings/categories").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/listings/categories/*").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/content/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/ratings/users/*").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/ratings/users/*/recent").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/ratings/users/*/summary").permitAll()
    .anyRequest().authenticated())
```

### Pattern 2: Shared Listing Count Aggregate
**What:** Introduce one repository/service-level aggregate for "truthful listing count" and reuse it for profile payloads and seller detail payloads.
**When to use:** Any profile or seller surface that shows listing totals.
**Example:**
```java
// Source: repo pattern, recommended extraction
@Query("select count(l) from Listing l where l.userId = :userId and l.deleted = false")
long countVisibleByUserId(@Param("userId") Long userId);
```

### Pattern 3: Public Route Contract Verification
**What:** Test public pages through the same API client/query stack they use in production instead of mocking away the authorization boundary entirely.
**When to use:** Phase gate tests for homepage, browse, and public profile.
**Example:**
```typescript
// Source: repo testing pattern
render(
  <QueryClientProvider client={queryClient}>
    <MemoryRouter initialEntries={['/users/42']}>
      <App />
    </MemoryRouter>
  </QueryClientProvider>
)
```

### Anti-Patterns to Avoid
- **Making whole controller trees public:** Only the public `GET` reads should open; listing creation, status updates, avatar mutation, rating submission, and `can-rate` stay authenticated.
- **Duplicating count logic in `UserController`:** Use a shared repository/service aggregate, not another inline stream count.
- **Frontend-only "fixes" for backend `401`s:** The axios client currently redirects on failed refresh. Fix the backend route policy rather than masking errors in the UI.
- **Changing profile count semantics in one place only:** If the count definition changes, update seller detail and profile surfaces together.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Public route authorization | Ad hoc controller exceptions and frontend fallbacks | Central `SecurityConfig` matcher policy | Easier to audit and less likely to drift |
| Listing totals | Repeated stream counts over full listing collections | Repository `count(...)` query | Avoids N+1-ish reads and keeps semantics consistent |
| Browse/filter behavior | New search endpoints for anonymous users | Existing `GET /api/listings` + current `ListingSpecification` | Phase 2 already implements the browse/filter/search contract |

**Key insight:** The app already has the product flows and data contracts. Phase 8 is about restoring parity between public UI exposure and backend read authorization, then replacing one known stub with a shared aggregate.

## Common Pitfalls

### Pitfall 1: Opening `/api/users/**` or `/api/ratings/**` too broadly
**What goes wrong:** Mutating routes become public.
**Why it happens:** Broad wildcard matchers are faster to type than exact `GET` rules.
**How to avoid:** Permit only the public reads and keep `anyRequest().authenticated()` as the default.
**Warning signs:** Anonymous `POST`/`PUT`/`PATCH` requests stop returning `401/403`.

### Pitfall 2: Fixing profile counts but leaving seller detail counts inconsistent
**What goes wrong:** Listing detail seller card and profile page show different numbers.
**Why it happens:** `ListingService.toListingDetailResponse` already computes a count separately.
**How to avoid:** Route both surfaces through one shared count method.
**Warning signs:** Same seller shows different counts between `/listings/:id` and `/users/:id`.

### Pitfall 3: Forgetting public rating reads
**What goes wrong:** Public profile shell loads, then the ratings panel triggers `401` and the axios interceptor redirects to `/login`.
**Why it happens:** `/api/users/{id}` is already public, which can hide that `ProfileRatingSection` makes additional requests.
**How to avoid:** Include `GET /api/ratings/users/{id}`, `/recent`, and `/summary` in the public-read audit.
**Warning signs:** Public profile briefly renders, then navigates to the login page.

### Pitfall 4: Leaving a frontend-only public profile bug behind
**What goes wrong:** Public profiles can render `@undefined` because the backend hides `email` for non-owners.
**Why it happens:** `UserProfilePage` derives a handle from `profile.email` even for public visitors.
**How to avoid:** Treat email as absent on public profiles and render only public-safe identity fields.
**Warning signs:** Public profiles with display names show a bogus handle line.

## Code Examples

Verified patterns from repo and official docs:

### Existing Public Route Surface
```tsx
// Source: frontend/src/App.tsx
<Route path="/" element={<HomePage />} />
<Route path="/users/:id" element={<UserProfilePage />} />
<Route path="/listings" element={<BrowseListingsPage />} />
<Route path="/listings/:id" element={<ListingDetailPage />} />
```

### Existing Browse/Search Backend Contract
```java
// Source: backend/src/main/java/com/tradingplatform/listing/service/ListingService.java
public Page<ListingResponse> searchListings(ListingSearchRequest request, Pageable pageable) {
    if (request.getQuery() != null && !request.getQuery().isBlank()) {
        return listingRepository.searchByFullText(sanitizeSearchQuery(request.getQuery()), pageable)
                .map(this::toListingResponse);
    }
    Specification<Listing> spec = ListingSpecification.withFilters(request, categoryIds);
    return listingRepository.findAll(spec, pageable).map(this::toListingResponse);
}
```

### Existing Frontend Failure Amplifier
```typescript
// Source: frontend/src/api/client.ts
if (error.response?.status === 401 && !originalRequest._retry) {
  // ...
  useAuthStore.getState().logout()
  window.location.href = '/login'
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hardcoded homepage content | Backend content records + ordered module APIs | Phase 7, 2026-03-26 | Public homepage now depends on content API availability |
| Static browse entry links | URL-backed category and collection entry flows | Phase 7, 2026-03-26 | Public browse correctness now depends on anonymous listing/content access |
| Stubbed profile listing totals | Listing-backed aggregate | Required in Phase 8 | Makes self/public profile counts truthful |

**Deprecated/outdated:**
- `UserController.buildProfileResponse(...).listingCount(0L)`: outdated stub left from Phase 1 and must be removed in this phase.

## Open Questions

1. **What exact listing-count semantic should profile surfaces show?**
   - What we know: seller detail currently counts all non-deleted listings, regardless of status.
   - What's unclear: whether product wants "all visible marketplace activity" or only "currently available" listings.
   - Recommendation: Default Phase 8 to non-deleted listings to match existing seller detail behavior; if product wants active-only, change both surfaces together and document it explicitly.

2. **Should paginated public review history be opened now or only summary/recent?**
   - What we know: current public profile uses summary and recent reviews; "View all reviews" is not wired.
   - What's unclear: whether Phase 8 should open `GET /api/ratings/users/{id}` proactively.
   - Recommendation: Open it now with the other public rating reads for consistency and lower future drift.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Spring Boot Test + MockMvc, Vitest 4.1.0 |
| Config file | `backend/pom.xml`, `frontend/package.json` |
| Quick run command | `cd backend && mvn -Dtest="UserControllerTest,ListingControllerIT" test` |
| Full suite command | `cd backend && mvn test && cd ../frontend && npm test -- --run` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DISC-01 | Anonymous browse can load categories and listings | backend integration + frontend component | `cd backend && mvn -Dtest=ListingControllerIT test` | yes, expand |
| DISC-02 | Anonymous full-text browse search returns 200 | backend integration | `cd backend && mvn -Dtest=ListingControllerIT test` | yes, expand |
| DISC-03 | Anonymous price filters return 200 and filtered results | backend/service | `cd backend && mvn -Dtest="ListingControllerIT,ListingSearchServiceTest" test` | yes, expand |
| DISC-04 | Anonymous condition filters return 200 and filtered results | backend/service | `cd backend && mvn -Dtest="ListingControllerIT,ListingSearchServiceTest" test` | yes, expand |
| DISC-05 | Anonymous location filters return 200 and filtered results | backend/service | `cd backend && mvn -Dtest="ListingControllerIT,ListingSearchServiceTest" test` | yes, expand |
| P7-01 | Category disclosure preview stays URL-safe and still works anonymously | frontend component | `cd frontend && npm test -- browse-category-hover.test.tsx` | yes |
| P7-02 | Homepage/category/collection entry URLs resolve into public browse | frontend route + backend auth | `cd frontend && npm test -- homepage-modules.test.tsx` | yes, expand |
| P7-03 | Homepage modules remain server-driven for anonymous users | backend + frontend | `cd backend && mvn -Dtest=ContentControllerTest test` | yes, replace/expand |
| PROF-03 | Own profile shows truthful listing count | backend integration | `cd backend && mvn -Dtest=UserControllerTest test` | yes, expand |
| PROF-04 | Public profile and public ratings remain accessible with truthful counts | backend integration + frontend route | `cd backend && mvn -Dtest=UserControllerTest test` | yes, expand |

### Sampling Rate
- **Per task commit:** `cd backend && mvn -Dtest="UserControllerTest,ListingControllerIT" test` and `cd frontend && npm test -- browse-category-hover.test.tsx homepage-modules.test.tsx`
- **Per wave merge:** `cd backend && mvn test`
- **Phase gate:** Backend targeted auth/profile tests plus frontend anonymous homepage/browse/public-profile tests green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] Add backend test coverage for anonymous `GET /api/listings`, `GET /api/listings/categories`, `GET /api/content/homepage`, `GET /api/content/collections/{slug}`, and public rating reads
- [ ] Add backend profile test proving listing counts change after creating/deleting listings
- [ ] Add frontend `UserProfilePage` test for anonymous viewing with ratings panel and no login redirect
- [ ] Replace the current source-string `ContentControllerTest` with a real MockMvc authorization/response contract test

## Sources

### Primary (HIGH confidence)
- Repo source inspection: `SecurityConfig`, `UserController`, `ListingService`, `ListingRepository`, `App.tsx`, `BrowseListingsPage.tsx`, `HomePage.tsx`, `UserProfilePage.tsx`, `ProfileRatingSection.tsx`, existing backend/frontend tests
- Spring Security reference: https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html - verified ordered HTTP authorization rules and matcher-based `permitAll` approach

### Secondary (MEDIUM confidence)
- `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/ROADMAP.md`, `.planning/v1.0-MILESTONE-AUDIT.md` - phase scope, reopened requirements, and audit evidence

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Phase 8 can stay entirely within the existing repo stack and versions
- Architecture: HIGH - The broken flows map directly to inspected code paths and route contracts
- Pitfalls: HIGH - Each pitfall is grounded in current repo behavior, especially `SecurityConfig`, `UserController`, and the axios `401` interceptor

**Research date:** 2026-03-29
**Valid until:** 2026-04-05
