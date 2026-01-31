package com.kweku.armah.firebase.remote_config

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "MyFirebaseRemoteConfig")
expect class MyFirebaseRemoteConfig() {
    fun fetchAndActivate(): Unit
    fun setListener(): Unit
    fun getLong(key: String): Long
    fun getDouble(key: String): Double
    fun getString(key: String): String
    fun getBoolean(key: String): Boolean
    // fun getJson(key: String): Map<String, JsonElement>
}
