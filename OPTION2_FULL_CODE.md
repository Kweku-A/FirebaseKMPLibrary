# Firebase KMP Library - Quick Reference

## Full Code Implementation (Option 2: Embed Firebase)

This is the complete, working implementation that embeds Firebase frameworks into your KMP library.

### Complete build.gradle.kts

```kotlin
import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.kotlin"
version = "1.0.0"

kotlin {
    androidLibrary {
        namespace = "org.jetbrains.kotlinx.multiplatform.library.template"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    val iOSTargets = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    val firebaseFrameworks = listOf(
        "FirebaseCrashlytics",
        "FirebaseMessaging",
        "FirebaseRemoteConfig"
    )

    iOSTargets.forEach { iosTarget ->
        val main by iosTarget.compilations.getting
        val archType = when (iosTarget.name) {
            "iosArm64" -> "ios-arm64"
            "iosSimulatorArm64", "iosX64" -> "ios-arm64_x86_64-simulator"
            else -> throw GradleException("Unknown target: ${iosTarget.name}")
        }

        // Create cinterop definitions for Firebase frameworks
        firebaseFrameworks.forEach { frameworkName ->
            main.cinterops.create(frameworkName) {
                val frameworkPath =
                    "${project.rootDir}/library/src/iosMain/resources/files/firebase/$frameworkName/$frameworkName.xcframework/$archType"

                compilerOpts("-F$frameworkPath", "-framework", frameworkName)
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        }

        // Configure the framework binary to embed Firebase
        iosTarget.binaries.framework {
            baseName = "KMPFirebase"
            isStatic = true

            // Link Firebase frameworks
            firebaseFrameworks.forEach { frameworkName ->
                val frameworkPath =
                    "${project.rootDir}/library/src/iosMain/resources/files/firebase/$frameworkName/$frameworkName.xcframework/$archType"

                linkerOpts("-F$frameworkPath", "-framework", frameworkName)
            }

            // Essential for static Firebase libraries
            linkerOpts("-ObjC")
            linkerOpts("-all_load")
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            api(project.dependencies.enforcedPlatform(libs.firebase.bom))
            api(libs.firebase.crashlytics)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // signAllPublications()

    coordinates("com.kweku.armah", "kmpfirebase", "0.0.1")

    pom {
        name = "FirebaseMultiplatformLibrary"
        description = "A wrapper library for using Firebase in Kotlin Multiplatform Mobile projects with embedded Firebase frameworks"
        inceptionYear = "2025"
        url = "https://github.com/kotlin/multiplatform-library-template/"
        licenses {
            license {
                name = "Apache License 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "kwekuarmah"
                name = "KwekuArmah"
                url = "www.kwekuarmah.com"
            }
        }
        scm {
            url = "https://github.com/kotlin/multiplatform-library-template/"
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    suppressedValidationErrors.add("enforced-platform")
}

// Task to create a combined XCFramework with embedded Firebase
val createCombinedXCFramework by tasks.registering(Exec::class) {
    group = "build"
    description = "Create a combined XCFramework with embedded Firebase for all iOS architectures"

    dependsOn("linkDebugFrameworkIosArm64", "linkDebugFrameworkIosX64", "linkDebugFrameworkIosSimulatorArm64")

    val frameworkName = "KMPFirebase"
    val buildDir = layout.buildDirectory.get().asFile
    val outputDir = file("$buildDir/xcframeworks")

    doFirst {
        outputDir.mkdirs()
    }

    commandLine(
        "xcodebuild", "-create-xcframework",
        "-framework", "$buildDir/bin/iosArm64/debugFramework/$frameworkName.framework",
        "-framework", "$buildDir/bin/iosX64/debugFramework/$frameworkName.framework",
        "-framework", "$buildDir/bin/iosSimulatorArm64/debugFramework/$frameworkName.framework",
        "-output", "$outputDir/$frameworkName.xcframework"
    )
}

// Package the combined XCFramework for distribution
val packageCombinedXCFramework by tasks.registering(Zip::class) {
    group = "publishing"
    description = "Package the combined XCFramework with embedded Firebase"

    dependsOn(createCombinedXCFramework)

    from(layout.buildDirectory.dir("xcframeworks"))

    archiveBaseName.set("kmpfirebase")
    archiveVersion.set(version.toString())
    archiveClassifier.set("xcframework")
    destinationDirectory.set(layout.buildDirectory.dir("outputs"))
}

// Attach combined XCFramework to maven publications
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

// Ensure publish tasks build the combined XCFramework first
tasks.matching { it.name.startsWith("publish") || it.name.startsWith("upload") }.configureEach {
    dependsOn(packageCombinedXCFramework)
}
```

