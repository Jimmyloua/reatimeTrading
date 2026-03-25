# Phase 7: Browse category hover filtering, product collections, notification management, and homepage image modules inspired by Goofish - Research

**Researched:** 2026-03-25
**Domain:** Marketplace discovery UX, curated content modules, and notification center refinement
**Confidence:** MEDIUM

## User Constraints

No `*-CONTEXT.md` exists for Phase 7, so there are no locked user decisions beyond the roadmap entry itself.

- Locked decisions: None documented
- Claude's discretion: Infer scope, backend/frontend surfaces, data model changes, APIs, real-time considerations, testing strategy, and rollout risks
- Deferred ideas: None documented specifically for this phase. Continue honoring global v2 deferrals from [REQUIREMENTS.md](/d:/Java/Projects/realTimeTrading/.planning/REQUIREMENTS.md).

## Summary

Phase 7 should stay on the existing Spring Boot + React + TanStack Query + Zustand stack and add a focused discovery/content layer instead of introducing a new CMS, search engine, or frontend framework. The current browse flow already supports URL-backed category/search/filter parameters on [BrowseListingsPage.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/pages/BrowseListingsPage.tsx), and the current notification flow already supports persisted preferences, read state, and realtime pushes via [NotificationController.java](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/notification/controller/NotificationController.java), [NotificationService.java](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/notification/service/NotificationService.java), and [useNotifications.ts](/d:/Java/Projects/realTimeTrading/frontend/src/hooks/useNotifications.ts). The missing work is productization: richer browse navigation, reusable curated collections, a real notification management surface, and server-driven homepage modules.

The safest implementation boundary is:

1. Keep browse filtering URL-driven and query-param compatible with the existing `/listings` route.
2. Treat category hover as a disclosure/preview pattern, not a hover-only menu.
3. Add reusable curated collections as first-class backend records with explicit ordering.
4. Add homepage modules as ordered content records that reference collections, categories, or listings.
5. Upgrade notifications into a filtered management center without adding delete/archive semantics in this phase.

**Primary recommendation:** Build Phase 7 around server-driven curated collections and homepage modules, while keeping browse and notification state URL-backed and accessible.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.4.2 (repo-pinned) | Backend REST + JPA + validation | Already powers listings and notifications; Phase 7 is an extension, not a platform migration |
| React | 19.2.4 (repo-pinned) | Browse/homepage/notification UI | Already established across all shipped frontend phases |
| React Router DOM | 7.1.0 in repo, 7.13.2 latest verified 2026-03-23 | URL state for browse and notification filters | `useSearchParams` already matches the current browse architecture and keeps deep links sharable |
| TanStack Query | 5.91.3 in repo, 5.95.2 latest verified 2026-03-23 | Server state for listings, collections, modules, notifications | Official paginated-query support fits collection rails and browse result transitions |
| Zustand | 5.0.12 | Local UI state for hover preview, quick settings, transient notification state | Already used for notifications, auth, chat, and seller presence |
| Liquibase | via `liquibase-core` | Schema/data seeding for collections and homepage modules | Best fit for editorial seed data and deterministic ordering |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Embla Carousel React | 8.6.0 latest verified 2025-04-04 | Touch-friendly image or collection rail behavior | Use only if homepage image modules need swipe/drag rails; skip if static tiles are enough |
| MockMvc + Spring Boot Test | existing | Backend endpoint/integration verification | Use for collection/module and notification-management endpoints |
| Vitest + Testing Library | existing | Frontend interaction tests | Use for hover/disclosure behavior, URL sync, and notification management flows |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| URL-backed browse filters with React Router | Local-only component state | Faster to prototype, but breaks deep links, back/forward behavior, and sharable filtered entry points |
| Manual curated collections table | Dynamic rule engine or search-derived collections | Dynamic rules are tempting, but they add hidden query complexity and admin ambiguity too early |
| Static homepage JSX only | Generic CMS/JSON blob settings system | Static JSX is too rigid; a generic CMS is too broad. Ordered content tables are the right middle ground |
| CSS-only swipe/overflow rails | Embla Carousel | CSS is enough for simple scroll-snap; use Embla only if product requires proper drag controls and pagination |

