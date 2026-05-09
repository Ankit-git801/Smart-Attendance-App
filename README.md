# 📱 Smart Attendance App ✨📚

Offline-first, modern Android app to track class attendance, hit targets, and get timely reminders — built with **Kotlin** and **Jetpack Compose**.

---

## ⚡ Why this project?
Most students struggle with tracking attendance vs. university requirements. This app ensures reliable, automated attendance tracking that works fully offline, with smart reminders, holiday awareness, and progress analytics.

🚀 Built end-to-end using Android’s modern toolkit with a focus on clean architecture, offline resilience, and great UX.

---

## 🌟 Highlights

- 🎯 **Target-driven tracking** → Subject-wise goals with progress bars & donut charts.  
- 🗓️ **Weekly schedules** → Auto-generates class reminders & daily views.  
- ✅ **One-tap attendance** → From home cards or actionable notifications.  
- 📅 **Holiday-aware calendar** → Toggle holidays & auto-adjust stats.  
- 🔢 **Visual Summary** → Subject-specific calendar shows daily attendance counts (Green: Present, Red: Absent).
- 👋 **Personalized Onboarding** → Welcoming name-based setup on first launch.
- 🔕 **Offline-first Room DB** → Works entirely without internet.  
- ⏰ **Reliable reminders** → Exact alarms + foreground notifications (Auto-rescheduled for next week).
- ♻️ **Boot-resilient** → Alarms survive reboots & battery optimizations.  
- ✨ **Modern UX** → AMOLED black immersive bars, animated indicators, haptics.

---

## 🛠️ Tech Stack

- **Language**: Kotlin (100%)  
- **UI**: Jetpack Compose, Material 3, AnimatedVisibility, Donut Chart  
- **Architecture**: MVVM + ViewModel + StateFlow + Coroutines  
- **Local Storage**: Room (v2.6.1) + TypeConverters  
- **Preferences**: DataStore (1.1.1)  
- **Navigation**: Navigation-Compose (2.7.7)  
- **Calendar UI**: Kizitonwose Calendar Compose  
- **Reminders**: AlarmManager + Exact Alarms + Foreground Service  
- **Notifications**: Notification Channels, Action Buttons, Full-Screen Intents  
- **Permissions**: Accompanist Permissions (0.34.0)  
- **Build**: Gradle 8.13, Kotlin 1.9.22, AGP 8.4.1  
- **Testing**: JUnit4, Espresso, Compose UI Tests  

---

## 🏗️ System Design

- **Data Layer**: Room entities (Subject, ClassSchedule, AttendanceRecord) + DAOs.  
- **Domain Logic**: Attendance % computation, progress monitoring, holiday overrides.  
- **Schedules**: Day+time model with auto-status (LIVE, upcoming, completed).  
- **Reminders**: Weekly exact alarms, rescheduled on boot & app restarts.  
- **Notifications**: Foreground service with Present/Absent actions.  
- **Holiday Logic**: One HOLIDAY record per day → clears conflicts automatically.  

---

## 📲 Core Screens

- **Home** → Greeting, today’s schedule, quick mark attendance.  
- **Calendar** → Present/Absent/Holiday color-coded views.  
- **Stats** → Donut chart with subject breakdown & goal comparisons.  
- **Subject Detail** → Subject-specific calendar, manual backfill, edit/delete.  

---

## 🔑 Permissions Used

- `POST_NOTIFICATIONS` → Android 13+ reminders.  
- `RECEIVE_BOOT_COMPLETED` → Reschedule alarms after reboot.  
- `SCHEDULE_EXACT_ALARM` → Precise class reminders.  
- `USE_FULL_SCREEN_INTENT` → High-priority reminders.  
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → Reliable background delivery.  

---

## 🧑‍💻 Developer Notes

- **Architecture-first design**: MVVM with clean data flow.  
- **Offline-first**: No internet dependency.  
- **Performance**: Coroutines + Flows for reactive UI.  
- **UX polish**: Animations, haptics, and battery-aware notifications.  

---

## 🚀 Getting Started

```bash
git clone https://github.com/Ankit-git801/Smart-Attendance-App.git
