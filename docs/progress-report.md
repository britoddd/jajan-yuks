# JajanYuks progress report (2026-11-19)

Demo deadline: 2026-11-30. Plan: see `AGENTS.md` → Plan.

## Status by sprint

| Sprint | Scope | Status |
|---|---|---|
| 1 | Seller Go Live → live location → buyer map pin | Done, tested on emulator |
| 2 | Email sign-up with role, seller menu + stock, buyer list/detail within 1 km | Done, tested on emulator |
| 3 | Remote order (seller must accept), voice status via Gemini | Done, tested on emulator |
| 4 | Order-ready notification, bug fixes, demo video | Notification and stale-seller fix done; real-phone test and demo video left |

## What works

### Accounts
- Email + password sign-up as **Penjual** (seller) or **Pembeli** (buyer); login, logout, staying logged in after restart.
- A network error at startup no longer logs the user out (retries loading the role).

### Seller
- **Go Live / Berhenti Jualan**: a foreground service sends location every 10–20 s or after ~20 m, plus a heartbeat every 4 min while standing still.
- **Menu**: add item (name, price, stock), change stock with − / +, delete.
- **Incoming orders** ("Pesanan masuk"): Terima / Tolak → Siap diambil → Selesai.
  - Accepting takes stock in a Firestore transaction. If stock is short, the seller sees "Stok tidak cukup…" and nothing is sold twice.
- **Voice update** ("🎤 Update stok/status lewat suara"):
  1. The seller speaks Indonesian (system speech screen, `id-ID`) or types.
  2. Gemini (`gemini-3.5-flash-lite` via Firebase AI Logic) returns JSON.
  3. The seller sees the proposal (e.g. "Cilok Kuah: stok 7 → 5", "Status: Lagi di depan SD Harapan") and taps Simpan.
  - Unknown items, negative stock and unchanged values from the AI are ignored.

### Buyer
- **Map** (MapLibre + OpenFreeMap, free, no API key) showing the buyer's own location and live sellers within 1 km. Tap a pin to open the seller.
- **Nearby list** with distances.
  - Sellers that are offline, or silent for 10+ minutes (app killed), are hidden.
- **Seller detail**: status text, menu with stock, quantity steppers, "Pesan · RpX".
- **Order screen**: live status, walking directions (opens Google Maps), cancel while pending.
- **Pesanan saya**: list of the buyer's orders.
- **Notification** "Pesananmu siap diambil!" when an order turns ready, while the app is open.

### Backend / security
- Firestore Security Rules for `users`, `vendors`, `vendors/*/menu`, `orders`. No test-mode rules.
  - Only the buyer and seller of an order can read it.
  - Buyers can only create their own pending orders and cancel them while still pending.
  - Only the seller moves an order forward.
- No secrets in git: `google-services.json` and `local.properties` are ignored.
- Unit tests: 12, all passing (`./gradlew test`): distance/1 km filter, stale sellers, order totals, stock check, order-ready detection, parsing the AI's answer, rupiah/distance formatting.

## Tested on the emulator (2026-11-19)
- Ordering end to end: order → accept (stock 20 → 18) → ready → completed; buyer cancel; out-of-stock message.
- Voice: typed "cilok tinggal lima, es teh tambah sepuluh, saya lagi di depan SD Harapan" → correct proposal in about 3 s → saved → buyer sees new stock and status.
- Stale seller hidden after 10 min; shown again after a fresh update.
- Order-ready notification shown.

## Known limits
- **Notification only while the app is open.** Android cuts network for apps in the background, so the listener can't hear the change. A push while the app is closed needs FCM + Cloud Functions (Blaze billing plan).
- **App Check is not enforced** for Firebase AI Logic (turned off for the demo). Add App Check (debug provider + Play Integrity) before anyone outside the team uses the app.
- `gemini-3.8-flash` did not answer (timed out) on 2026-11-19, so the app uses `gemini-3.5-flash-lite` with minimal thinking and a 30 s timeout.
- Real speech is untested: the emulator has no microphone (the Indonesian speech screen does open).
- Two real phones at once are untested.

## Next steps (from 2026-11-20)
1. **Real-phone test.** Phone = seller (real GPS + mic), emulator = buyer moved next to the phone with `adb emu geo fix`. Install on both with `.\gradlew installDebug`.
   - Check Go Live, real voice update, order → accept → ready → notification.
2. Fix whatever the phone test finds.
3. Optional: App Check.
4. Record the demo video.

## Setup notes for the team
- Deploy rules after changing `firestore.rules`: `firebase deploy --only firestore:rules`.
- Emulator DNS can break; start it with `-dns-server 8.8.8.8`.
- Test accounts exist for one seller ("Cilok Pak Budi") and buyers ("Rina", "Dewi"); ask the team for the logins (not stored in the repo).
