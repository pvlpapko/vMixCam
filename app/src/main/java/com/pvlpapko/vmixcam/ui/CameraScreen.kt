package com.pvlpapko.vmixcam.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraReady by remember { mutableStateOf(false) }
    var micEnabled by remember { mutableStateOf(true) }
    var isLive by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var connectionOpen by remember { mutableStateOf(false) }
    var stabilizationMode by remember { mutableStateOf("Выкл") }
    var colorMode by remember { mutableStateOf("Натуральный") }

    val hasCameraPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                                cameraReady = true
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Ошибка запуска камеры", Toast.LENGTH_LONG).show()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                }
            )
        } else {
            Text(
                text = "Нет доступа к камере",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        TopStatusBar(
            cameraReady = cameraReady,
            isLive = isLive,
            stabilizationMode = stabilizationMode,
            colorMode = colorMode
        )

        MainLiveButton(
            isLive = isLive,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
            onClick = {
                isLive = !isLive
                Toast.makeText(
                    context,
                    if (isLive) "Эфир включён" else "Эфир остановлен",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        QuickButtons(
            micEnabled = micEnabled,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
            onMicClick = {
                micEnabled = !micEnabled
                Toast.makeText(
                    context,
                    if (micEnabled) "Микрофон включён" else "Микрофон выключен",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onStabClick = {
                stabilizationMode = when (stabilizationMode) {
                    "Выкл" -> "Стаб"
                    "Стаб" -> "Стаб+"
                    "Стаб+" -> "Ультра"
                    else -> "Выкл"
                }
                Toast.makeText(context, "Стабилизация: $stabilizationMode", Toast.LENGTH_SHORT).show()
            },
            onColorClick = {
                colorMode = when (colorMode) {
                    "Натуральный" -> "Кино"
                    "Кино" -> "Яркий"
                    "Яркий" -> "Ночь"
                    else -> "Натуральный"
                }
                Toast.makeText(context, "Цвет: $colorMode", Toast.LENGTH_SHORT).show()
            },
            onConnectClick = {
                connectionOpen = true
            },
            onMenuClick = {
                menuOpen = true
            }
        )

        if (menuOpen) {
            SideMenu(
                onClose = { menuOpen = false },
                onSettings = {
                    menuOpen = false
                    settingsOpen = true
                },
                onConnection = {
                    menuOpen = false
                    connectionOpen = true
                }
            )
        }

        if (settingsOpen) {
            SettingsPanel(
                stabilizationMode = stabilizationMode,
                colorMode = colorMode,
                onStabChange = {
                    stabilizationMode = it
                    Toast.makeText(context, "Стабилизация: $it", Toast.LENGTH_SHORT).show()
                },
                onColorChange = {
                    colorMode = it
                    Toast.makeText(context, "Цвет: $it", Toast.LENGTH_SHORT).show()
                },
                onClose = { settingsOpen = false }
            )
        }

        if (connectionOpen) {
            ConnectionPanel(
                onAutoSearch = {
                    Toast.makeText(context, "Поиск vMix в сети...", Toast.LENGTH_SHORT).show()
                },
                onClose = { connectionOpen = false }
            )
        }
    }
}

@Composable
private fun TopStatusBar(
    cameraReady: Boolean,
    isLive: Boolean,
    stabilizationMode: String,
    colorMode: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(if (cameraReady) "КАМЕРА ГОТОВА" else "ЗАПУСК КАМЕРЫ")
        StatusChip(if (isLive) "В ЭФИРЕ" else "ГОТОВО")
        StatusChip("СТАБ: $stabilizationMode")
        StatusChip("ЦВЕТ: $colorMode")
    }
}

@Composable
private fun StatusChip(text: String) {
    Text(
        text = text,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun MainLiveButton(
    isLive: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(if (isLive) Color.Red else Color.Black.copy(alpha = 0.65f))
        ) {
            Icon(
                imageVector = if (isLive) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isLive) "СТОП" else "В ЭФИР",
            color = Color.White
        )
    }
}

@Composable
private fun QuickButtons(
    micEnabled: Boolean,
    modifier: Modifier,
    onMicClick: () -> Unit,
    onStabClick: () -> Unit,
    onColorClick: () -> Unit,
    onConnectClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SmallRoundButton(
            icon = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
            onClick = onMicClick
        )

        SmallRoundButton(
            icon = Icons.Default.Videocam,
            onClick = onConnectClick
        )

        SmallRoundButton(
            icon = Icons.Default.AutoFixHigh,
            onClick = onStabClick
        )

        SmallRoundButton(
            icon = Icons.Default.Palette,
            onClick = onColorClick
        )

        SmallRoundButton(
            icon = Icons.Default.Menu,
            onClick = onMenuClick
        )
    }
}

@Composable
private fun SmallRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SideMenu(
    onClose: () -> Unit,
    onSettings: () -> Unit,
    onConnection: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xEE111827))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Меню", color = Color.White, style = MaterialTheme.typography.titleLarge)

            Button(onClick = onConnection, modifier = Modifier.fillMaxWidth()) {
                Text("Подключение к vMix")
            }

            Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Расширенные настройки")
            }

            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    stabilizationMode: String,
    colorMode: String,
    onStabChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xEE111827))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Расширенные настройки", color = Color.White, style = MaterialTheme.typography.titleLarge)

            Text("Стабилизация", color = Color.White)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Выкл", "Стаб", "Стаб+", "Ультра").forEach {
                    Button(
                        onClick = { onStabChange(it) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (stabilizationMode == it) Color.Red else Color.DarkGray
                        )
                    ) {
                        Text(it)
                    }
                }
            }

            Text("Цветокоррекция", color = Color.White)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Натуральный", "Кино", "Яркий", "Ночь").forEach {
                    Button(
                        onClick = { onColorChange(it) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colorMode == it) Color.Red else Color.DarkGray
                        )
                    ) {
                        Text(it)
                    }
                }
            }

            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Готово")
            }
        }
    }
}

@Composable
private fun ConnectionPanel(
    onAutoSearch: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xEE111827))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Подключение к vMix", color = Color.White, style = MaterialTheme.typography.titleLarge)

            Text(
                "Подключи телефон и ПК к одной Wi-Fi сети, затем нажми автопоиск.",
                color = Color.White.copy(alpha = 0.8f)
            )

            Button(onClick = onAutoSearch, modifier = Modifier.fillMaxWidth()) {
                Text("Автопоиск vMix")
            }

            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть")
            }
        }
    }
}
