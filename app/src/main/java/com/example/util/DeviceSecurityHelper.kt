package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

object DeviceSecurityHelper {
    private const val PREF_NAME = "geoattend_device_prefs"
    private const val KEY_PERSISTENT_DEVICE_UUID = "persistent_device_uuid"

    /**
     * Retrieves or generates a consistent unique device identifier for this phone.
     */
    @SuppressLint("HardwareIds")
    fun getUniqueDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var cachedUuid = prefs.getString(KEY_PERSISTENT_DEVICE_UUID, null)

        if (cachedUuid.isNullOrBlank()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }

            cachedUuid = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                "DEVICE-$androidId-${Build.MANUFACTURER.take(3).uppercase()}"
            } else {
                "DEVICE-${UUID.randomUUID().toString().take(12).uppercase()}"
            }
            prefs.edit().putString(KEY_PERSISTENT_DEVICE_UUID, cachedUuid).apply()
        }

        return cachedUuid
    }

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return "$manufacturer $model"
    }
}
