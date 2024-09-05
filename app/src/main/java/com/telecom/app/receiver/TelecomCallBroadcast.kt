package com.telecom.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.telecom.app.data.model.TelecomCallData
import com.telecom.app.data.repository.TelecomCallRepository
import com.telecom.app.domain.usecase.TelecomCallAction
import com.telecom.app.notification.TelecomCallNotificationManager

@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallBroadcast : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getTelecomCallAction() ?: return
        val repo = TelecomCallRepository.instance ?: TelecomCallRepository.create(context)
        val call = repo.currentCall.value

        if (call is TelecomCallData.Registered) {
            call.processAction(action)
        } else {
            TelecomCallNotificationManager(context).updateCallNotification(call)
        }
    }

    private fun Intent.getTelecomCallAction() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(
                TelecomCallNotificationManager.TELECOM_NOTIFICATION_ACTION,
                TelecomCallAction::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(TelecomCallNotificationManager.TELECOM_NOTIFICATION_ACTION)
        }
}
