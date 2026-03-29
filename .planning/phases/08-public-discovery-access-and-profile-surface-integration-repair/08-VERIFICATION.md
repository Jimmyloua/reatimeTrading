# Phase 08 Verification

## Status

- Automation: passed on 2026-03-29
- Manual logged-out verification: approved on 2026-03-29

## Automated Commands

### Backend

```bash
mvn -q -Dtest=UserControllerTest,ContentControllerTest,ListingControllerIT,RatingControllerTest test
```

Result: passed in the focused backend verification slice within the 60-second cap.

Proves:
- Public profile responses expose truthful listing counts.
- Anonymous homepage, browse, and public rating reads remain accessible.
- Authenticated write boundaries remain protected on the repaired discovery surface.

### Frontend

```bash
npm test -- --run src/tests/homepage-modules.test.tsx src/tests/browse-category-hover.test.tsx src/tests/user-profile-page.test.tsx
```

Result: passed.

Proves:
- Homepage module CTAs still resolve to exact `/listings?categoryId={id}` and `/listings?collection={slug}` URLs.
- Browse category disclosure keeps preview-only hover local and commits URL changes only after explicit child selection.
- Anonymous public profile rendering shows the public handle, truthful listing count, and ratings section without redirecting to login.

## Manual Logged-Out Checklist

Run these steps in a logged-out browser session:

1. Visit `/` and confirm homepage content loads without redirecting to `/login`.
2. Click a homepage module CTA and confirm the browser lands on `/listings?categoryId={id}` or `/listings?collection={slug}`.
3. Visit `/listings`, open the category disclosure, and confirm hover/focus preview does not change the URL before selection.
4. Choose a child category from the disclosure and confirm the URL updates to the explicit `categoryId` selection.
5. Visit `/users/{id}` for a seller with listings and confirm the page loads while logged out, the listing count is non-zero when expected, and the ratings section renders.

## Manual Approval Record

- Status: approved
- User response: `approved`
