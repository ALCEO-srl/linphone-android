/*
 * Copyright (c) 2010-2026 Belledonne Communications SARL.
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
package org.linphone.compatibility

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import org.linphone.R
import org.linphone.core.tools.Log

// dms: Android 17 mutes the audio played by an app in background (background audio hardening), so the ringtone
// of a call received by push, played by the SDK from the app process, was often silent. The ringtone is now
// played by the system as the sound of the incoming call notification channel (insistent notification).
@TargetApi(37)
class Api37Compatibility {
    companion object {
        fun createIncomingCallChannel(
            context: Context,
            notificationManager: NotificationManagerCompat
        ) {
            // The sound of an existing channel can't be changed, the old channel (without sound) is replaced
            notificationManager.deleteNotificationChannel(context.getString(R.string.notification_channel_incoming_call_id))

            val id = context.getString(R.string.notification_channel_incoming_call_ringing_id)
            val name = context.getString(R.string.notification_channel_incoming_call_name)
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            channel.description = name
            channel.lightColor = context.getColor(R.color.notification_led_color)
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.setShowBadge(true)
            // Follows the ringtone chosen in the device settings
            channel.setSound(
                Settings.System.DEFAULT_RINGTONE_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            notificationManager.createNotificationChannel(channel)

            // Same channel without sound nor vibration: a notification that makes noise and is posted again
            // shortly after is muted by the system ("recently noisy"), so only the post asked by Telecom rings
            val silentId = context.getString(R.string.notification_channel_incoming_call_silent_id)
            val silentChannel = NotificationChannel(silentId, name, NotificationManager.IMPORTANCE_HIGH)
            silentChannel.description = name
            silentChannel.enableVibration(false)
            silentChannel.enableLights(true)
            silentChannel.lightColor = context.getColor(R.color.notification_led_color)
            silentChannel.setShowBadge(true)
            silentChannel.setSound(null, null)
            notificationManager.createNotificationChannel(silentChannel)
            Log.i("[Api37 Compatibility] Incoming call channels (ringing and silent) created")
        }
    }
}
