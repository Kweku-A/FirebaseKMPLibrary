package com.kweku.armah.firebase.messaging

import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging


actual class  MyFirebaseMessaging actual constructor(){
    actual fun registerForFirebaseNotifications(topic: String) {
        Firebase.messaging.subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Subscribed"
                if (!task.isSuccessful) {
                    msg = "Subscribe failed"
                }
            }
    }

    actual fun unregisterForFirebaseNotifications(topic: String) {
        Firebase.messaging.unsubscribeFromTopic(topic)
    }

    actual fun getFirebaseToken(token: (String) -> Unit) {
        Firebase.messaging.token.addOnCompleteListener { task ->
            val msg =
                if (task.isSuccessful) {
                    task.result
                } else {
                    "error"
                }
            token(msg)
        }
    }
}