## Key Features Explained

### 1. **Cinterop Configuration**
```kotlin
main.cinterops.create(frameworkName) {
    compilerOpts("-F$frameworkPath", "-framework", frameworkName)
    extraOpts += listOf("-compiler-option", "-fmodules")
}
```
- Creates Kotlin bindings for Firebase Objective-C APIs
- Generates `FirebaseCrashlytics`, `FirebaseMessaging`, etc. modules
- Makes Firebase APIs available in Kotlin code

### 2. **Static Linking**
```kotlin
iosTarget.binaries.framework {
    isStatic = true
    linkerOpts("-F$frameworkPath", "-framework", frameworkName)
    linkerOpts("-ObjC", "-all_load")
}
```
- `isStatic = true`: Creates static framework for better embedding
- `-ObjC`: Required for Objective-C categories (Firebase uses them)
- `-all_load`: Ensures all symbols are included

### 3. **XCFramework Creation**
```kotlin
commandLine(
    "xcodebuild", "-create-xcframework",
    "-framework", "iosArm64/debugFramework/KMPFirebase.framework",
    "-framework", "iosX64/debugFramework/KMPFirebase.framework",
    "-framework", "iosSimulatorArm64/debugFramework/KMPFirebase.framework",
    "-output", "KMPFirebase.xcframework"
)
```
- Combines all architectures into one XCFramework
- Supports physical devices (arm64) and simulators (x64, simulatorArm64)

### 4. **Maven Publishing**
```kotlin
artifact(packageCombinedXCFramework) {
    classifier = "xcframework"
    extension = "zip"
}
```
- Publishes XCFramework as ZIP artifact
- Consumers can download from Maven Central

## Build Commands

```bash
# 1. Build the combined XCFramework
./gradlew createCombinedXCFramework

# 2. Package it as ZIP
./gradlew packageCombinedXCFramework

# 3. Test publish to Maven Local
./gradlew publishToMavenLocal

# 4. Publish to Maven Central
./gradlew publishAllPublicationsToMavenCentralRepository
```

## Consumer Usage

### Kotlin (Shared Module)
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.kweku.armah:kmpfirebase:0.0.1")
        }
    }
}
```

### iOS (Xcode)
**CocoaPods:**
```ruby
pod 'shared', :path => '../shared'
```

**Manual:**
1. Download `kmpfirebase-0.0.1-xcframework.zip` from Maven
2. Extract and add to Xcode project
3. Embed & Sign in project settings

### Use in Code
```kotlin
// src/iosMain/kotlin/FirebaseService.kt
import FirebaseCrashlytics.FIRCrashlytics
import FirebaseMessaging.FIRMessaging

class FirebaseService {
    fun logCrash(message: String) {
        FIRCrashlytics.crashlytics().log(message)
    }
    
    fun getToken(): String? {
        return FIRMessaging.messaging().FCMToken
    }
}
```

## What Gets Published

When you run publish, Maven receives:
- `kmpfirebase-0.0.1.klib` (Kotlin/Native binary)
- `kmpfirebase-0.0.1-xcframework.zip` (Combined XCFramework with Firebase)
- `kmpfirebase-0.0.1.aar` (Android library)
- `kmpfirebase-0.0.1.module` (Gradle metadata)
- `kmpfirebase-0.0.1.pom` (Maven POM)

## How It Works

1. **Compilation**: Cinterops generate Kotlin bindings for Firebase
2. **Linking**: Firebase frameworks are statically linked into KMPFirebase
3. **Packaging**: All architectures combined into one XCFramework
4. **Publishing**: XCFramework ZIP uploaded to Maven
5. **Consumer**: Downloads and integrates single framework

## Advantages ✅

- **Simple integration**: One framework to add
- **No manual setup**: Firebase is already embedded
- **Version control**: Locked Firebase version
- **Consistent behavior**: All users get same Firebase version

## Considerations ⚠️

- **Binary size**: Larger framework (includes Firebase)
- **Flexibility**: Consumers can't change Firebase version
- **Licensing**: Must comply with Firebase licenses
- **Updates**: Require new library version to update Firebase

## Troubleshooting

**"Undefined symbols"**: Check `-ObjC` and `-all_load` flags  
**"Firebase not initialized"**: Consumer must call `FirebaseApp.configure()`  
**"Large binary"**: Consider Option 1 (separate distribution) instead  
**"Version conflicts"**: This approach doesn't support multiple Firebase versions

## License Note

When embedding Firebase, ensure:
- Include Firebase license notices in your docs
- Comply with Firebase Terms of Service  
- Your license is compatible with Apache 2.0
- Consumers understand they're bound by Firebase terms

---

For detailed explanation, see [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md)