**Installation:**
```bash
npm install embla-carousel-react
```

Only install `embla-carousel-react` if Phase 7 explicitly includes swipeable rails. No other new library is necessary.

**Version verification:**
- `@tanstack/react-query`: 5.95.2 latest on npm as of 2026-03-23; repo uses 5.91.3
- `react-router-dom`: 7.13.2 latest on npm as of 2026-03-23; repo uses 7.1.0
- `zustand`: 5.0.12 latest on npm as of 2026-03-16; repo already matches latest
- `embla-carousel-react`: 8.6.0 latest stable on npm as of 2025-04-04

## Architecture Patterns

### Recommended Project Structure

```text
backend/src/main/java/com/tradingplatform/
|-- content/                 # Curated collections + homepage modules
|   |-- controller/
|   |-- dto/
|   |-- entity/
|   |-- repository/
|   `-- service/
|-- listing/                 # Browse/filter query composition
`-- notification/            # Notification history + management filters

frontend/src/
|-- components/browse/       # Category hover/disclosure nav, collection rails
|-- components/home/         # Homepage modules, image tiles, hero rails
|-- components/notifications/# Notification center controls
|-- hooks/                   # Query + URL-sync hooks
|-- pages/                   # Home, browse, notifications
`-- types/                   # Content module and collection DTOs
```

### Pattern 1: URL-Backed Browse State With Local Hover Preview

**What:** Keep the canonical applied browse filter in the URL, but keep transient hover/open state local to the navigation component.

**When to use:** Category hover filtering, collection deep links, unread/type-filtered notification management, and homepage CTA routing into browse.

**Why:** React Router documents that `setSearchParams` causes navigation, and its callback form does not queue like React state. That makes it good for committed filters, but wrong for every hover event.

**Example:**
```tsx
// Source: https://reactrouter.com/api/hooks/useSearchParams
const [searchParams, setSearchParams] = useSearchParams()
const [hoveredCategoryId, setHoveredCategoryId] = useState<number | null>(null)

function applyCategory(categoryId: number) {
  setSearchParams((current) => {
    current.set('categoryId', String(categoryId))
    current.delete('page')
    return current
  })
}
```

### Pattern 2: Server-Driven Curated Collections

**What:** Model collections explicitly in the backend instead of synthesizing them in JSX or deriving them from ad hoc query strings.

**When to use:** "Trending cameras", "Student laptops", "Creator setup", "Recently price-dropped" if editorially chosen, and homepage browse modules.

**Recommended schema:**
```text
curated_collections
- id
- slug (unique)
- title
- subtitle
- description
- cover_image_url nullable
- target_type enum(category, listing, route)
- target_value nullable
- active
- display_order
- created_at / updated_at

curated_collection_items
- id
- collection_id
- listing_id
- display_order
- badge_text nullable
```

**Prescriptive guidance:** Start with manual collection membership and explicit ordering. Do not add rule-based auto-population in this phase.

### Pattern 3: Ordered Homepage Modules, Not Hardcoded Sections

**What:** Expose a read-only homepage payload such as `GET /api/content/homepage` that returns ordered modules.

**When to use:** Hero image modules, split-image banners, collection rails, category spotlights, and browse CTA tiles inspired by Goofish's image-first merchandising.

**Recommended schema:**
```text
homepage_modules
- id
- slug
- module_type enum(hero, image_tiles, collection_row, category_spotlight)
- title nullable
- subtitle nullable
- active
- display_order

