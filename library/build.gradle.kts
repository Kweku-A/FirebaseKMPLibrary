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

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(
                    JvmTarget.JVM_11
                )
            }
        }
    }

    val iOSTargets = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    val firebaseFrameworks =
        listOf("FirebaseCrashlytics", "FirebaseMessaging", "FirebaseRemoteConfig")

    iOSTargets.forEach { iosTarget ->
        val main by iosTarget.compilations.getting
        val archType = when (iosTarget.name) {
            "iosArm64" -> "ios-arm64" // Physical Device
            "iosSimulatorArm64", "iosX64" -> "ios-arm64_x86_64-simulator" // Simulator
            else -> throw GradleException("Unknown target: ${iosTarget.name}")
        }

        firebaseFrameworks.forEach { frameworkName ->
            main.cinterops.create(frameworkName) {
                val frameworkPath =
                    "${project.rootDir}/library/src/iosMain/resources/files/firebase/$frameworkName/$frameworkName.xcframework/$archType"

                // Tell the compiler WHERE the framework is and WHAT it is
                compilerOpts("-F$frameworkPath", "-framework", frameworkName)
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        }

        // 2. Dynamic Linker Configuration
        iosTarget.binaries.framework {
            baseName = "libxlsxwriter" // Or your shared library name

            firebaseFrameworks.forEach { frameworkName ->
                val frameworkPath =
                    "${project.rootDir}/library/src/iosMain/resources/files/firebase/$frameworkName/$frameworkName.xcframework/$archType"

                linkerOpts("-F$frameworkPath", "-framework", frameworkName)
                linkerOpts("-ObjC")
            }

            // Essential for static Firebase libraries to load categories/classes correctly
            linkerOpts("-ObjC")
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
            api(libs.firebase.analytics)
            api(libs.firebase.auth)
            api(libs.firebase.config)
            api(libs.firebase.messaging)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

   // signAllPublications()

    coordinates("com.kweku.armah", "kmpfirebase", "0.0.1")

    pom {
        name = "FirebaseMultiplatformLibrary"
        description = "A wrapper library for using Firebase in Kotlin Multiplatform Mobile projects"
        inceptionYear = "2025"
        url = "https://github.com/kotlin/multiplatform-library-template/"
        licenses {
            license {
                name = "XXX"
                url = "YYY"
                distribution = "ZZZ"
            }
        }
        developers {
            developer {
                id = "XXX"
                name = "KwekuArmah"
                url = "www.kwekuarmah.com"
            }
        }
        scm {
            url = "XXX"
            connection = "YYY"
            developerConnection = "ZZZ"
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    // The value 'enforced-platform' is provided in the validation
    // error message you got
    suppressedValidationErrors.add("enforced-platform")
}

// Package Firebase XCFrameworks for distribution
val packageFirebaseXCFrameworks by tasks.registering(Zip::class) {
    group = "publishing"
    description = "Package Firebase XCFrameworks for iOS consumers"

    val firebaseFrameworks = listOf("FirebaseCrashlytics", "FirebaseMessaging", "FirebaseRemoteConfig")

    // Include all Firebase XCFrameworks
    from(file("${project.rootDir}/library/src/iosMain/resources/files/firebase")) {
        firebaseFrameworks.forEach { framework ->
            include("$framework/$framework.xcframework/**")
        }
        // Also include Firebase.h and module.modulemap if they exist
        include("Firebase.h")
        include("module.modulemap")
        include("README.md")
        include("METADATA.md")
    }

    archiveBaseName.set("firebase-ios-frameworks")
    archiveVersion.set(version.toString())
    archiveClassifier.set("xcframeworks")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/firebase"))
}

// Attach Firebase frameworks zip to all maven publications
afterEvaluate {
    publishing {
        publications.withType<MavenPublication> {
            artifact(packageFirebaseXCFrameworks) {
                classifier = "xcframeworks"
                extension = "zip"
            }
        }
    }
}

// Ensure publish tasks build the Firebase frameworks package first
tasks.matching { it.name.startsWith("publish") || it.name.startsWith("upload") }.configureEach {
    dependsOn(packageFirebaseXCFrameworks)
}

