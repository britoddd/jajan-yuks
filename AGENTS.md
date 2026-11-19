# JajanYuks — Project Guide for AI coding agents

## What this app is
JajanYuks connects mobile street-food vendors (UMKM: cilok, bakso, drinks) with nearby buyers in real time.
- One Android app, two account types: **seller** and **buyer**.
- Seller: goes live → shares live location, menu, remaining stock. Can update status by voice (Indonesian).
- Buyer: sees live vendors nearby on a map, orders remotely, walks to the vendor to pick up.

## Team context
- The team is NEW to Kotlin. When you write code, explain non-obvious Kotlin/Compose concepts in 1–3 short sentences.
- Prefer simple, readable code over clever code. No advanced patterns unless needed.
- Make small changes. One feature per task. Run the build after each change.

## Stack
- Language: Kotlin. UI: Jetpack Compose + Material 3.
- Min SDK 26. Target SDK: latest stable.
- Build: Gradle Kotlin DSL (`build.gradle.kts`) + version catalog (`gradle/libs.versions.toml`). Always add dependencies through the version catalog. Use current stable versions.
- Architecture: MVVM. `ViewModel` + `StateFlow` for UI state. Coroutines for async work.
- Dependency injection: manual (simple `AppContainer` class). Do NOT add Hilt unless the team asks.
- Navigation: Navigation Compose.
- Maps: Google Maps SDK + `maps-compose`.
- Location: `FusedLocationProviderClient` inside a foreground service (type `location`).
- Backend: Supabase (Auth, Postgres + PostGIS, Realtime) through `supabase-kt`, with Ktor client and kotlinx.serialization.
- Speech: Android `SpeechRecognizer` with locale `id-ID`. The text goes to a backend function that returns structured JSON.

## Package structure
```
com.amdg.jajanyuks
├── data/          # repositories, Supabase calls, data models (DTOs)
├── domain/        # plain Kotlin models used by the UI (Vendor, MenuItem, Order)
├── location/      # foreground service and location helpers
├── speech/        # speech recognition helpers
├── ui/
│   ├── seller/    # seller screens + ViewModels
│   ├── buyer/     # buyer screens + ViewModels
│   ├── auth/      # login / register
│   ├── components/# shared composables
│   └── theme/
└── AppContainer.kt
```

## Rules
- Never commit secrets. Google Maps API key and Supabase URL/key go in `local.properties` and are read through the Secrets Gradle Plugin / `BuildConfig`.
- Supabase: every table must have Row Level Security (RLS) enabled. A seller can only edit their own vendor row.
- Do NOT store KTP images or KTP numbers in the prototype. Use phone/email auth.
- Location updates: send at most every 10–20 seconds or after ~20 m of movement. Not every second.
- Start the location foreground service only when the seller taps "Go Live" while the app is visible. Request only "while using the app" location permission.
- Strings shown to users: Indonesian, in `res/values/strings.xml`. Code and comments: English.

## Commands
- Build: `./gradlew assembleDebug`
- Install on connected phone: `./gradlew installDebug`
- Unit tests: `./gradlew test`
- Lint: `./gradlew lint`

## Current milestone (Week 1)
Seller taps "Go Live" → location is sent to Supabase → buyer map shows the seller pin moving in real time.
Do this end-to-end before other features.
