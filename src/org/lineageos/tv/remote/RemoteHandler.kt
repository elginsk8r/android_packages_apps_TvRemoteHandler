/*
 * Copyright (C) 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.tv.remote

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.android.internal.os.DeviceKeyHandler
import org.lineageos.tv.remote.R

class RemoteHandler(private val context: Context) : DeviceKeyHandler {

    private val remoteSpecialKeys: Map<Int, String> by lazy {
        val keycodes = context.resources.getIntArray(
            R.array.config_remoteSpecialKeyCode
        )
        val packages = context.resources.getStringArray(
            R.array.config_remoteSpecialActionOrPackage
        )
        keycodes.zip(packages).toMap()
    }

    override fun handleKeyEvent(event: KeyEvent): KeyEvent? =
        event.takeIf { it.action != KeyEvent.ACTION_UP || !isSetupComplete() }
            ?: remoteSpecialKeys[event.keyCode]?.also {
                launchTarget(it)
            }?.let { null } ?: event

    private fun isSetupComplete(): Boolean =
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.TV_USER_SETUP_COMPLETE,
            0
        ) != 0

    private fun launchTarget(targetName: String) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(targetName)
            ?: Intent(targetName).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("no_input_mode", true)
            }.takeIf {
                it.resolveActivity(pm) != null
            }

        launchIntent?.let {
            context.startActivity(it)
        } ?: Log.w(TAG, "Cannot launch $targetName: package/intent not found.")
    }

    companion object {
        private val TAG = RemoteHandler::class.java.simpleName
    }
}
