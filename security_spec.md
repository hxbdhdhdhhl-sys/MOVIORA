# MOVIORA Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Default-Deny Catch-All**: Unmatched paths deny all reads and writes.
2. **PII Isolation**: `/users/{userId}` containing `email` can ONLY be read/updated/deleted by `request.auth.uid == userId` or verified Admin. Public community data lives strictly in `/public_profiles/{userId}` without `email`.
3. **Server-Side Admin RBAC**: Admin access requires either verified owner email (`hxbdhdhdhhl@gmail.com` with `request.auth.token.email_verified == true`) or an existing `/admins/{uid}` document. Users can never self-assign admin privileges.
4. **No Blanket Reads**: Every `allow list` rule enforces `resource.data` predicates (`isPublished == true`, `userId == request.auth.uid`, `isPublic == true`, `targetUid == 'ALL'`, etc.) with zero `get()`/`exists()` calls inside `allow list`.
5. **Composite Review Uniqueness**: Reviews at `/reviews/{reviewId}` require `reviewId == request.auth.uid + '_' + incoming().mediaId`, preventing duplicate reviews from the same account.
6. **Verified VIP Activation**: Users cannot self-grant `admin_grant` VIP subscriptions; user-initiated subscription writes require `provider == 'google_play'` with a non-empty `purchaseToken`.

## 2. The Dirty Dozen Adversarial Payloads
1. **Unauthenticated Catalogue Read**: `auth = null` reading `/media_items` -> REJECTED.
2. **Cross-User PII Leak**: `bob` reading `/users/alice` -> REJECTED.
3. **Unfiltered Private List Query**: `alice` listing `/watchlists` without `where('userId','==','alice')` or `where('isPublic','==',true)` -> REJECTED.
4. **Shadow Ghost Field Injection**: `alice` updating `/users/alice` with `{"isAdmin": true}` -> REJECTED via `hasOnly` + `affectedKeys().hasOnly()`.
5. **Unverified Admin Email Spoof**: Attacker token `email: "hxbdhdhdhhl@gmail.com", email_verified: false` writing `/media_items/m1` -> REJECTED (`email_verified == true` enforced).
6. **Self-Assigned Admin Escalation**: `bob` creating `/admins/bob` -> REJECTED.
7. **Duplicate Review Bypass**: `alice` creating `/reviews/random_id` instead of `alice_media1` -> REJECTED.
8. **Fake Free VIP Activation**: `bob` creating `/subscriptions/bob` with `provider: "admin_grant"` -> REJECTED.
9. **Oversized String DoW Attack**: `alice` writing a 10,000-char `comment` in `/reviews` -> REJECTED via `.size() <= 2000`.
10. **Immutable Timestamp Tampering**: `alice` modifying `createdAt` during `update` on `/users/alice` -> REJECTED via `incoming().createdAt == existing().createdAt`.
11. **Insecure HTTP Stream Injection**: Admin creating `/video_sources/s1` with `http://...` cleartext URL -> REJECTED (`matches('^https://.*')`).
12. **Audit Log Deletion**: Even an admin attempting `delete` on `/admin_audit_logs/log1` -> REJECTED (append-only immutable audit trail).
