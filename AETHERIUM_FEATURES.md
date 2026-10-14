# Aetherium Phone — Feature Specification (to re-apply on the AOSP base)

This document records every feature of the previous custom Aetherium Phones app
(`net.aetherium.phones`, v0.2.0, commit e96d8f2). The app is now deleted and
re-based on the **AOSP Phone/Dialer** source. Re-implement these features on top
of the AOSP base.

## 1. Launcher / identity
- **Two launcher entries** in one installed app:
  - **Aetherium Phone** (`PhoneActivity`) — blue phone icon.
  - **Aetherium Contacts** (`ContactsActivity`) — green contacts icon.
  - Distinct `android:icon` / `android:roundIcon` per activity; both `MAIN`+`LAUNCHER`.
- Application id: `net.aetherium.phones`; app label `Aetherium Phones`.

## 2. Phone UI (AOSP / Google Phone look)
- Material 3, light + dark (`values-night`).
- Google palette: blue `#1A73E8`, call green `#1E8E3E`, end-call red `#D93025`, grey `#5F6368`.
- **Bottom navigation with 3 tabs**: `Dialpad`, `Contacts`, `Recents`
  (ViewPager2 + Fragments, synced with the bottom nav).
- **Dialpad tab**: number field + AOSP-style 12-key keypad (1–9, `*`, `0`, `#`)
  with letter sub-labels (ABC, DEF, …), Call button.
- **Contacts tab**: search field, RecyclerView, circular avatar with initials,
  tap row to call.
- **Recents tab**: call log list with incoming/outgoing/missed markers, tap to call.
- Empty states ("No contacts", "No recent calls").

## 3. Cryptography
- X25519 ECDH key agreement (`Crypto.generateX25519`, `x25519Shared`).
- HKDF-SHA256 with info `aetherium-call-v1` (`Crypto.hkdfSha256`).
- ChaCha20-Poly1305 AEAD encrypt/decrypt (`Crypto.chacha20Poly1305Encrypt/Decrypt`).
- Secure random nonces (`randomNonce12`), SHA-256 helper.
- Target: even a **SIM call** is encrypted end-to-end so the carrier/operator
  hears only gibberish.

## 4. Pairing
- `PairingManager` backed by **EncryptedSharedPreferences** (AES256-SIV keys /
  AES256-GCM values).
- **6-digit** ephemeral pairing code (`generateCode6`).
- **60-character** physical pairing code (`generateCode60`, alphanumeric).
- Per-number peer public-key storage (`pk:<number>`), paired check.
- Physical pair code storage (`pp:<number>`), `hasPhysicalPair`.
- Per-contact option **"Fiziksel olarak eşleştir"** → two choices:
  **"Kod oluştur"** (create 60-char code) and **"Kod gir"** (enter code).
  Saved encrypted on device (and can sync encrypted to cloud).

## 5. Account
- `AccountManager.getOwnAccountId` — reads the signed-in AetheriumServices
  account id from encrypted prefs (tries keys `account_id`, `accountId`, `id`,
  `user_id`, `uid` across several pref file names).
- The API call requires being logged into AetheriumServices and uses the user's
  own id **and** the peer's account id.

## 6. API — one-time secure code exchange
- Server route: `GET https://api.aetheriumos.com/phone/codesetup`
  with query `ownid=<own account id>&accountid=<peer account id>`
  (spec: `?accountid=KARSIDAKININ_ACCOUNTID`).
- Response JSON: `ok`, `code` (**60 chars**), `expires_in_ms`, `retrieved_count`.
- Server (Cloudflare Worker + D1, `AetheriumAPI/src/phone.ts`):
  - Generates a **60-character** CSPRNG code.
  - Stored in **D1**; valid **1 minute**; **deleted permanently after 2 retrievals**
    (so the two sides never get different ids).
- `Api.isInternetAvailable` — needs `NET_CAPABILITY_INTERNET` + `VALIDATED`.

## 7. Hybrid secure-channel negotiation
- `HybridDetector`, 7-second window.
- Path: **Acoustic (18–22 kHz) → Data-over-Voice (AFSK) → VoIP upgrade** if internet.
- Verdicts: `USES_AETH_NOW`, `NOT_USING_AETH_NOW`, `USED_BUT_NOT_NOW`, `UNKNOWN`.
- Timing rule: wait a **single frequency/beacon**; the **first beacon** in the
  window decides. If a non-Aetherium beacon arrives first, or a different beacon
  arrives before/after Aetherium's → not encrypted. Not permanent — re-check on
  every call/app open.

## 8. User popups (exact copy)
- No popup when the other party does not use Aetherium Phone.
- No internet:
  "The User uses Aetherium Phone. But the Secure Encryption key is not transferred
  because you or (KAYITLIOLANKIŞININADI) does ont have access to wifi."
- Success:
  "The user uses Aetherium Phone. Even if its called via SIM, it will be encrypted
  so operator cant hear it."
- Used before but not now:
  "The user used aetherium phone, But right now, it appears its not using. The call
  will not be encrypted".

## 9. Telecom integration
- `AetheriumConnectionService` (ConnectionService) — outgoing/incoming stubs that
  mark the connection active. Registered with `BIND_TELECOM_CONNECTION_SERVICE`.
- `CallStateReceiver` — listens to call-state to trigger hybrid detection (stub).
- `InCallActivity` — shows callee number, End-call button, pair entry point.

## 10. Permissions
`CALL_PHONE`, `READ_PHONE_STATE`, `READ_CALL_LOG`, `ANSWER_PHONE_CALLS`,
`RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `READ_CONTACTS`, `WRITE_CONTACTS`,
`INTERNET`, `ACCESS_NETWORK_STATE`.

## 11. Localization
- English base + 12 translations: `ar, bg, de, es, fr, hi, it, ja, ko, pt, ru, tr, zh`.
- All UI strings resource-based (no hardcoded strings).

## 12. Constraints / decisions
- No new external Android libraries were added for the Android Drive/Mail apps;
  Phones used AndroidX + Material + `androidx.security:security-crypto`.
- `usesCleartextTraffic="false"`.
