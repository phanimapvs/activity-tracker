# Fix Gradle/AGP Incompatibility

The project is failing to build because the current Android Gradle Plugin (AGP) version (reported as 8.13.2 in the error) is incompatible with Gradle 9.6.0+. Specifically, it relies on an internal Gradle API (`InternalProblems`) that was removed in Gradle 9.6.0.

Although the `gradle-wrapper.properties` specifies Gradle 9.5.1, the error message suggests that a newer version of Gradle is being used (possibly 9.6.1), or that the current plugin version is explicitly incompatible with the Gradle environment.

## Proposed Changes

### Gradle Configuration

#### [MODIFY] [gradle-wrapper.properties](file:///D:/Projects/Activity Tracker/gradle/wrapper/gradle-wrapper.properties)
Update the Gradle distribution to version 9.5 as recommended in the error message to ensure compatibility with AGP 8.x/9.x.

#### [MODIFY] [libs.versions.toml](file:///D:/Projects/Activity Tracker/gradle/libs.versions.toml)
Ensure the `androidGradlePlugin` version is set to a stable, compatible version. We will keep it at `9.3.1` (or adjust if necessary) but ensure it's being correctly applied.

## Verification Plan

### Automated Tests
- Run `.\gradlew help` to verify that the project can now be initialized without the plugin error.
- Run `.\gradlew assembleDebug` to verify the build process.
