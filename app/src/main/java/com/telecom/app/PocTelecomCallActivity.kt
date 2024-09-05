package com.telecom.app

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import com.telecom.app.application.util.PhoneAccountUtils.registerPhoneAccount
import com.telecom.app.data.repository.TelecomCallRepository
import com.telecom.app.presentation.ui.components.TelecomCallScreen
import com.telecom.app.application.service.TelecomCallService

@RequiresApi(Build.VERSION_CODES.O)
class PocTelecomCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestDefaultDialer(this)
        registerPhoneAccount(this)

        val repository =
            TelecomCallRepository.instance ?: TelecomCallRepository.create(applicationContext)

        setupCallActivity()

        setContent {
            MaterialTheme {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    TelecomCallScreen(repository) {
                        finishAndRemoveTask()
                        Log.d("TelecomCallActivity", "Call finished. Finishing activity")
                    }
                }
            }
        }
    }

    private fun requestDefaultDialer(context: Context) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val packageName = context.packageName

        if (telecomManager.defaultDialerPackage != packageName) {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "App is already the default dialer", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        startService(
            Intent(this, TelecomCallService::class.java).apply {
                action = TelecomCallService.ACTION_UPDATE_CALL
            },
        )
    }

    private fun setupCallActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
            )
        }

        val keyguardManager = getSystemService<KeyguardManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }
}
