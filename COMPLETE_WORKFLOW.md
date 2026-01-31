# Complete Workflow: Build, Publish, and Consume

## For Library Maintainer (You)

### Step 1: Build Everything
```bash
cd /Users/hu901492/StudioProjects/FirebaseLibrary

# Clean previous builds
./gradlew clean

# Build iOS frameworks
./gradlew linkDebugFrameworkIosArm64 linkDebugFrameworkIosSimulatorArm64

# Create combined XCFramework
./gradlew createCombinedXCFramework

# Package as ZIP
./gradlew packageCombinedXCFramework

# Generate Podspec and Package.swift
./gradlew generatePodspec generateSwiftPackage
```

### Step 2: Verify Artifacts
```bash
# Check XCFramework
ls -lh library/build/xcframeworks/KMPFirebase.xcframework/

# Check ZIP
ls -lh library/build/outputs/kmpfirebase-1.0.0-xcframework.zip

# Check Podspec
cat KMPFirebase.podspec

# Check Package.swift
cat Package.swift
```

### Step 3: Test Locally
```bash
# Publish to Maven Local
./gradlew publishToMavenLocal

# Verify published artifacts
ls -la ~/.m2/repository/com/kweku/armah/kmpfirebase/1.0.0/
```

Expected files:
```
kmpfirebase-1.0.0-iosArm64.klib
kmpfirebase-1.0.0-iosSimulatorArm64.klib
kmpfirebase-1.0.0-xcframework.zip  ← KEY FILE
kmpfirebase-1.0.0.pom
kmpfirebase-1.0.0.module
... (other files)
```

### Step 4: Publish to Maven Central
```bash
# When ready, publish
./gradlew publishAndReleaseToMavenCentral

# Or just publish without auto-release
./gradlew publishAllPublicationsToMavenCentralRepository
```

### Step 5: Push to GitHub
```bash
git add KMPFirebase.podspec Package.swift library/build.gradle.kts
git commit -m "Add automatic XCFramework distribution via CocoaPods/SPM"
git push

# Create tag for SPM
git tag 1.0.0
git push --tags
```

### Step 6: (Optional) Publish Podspec to CocoaPods Trunk
```bash
# Register (first time only)
pod trunk register your@email.com 'Your Name'

# Push podspec
pod trunk push KMPFirebase.podspec
```

---

## For Consumers

### Option 1: CocoaPods (iOS App) - AUTOMATIC ⭐

**Setup:**
```ruby
# iOS Project Podfile
platform :ios, '13.0'
use_frameworks!

target 'MyApp' do
  # If published to CocoaPods Trunk:
  pod 'KMPFirebase', '~> 1.0.0'
  
  # OR if using GitHub:
  pod 'KMPFirebase', :podspec => 'https://raw.githubusercontent.com/you/repo/main/KMPFirebase.podspec'
end
```

**Install:**
```bash
pod install
```

**What Happens:**
1. CocoaPods reads the podspec
2. Downloads `kmpfirebase-1.0.0-xcframework.zip` from Maven Central
3. Extracts `KMPFirebase.xcframework`
4. Integrates into Xcode project
5. Configures build settings
6. ✅ Ready to use!

**Usage:**
```swift
import UIKit
import KMPFirebase  // Your framework
import FirebaseCore

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Initialize Firebase
        FirebaseApp.configure()
        
        // Use your library
        // (Your Kotlin code is available here)
        
        return true
    }
}
```

---

### Option 2: Swift Package Manager (iOS App) - AUTOMATIC ⭐

**In Xcode:**
1. File → Add Package Dependencies
2. Enter: `https://github.com/you/kmpfirebase`
3. Select version: `1.0.0`
4. Click "Add Package"

**What Happens:**
1. SPM reads `Package.swift` from your repo
2. Downloads XCFramework from Maven Central
3. Verifies SHA256 checksum
4. Integrates into project
5. ✅ Ready to use!

**Usage:**
Same as CocoaPods example above.

---

### Option 3: Kotlin Multiplatform Project - SEMI-AUTOMATIC

**In shared module:**
```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.kweku.armah:kmpfirebase:1.0.0")
        }
    }
}
```

**What Happens:**
1. Gradle downloads klib from Maven
2. Kotlin code is available in common/iOS code
3. Framework built with proper linking
4. iOS app needs to add XCFramework to Xcode (standard KMP workflow)

**iOS Integration:**
If using CocoaPods for the shared framework:
```ruby
# iosApp/Podfile
pod 'shared', :path => '../shared'
```

The shared framework will include references to KMPFirebase, and CocoaPods will handle it.

---

## What Each File Does

### KMPFirebase.podspec
```ruby
Pod::Spec.new do |spec|
  spec.name = 'KMPFirebase'
  spec.source = { 
    :http => 'https://repo1.maven.org/.../kmpfirebase-1.0.0-xcframework.zip'
  }
  spec.vendored_frameworks = 'KMPFirebase.xcframework'
end
```

