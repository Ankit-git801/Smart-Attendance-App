Smart Attendance App ✨📚
An offline-first, modern Android app to track class attendance, hit targets, and get timely reminders — built 100% with Kotlin and Jetpack Compose.

— sleek UI - local-first data - smart reminders —

Highlights
🎯 Target-driven tracking with subject-wise goals, progress bars, and a donut chart for quick insight.

🗓️ Weekly schedules with day/time slots; home shows a clean daily view of classes.

✅ One-tap attendance from cards and notifications with Present/Absent actions.

📅 Holiday-aware calendar that marks present/absent/mixed days and supports toggle-to-holiday flow.

🔕 Offline-first Room database with MVVM + Flow; works entirely without internet.

⏰ Exact alarms + foreground notifications to never miss the marking window.

♻️ Boot-resilient alarm rescheduling and battery optimization prompts for reliable reminders.

Features
Subject management: add, edit, delete with custom colors and targets.

Attendance marking from Home, Subject detail calendar, and notification actions.

Extra class quick-mark dialog for ad-hoc sessions.

Manual attendance backfill for past totals (present/absent).

Holiday toggle that clears other records for the day and sets a holiday record.

Personalized theme (Light/Dark/System) and user name in greeting.

Battery optimization, exact alarm, and full-screen intent settings surfaced in-app.

Tech Stack 💻
Language: Kotlin (100% Kotlin)

UI: Jetpack Compose with Material 3 and icons, Compose BOM 2024.06.00, compiler extension 1.5.8

Architecture: MVVM with Android ViewModel, Coroutines, StateFlow, and unidirectional data flow

Local storage: Room (runtime/ktx/compiler 2.6.1) with TypeConverters and destructive migration fallback

Preferences: AndroidX DataStore (datastore-preferences 1.1.1)

Navigation: Navigation Compose 2.7.7

Calendar UI: Kizitonwose Calendar Compose 2.5.1

Permissions: Google Accompanist Permissions 0.34.0

Scheduling & notifications: AlarmManager, exact alarms, foreground Service, Notification channels, BroadcastReceivers, PendingIntent actions

Haptics and UX: Vibrator/VibrationEffect and animated indicators/badges

Build: Gradle 8.13, AGP 8.4.1, Kotlin 1.9.22, KSP 1.9.22-1.0.17, coreLibraryDesugaring 2.0.4

Android versions: minSdk 26, target/compile 34, JDK/bytecode 21

Testing: JUnit4, AndroidX test/junit/espresso, Compose UI test

System Design
Data layer: Room Entities (Subject, ClassSchedule, AttendanceRecord) with DAOs for Subjects, Schedules, and Records.

Domain logic: Attendance math (present/total) and percentage computations exposed via ViewModel methods.

Schedules: Day-of-week + start/end time; “current” and “completed” state derived using LocalTime.

Reminders: Weekly exact alarms at class end-time per schedule; rescheduled weekly and after boot.

Foreground notifications: Full-screen-capable high-priority channel with Present/Absent action buttons.

Holiday logic: A single HOLIDAY record per date; day tap requests confirmation to convert and remove other records.

Core Screens
Home: Greeting with name, quick actions (Extra Class/New Subject), today’s schedule cards with animated “LIVE” badge and mark controls.

Calendar: Horizontal calendar showing present/absent/mixed/holiday day coloring with today’s highlight and holiday toggle.

Stats: Donut chart, total/present/absent counts, and per-subject breakdown with color and target comparison.

Subject detail: Per-subject donut, target, and calendar for date-specific mark/clear (+ manual add and edit/delete subject).

Permissions Used
POST_NOTIFICATIONS for Android 13+ reminders.

RECEIVE_BOOT_COMPLETED to reschedule alarms on device restart.

SCHEDULE_EXACT_ALARM for precise class reminders.

VIBRATE for subtle haptic feedback.

REQUEST_IGNORE_BATTERY_OPTIMIZATIONS to ensure background delivery.

USE_FULL_SCREEN_INTENT for high-priority attendance prompts.

FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE for in-session notifications.

Notifications & Alarms
Exact weekly alarms per schedule via AlarmManager with allow-while-idle behavior.

Foreground service renders a persistent notification with action buttons to mark attendance.

Action receivers save attendance, update stats notification, and warn if below target.

Boot receiver enumerates subjects/schedules to reschedule all alarms after restart.

Data Model
Subject: id, name, color, targetAttendance.

ClassSchedule: id, subjectId, dayOfWeek, startHour:Minute, endHour:Minute.

AttendanceRecord: id, subjectId?, scheduleId?, date (epochDay), isPresent, note, type {CLASS, HOLIDAY, MANUAL}.

Getting Started 🚀
Open in Android Studio (latest) and let Gradle sync automatically.

Build and run on a device/emulator with API 26+.

On first launch, accept notifications and allow “ignore battery optimizations” for reliable reminders.

Add a subject, configure weekly schedules, set a target, and start marking attendance.

Developer Notes
Room db: AppDatabase v2 with fallbackToDestructiveMigration for development convenience.

ViewModel: Single source for flows: subjects, attendance records, subjectsWithAttendance, and today’s schedule with holiday awareness.

Exact alarms: Conditionally scheduled if allowed on Android 12+; legacy path for older devices.

Animations: Compose AnimatedVisibility, tween transitions, and custom progress arcs for delightful UX.

Build Config
Compose enabled with Kotlin compiler extension 1.5.8 and Compose BOM 2024.06.00.

Dependencies: lifecycle-runtime-ktx 2.8.3, activity-compose 1.9.0, core-ktx 1.13.1, accompanist-permissions 0.34.0.

Packaging excludes and resource namespace defaults applied.

Roadmap Ideas
Widgets and Glance-powered quick actions.

Cloud backup/sync while keeping offline-first behavior.

More granular stats (weekly/monthly trends and goal projections).

Privacy
All data is stored locally on-device; no network required for core functionality.

License
MIT License; see LICENSE file.

Credits
Built with love using Kotlin, Jetpack Compose, Room, and Android’s modern toolkit.
