package com.telecom.app.presentation.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.telecom.app.PocTelecomCallActivity
import com.telecom.app.data.model.TelecomCallData
import com.telecom.app.data.repository.TelecomCallRepository
import com.telecom.app.application.service.TelecomCallService
import com.telecom.app.util.PermissionBox

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PocCallMainScreen() {
    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        permissions.add(Manifest.permission.MANAGE_OWN_CALLS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        permissions.add(Manifest.permission.READ_PHONE_STATE)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    PermissionBox(permissions = permissions) {
        TelecomCallOptions()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TelecomCallOptions() {
    val context = LocalContext.current
    val repository = remember {
        TelecomCallRepository.instance ?: TelecomCallRepository.create(context.applicationContext)
    }
    val call by repository.currentCall.collectAsState()
    val hasOngoingCall = call is TelecomCallData.Registered

    if (hasOngoingCall) {
        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, PocTelecomCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        }
    }

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val title = if (hasOngoingCall) {
            "There is an active call"
        } else {
            "No active call"
        }
        Spacer(modifier = Modifier.padding(50.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(30.dp))

        OutlinedTextFieldBackground(Color.LightGray) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(
                        text = "Name"
                    )
                },
            )
        }

        OutlinedTextFieldBackground(Color.LightGray) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = {
                    Text(
                        text = "Mobile"
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !hasOngoingCall,
            onClick = {
                if (phoneNumber.isNotEmpty()) {
                    context.launchCall(
                        action = TelecomCallService.ACTION_OUTGOING_CALL,
                        name = name,
                        uri = Uri.parse("tel:${phoneNumber}"),
                    )
                }else
                    Toast.makeText(context, "Please enter valid number", Toast.LENGTH_SHORT).show()
            },
        ) {
            Text(text = "Call")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = !hasOngoingCall,
            onClick = {
                Toast.makeText(context, "Call starting", Toast.LENGTH_SHORT).show()
                context.launchCall(
                    action = TelecomCallService.ACTION_INCOMING_CALL,
                    name = "RR",
                    uri = Uri.parse("tel:8527561611"),
                )
            },
        ) {
            Text(text = "Receive call")
        }
    }
}

@Composable
fun OutlinedTextFieldBackground(
    color: Color,
    content: @Composable () -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .background(
                    color,
                    shape = RoundedCornerShape(4.dp)
                )
        )
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Context.launchCall(action: String, name: String, uri: Uri) {
    startService(
        Intent(this, TelecomCallService::class.java).apply {
            this.action = action
            putExtra(TelecomCallService.EXTRA_NAME, name)
            putExtra(TelecomCallService.EXTRA_URI, uri)
        },
    )
}
