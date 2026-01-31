import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    id("maven-publish")
    id("signing")
}

group = "io.github.kotlin"
version = "1.0.0"

kotlin {
    androidLibrary {
        namespace = "com.kweku.armah.iofirebase"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    val iOSTargets = listOf(
        //iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    val firebaseFrameworks = listOf(
        "FirebaseCrashlytics",
    )

    iOSTargets.forEach { iosTarget ->
        val main by iosTarget.compilations.getting
        val archType = when (iosTarget.name) {
            "iosArm64" -> "ios-arm64" // Physical Device
            "iosSimulatorArm64" -> "ios-arm64_x86_64-simulator" // Simulator
            else -> throw GradleException("Unknown target: ${iosTarget.name}")
        }

        // Create cinterop definitions for Firebase frameworks
        firebaseFrameworks.forEach { frameworkName ->
            main.cinterops.create(frameworkName) {
                val frameworkPath =
                    "${project.rootDir}/libs/firebase/$frameworkName/$frameworkName.xcframework/$archType"

                // Tell the compiler WHERE the framework is and WHAT it is
                compilerOpts("-F$frameworkPath", "-framework", frameworkName)
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        }

        // Configure the framework binary to embed Firebase
        iosTarget.binaries.framework {
            baseName = "KMPFirebase" // Your framework name
            isStatic = true // Use static framework for better embedding

            // Link Firebase frameworks
            firebaseFrameworks.forEach { frameworkName ->
                val frameworkPath =
                    "${project.rootDir}/libs/firebase/$frameworkName/$frameworkName.xcframework/$archType"

                linkerOpts("-F$frameworkPath", "-framework", frameworkName)
            }

            // Essential for static Firebase libraries to load categories/classes correctly
            linkerOpts("-ObjC")

            // Additional linker options for proper embedding
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

tasks.withType<GenerateModuleMetadata> {
    suppressedValidationErrors.add("enforced-platform")
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates("io.github.kweku-a", "kmp.crashlytics", "0.0.2")

    pom {
        name = "KMPCrashlyticsLibrary"
        description = "A wrapper library for using Firebase Crashlytics"
        inceptionYear = "2025"
        url = "https://github.com/Kweku-A/FirebaseKMPLibrary"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "b5e7ac49-7148-4b8e-adba-2cd175ed6edf"
                name = "KwekuArmah"
                url = "https://www.kwekuarmah.com"
            }
        }
        scm {
            url = "https://github.com/Kweku-A/FirebaseKMPLibrary.git"
            connection = "scm:git:git://github.com/Kweku-A/FirebaseKMPLibrary.git"
            developerConnection = "scm:git:ssh://git@github.com:Kweku-A/FirebaseKMPLibrary.git"
        }
    }
}

signing {
//    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
//    sign(publishing.publications)

println("====> check signing credentials")
        val signingKey = providers.gradleProperty("signingInMemoryKey")
        val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword")

    println("====> signingKey present: ${signingKey.isPresent}")
    println("====> signingPassword present: ${signingPassword.isPresent}")
    println("====> signingKey value: ${signingKey.orNull?.take(10)?.plus("...")}")
    println("====> signingPassword value: ${signingPassword.orNull?.take(10)?.plus("...")}")

        if (signingKey.isPresent && signingPassword.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
            //sign(publishing.publications["androidPublication"])
            sign(publishing.publications)
            println("Signing credentials found and signed.")
        } else {
            println("Signing credentials not found. Skipping signing.")
        }

}
