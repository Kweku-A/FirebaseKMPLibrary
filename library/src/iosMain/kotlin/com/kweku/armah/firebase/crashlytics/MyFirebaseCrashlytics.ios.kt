package com.kweku.armah.firebase.crashlytics


import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.firebase.crashlytics.FIRCrashlytics

@OptIn(ExperimentalForeignApi::class)
actual class MyFirebaseCrashlytics actual constructor(){

    private val firebaseCrashlytics = FIRCrashlytics.crashlytics()
    actual fun recordNonFatalException(throwable: Throwable) {

        firebaseCrashlytics.recordError(
            NSError(
                domain = null,
                code = 0,
                userInfo = mapOf(throwable.cause to throwable.message),
            ),
        )
    }

    actual fun setCrashlyticsUserIdentifier(userId: String) {
        firebaseCrashlytics.setUserID(userId)
    }

    init {
        firebaseCrashlytics.setCrashlyticsCollectionEnabled(true)
    }
}
