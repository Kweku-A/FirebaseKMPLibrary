package com.kweku.armah.iofirebase.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

actual class MyFirebaseCrashlytics actual constructor(){

     actual fun recordNonFatalException(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    actual fun setCrashlyticsUserIdentifier(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    init {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}
