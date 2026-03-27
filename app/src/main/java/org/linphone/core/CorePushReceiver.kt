/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.linphone.LinphoneApplication.Companion.ensureCoreExists
import org.linphone.compatibility.Compatibility
import org.linphone.core.tools.Log

class CorePushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("[Push Notification] Push notification has been received in broadcast receiver")
        ensureCoreExists(context.applicationContext, true)

        // Android 15+: start CoreService as foreground immediately within the push handling
        // window. If we wait for the SIP INVITE to trigger showForegroundServiceNotification(),
        // the window may have expired and startForeground(MICROPHONE) will throw a silent
        // SecurityException, leaving the microphone suspended in background.
        val serviceIntent = Intent(context, CoreService::class.java).apply {
            putExtra("PushReceived", true)
        }
        Compatibility.startForegroundService(context, serviceIntent)
    }
}
