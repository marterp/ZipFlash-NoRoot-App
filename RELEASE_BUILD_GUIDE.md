# Release Build Guide for Google Play Store

This guide walks through creating a release build of ZipFlash for Google Play Store distribution.

## Prerequisites

### 1. Create or Obtain a Keystore File

If you don't have a keystore yet, create one:

```bash
keytool -genkey -v -keystore release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias zipflash_key
```

This will prompt you for:
- Keystore password
- Key password (can be same as keystore)
- Your name, organization, city, state, country

**Save this keystore securely** — you'll need it for all future Play Store updates.

### 2. Configure Signing in `app/build.gradle`

Add signing configuration to your `app/build.gradle` file (in the `android` block):

```gradle
android {
    // ... existing config ...

    signingConfigs {
        release {
            storeFile file("../release.keystore")  // Path to your keystore
            storePassword System.getenv("KEYSTORE_PASSWORD") ?: "your_keystore_password"
            keyAlias "zipflash_key"
            keyPassword System.getenv("KEY_PASSWORD") ?: "your_key_password"
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

**Security Note**: For CI/CD, use environment variables (`System.getenv()`) instead of hardcoding passwords.

## Building Release Artifacts

### Option 1: Build Release APK (Simple Distribution)

```bash
cd /workspaces/ZipFlash-NoRoot-App/ZipFlash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Option 2: Build Android App Bundle (Recommended for Play Store)

The App Bundle allows Google Play to optimize app size per device:

```bash
cd /workspaces/ZipFlash-NoRoot-App/ZipFlash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Pre-Release Testing

### 1. Test the Release Build Locally

```bash
# Install the release APK on a test device
adb install app/build/outputs/apk/release/app-release.apk
```

### 2. Test Key Features
- [ ] App launches without crashes
- [ ] Permissions flow works correctly
- [ ] Main functionality (file operations, module management) works
- [ ] No ProGuard obfuscation breaks critical code

### 3. Run ProGuard Mapping Check

Verify ProGuard didn't break important classes:

```bash
# Check the mapping file for any issues
cat app/build/outputs/mapping/release/mapping.txt | head -50
```

## Preparing for Play Store Upload

### 1. Update Version Code and Name

In `app/build.gradle`:

```gradle
defaultConfig {
    applicationId "com.zipflash.mrp"
    minSdk 26
    targetSdk 36
    versionCode 10  // Increment this for each release
    versionName "1.0.1"  // Update this
}
```

### 2. Review AndroidManifest.xml Changes

Before uploading, verify these critical permissions are justified:
- `REQUEST_INSTALL_PACKAGES` — Only if keeping custom updater (not recommended for Play Store)
- `QUERY_ALL_PACKAGES` — Must be declared if needed for app functionality
- `WRITE_SECURE_SETTINGS` — High-risk permission; ensure it's necessary

**Recommendation**: Remove or conditionally disable the updater for Play Store builds.

### 3. Create a Play Store Release Notes File

Create `PLAY_STORE_RELEASE_NOTES.txt`:

```
Version 1.0.1 Release Notes

✨ New Features:
- Improved module management UI
- Better error handling

🐛 Bug Fixes:
- Fixed crash on app startup
- Resolved permission issues on Android 13+

⚙️ Technical Updates:
- Updated dependencies
- Improved code performance
```

## Uploading to Google Play Console

### 1. Create a Google Play Developer Account
- Visit [Google Play Console](https://play.google.com/console)
- Pay the one-time $25 registration fee
- Set up your developer profile

### 2. Create a New App
1. In Play Console, click **Create app**
2. Enter app name: "ZipFlash"
3. Select category (Tools/System Tools)
4. Confirm you meet Play Store policies

### 3. Upload the Release Build
1. Navigate to **Release** → **Production** (or **Internal testing** first)
2. Click **Create new release**
3. Upload your `.aab` (App Bundle) or `.apk` file
4. Review and accept content rating questionnaire
5. Add release notes

### 4. Fill in App Details
- **App Title**: ZipFlash
- **Short Description**: One-line summary
- **Full Description**: Detailed explanation of what the app does
- **Screenshots**: 2-8 screenshots showing key features
- **App Icon**: 512x512 PNG
- **Feature Graphic**: 1024x500 PNG
- **Category**: Tools or System Tools

### 5. Configure Privacy Policy
- Go to **Policy** → **App privacy**
- Add your privacy policy URL or use a template
- Declare all permissions and data usage

### 6. Set Content Rating
1. Go to **Setup** → **App content**
2. Complete the IARC questionnaire
3. Receive content rating

### 7. Review Play Store Policies
- [ ] No unauthorized system changes outside scope
- [ ] Honest app description
- [ ] No deceptive permissions
- [ ] Proper data handling

## Important Notes for This App

⚠️ **Critical**: The current `UpdateChecker` implementation downloads APKs from external sources. **This violates Play Store policy** for apps distributed through Google Play.

### Before Publishing:
1. **Remove or disable the updater** for Play Store builds
2. Rely on Google Play's native update mechanism
3. Use a separate build flavor for side-loaded versions if needed

Example: Disable updater in MainActivity:

```java
// Comment out or remove for Play Store build
/*
new UpdateChecker(this,
    "https://raw.githubusercontent.com/marterp/ZipFlash-NoRoot/refs/heads/main/update.json"
).checkForUpdate(currentVersion);
*/
```

## Troubleshooting

### Build Fails: "keystore not found"
- Ensure `release.keystore` is in the project root
- Verify path in `build.gradle` is correct

### APK won't install: "signatures do not match"
- Use the same keystore for all versions
- Don't change the signing certificate

### App crashes after release build
- Check ProGuard rules in `proguard-rules.pro`
- Add exceptions for critical classes (e.g., `UpdateChecker` if kept)

### Play Console rejects app
- Review Play Policy Center for policy violations
- Check comments in app review feedback
- Fix and resubmit

## Post-Release

1. Monitor crash logs in Play Console
2. Read user reviews and respond
3. Track analytics in Google Play Console
4. Plan next update with feedback

---

**For questions or issues**, refer to:
- [Google Play Developer Documentation](https://developer.android.com/distribute/play)
- [Android App Bundle Guide](https://developer.android.com/guide/app-bundle)