homepage_module_items
- id
- homepage_module_id
- image_url
- headline nullable
- subheadline nullable
- link_type enum(category, listing, collection, route)
- link_value
- accent_label nullable
- display_order
```

**Prescriptive guidance:** Use backend ordering and activation flags. Seed initial data with Liquibase. Do not build an admin UI in the same phase unless the planner explicitly scopes it in.

### Pattern 4: Notification Management As Filtering + Preference Control

**What:** Expand the notification page from simple history into a management center with:

- URL-backed tabs: `all`, `unread`
- type filters matching current preference groups
- mark-visible-as-read
- grouped settings surface
- consistent unread count updates across dropdown and page

**When to use:** The current [NotificationsPage.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/pages/NotificationsPage.tsx) and [NotificationDropdown.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/components/notifications/NotificationDropdown.tsx) both hydrate overlapping state; Phase 7 should consolidate this behavior through shared query hooks and store updates.

### Anti-Patterns to Avoid

- **Hover-only category navigation:** Touch users and keyboard users get blocked. Use click/focus disclosure semantics with hover as enhancement.
- **Writing search params on every pointer move:** This will spam navigations and refetches.
- **Hardcoding homepage modules in `App.tsx`:** It makes future ordering and activation changes a code deployment problem.
- **Generic JSON blob CMS table:** It looks flexible but removes schema validation and creates brittle frontend parsing.
- **Adding delete/archive notification semantics now:** The current backend only supports read state and preferences. Keep Phase 7 on management, not retention policy redesign.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Accessible top-nav dropdown behavior | A custom `role="menu"` nav hacked together from divs | Semantic buttons/links using the WAI disclosure navigation pattern | W3C explicitly warns not to use ARIA menu roles for typical site navigation |
| Swipe/drag carousel physics | Homegrown pointer math for rails | Embla Carousel if swipe rails are required | Drag thresholds, cleanup, and controls are easy to get subtly wrong |
| Homepage merchandising storage | Hardcoded React arrays or untyped JSON settings | Liquibase-seeded relational tables + DTOs | Ordered content with links/images needs validation and deterministic rollout |
| Paginated browse transitions | Manual loading-state juggling between page changes | TanStack Query `placeholderData` and stable query keys | Official paginated-query support avoids jarring list flicker |
| Notification read-state sync | Page-local counters independent from store | Existing Zustand notification store as the single client-side source of truth | Prevents dropdown/page unread drift |

**Key insight:** The deceptive complexity in this phase is not rendering cards. It is keeping links, filters, ordering, accessibility, and read state consistent across browse, home, and notifications.

## Common Pitfalls

### Pitfall 1: Using ARIA Menu Roles For Marketplace Navigation

**What goes wrong:** Developers implement category hover as an ARIA `menu`/`menubar`, which is the wrong semantic model for ordinary site navigation.

**Why it happens:** "Mega menu" sounds like "menu", but W3C's disclosure-navigation guidance calls out that common navigation should not use the ARIA menu role.

**How to avoid:** Use semantic nav + buttons/links + disclosure behavior. Support keyboard focus, Escape, and click activation. Treat hover as optional enhancement.

**Warning signs:** Arrow-key traps, screen readers announcing menu semantics for normal links, or no usable behavior on touch.

### Pitfall 2: Coupling Hover Preview To Applied Filters

**What goes wrong:** Merely moving the pointer across categories rewrites `categoryId` in the URL and refetches listings repeatedly.

**Why it happens:** The current browse page already uses query params for filters, so it is tempting to drive hover from the same state.

**How to avoid:** Maintain separate state:

- preview/open category: local component state
- applied browse category: URL search param

**Warning signs:** Network chatter during hover, broken browser history, and page reset while users are exploring the menu.

### Pitfall 3: Collection Cards Breaking On Sold Or Imageless Listings

**What goes wrong:** Curated collection modules render dead or low-quality cards because referenced listings are sold, deleted, or lack a primary image.

**Why it happens:** Current listing responses compute primary image URLs per listing, and collections will likely reuse those cards at higher density.

**How to avoid:** Add module/collection validation rules:

- only include `AVAILABLE` listings by default
- require a primary image or collection-level fallback image
- fall back gracefully when an item becomes unavailable

**Warning signs:** Empty image frames, modules with fewer items than expected, or 404 links from the homepage.

### Pitfall 4: Duplicate Notification Hydration Paths

**What goes wrong:** The dropdown and the page fetch overlapping data separately, causing inconsistent unread counts or preference snapshots.

**Why it happens:** Current code hydrates notifications in both [NotificationDropdown.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/components/notifications/NotificationDropdown.tsx) and [NotificationList.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/components/notifications/NotificationList.tsx).

**How to avoid:** Introduce shared query hooks for notifications/preferences and let the store handle realtime upserts plus optimistic read actions.

**Warning signs:** Counts changing unexpectedly after page navigation, stale quick-setting toggles, or duplicate notification rows.

### Pitfall 5: N+1 Work When Rendering Multiple Curated Rows

**What goes wrong:** Every collection row forces repeated listing and primary-image lookups, making the homepage heavy.

**Why it happens:** Current listing mapping fetches primary images per listing in [ListingService.java](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/listing/service/ListingService.java).

**How to avoid:** Design collection queries to fetch all needed listings and primary images in batch, or create a lean collection-card projection DTO.

**Warning signs:** Slow home page response times, large query counts in logs, and inconsistent image ordering.

## Code Examples

Verified patterns from official sources:

### Committing URL Search Params

```tsx
// Source: https://reactrouter.com/api/hooks/useSearchParams
const [searchParams, setSearchParams] = useSearchParams()

