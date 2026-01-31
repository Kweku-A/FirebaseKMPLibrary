package com.kweku.armah.firebase.messaging

import kotlinx.cinterop.ExperimentalForeignApi
import platform.firebase.messaging.FIRMessaging


@OptIn(ExperimentalForeignApi::class)
actual class MyFirebaseMessaging actual constructor() {
    val firebaseMessaging = FIRMessaging.messaging()

    actual fun registerForFirebaseNotifications(topic: String) {
        firebaseMessaging.subscribeToTopic(topic = topic, completion = {
//            Napier.d(tag = "FirebaseMessaging", message = "isSubscribedError => $it")
        })
    }

    actual fun unregisterForFirebaseNotifications(topic: String) {
        firebaseMessaging.unsubscribeFromTopic(topic = topic, completion = {
//            Napier.d(tag = "FirebaseMessaging", message = "isUnsubscribedError => $it")
        })
    }

    actual fun getFirebaseToken(token: (String) -> Unit) {
        firebaseMessaging.tokenWithCompletion { s, nsError ->
            if (nsError != null) {
//                Napier.d(tag = "FirebaseMessaging", message = "isTokenError => $nsError")
            }
            if (s != null) {
                token(s)
            }
        }
    }
}
