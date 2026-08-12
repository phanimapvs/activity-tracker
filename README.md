<h1 align="center">
  Activity Tracker 🧘‍♂️📱
</h1>

<p align="center">
  <strong>A local-first, privacy-conscious Android app to track your lifestyle, routines, and screen time.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-100%25-blue.svg?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-green.svg?logo=android" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Architecture-MVVM-orange.svg" alt="MVVM">
  <img src="https://img.shields.io/badge/Privacy-Local_First-success.svg" alt="Privacy">
</p>

---

## 📖 Overview

**Activity Tracker** is designed for individuals who want deep insights into their daily routines—without compromising their privacy. This app is strictly local-first and completely free of tracking pixels, generic cloud analytics, or unnecessary permissions. 

Whether you're trying to track your sleep schedule, monitor mobile screen usage, or ensure you maintain an "Empty Stomach" window for spiritual/yogic practices (Sadhana), Activity Tracker gives you all the tools in a beautiful, modern interface.

## ✨ Key Features

- **🧘‍♂️ Practice Readiness & Empty Stomach Tracking:** Calculate exactly when your stomach will be optimally empty based on your last meal (light snack vs. full meal) so you can safely perform your Sadhana or exercises.
- **📱 Native Screen Time Monitoring:** Connects directly with Android's `UsageStatsManager` via background workers to accurately track your daily screen time, keeping all usage data securely on your device.
- **🛌 Advanced Sleep & Activity Logging:** Track sleep cycles, work blocks, meals, and custom activities seamlessly from an intuitive "Quick Actions" dashboard.
- **📊 Daily Stats & Timeline:** Review your day at a glance. See a full chronological timeline of your activities alongside completion ratios for your planned daily goals.
- **🔒 100% Privacy Focused:** No cloud sync, no analytics, no external servers. Your data belongs to you and lives strictly within a local Room SQLite Database.

## 🛠 Tech Stack & Architecture

Built with modern Android development standards to ensure scalability, performance, and maintainability:

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Dependency Injection:** [Dagger Hilt](https://dagger.dev/hilt/)
- **Local Persistence:** [Room Database](https://developer.android.com/training/data-storage/room) & [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
- **Background Processing:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for automated, localized Screen Time Syncing.
- **Navigation:** Compose Navigation with Type-Safe Routing modularized by feature.

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Giraffe or newer recommended)
- **JDK 17+** 

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/phanimapvs/activity-tracker.git
   ```
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Run the `app` configuration on your connected device or emulator.

*(Note: On first launch, you will need to grant "Usage Access" in Android Settings to enable Screen Time Tracking).*

## 🤝 Contributing

This is a personal project, but suggestions, bug reports, and pull requests are always welcome! Ensure any major architectural changes are discussed via an Issue first.

---
*Built with intention for a focused life.*
