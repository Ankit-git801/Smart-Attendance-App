# 📱 AttendWise App ✨📚

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4+-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack--Compose-orange.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-red.svg)](https://firebase.google.com/)

**Offline-first, cloud-synced, modern Android app** designed to help students master their attendance, hit academic targets, and manage complex weekly schedules—built with Kotlin and 100% Jetpack Compose.

⚡ **Why AttendWise?**
Managing attendance requirements across multiple subjects is a mental burden. AttendWise provides a professional, automated solution that works fully offline, featuring smart reminders, atomic cloud backup, and precision mathematical analytics.

---

## 🌟 Highlights

- 🎯 **Target-Driven Tracking** → Set subject goals and let the app handle the math.
- ☁️ **Atomic Cloud Sync** → Real-time Firestore synchronization with transactional data restoration.
- 🔐 **Secure Authentication** → Firebase-powered Email/Password login with proprietary data protection.
- 🗓️ **Interactive Calendar** → Comprehensive history view with month-navigation and holiday markers.
- 📉 **Precision Bunk Analysis** → Uses direct mathematical formulas (O(1) complexity) to calculate exactly how many classes you can skip or must attend.
- 🌴 **Smart Holiday Management** → One-tap holiday toggle with automatic cloud cleanup and alarm rescheduling.
- ⏰ **Reliable Reminders** → Exact alarms and actionable notifications that survive reboots and aggressive battery optimizations.
- 🚀 **In-App Update System** → Remote-controlled "Force Update" mechanism to ensure all users are on the latest version.
- ✨ **Modern UX** → Glassmorphism (Haze), fluid animations, haptic feedback, and AMOLED support.
- 🛡️ **IP Protected** → Explicit copyright headers and proprietary licensing.

---

## 🛠 Tech Stack (2026 Edition)

- **UI Layer:** Jetpack Compose (Material 3), Compose Animation API, Haze (Glassmorphism).
- **Architecture:** MVVM + StateFlow + Kotlin Coroutines (Reactive UI).
- **Local Storage:** Room Persistence Library (v2.8.4) with atomic transactions.
- **Backend:** Firebase Authentication & Cloud Firestore (NoSQL).
- **Preferences:** Jetpack DataStore (v1.2.1).
- **Gradle:** AGP 9.3.1 + Kotlin 2.4.10 + KSP.
- **Reminders:** AlarmManager (Exact Alarms) + Broadcast Receivers + Boot Compatibility.

---

## 🏗 System Design & Architecture

- **Transactional Restoration:** A robust cloud-to-local restoration engine that uses database transactions to ensure data integrity during account recovery.
- **Optimized Calculation Engine:** Replaced iterative simulations with closed-form mathematical models for real-time bunk analysis calculations.
- **Service Efficiency:** Smart `AlarmReceiver` logic that prevents unnecessary foreground service starts on holidays, significantly improving battery life.
- **Dual-Mode Updates:** Backend-controlled update logic allowing for either "Optional" or "Mandatory" application upgrades.
- **Manufacturer Guidance:** Custom logic to guide users through aggressive battery optimization settings on specific devices (OnePlus, Oppo, etc.).

---

## 📲 Core Screens

- **Home** → Dynamic "Today's Schedule," real-time attendance actions, and holiday status.
- **Statistics** → Reactive, flicker-free dashboard showing overall and subject-specific performance.
- **Calendar** → Visual month-by-month history with detailed day-view dialogs.
- **Subject Details** → Deep-dive history, schedule management, and manual record backfilling.
- **Settings** → Cloud backup status, theme customization, and user feedback portal.

---

## 🔑 Permissions & Security

- `POST_NOTIFICATIONS`: Android 13+ support for attendance reminders.
- `SCHEDULE_EXACT_ALARM`: High-precision class-end notification triggers.
- `RECEIVE_BOOT_COMPLETED`: Automatic scheduling engine restoration after device restarts.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Critical for reliable foreground tasks.

---

## 🚀 Deployment & Distribution

### **Sharing with Users (Pre-Play Store)**
1. Generate a **Signed Release APK** using your production Key Store.
2. Upload to Google Drive and share the link.
3. Once published to Play Store, update the `app_metadata/version_info` in Firebase to trigger the "Force Update" for all Drive users.

---

## 📄 License
Copyright (c) 2026 Ankit. All rights reserved.  
**Proprietary and Confidential.** Unauthorized copying, modification, or distribution of this software is strictly prohibited.
