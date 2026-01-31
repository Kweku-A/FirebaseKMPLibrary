package com.kweku.armah.firebase.crashlytics

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "MyFirebaseCrashlytics")
expect class MyFirebaseCrashlytics() {

    fun recordNonFatalException(throwable: Throwable)

    fun setCrashlyticsUserIdentifier(userId: String)
}
