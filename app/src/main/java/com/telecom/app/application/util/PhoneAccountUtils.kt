
package com.telecom.app.application.util

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.telecom.app.R
import com.telecom.app.application.service.TelephonyService
import java.util.UUID

object PhoneAccountUtils {

    fun registerPhoneAccount(context: Context) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val phoneAccountHandle = getPhoneAccountHandle(context)
        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "My Telecom App")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setIcon(Icon.createWithResource(context, R.drawable.ic_round_call_24))
            .setHighlightColor(context.getColor(R.color.black))
            .setShortDescription("Custom Call Service")
            .build()

        telecomManager.registerPhoneAccount(phoneAccount)
    }

    private fun getPhoneAccountHandle(context: Context): PhoneAccountHandle {
        val componentName = ComponentName(context, TelephonyService::class.java)
        val accountId = UUID.randomUUID().toString()

        return PhoneAccountHandle(componentName, accountId)
    }
}
