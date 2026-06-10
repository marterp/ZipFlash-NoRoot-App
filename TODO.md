# ZipFlash 2026 Modernization

## Phase 1: Foundation — Build Tooling + ViewBinding

- [x] 1.1 Convert root `build.gradle` → `build.gradle.kts`
- [x] 1.2 Convert app `build.gradle` → `build.gradle.kts`
- [x] 1.3 Create `gradle/libs.versions.toml` with all dependencies
- [x] 1.4 Enable ViewBinding in app build config
- [x] 1.5 Add lifecycle + coroutines + navigation dependencies
- [x] 1.6 Enable configuration cache (`gradle.properties`)
- [x] 1.7 Add lint baseline (`lint-baseline.xml`)
- [x] 1.8 ViewBinding: Convert `Welcome.java`
- [x] 1.9 ViewBinding: Convert `LaunchPerm.java`
- [x] 1.10 ViewBinding: Convert `CheckPerm.java`
- [x] 1.11 ViewBinding: Convert `MainActivity.java`
- [x] 1.12 ViewBinding: Convert `Shell.java`
- [x] 1.13 ViewBinding: Convert `Modules.java`
- [x] 1.14 ViewBinding: Convert `ModuleManagerActivity.java`
- [x] 1.15 ViewBinding: Convert `SettingsActivity.java`
- [x] 1.16 ViewBinding: Convert `About.java`
- [x] 1.17 Verify: `./gradlew assembleDebug` passes

---

## Phase 2: Navigation — Fragment + NavHost

- [x] 2.1 Create `nav_graph.xml` with all destinations
- [x] 2.2 Create fragment layouts (9 screens)
- [x] 2.3 Refactor host `MainActivity` for NavHost + DrawerLayout
- [x] 2.4 Convert `FlashFragment` from MainActivity flash UI
- [x] 2.5 Convert `ShellFragment` from Shell activity
- [x] 2.6 Convert `ModulesFragment` from Modules activity
- [x] 2.7 Convert `ModuleManagerFragment` from ModuleManagerActivity
- [x] 2.8 Convert `SettingsFragment` from SettingsActivity
- [x] 2.9 Convert `AboutFragment` from About activity
- [x] 2.10 Convert `WelcomeFragment` from Welcome activity
- [x] 2.11 Convert `LaunchPermFragment` from LaunchPerm activity
- [x] 2.12 Convert `CheckPermFragment` from CheckPerm activity
- [x] 2.13 Wire DrawerLayout navigation to NavHost
- [x] 2.14 Clean up AndroidManifest (remove old activity registrations)
- [x] 2.15 Verify: `./gradlew assembleDebug` passes

---

## Phase 3: Kotlin Migration

- [x] 3.1 Add Kotlin plugin + stdlib
- [x] 3.2 Convert `AppInfo.java` → `AppInfo.kt`
- [x] 3.3 Convert `FileHelper.java` → `FileHelper.kt`
- [x] 3.4 Convert `PermissionHelper.java` → `PermissionHelper.kt`
- [x] 3.5 Convert `CheckPermHelper.java` → `CheckPermHelper.kt`
- [x] 3.6 Convert `SettingsHelper.java` → `SettingsHelper.kt`
- [x] 3.7 Convert `ShellHelper.java` → `ShellHelper.kt`
- [x] 3.8 Convert `ScriptRunner.java` → `ScriptRunner.kt`
- [x] 3.9 Convert `ZipExtractor.java` → `ZipExtractor.kt`
- [x] 3.10 Convert `AppManager.java` → `AppManager.kt`
- [x] 3.11 Convert `UpdateChecker.java` → `UpdateChecker.kt`
- [x] 3.12 Convert `ModuleManager.java` → `ModuleManager.kt`
- [x] 3.13 Convert `AppsAdapter.java` → `AppsAdapter.kt`
- [x] 3.14-22 Convert all 9 fragments Java → Kotlin
- [x] 3.23 Convert `BaseActivity.kt`
- [x] 3.24 Convert host `MainActivity.kt`
- [x] 3.25 Verify: zero `.java` files remain, build passes

---

## Phase 4: Architecture — ViewModel + Coroutines

- [x] 4.1 Create `FlashViewModel`
- [x] 4.2 Create `ShellViewModel`
- [x] 4.3 Create `ModulesViewModel`
- [x] 4.4 Create `ModuleManagerViewModel`
- [x] 4.5 Create `SettingsViewModel`
- [x] 4.6 Wire ViewModels to Fragments via `by viewModels()`
- [x] 4.7 Replace ExecutorService with `viewModelScope.launch`
- [x] 4.8 Replace raw Thread with coroutines
- [x] 4.9 Replace `runOnUiThread` with `StateFlow.collect`
- [x] 4.10 Verify: no leaked threads, state survives config changes

---

## Phase 5: Networking — Retrofit + OkHttp

- [x] 5.1 Create `ZipFlashApi` Retrofit interface
- [x] 5.2 Create `ApiClient` singleton
- [x] 5.3 Refactor `ModulesFragment`/`ModulesViewModel` to use Retrofit
- [x] 5.4 Refactor `UpdateChecker` to use Retrofit
- [x] 5.5 Verify: module listing + update check still works

---

## Phase 6: Modern Android 2026 Features

- [x] 6.1 SplashScreen API
- [x] 6.2 Predictive Back Gesture
- [x] 6.3 Photo Picker (replace storage permission)
- [x] 6.4 Notification Permission runtime handling
- [x] 6.5 Custom Tabs (for external links)
- [x] 6.6 App Shortcuts (Flash, Shell, Modules)
- [x] 6.7 Share Sheet integration

---

## Phase 7: Testing

- [x] 7.1 Add JUnit 5 + MockK + Turbine deps
- [x] 7.2 Unit test FileHelper
- [x] 7.3 Unit test ShellHelper
- [x] 7.4 Unit test SettingsHelper
- [x] 7.5 Unit test FlashViewModel
- [x] 7.6 Unit test ModulesViewModel
- [x] 7.7 Unit test ZipFlashApi (MockWebServer)
- [ ] 7.8 UI test: flash flow (requires device/emulator)
- [ ] 7.9 UI test: drawer navigation (requires device/emulator)

---

## Phase 8: CI + Cleanup

- [x] 8.1 GitHub Actions (lint + build + test)
- [x] 8.2 Remove `drawable-v24/` (minSdk = 26) — already clean
- [x] 8.3 Clean up unused resources
- [x] 8.4 ProGuard rules for new libraries — already in place
- [x] 8.5 Verify release APK with minification
