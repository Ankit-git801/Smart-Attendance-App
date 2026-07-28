# 📱 AttendWise App ✨📚

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack--Compose-orange.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-red.svg)](https://firebase.google.com/)

**Offline-first, cloud-synced, modern Android app** to track class attendance, hit academic targets, and manage schedules—built with Kotlin and 100% Jetpack Compose.

⚡ **Why this project?**
Most students struggle with tracking attendance against university requirements. This app provides reliable, automated tracking that works fully offline, with smart reminders, real-time cloud backup, and advanced progress analytics.

---

## 🌟 Highlights

- 🎯 **Target-Driven Tracking** → Subject-wise goals with animated progress bars & visual analytics.
- ☁️ **Real-Time Cloud Sync** → Instant Firestore synchronization for subjects, schedules, and records.
- 🔐 **Hybrid Authentication** → Secure Email/Password login with automated background data restoration.
- 🗓️ **Interactive Calendar** → New month-navigation arrows with color-coded history (Green: Present, Red: Absent, Gray: Cancelled).
- 🔴 **"LIVE" State Engine** → Real-time identification of active classes based on system time and schedules.
- 🌴 **Holiday Management** → One-tap holiday toggle that intelligently clears data conflicts across local and cloud.
- ⏰ **Reliable Reminders** → Exact alarms + actionable notifications that survive reboots and battery optimizations.
- 👋 **Personalized Onboarding** → Welcoming name-based setup with an option to restore existing cloud accounts.
- ✨ **Modern UX** → AMOLED black theme, glassmorphism effects, fluid animations, and haptic feedback.
- 🛡️ **Safe Operations** → Integrated confirmation dialogs for all destructive actions (Delete/Sign-out/Clear).

---

## 🛠 Tech Stack

- **UI Layer:** Jetpack Compose (Material 3), Compose Animation API, Haze (Glassmorphism).
- **Architecture:** MVVM + StateFlow + Kotlin Coroutines (Reactive UI flow).
- **Local Storage:** Room Persistence Library (v2.6.1) with Offline-first architecture.
- **Backend:** Firebase Authentication & Cloud Firestore (NoSQL).
- **Preferences:** Jetpack DataStore (v1.1.1).
- **Navigation:** Jetpack Navigation Compose (v2.7.7).
- **Reminders:** AlarmManager (Exact Alarms) + Broadcast Receivers + Boot Compatibility.
- **Calendar UI:** Kizitonwose Calendar for Compose.

---

## 🏗 System Design & Architecture

- **Distributed Data Logic:** A multi-layered strategy where the app operates on Room for offline speed and propagates changes to Firestore for cloud reliability.
- **Automated State Restoration:** Engineering a silent restoration process that rebuilds the local environment immediately upon login.
- **Bunk Analysis Algorithm:** Sophisticated mathematical model calculating exactly how many classes can be missed or must be attended to meet goals.
- **Boot-Resilient Scheduling:** Logic to re-register all exact alarms upon `BOOT_COMPLETED` and app updates.
- **Firestore Integrity:** Custom PropertyName mapping to ensure complex Boolean states (isPresent) survive cloud serialization.

---

## 📲 Core Screens

- **Home** → Personalized greeting, dynamic "Today's Schedule," and one-tap attendance markers.
- **Calendar** → Visual month-by-month history with detailed day-view dialogs.
- **Statistics** → Comprehensive breakdown of subject performance vs. targets.
- **Subject Details** → Deep-dive history, schedule editing, and manual backfill options.
- **Settings** → Cloud account management, theme customization, and system permission checks.

---

## 🔑 Permissions Used

- `POST_NOTIFICATIONS`: Android 13+ support for reminders.
- `SCHEDULE_EXACT_ALARM`: Ensuring notifications fire at the precise class end time.
- `RECEIVE_BOOT_COMPLETED`: Restoring the scheduling engine after device restarts.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Preventing the system from killing reminder tasks.

---

## 🚀 Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Ankit-git801/Smart-Attendance-App.git
   ```
2. **Firebase Setup:**
   - Create a Firebase project and enable **Email/Password Auth** and **Firestore**.
   - Download `google-services.json` and place it in the `/app` directory.
3. **Build:**
   - Sync Gradle and Run the application on a device or emulator.

---

## 📄 License
This project is licensed under the **MIT License**.
