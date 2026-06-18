# ZipFlash - Android Module Management App

## Overview

ZipFlash is an Android application that enables system-level module management and app manipulation without requiring traditional root access. It leverages **Shizuku** (privilege delegation framework) for elevated operations and supports a limited mode for devices without root privileges.

## Key Features

- 🔧 **Module Management** — Install, remove, and manage system modules
- 🔐 **Shizuku Integration** — Root-like capabilities without rooting your device
- 📱 **App Manager** — View and manage installed applications with detailed information
- 🛡️ **Limited Mode** — Full functionality even without elevated permissions
- 📡 **Shell Access** — Execute commands with proper privilege handling
- 🎨 **Multi-language Support** — Localized interface
- 🌓 **Dark/Light Theme** — Customizable UI appearance

## Technical Specs

| Aspect | Details |
|--------|---------|
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 36 (Android 15) |
| **Language** | Java |
| **Build System** | Gradle |
| **Current Version** | 1.0.0.9 |

## Project Structure

```
ZipFlash/
├── app/                          # Main application module
│   ├── src/main/java/           # Java source code
│   │   └── com/zipflash/mrp/
│   │       ├── MainActivity.java           # Main entry point
│   │       ├── ModuleManagerActivity.java  # Module management UI
│   │       ├── Shell.java                  # Shell command execution
│   │       ├── helper/
│   │       │   ├── UpdateChecker.java      # Update mechanism
│   │       │   ├── AppManager.java         # App listing & info
│   │       │   ├── PermissionHelper.java   # Permission handling
│   │       │   └── CheckPerm.java          # Shizuku permission check
│   │       └── ...
│   ├── src/main/res/            # Resources (layouts, strings, drawables)
│   ├── AndroidManifest.xml      # App manifest
│   └── build.gradle             # App module config
├── gradle/                       # Gradle wrapper
├── build.gradle                 # Root project config
├── gradlew / gradlew.bat       # Gradle wrapper scripts
└── settings.gradle             # Gradle settings

```

## Dependencies

- **AndroidX** — Modern Android support libraries
- **Material Design** — UI components and theming
- **Shizuku** — Privilege delegation framework
- **RecyclerView** — Efficient list rendering
- **SwipeRefreshLayout** — Pull-to-refresh functionality

## Getting Started

### Build Requirements
- Java 17+
- Android SDK 36
- Gradle 8.x

### Build the App

```bash
cd ZipFlash
./gradlew assembleDebug      # Debug build
./gradlew assembleRelease    # Release build (requires signing setup)
```

For detailed release build instructions, see [RELEASE_BUILD_GUIDE.md](RELEASE_BUILD_GUIDE.md).

### Installation

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Current Status

✅ **Functional** — All core features working  
⚠️ **Not Play Store Ready** — Custom updater violates Play Store policy  
🔄 **Before Release** — Requires signing configuration and updater removal

## Known Issues & Limitations

1. **Custom Updater** — Current implementation downloads APKs from external sources (not compliant with Play Store policy)
2. **Permissions** — Requires `REQUEST_INSTALL_PACKAGES` and `QUERY_ALL_PACKAGES` (sensitive permissions)
3. **Storage** — Uses legacy external storage APIs; needs migration to scoped storage for modern Android

## Play Store Preparation

Before publishing to Google Play:
- [ ] Remove or disable custom updater
- [ ] Replace with Google Play In-App Updates API
- [ ] Ensure all permissions are justified
- [ ] Complete app content rating and privacy policy
- [ ] Test thoroughly on various Android versions

See [RELEASE_BUILD_GUIDE.md](RELEASE_BUILD_GUIDE.md) for step-by-step Play Store submission process.

## Contributing

To contribute improvements:
1. Test on multiple Android versions (API 26-36)
2. Follow existing code style
3. Document any new features
4. Ensure ProGuard rules are updated for release builds

## License

(Specify your project license here)

## Support

For issues, feature requests, or technical questions, refer to the project documentation or contact the development team.

---

**Version**: 1.0.0.9  
**Last Updated**: 2026-06-18
