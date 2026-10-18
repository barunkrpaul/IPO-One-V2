# Security Specification — IPO One Cloud Profile & Watchlist Sync

## 1. Data Invariants
1. **Strict Ownership Isolation (`/users/{userId}`)**: A user profile document at `/users/{userId}` can only be read, created, updated, or deleted by an authenticated user whose `request.auth.uid == userId` and `incoming().userId == request.auth.uid`.
2. **Subcollection Ownership Isolation (`/users/{userId}/ipo_watchlists/{watchlistId}`)**: A watchlist item can only be read, listed, created, updated, or deleted by the authenticated owner where `request.auth.uid == userId` and `incoming().userId == request.auth.uid`.
3. **Immutable Identity & Creation Timestamp**: On updates to `/users/{userId}` and `/users/{userId}/ipo_watchlists/{watchlistId}`, `userId` and `createdAt` must remain strictly equal to `existing().userId` and `existing().createdAt`.
4. **Path Variable Hardening**: `isValidId(userId)` and `isValidId(watchlistId)` enforce `^[a-zA-Z0-9_\-]+$` and `.size() <= 128` on all single-document operations (`get`, `create`, `update`, `delete`).
5. **String & Number Bounds**: Every string field enforces `.size() <= MAX` and every number field enforces realistic numeric ranges.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Profile Read**: `unauthDb.collection("users").doc("alice_123").get()` -> REJECTED (`request.auth != null`).
2. **Cross-User PII Profile Read**: `bobDb.collection("users").doc("alice_123").get()` -> REJECTED (`request.auth.uid == userId`).
3. **Identity Spoofing on Create**: Alice creates `/users/alice_123` with `{ userId: "bob_456" }` -> REJECTED (`incoming().userId == request.auth.uid`).
4. **Shadow Update / Ghost Field Injection**: Alice updates `/users/alice_123` adding `{ isAdmin: true }` -> REJECTED (`affectedKeys().hasOnly(...)`).
5. **Immutable Field Mutation (`createdAt`)**: Alice updates `createdAt` on `/users/alice_123` -> REJECTED (`incoming().createdAt == existing().createdAt`).
6. **Immutable Field Mutation (`userId`)**: Alice updates `userId` on `/users/alice_123` -> REJECTED (`incoming().userId == existing().userId`).
7. **Value Poisoning (`displayName` > 100 chars)**: Alice sends a 500-char `displayName` -> REJECTED (`isValidUserProfile`).
8. **Invalid Mobile Number Format**: Alice sends a 100-char string in `mobileNumber` -> REJECTED (`data.mobileNumber.size() <= 20`).
9. **Future Timestamp Spoofing**: Alice sends `createdAt` in the future (`> request.time`) -> REJECTED (`incoming().createdAt <= request.time`).
10. **Cross-User Subcollection List**: Bob lists `/users/alice_123/ipo_watchlists` -> REJECTED (`request.auth.uid == userId`).
11. **Watchlist Negative Target Lots**: Alice creates a watchlist item with `targetLots: 0` or `targetLots: 500` -> REJECTED (`data.targetLots >= 1 && data.targetLots <= 100`).
12. **Missing Required Keys on Create**: Alice creates `/users/alice_123` omitting `displayName` or `createdAt` -> REJECTED (`incoming().keys().hasAll(...)`).
