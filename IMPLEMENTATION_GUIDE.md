# Firebase KMP Library - Implementation Guide (Option 2: Embedded Firebase)

## Overview

This library uses **Option 2: Embed Firebase in Your Framework** approach. This means:
- Firebase XCFrameworks are **statically linked** into the KMPFirebase.xcframework
- Consumers only need to download and integrate **one binary artifact**
- The Firebase frameworks are automatically included when using the library
- No manual Firebase setup required by consumers on iOS

## How It Works

### 1. Build Process

The build.gradle.kts file is configured to:

#### a) Configure cinterops for each Firebase framework
```kotlin
val firebaseFrameworks = listOf(
    "FirebaseCrashlytics",
    "FirebaseMessaging",
    "FirebaseRemoteConfig"
)

firebaseFrameworks.forEach { frameworkName ->
    main.cinterops.create(frameworkName) {
        val frameworkPath = "${project.rootDir}/library/src/iosMain/resources/files/firebase/$frameworkName/$frameworkName.xcframework/$archType"
        compilerOpts("-F$frameworkPath", "-framework", frameworkName)
        extraOpts += listOf("-compiler-option", "-fmodules")
    }
}
```

This tells Kotlin/Native where to find the Firebase XCFrameworks during compilation and generates Kotlin bindings for the Objective-C APIs.

#### b) Link Firebase frameworks into the binary
```kotlin
iosTarget.binaries.framework {
    baseName = "KMPFirebase"
    isStatic = true  // Static framework for better embedding
    
    // Link Firebase frameworks
    firebaseFrameworks.forEach { frameworkName ->
        val frameworkPath = "${project.rootDir}/library/src/iosMain/resources/files/firebase/$frameworkName/$frameworkName.xcframework/$archType"
        linkerOpts("-F$frameworkPath", "-framework", frameworkName)
    }
    
    // Essential linker flags
    linkerOpts("-ObjC")        // Required for Firebase categories
    linkerOpts("-all_load")    // Load all symbols from archives
}
```

The linker options ensure:
- `-F$frameworkPath`: Tells the linker where to find the frameworks
- `-framework $frameworkName`: Links the framework
- `-ObjC`: Required for Objective-C categories (Firebase uses these extensively)
- `-all_load`: Ensures all symbols from static libraries are included

#### c) Create combined XCFramework
```kotlin
val createCombinedXCFramework by tasks.registering(Exec::class) {
    dependsOn("linkDebugFrameworkIosArm64", "linkDebugFrameworkIosX64", "linkDebugFrameworkIosSimulatorArm64")
    
    commandLine(
        "xcodebuild", "-create-xcframework",
        "-framework", "$buildDir/bin/iosArm64/debugFramework/KMPFirebase.framework",
        "-framework", "$buildDir/bin/iosX64/debugFramework/KMPFirebase.framework",
        "-framework", "$buildDir/bin/iosSimulatorArm64/debugFramework/KMPFirebase.framework",
        "-output", "$outputDir/KMPFirebase.xcframework"
    )
}
```

This creates a fat XCFramework containing:
- iosArm64 (physical devices)
- iosX64 (Intel simulators)
- iosSimulatorArm64 (Apple Silicon simulators)

#### d) Package for Maven distribution
```kotlin
val packageCombinedXCFramework by tasks.registering(Zip::class) {
    dependsOn(createCombinedXCFramework)
    from(layout.buildDirectory.dir("xcframeworks"))
    archiveBaseName.set("kmpfirebase")
    archiveVersion.set(version.toString())
    archiveClassifier.set("xcframework")
}
```

This creates a ZIP file containing the combined XCFramework.

#### e) Attach to Maven publication
```kotlin
afterEvaluate {
    publishing {
        publications.withType<MavenPublication> {
            artifact(packageCombinedXCFramework) {
                classifier = "xcframework"
                extension = "zip"
            }
        }
    }
}
```

The XCFramework ZIP is published as an artifact alongside the Kotlin Multiplatform artifacts.

### 2. Consumer Usage

When a consumer adds this library as a dependency:

#### For Kotlin Code (Shared Module)
```kotlin
// build.gradle.kts in shared module
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.kweku.armah:kmpfirebase:0.0.1")
        }
    }
}
```

#### For iOS (Xcode)

**Option A: CocoaPods** (Recommended)
If you use CocoaPods, the framework is automatically integrated:
```ruby
# Podfile
pod 'shared', :path => '../shared'
```

**Option B: Manual Integration**
1. Download the `kmpfirebase-0.0.1-xcframework.zip` from Maven
2. Extract and add `KMPFirebase.xcframework` to your Xcode project
3. In Xcode: Project Settings → General → Frameworks, Libraries, and Embedded Content
4. Add the XCFramework with "Embed & Sign"

### 3. What Gets Downloaded