**Purpose:** Tells CocoaPods where to download the XCFramework

### Package.swift
```swift
.binaryTarget(
    name: "KMPFirebase",
    url: "https://repo1.maven.org/.../kmpfirebase-1.0.0-xcframework.zip",
    checksum: "abc123..."
)
```

**Purpose:** Tells SPM where to download the XCFramework and verifies integrity

### klib Files
```
kmpfirebase-1.0.0-iosArm64.klib
```

**Purpose:** 
- Contains compiled Kotlin code
- Contains cinterop definitions (Firebase headers/metadata)
- Used by Kotlin/Native compiler when consumers build
- Does NOT contain XCFramework binaries (only references)

### XCFramework ZIP
```
kmpfirebase-1.0.0-xcframework.zip
  └── KMPFirebase.xcframework/
      ├── ios-arm64/ (device)
      └── ios-arm64_x86_64-simulator/ (simulator)
```

**Purpose:**
- Contains the actual binary with embedded Firebase
- Downloaded by CocoaPods/SPM
- Contains your Kotlin code + Firebase frameworks statically linked

---

## Key Points

### 1. Klib Contents (What's in the Include Folder)
```
kmpfirebase-iosArm64.klib/
└── targets/iosArm64/native/included/
    └── FirebaseCrashlytics/
        ├── FirebaseCrashlytics.h        # ← Headers only!
        └── module.modulemap              # ← Module definition
```

**NOT included:** `.framework` or `.xcframework` binaries

### 2. Why Headers Are Enough
- Klib is for **compilation** (not runtime)
- Headers provide API definitions for IDE autocomplete
- Actual binaries come from the XCFramework
- This is how all native iOS dependencies work

### 3. How It All Works Together

```
Consumer adds dependency
        ↓
CocoaPods reads podspec
        ↓
Downloads XCFramework ZIP from Maven
        ↓
Extracts to Pods/KMPFirebase/
        ↓
Xcode links against the framework
        ↓
✅ App runs with Firebase embedded
```

---

## Troubleshooting

### Issue: "Pod not found"
**Solution:** Ensure `KMPFirebase.podspec` is accessible:
- Push to GitHub
- Or publish to CocoaPods Trunk

### Issue: "XCFramework download failed"
**Solution:** Verify Maven URL in podspec:
```bash
curl -I https://repo1.maven.org/maven2/com/kweku/armah/kmpfirebase/1.0.0/kmpfirebase-1.0.0-xcframework.zip
```

Should return `200 OK`

### Issue: "Checksum mismatch" (SPM)
**Solution:** Regenerate checksum:
```bash
./gradlew generateSwiftPackage
# This recalculates the SHA256
```

### Issue: "Firebase not initialized"
**Reminder:** Consumers still need to:
1. Add `GoogleService-Info.plist` to their iOS project
2. Call `FirebaseApp.configure()` in AppDelegate
3. These are Firebase requirements, not specific to your library

---

## Comparison with Other Libraries

Your approach matches industry standards:

| Library | Distribution | How Consumers Get It |
|---------|-------------|----------------------|
| **Firebase** | CocoaPods/SPM | `pod 'Firebase'` |
| **Alamofire** | CocoaPods/SPM | `pod 'Alamofire'` |
| **SDWebImage** | CocoaPods/SPM | `pod 'SDWebImage'` |
| **Your KMPFirebase** | CocoaPods/SPM | `pod 'KMPFirebase'` ✅ |

All follow the same pattern:
1. Binaries hosted on CDN/Maven
2. Podspec/Package.swift points to binary URL
3. Package manager downloads automatically

---

## Next Version Updates

When releasing v2.0.0:

```bash
# Update version in build.gradle.kts
version = "2.0.0"

# Rebuild and publish
./gradlew clean publishToMavenCentral

# Regenerate distribution files
./gradlew generatePodspec generateSwiftPackage

# Commit and tag
git add KMPFirebase.podspec Package.swift
git commit -m "Release v2.0.0"
git tag 2.0.0
git push --tags

# Update podspec
pod trunk push KMPFirebase.podspec
```

Consumers update:
```ruby
pod 'KMPFirebase', '~> 2.0.0'
pod update
```

---

## Summary

✅ **Your build.gradle.kts correctly:**
- Bundles Firebase headers in klib include folder
- Creates combined XCFramework with embedded Firebase
- Publishes both klib and XCFramework ZIP to Maven
- Generates Podspec and Package.swift for auto-download

✅ **Consumers get automatic integration via:**
- CocoaPods: `pod install`
- SPM: Add Package in Xcode
- KMP: Gradle dependency

✅ **No manual ZIP downloads needed!**

The klib's include folder has headers (which is correct), and the XCFramework binaries are distributed separately via CocoaPods/SPM (which is the industry standard approach).

🎉 **Everything is working as designed!**
