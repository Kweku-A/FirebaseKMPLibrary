package com.kweku.armah.firebase.messaging
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "MyFirebaseMessaging")
expect class MyFirebaseMessaging() {
    fun registerForFirebaseNotifications(topic: String)
    fun unregisterForFirebaseNotifications(topic: String)
    fun getFirebaseToken(token: (String) -> Unit)
}