When a consumer adds the dependency, they get:

**From Maven Central:**
- `kmpfirebase-0.0.1.klib` (Kotlin/Native binary for linking)
- `kmpfirebase-0.0.1-xcframework.zip` (Contains KMPFirebase.xcframework with embedded Firebase)
- `kmpfirebase-0.0.1.aar` (Android library)
- Various metadata files

**The XCFramework contains:**
- Your KMPFirebase compiled code
- Firebase frameworks (Crashlytics, Messaging, RemoteConfig) **statically linked**
- All necessary symbols and headers

### 4. How Kotlin Code Finds the Frameworks

The Kotlin code can use Firebase APIs because:

1. **At compile time**: The cinterops create Kotlin bindings for the Firebase Objective-C APIs
2. **At link time**: The linker embeds the Firebase frameworks into your framework binary
3. **At runtime**: When the iOS app loads your framework, Firebase is already there

Example Kotlin code:
```kotlin
// src/iosMain/kotlin/FirebaseService.kt
import FirebaseCrashlytics.FIRCrashlytics

actual class FirebaseService {
    actual fun logCrash(message: String) {
        FIRCrashlytics.crashlytics().log(message)
    }
}
```

This works because:
- The cinterop generated `FirebaseCrashlytics` module
- The framework includes the compiled Firebase code
- No additional setup needed by consumers

## Advantages of This Approach

✅ **Simple for consumers**: Only one framework to integrate  
✅ **No manual Firebase setup**: Firebase is already embedded  
✅ **Version control**: Firebase version is locked by your library  
✅ **Consistent behavior**: All consumers use the same Firebase version  

## Disadvantages

❌ **Larger binary size**: All Firebase frameworks are included  
❌ **Less flexible**: Consumers can't choose different Firebase versions  
❌ **Update process**: To update Firebase, you must publish a new library version  
❌ **Licensing**: You must ensure compliance with Firebase licenses  

## Build Commands

To build and publish:

```bash
# Build the combined XCFramework
./gradlew createCombinedXCFramework

# Package it
./gradlew packageCombinedXCFramework

# Publish to Maven Local (for testing)
./gradlew publishToMavenLocal

# Publish to Maven Central
./gradlew publishAllPublicationsToMavenCentralRepository
```

## Directory Structure

```
library/
├── src/
│   └── iosMain/
│       └── resources/
│           └── files/
│               └── firebase/
│                   ├── FirebaseCrashlytics/
│                   │   └── FirebaseCrashlytics.xcframework/
│                   │       ├── ios-arm64/
│                   │       │   └── FirebaseCrashlytics.framework
│                   │       └── ios-arm64_x86_64-simulator/
│                   │           └── FirebaseCrashlytics.framework
│                   ├── FirebaseMessaging/
│                   └── FirebaseRemoteConfig/
├── build/
│   ├── xcframeworks/
│   │   └── KMPFirebase.xcframework  # Combined framework with Firebase
│   └── outputs/
│       └── kmpfirebase-0.0.1-xcframework.zip  # Published artifact
```

## Troubleshooting

### Issue: "Undefined symbols" errors
**Solution**: Ensure `-ObjC` and `-all_load` linker flags are present

### Issue: Firebase not initialized
**Solution**: Consumer must still initialize Firebase in their iOS app:
```swift
// iOS AppDelegate
import FirebaseCore

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(...) {
        FirebaseApp.configure()
    }
}
```

### Issue: Large binary size
**Solution**: Consider Option 1 (separate Firebase distribution) or use ProGuard/R8 rules

### Issue: Conflicts with other Firebase versions
**Solution**: This approach doesn't allow multiple Firebase versions. All must use the embedded version.

## Firebase Initialization (Consumer Responsibility)

While Firebase frameworks are embedded, consumers still need to:

1. Add `GoogleService-Info.plist` to their iOS app
2. Call `FirebaseApp.configure()` in AppDelegate
3. Handle Firebase dependencies on Android side separately

Example:
```swift
// iOS
import UIKit
import FirebaseCore

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, 
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        FirebaseApp.configure()
        return true
    }
}
```

## License Considerations

When embedding Firebase, ensure you:
- ✅ Include Firebase license notices in your documentation
- ✅ Comply with Firebase Terms of Service
- ✅ Note that consumers are bound by Firebase's terms
- ✅ Consider if your license is compatible with Firebase's Apache 2.0 license

## Summary

This implementation successfully:
1. ✅ Embeds Firebase XCFrameworks into your KMP framework
2. ✅ Creates Kotlin bindings via cinterops
3. ✅ Produces a single combined XCFramework
4. ✅ Publishes it as a Maven artifact
5. ✅ Makes it simple for consumers to integrate

The combined XCFramework contains everything needed, and consumers just need to add your dependency and initialize Firebase in their iOS app.
