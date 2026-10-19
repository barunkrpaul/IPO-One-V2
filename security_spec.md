# IPO One — Firestore Security Specification

## 1. Data Invariants
1. **User Profile (`/users/{userId}`)**:
   - Only authenticated owner (`request.auth.uid == userId`) can read, create, update, or delete their own profile document.
   - `userId` in payload must match `request.auth.uid` on create and is immutable on update.
   - `createdAt` must be a valid server timestamp (`<= request.time`) and is immutable on update.
2. **PAN Profiles (`/users/{userId}/pan_profiles/{panId}`)**:
   - Strictly isolated per user under `/users/{userId}/pan_profiles/{panId}`.
   - `userId` must equal `request.auth.uid` and is immutable on update.
   - `panNumber` must be a 10-character uppercase alphanumeric string; `holderName` 2..80 chars.
3. **IPO Applications (`/users/{userId}/applications/{appId}`)**:
   - Strictly isolated per user under `/users/{userId}/applications/{appId}`.
   - `userId` must equal `request.auth.uid` and is immutable on update.
   - `lotsApplied` >= 1 and <= 1000; `bidPrice` >= 1; `totalAmount` >= 1.
4. **Watchlist (`/users/{userId}/watchlist/{ipoId}`)**:
   - Strictly isolated per user under `/users/{userId}/watchlist/{ipoId}`.
   - `userId` must equal `request.auth.uid` and is immutable on update.

## 2. The "Dirty Dozen" Payloads
1. Unauthenticated read on `/users/alice_123` -> REJECT
2. Cross-user read by `bob_456` on `/users/alice_123/pan_profiles/pan1` -> REJECT
3. Identity spoofing on create: `userId: "bob_456"` written by `alice_123` -> REJECT
4. Shadow field injection: `isAdmin: true` added to `/users/alice_123` -> REJECT
5. Immutable field mutation: changing `createdAt` or `userId` on update -> REJECT
6. Value poisoning: setting `lotsApplied: "ten"` (string instead of int) -> REJECT
7. Boundary overflow: `holderName` exceeding 80 characters -> REJECT
8. Future timestamp injection: `createdAt` in the future (`> request.time`) -> REJECT
9. Invalid document ID injection (>128 chars or special characters) -> REJECT
10. Missing required keys on `create` -> REJECT
11. Unauthorized field modification on `update` outside `affectedKeys().hasOnly(...)` -> REJECT
12. Cross-user list query on `/users/bob_456/applications` by `alice_123` -> REJECT