function setUnreadOnly(enabled: boolean) {
  setSearchParams((current) => {
    if (enabled) current.set('tab', 'unread')
    else current.delete('tab')
    return current
  })
}
```

### Smoother Paginated Collection/Browse Queries

```tsx
// Source: https://tanstack.com/query/v5/docs/framework/react/guides/paginated-queries
const result = useQuery({
  queryKey: ['listings', filters, page],
  queryFn: () => fetchListings(filters, page),
  placeholderData: (previousData) => previousData,
})
```

Use this for browse pagination and collection rails that page or swap filters, so cards do not disappear between requests.

### Optional Embla Rail Structure

```tsx
// Source: https://www.embla-carousel.com/get-started/react/
const [emblaRef] = useEmblaCarousel()

return (
  <div className="embla">
    <div className="embla__viewport" ref={emblaRef}>
      <div className="embla__container">{slides}</div>
    </div>
  </div>
)
```

Use only if Phase 7 requires real swipe rails. For static desktop rows, regular CSS grid or horizontal overflow is simpler.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hover-only desktop mega menus | Hybrid disclosure navigation with hover enhancement | Current W3C APG guidance, updated 2026 example | Accessibility and touch support matter more than pure hover polish |
| Jarring paginated refetches | Keep previous query data during page changes | TanStack Query v5 docs | Browse and collection transitions feel stable instead of blanking |
| Static homepage sections in JSX | Server-driven ordered content modules | Common marketplace merchandising pattern; reinforced by Goofish-style image-first merchandising surfaces | Lets planners separate content seeding from UI rendering |

**Deprecated/outdated:**
- Using ARIA menu roles for common site navigation: W3C disclosure-navigation guidance explicitly warns against it for ordinary nav links.
- Treating hover as the only interaction model: unacceptable for touch and keyboard users.

## Open Questions

1. **Who authors collections and homepage modules?**
   - What we know: The repo has no admin surface today.
   - What's unclear: Whether Phase 7 needs authoring UI or only seeded/default content.
   - Recommendation: Plan Phase 7 as read-only consumption plus Liquibase seed data. Defer authoring UI.

2. **Should collections be manual or rules-based?**
   - What we know: Existing listing/category APIs are simple and deterministic.
   - What's unclear: Whether "collection" means curated editorial sets or saved search rules.
   - Recommendation: Use manual join-table membership this phase. Dynamic rules are a later enhancement.

3. **Does notification management include deletion/archive?**
   - What we know: Existing backend only supports history, unread count, read actions, and preferences.
   - What's unclear: Whether "management" means retention controls or just filtering/settings/read state.
   - Recommendation: Keep scope to filters, tabs, grouped settings, and read actions unless the planner gets explicit user confirmation.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.0 + Testing Library on frontend; JUnit 5 + Spring Boot Test + MockMvc on backend |
| Config file | [vitest.config.ts](/d:/Java/Projects/realTimeTrading/frontend/vitest.config.ts); backend uses Spring Boot test defaults |
| Quick run command | `cd frontend && npm test -- browse-listings.test.tsx notification-preferences.test.tsx` |
| Full suite command | `cd frontend && npm test && cd ../backend && mvn test` |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| P7-01 | Category disclosure/hover preview does not rewrite applied filters until commit | frontend component/integration | `cd frontend && npm test -- browse-category-hover.test.tsx` | NO - Wave 0 |
| P7-02 | Clicking category/collection/homepage tile routes into correct browse params | frontend integration | `cd frontend && npm test -- homepage-modules.test.tsx browse-category-hover.test.tsx` | NO - Wave 0 |
| P7-03 | Curated collections and homepage modules return only active, ordered content | backend integration | `cd backend && mvn -Dtest=ContentControllerTest test` | NO - Wave 0 |
| P7-04 | Notification management tabs/filters/settings stay synchronized with unread counts and store state | frontend integration | `cd frontend && npm test -- notification-management.test.tsx notification-preferences.test.tsx` | NO - Wave 0 |
| P7-05 | Notification management endpoints support filtering/mark-visible-read contract if added | backend integration | `cd backend && mvn -Dtest=NotificationControllerTest test` | YES - partial |

### Sampling Rate

- **Per task commit:** run the focused frontend or backend command that matches the touched surface
- **Per wave merge:** `cd frontend && npm test` and `cd backend && mvn test`
- **Phase gate:** Full frontend and backend suites green, plus manual desktop/mobile verification of category disclosure and homepage modules before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `frontend/src/tests/browse-category-hover.test.tsx` - covers local preview vs committed browse filter behavior
- [ ] `frontend/src/tests/homepage-modules.test.tsx` - covers server-driven module rendering and click-through routing
- [ ] `frontend/src/tests/notification-management.test.tsx` - covers tabs, type filters, mark-visible-as-read, and unread-count sync
- [ ] `backend/src/test/java/com/tradingplatform/content/controller/ContentControllerTest.java` - covers homepage module and collection payload ordering/activation
- [ ] `backend/src/test/java/com/tradingplatform/content/service/ContentServiceTest.java` - covers collection membership filtering and fallback logic

## Sources

### Primary (HIGH confidence)

- React Router official docs: https://reactrouter.com/api/hooks/useSearchParams - verified URL search param behavior and navigation semantics
- TanStack Query official docs: https://tanstack.com/query/v5/docs/framework/react/guides/paginated-queries - verified paginated-query `placeholderData` guidance
- W3C APG disclosure navigation example: https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/examples/disclosure-navigation/ - verified accessibility guidance for nav disclosures and warning against ARIA menu roles
- Embla official docs: https://www.embla-carousel.com/get-started/react/ - verified React integration and component structure for optional rails
- Local codebase:
  - [BrowseListingsPage.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/pages/BrowseListingsPage.tsx)
  - [ListingFilters.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/components/ListingFilters.tsx)
  - [NotificationsPage.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/pages/NotificationsPage.tsx)
  - [NotificationDropdown.tsx](/d:/Java/Projects/realTimeTrading/frontend/src/components/notifications/NotificationDropdown.tsx)
  - [NotificationController.java](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/notification/controller/NotificationController.java)
  - [NotificationService.java](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/notification/service/NotificationService.java)
  - [ListingService.java](/d:/Java/Projects/realTimeTrading/backend/src/main/java/com/tradingplatform/listing/service/ListingService.java)
  - [005-seed-categories.xml](/d:/Java/Projects/realTimeTrading/backend/src/main/resources/db/changelog/005-seed-categories.xml)

### Secondary (MEDIUM confidence)

- npm registry metadata for current package versions:
  - `@tanstack/react-query`
  - `react-router-dom`
  - `zustand`
  - `embla-carousel-react`

### Tertiary (LOW confidence)

- Goofish App Store listing: https://apps.apple.com/us/app/goofish-selling-buying/id6714475519
- Goofish Google Play listing: https://play.google.com/store/apps/details?id=com.igoofish.fleamarket4Android

These tertiary sources were used only to confirm the product inspiration trend: image-first merchandising, collection-style presentation, and stronger notification/productization patterns. They were not used to define technical contracts.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Existing repo stack is sufficient; optional carousel dependency was verified from official docs and npm
- Architecture: MEDIUM - Strongly grounded in current codebase and official routing/query/accessibility docs, but exact Phase 7 UX scope still needs planning decisions
- Pitfalls: MEDIUM - Codebase inspection clearly shows duplicate hydration and URL-state risks; content-module pitfalls are inferred from existing listing/image patterns

**Research date:** 2026-03-25
**Valid until:** 2026-04-01
