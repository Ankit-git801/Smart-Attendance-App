# Smart Attendance App

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blueviolet.svg)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-brightgreen.svg)

A modern, offline-first attendance tracking application for Android, built with 100% Kotlin and the latest Jetpack Compose toolkit. This app helps students monitor their class attendance, track their progress towards target percentages, and receive timely reminders for upcoming classes.

---

## 📸 Screenshots

*(**Instructions for you:** Take screenshots of your app's main screens and place them in a folder named `screenshots` in your project's root directory. Then, uncomment the lines below and update the paths.)*

| **Home Screen**                                       | **Add/Edit Subject Screen**                               |
| ----------------------------------------------------- | --------------------------------------------------------- |
| ![Home Screen](screenshots/home.png) | ![Add/Edit Screen](screenshots/add_edit.png) |

| **Subject Detail Screen**                             | **Statistics Screen**                                     |
| ----------------------------------------------------- | --------------------------------------------------------- |
| ![Detail Screen](screenshots/detail.png) | ![Stats Screen](screenshots/stats.png) |

---

## ✨ Features

-   **Subject Management:** Add, edit, and delete subjects with custom names and colors.
-   **Attendance Target:** Set a target attendance percentage for each subject.
-   **Class Scheduling:** Define a weekly schedule for each subject, specifying the day and time.
-   **Daily Schedule View:** The home screen provides a clear overview of the day's classes.
-   **Attendance Marking:** Easily mark attendance as "Present" or "Absent" directly from the home screen.
-   **Detailed Statistics:** View overall and subject-specific attendance statistics, including total classes, present/absent counts, and current percentage.
-   **Visual Progress:** A donut chart and progress bars provide a quick visual indication of your attendance status.
-   **Calendar View:** An in-app calendar visually displays your attendance history with color-coded dates for present, absent, or mixed attendance days.
-   **Offline First:** All data is stored locally on the device using a Room database, making the app fully functional without an internet connection.
-   **Class Reminders:** *(Experimental)* The app includes a notification system to remind users about upcoming classes.

---

## 🛠️ Tech Stack & Architecture

-   **Kotlin:** The entire application is written in 100% Kotlin.
-   **Jetpack Compose:** The UI is built entirely with Jetpack Compose, using Material 3 design components for a modern look and feel.
-   **MVVM Architecture:** The app follows the Model-View-ViewModel architecture pattern to ensure a clean separation of concerns and a scalable codebase.
-   **Room Database:** Used for local, persistent storage of all subjects, schedules, and attendance records.
-   **Coroutines & Flow:** Asynchronous operations and reactive data streams are handled using Kotlin Coroutines and StateFlow.
-   **Android ViewModel:** Manages UI-related data in a lifecycle-conscious way.
-   **Navigation Component:** Handles all in-app navigation between different screens.

---

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

-   Android Studio (latest version recommended)
-   An Android device or emulator running API level 26 or higher

### Installation

1.  **Clone the repository:**
    ```
    git clone https://github.com/Ankit-git801/Smart-Attendance-App.git
    ```
2.  **Open the project in Android Studio:**
    -   Open Android Studio.
    -   Click on `File` -> `Open`.
    -   Navigate to the cloned repository directory and select it.
3.  **Build the project:**
    -   Let Android Studio sync the Gradle files.
    -   Click on `Build` -> `Make Project` to build the application.
4.  **Run the app:**
    -   Select your target device (emulator or physical device).
    -   Click the `Run` button (▶️).

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
```

### **✅ STEP 2: Update Your `README.md` File**

1.  In your Android Studio project, find the `README.md` file in the project's root directory and open it.
2.  Delete any existing content in that file.
3.  **Paste** the content you copied from Step 1 into this file.
4.  Save the file.

### **✅ STEP 3: Push the New README to GitHub**

Now, let's update your GitHub repository with this new, professional README.

1.  Open the **Terminal** in Android Studio.
2.  **Stage the `README.md` file:**
    ```bash
    git add README.md
    ```
3.  **Commit the change with a message:**
    ```bash
    git commit -m "Docs: Create professional README file"
    ```
4.  **Push the commit to GitHub:**
    ```bash
    git push
    ```

Once the push is complete, refresh your GitHub repository page. You will now see your new, detailed README, which does a much better job of showcasing the hard work you've put into this project. Congratulations
