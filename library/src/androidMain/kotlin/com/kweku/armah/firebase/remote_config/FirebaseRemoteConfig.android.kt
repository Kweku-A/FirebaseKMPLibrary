package com.kweku.armah.firebase.remote_config

import android.util.Log
import com.google.firebase.Firebase

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineDispatcher

actual class  MyFirebaseRemoteConfig actual constructor() {
    private val remoteConfig: FirebaseRemoteConfig =
        Firebase.remoteConfig.apply {
            this.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = if (true) 0 else 3600
                },
            )
        }

    actual fun fetchAndActivate() {
        remoteConfig.fetch().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // val updated = task.result
                val task = remoteConfig.activate()
                task.addOnCompleteListener {
                    if (task.isSuccessful) {
                        Log.d(TAG, "Config params updated: ${task.result}")
                    } else {
                        Log.d(TAG, "Config params updated failed: ${task.result}")
                    }
                }
            } else {
                Log.d(TAG, "fetchAndActivate() failed")
                setDefaults()
            }
        }
    }

    actual fun setListener() {
        remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d(TAG, "Updated keys: " + configUpdate.updatedKeys)
                    remoteConfig.activate()
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Config update error with code: " + error.code, error)
                }
            },
        )
    }

    actual fun getLong(key: String): Long = remoteConfig.getLong(key)

    actual fun getDouble(key: String): Double = remoteConfig.getDouble(key)

    actual fun getString(key: String): String = remoteConfig.getString(key)

    actual fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)

    // override fun getJson(key: String): Map<String, JsonElement> = json.decodeFromString(remoteConfig.getString(key))

    private fun setDefaults() {
        // remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    init {
    }
}
