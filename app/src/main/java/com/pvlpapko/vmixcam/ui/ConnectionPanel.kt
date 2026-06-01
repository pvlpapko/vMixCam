
package com.pvlpapko.vmixcam.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pvlpapko.vmixcam.stream.ConnectionMode
import com.pvlpapko.vmixcam.stream.ConnectionState
import java.net.NetworkInterface

@Composable
fun ConnectionPanel(
    onClose: () -> Unit,
    onStartStream: (ConnectionState) -> Unit
) {

    val context = LocalContext.current
    val localIp = remember { getLocalIpAddress() }

    var state by remember {
        mutableStateOf(
            ConnectionState(
                mode = ConnectionMode.SRT_LISTENER,
                ip = localIp
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(430.dp)
                .background(
                    Color(0xFF111827),
                    RoundedCornerShape(22.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                "Подключение",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                ModeButton("SRT Listener",
                    selected = state.mode == ConnectionMode.SRT_LISTENER) {
                    state = state.copy(mode = ConnectionMode.SRT_LISTENER)
                }

                ModeButton("SRT Caller",
                    selected = state.mode == ConnectionMode.SRT_CALLER) {
                    state = state.copy(mode = ConnectionMode.SRT_CALLER)
                }

                ModeButton("RTSP",
                    selected = state.mode == ConnectionMode.RTSP) {
                    state = state.copy(mode = ConnectionMode.RTSP)
                }
            }

            Text(
                text = "IP телефона: ${state.ip}",
                color = Color.White
            )

            Text(
                text = "Порт: ${state.port}",
                color = Color.White
            )

            val output = state.outputUrl()

            OutlinedTextField(
                value = output,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    copyToClipboard(context, output)
                }
            ) {
                Text("Скопировать")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onStartStream(state)

                    Toast.makeText(
                        context,
                        "Поток запущен",
                        Toast.LENGTH_SHORT
                    ).show()

                    onClose()
                }
            ) {
                Text("Запустить поток")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClose
            ) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun ModeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor =
            if (selected) Color.Red
            else Color.DarkGray
        )
    ) {
        Text(title)
    }
}

private fun copyToClipboard(
    context: Context,
    text: String
) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager

    clipboard.setPrimaryClip(
        ClipData.newPlainText("vmix_url", text)
    )

    Toast.makeText(
        context,
        "Скопировано",
        Toast.LENGTH_SHORT
    ).show()
}

private fun getLocalIpAddress(): String {
    return try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull {
                !it.isLoopbackAddress &&
                it.hostAddress?.contains(":") == false
            }?.hostAddress ?: "192.168.0.100"
    } catch (_: Exception) {
        "192.168.0.100"
    }
}
