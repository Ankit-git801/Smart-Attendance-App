# Smart Attendance App

A modern, offline-first Android app to track class attendance with reminders, weekly schedules, target percentages, and rich statistics—built entirely with Kotlin and Jetpack Compose. [Source-derived]

## Features
- Subject management: add, edit, delete with custom color and target attendance. [Source-derived]
- Weekly class scheduling with start/end times per day. [Source-derived]
- Daily schedule view with “live” and “completed” state visualization. [Source-derived]
- One-tap attendance marking from the home screen. [Source-derived]
- Detailed statistics: subject-wise totals, presents/absents, and overall percentage with donut/progress visuals. [Source-derived]
- Calendar views for holidays and per-subject daily history. [Source-derived]
- Holiday toggling that clears class records for the day. [Source-derived]
- Local-only data with Room; fully functional offline. [Source-derived]
- Class reminders with Notification actions (Present/Absent), exact alarms, and post-update alerts. [Source-derived]

## Tech Stack
- Language/Runtime: Kotlin, Coroutines, Flow. [Source-derived]
- UI: Jetpack Compose (Material 3) with Navigation Compose. [Source-derived]
- Data: Room (runtime, ktx, compiler via KSP), DataStore Preferences. [Source-derived]
- Scheduling/Notifications: AlarmManager exact alarms, Foreground Service, Notification channels. [Source-derived]
- Utilities: Accompanist Permissions, Kizitonwose Compose Calendar. [Source-derived]
- Android: minSdk 26, target/compileSdk 34. [Source-derived]

Key versions (from build files):
- Compose BOM: 2024.06.00, Compose compiler extension: 1.5.8. [Source-derived]
- Room: 2.6.1 (KSP compiler). [Source-derived]
- Accompanist-Permissions: 0.34.0, Navigation Compose: 2.7.7, DataStore: 1.1.1. [Source-derived]
- Desugaring: com.android.tools:desugar_jdk_libs:2.0.4. [Source-derived]
- Gradle: Android Gradle Plugin 8.4.1; Kotlin 1.9.22; Wrapper 8.13. [Source-derived]

## Architecture
- MVVM with a single AppViewModel coordinating Room DAO, preferences, and UI flows. [Source-derived]
- Reactive UI via StateFlow/Flow, Compose screens for Home, Calendar, Statistics, Settings, Subject Detail, and Add/Edit Subject. [Source-derived]
- Database entities: Subject, ClassSchedule, AttendanceRecord with cascade deletes; enum RecordType { CLASS, HOLIDAY, MANUAL }. [Source-derived]

## Permissions
Declared in AndroidManifest.xml:
- POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED. [Source-derived]
- SCHEDULE_EXACT_ALARM, VIBRATE, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS. [Source-derived]
- USE_FULL_SCREEN_INTENT, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE. [Source-derived]

The app requests:
- Notification permission (Android 13+). [Source-derived]
- Battery optimization exclusion (dialog + Settings intent). [Source-derived]
- Exact alarm capability (Android 12+ flow). [Source-derived]

## Reminders & Notifications
- Exact alarms scheduled per ClassSchedule (dayOfWeek, endHour, endMinute). [Source-derived]
- Foreground service displays a high-priority notification with Present/Absent actions. [Source-derived]
- Action broadcasts write AttendanceRecord, update stats, and optionally warn if below target. [Source-derived]
- BootReceiver re-schedules alarms after device reboot or package replace. [Source-derived]

## Getting Started
Prerequisites:
- Android Studio (latest), JDK 21 runtime in IDE, device/emulator API 26+. [Source-derived]

Build & Run:
1) Open the project in Android Studio and let it sync. [Source-derived]  
2) Build and run the “app” configuration on a device or emulator. [Source-derived]  
3) On first launch, grant notifications and consider allowing battery optimization exclusion and exact alarms for reliable reminders. [Source-derived]

## Data & Migrations
- AppDatabase uses fallbackToDestructiveMigration for development; bumps version to 2 as schema evolves. [Source-derived]
- AttendanceRecord foreign key cascades clean up child rows when a Subject is deleted. [Source-derived]

## License
Distributed under the MIT License (see LICENSE in the repository). [Source-derived]
