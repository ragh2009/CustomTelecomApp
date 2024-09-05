package com.telecom.app.notification

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.PermissionChecker
import com.telecom.app.R
import com.telecom.app.PocTelecomCallActivity
import com.telecom.app.data.model.TelecomCallData
import com.telecom.app.domain.usecase.TelecomCallAction
import com.telecom.app.receiver.TelecomCallBroadcast

@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallNotificationManager(private val context: Context) {

    internal companion object {
        const val TELECOM_NOTIFICATION_ID = 200
        const val TELECOM_NOTIFICATION_ACTION = "poc_telecom_action"
        const val TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID = "poc_telecom_incoming_channel"
        const val TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID = "poc_telecom_ongoing_channel"

        private val ringToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    fun updateCallNotification(call: TelecomCallData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            return
        }

        createNotificationChannels()

        when (call) {
            TelecomCallData.None, is TelecomCallData.Unregistered -> {
                notificationManager.cancel(TELECOM_NOTIFICATION_ID)
            }

            is TelecomCallData.Registered -> {
                val notification = createNotification(call)
                notificationManager.notify(TELECOM_NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotification(call: TelecomCallData.Registered): Notification {
        val caller = Person.Builder()
            .setName(call.callAttributes.displayName)
            .setUri(call.callAttributes.address.toString())
            .setImportant(true)
            .build()

        val contentIntent = PendingIntent.getActivity(
            context,
             0,
            Intent(context, PocTelecomCallActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val isIncoming = call.isIncoming() && !call.isActive
        val callStyle = if (isIncoming) {
            NotificationCompat.CallStyle.forIncomingCall(
                caller,
                getPendingIntent(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(DisconnectCause.REJECTED),
                    ),
                ),
                getPendingIntent(TelecomCallAction.Answer),
            )
        } else {
            NotificationCompat.CallStyle.forOngoingCall(
                caller,
                getPendingIntent(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(DisconnectCause.LOCAL),
                    ),
                ),
            )
        }
        val channelId = if (isIncoming) {
            TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID
        } else {
            TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setSmallIcon(R.drawable.ic_round_call_24)
            .setOngoing(true)
            .setStyle(callStyle)

        if (call.isOnHold) {
            builder.addAction(
                R.drawable.ic_phone_paused_24, "Resume",
                getPendingIntent(
                    TelecomCallAction.Activate,
                ),
            )
        }
        return builder.build()
    }

    private fun getPendingIntent(action: TelecomCallAction): PendingIntent {
        val callIntent = Intent(context, TelecomCallBroadcast::class.java)
        callIntent.putExtra(
            TELECOM_NOTIFICATION_ACTION,
            action,
        )

        return PendingIntent.getBroadcast(
            context,
            callIntent.hashCode(),
            callIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannels() {
        val incomingChannel = NotificationChannelCompat.Builder(
            TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        ).setName("Incoming calls")
            .setDescription("Handles the notifications when receiving a call")
            .setVibrationEnabled(true).setSound(
                ringToneUri,
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build(),
            ).build()

        val ongoingChannel = NotificationChannelCompat.Builder(
            TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
        ).setName("Ongoing calls").setDescription("Displays the ongoing call notifications").build()

        notificationManager.createNotificationChannelsCompat(
            listOf(
                incomingChannel,
                ongoingChannel,
            ),
        )
    }
}
