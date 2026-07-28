# 📱 SmartAttendance

**SmartAttendance** is an **offline-first, cloud-synced attendance management app** built with **Kotlin** and **Jetpack Compose**.  
It helps students manage subject schedules, track attendance, analyse progress against target attendance, and receive reliable class reminders — all from a modern Android UI.

---

## ✨ Why this project stands out

This project is more than a basic CRUD app. It combines:

- **Local-first data storage** with Room for speed and reliability
- **Cloud sync** with Firebase Authentication + Cloud Firestore
- **Smart attendance analytics** with per-subject target tracking
- **Exact class reminders** that survive reboot and app updates
- **Calendar-based attendance history** with clear visual status
- **A polished Compose UI** with theme support, haptics, and modern UX

For recruiters, this shows real-world Android engineering, state management, persistence, background work, and product thinking.

---

## 🔥 Key Features

- **Subject Management**
  - Add, edit, and delete subjects
  - Set a target attendance percentage for each subject
  - Assign subject colours for better visual grouping

- **Weekly Schedule Builder**
  - Create weekly class schedules for each subject
  - View the full week in a clean schedule screen
  - Automatically map schedules to today’s classes

- **Attendance Tracking**
  - Mark attendance as **Present**, **Absent**, or **Cancelled**
  - Add manual attendance entries for past dates
  - Prevent duplicate records for the same class slot

- **Smart Bunk Analysis**
  - Shows how many classes can be missed safely
  - Shows how many classes must be attended to recover below-target attendance

- **Statistics Dashboard**
  - Overall attendance summary
  - Subject-wise breakdown
  - Visual progress indicators for quick understanding

- **Calendar View**
  - Month-wise attendance history
  - Colour-coded status for each day
  - Quick access to records and day-level details

- **Holiday Management**
  - Mark a date as holiday
  - Automatically clear conflicting attendance entries
  - Pause/remap class handling for holiday dates

- **Reminders & Notifications**
  - Exact class alarms
  - Notification actions for attendance updates
  - Boot-aware scheduling so reminders continue after restart

- **Cloud Sync & Restore**
  - Firebase Email/Password authentication
  - Sync subjects, schedules, attendance records, and profile data
  - Restore cloud data after login or reinstall
  - Clear local + cloud data safely on logout or delete

- **Personalisation**
  - Light, dark, and system theme support
  - Onboarding flow with user name setup
  - Haptic feedback for a smoother UX

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Architecture:** MVVM, Kotlin Coroutines, StateFlow
- **Local Database:** Room
- **Preferences:** DataStore
- **Cloud Backend:** Firebase Authentication, Cloud Firestore
- **Navigation:** Navigation Compose
- **Calendar UI:** Kizitonwose Calendar for Compose
- **Background Work:** AlarmManager, Broadcast Receivers, Foreground Service
- **UI Polish:** Haze glassmorphism effect, haptics, splash screen, edge-to-edge layout

---

## 🧠 Architecture Overview

The app follows a **local-first + cloud-sync** approach:

1. User actions are stored in **Room** for instant offline access.
2. Important data is synchronised to **Firestore** under the signed-in user.
3. On login, the app can restore cloud data back into the local database.
4. Notifications and alarms are scheduled locally, with boot recovery support.

This makes the app fast, offline-capable, and reliable.

---

## 📦 Data Model

The app is built around three main entities:

- **Subject**  
  Stores subject name, colour, attendance target, and timestamps.

- **ClassSchedule**  
  Stores day of week and class start/end time for each subject.

- **AttendanceRecord**  
  Stores attendance status, type, note, date, and sync metadata.

Room relationships and joined queries are used to calculate:
- subject-wise attendance
- overall attendance
- schedule-based attendance views
- bunk analysis

---

## 📲 Main Screens

- **Onboarding** – user introduction and setup
- **Home** – today’s schedule, attendance shortcuts, and greeting
- **Add / Edit Subject** – subject details and weekly schedule creation
- **Calendar** – monthly attendance history
- **Statistics** – overall and subject-level analytics
- **Subject Detail** – deep subject-level attendance tracking and bunk analysis
- **Weekly Schedule** – all classes grouped by day
- **Settings** – theme, account actions, permissions, and sync controls

---

## 🔐 Authentication & Cloud Sync

The app uses **Firebase Email/Password Authentication**.  
After login:

- user profile name is restored from cloud
- local data is cleared to avoid duplicates
- subjects, schedules, and attendance records are restored from Firestore

This helps the app behave consistently across reinstalls or multiple devices.

---

## ⏰ Permissions Used

The app uses these Android permissions to support reminders and reliability:

- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `USE_FULL_SCREEN_INTENT`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`

---

## 🚀 Getting Started

### 1) Clone the repository
```bash
git clone <your-repo-url>
```

### 2) Open in Android Studio
Open the project folder in **Android Studio** and wait for Gradle sync to finish.

### 3) Add Firebase configuration
- Create a Firebase project
- Enable **Authentication (Email/Password)**
- Enable **Cloud Firestore**
- Download `google-services.json`
- Place it inside the `app/` directory

### 4) Run the app
Build and run the app on a device or emulator.

---

## 📁 Project Structure

```text
com.ankit.smartattendance
├── data/          # Room entities, DAO, database, sync, preferences
├── models/        # Joined UI models and computed models
├── receivers/     # Alarm and notification receivers
├── services/      # Reminder foreground service
├── ui/            # Compose screens and theme
├── utils/         # Notifications, alarms, haptics
└── viewmodel/     # AppViewModel and app state logic
```

---

## 💡 What Recruiters Can Learn From This Project

This project demonstrates:

- Android app architecture with **MVVM**
- Local persistence using **Room**
- Cloud integration using **Firebase Auth + Firestore**
- Scheduling and background execution with **AlarmManager**
- Complex UI state handling with **Compose**
- Real attendance calculations and analytics
- Reliable user experience across reboots and app updates

---

## 🛣 Future Scope

Possible next upgrades:
- Google Sign-In
- Timetable import from image
- AI-based attendance advice
- Multi-device conflict resolution
- Push notifications via FCM
- Export to PDF / CSV
- More advanced analytics and predictions

---

## 📄 License

This project is available under the **MIT License**.
