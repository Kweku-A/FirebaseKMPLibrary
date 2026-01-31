package com.kweku.armah.firebase.remote_config


import kotlinx.cinterop.ExperimentalForeignApi
import platform.firebase.remoteconfig.FIRRemoteConfig
import platform.firebase.remoteconfig.FIRRemoteConfigSettings
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
actual class MyFirebaseRemoteConfig actual constructor() {
    @OptIn(ExperimentalNativeApi::class)
    private val remoteConfig =
        FIRRemoteConfig.remoteConfig() .apply {
            val configSettings: FIRRemoteConfigSettings =
                FIRRemoteConfigSettings().apply {
                    this.minimumFetchInterval = if (Platform.isDebugBinary) 0.0 else 3600.0 // for prod
                }
            this.configSettings = configSettings
        }

    actual fun fetchAndActivate() {
        remoteConfig.fetchAndActivateWithCompletionHandler { _, nsError ->
            if (nsError != null) {
                setDefaults()
            }
        }
    }

    actual fun setListener() {
        remoteConfig.addOnConfigUpdateListener { firRemoteConfigUpdate, _ ->
            firRemoteConfigUpdate?.let { _ ->
                remoteConfig.activateWithCompletion { _, _ ->
                }
            }
        }
    }

    actual fun getBoolean(key: String): Boolean = remoteConfig.configValueForKey(key).boolValue

    actual fun getString(key: String): String = remoteConfig.configValueForKey(key).stringValue.orEmpty()

    actual fun getDouble(key: String): Double = remoteConfig.configValueForKey(key).stringValue?.toDouble() ?: 0.0

    actual fun getLong(key: String): Long = remoteConfig.configValueForKey(key).numberValue.longValue

    private fun setDefaults() {
//        try {
//            val data = readPlistFile("remote_config_defaults")
//            remoteConfig.setDefaults(data)
//
//            _rcUpdateState.trySend(Random.nextInt(0, 100))
//        } catch (e: Exception) {
//            Napier.d(tag = TAG, message = "++++++++ERROR setDefaults++++++++${e.message}")
//        }
    }
}
