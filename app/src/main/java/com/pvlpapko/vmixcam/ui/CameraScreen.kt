package com.pvlpapko.vmixcam.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pvlpapko.vmixcam.stream.StreamProfile
import com.pvlpapko.vmixcam.vmix.NetworkHelper
import com.pvlpapko.vmixcam.vmix.VmixAutoDiscovery
import com.pvlpapko.vmixcam.vmix.VmixTallyClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var exposure by remember { mutableFloatStateOf(0.5f) }
    var micEnabled by remember { mutableStateOf(true) }
    var gridEnabled by remember { mutableStateOf(true) }
    var safeEnabled by remember { mutableStateOf(false) }
    var crossEnabled by remember { mutableStateOf(true) }
    var stabilizationMode by remember { mutableStateOf(StabilizationMode.STRONG) }
    var zebraEnabled by remember { mutableStateOf(false) }
    var peakingEnabled by remember { mutableStateOf(false) }
    var lowLightBoost by remember { mutableStateOf(false) }
    var hudHidden by remember { mutableStateOf(false) }
    var streaming by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var connectionPanel by remember { mutableStateOf(false) }
    var vmixIp by remember { mutableStateOf("") }
    var tallyLive by remember { mutableStateOf(false) }
    var discoveryStatus by remember { mutableStateOf("Нажми «Подключение», выбери Wi‑Fi и найди vMix") }
    var currentNetwork by remember { mutableStateOf(NetworkHelper.getCurrentNetworkName(context)) }
    var discovering by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("1080p / 30fps / 8 Mbps") }
    val scope = rememberCoroutineScope()
    val phoneIp = remember { getPhoneIp(context) }
    val gyroState = rememberGyroStabilizer(stabilizationMode)

    LaunchedEffect(vmixIp, connected) {
        if (!connected || vmixIp.isBlank()) return@LaunchedEffect
        val client = VmixTallyClient(vmixIp)
        while (true) {
            tallyLive = client.isProgramLive()
            delay(1200)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            lensFacing = lensFacing,
            stabilizationMode = stabilizationMode,
            gyroState = gyroState,
            onCameraReady = { camera = it },
            onTap = { x, y, view ->
                val point = view.meteringPointFactory.createPoint(x, y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera?.cameraControl?.startFocusAndMetering(action)
            }
        )

        if (lowLightBoost) Box(Modifier.fillMaxSize().background(Color(0x2200FF88)))
        if (gridEnabled) GridOverlay()
        if (safeEnabled) SafeZoneOverlay()
        if (crossEnabled) CrosshairOverlay()
        if (zebraEnabled) ZebraOverlay()
        if (peakingEnabled) FocusPeakingOverlay()

        if (!hudHidden) {
            TopStatusBar(streaming, connected, tallyLive, phoneIp, currentNetwork, profileName)
            LeftPanel(
                micEnabled = micEnabled,
                gridEnabled = gridEnabled,
                safeEnabled = safeEnabled,
                crossEnabled = crossEnabled,
                stabilizationMode = stabilizationMode,
            gyroState = gyroState,
                zebraEnabled = zebraEnabled,
                peakingEnabled = peakingEnabled,
                lowLightBoost = lowLightBoost,
                onMic = { micEnabled = !micEnabled },
                onGrid = { gridEnabled = !gridEnabled },
                onSafe = { safeEnabled = !safeEnabled },
                onCross = { crossEnabled = !crossEnabled },
                onStabilization = { stabilizationMode = stabilizationMode.next() },
                onZebra = { zebraEnabled = !zebraEnabled },
                onPeaking = { peakingEnabled = !peakingEnabled },
                onLowLight = { lowLightBoost = !lowLightBoost },
                onFlip = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }
            )
            RightControls(
                zoom = zoom,
                exposure = exposure,
                onZoom = {
                    zoom = it
                    camera?.cameraControl?.setZoomRatio(it)
                },
                onExposure = {
                    exposure = it
                    val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange
                    if (range != null) {
                        val value = (range.lower + ((range.upper - range.lower) * it)).toInt()
                        camera?.cameraControl?.setExposureCompensationIndex(value)
                    }
                }
            )
            CenterRecordButton(
                streaming = streaming,
                connected = connected,
                onClick = { if (connected) streaming = !streaming else connectionPanel = true }
            )
            BottomPanel(
                profile = StreamProfile(phoneIp = phoneIp),
                vmixIp = vmixIp,
                connectionPanel = connectionPanel,
                connected = connected,
                onVmixIp = { vmixIp = it },
                discoveryStatus = discoveryStatus,
                discovering = discovering,
                networkName = currentNetwork,
                profileName = profileName,
                onProfile = { profileName = it },
                onChooseWifi = {
                    NetworkHelper.openWifiChooser(context)
                    currentNetwork = NetworkHelper.getCurrentNetworkName(context)
                    discoveryStatus = "Выбери ту же Wi‑Fi сеть, где ПК с vMix, потом нажми «Найти vMix»."
                },
                onAutoDetect = {
                    scope.launch {
                        discovering = true
                        connected = false
                        discoveryStatus = "Ищу vMix в сети..."
                        currentNetwork = NetworkHelper.getCurrentNetworkName(context)
                        val found = VmixAutoDiscovery.findVmixHost(context)
                        if (found != null) {
                            vmixIp = found
                            connected = true
                            connectionPanel = false
                            discoveryStatus = "vMix подключен: $found"
                        } else {
                            discoveryStatus = "vMix не найден. Проверь Wi‑Fi и Web Controller в vMix."
                        }
                        discovering = false
                    }
                },
                onManualConnect = {
                    connected = vmixIp.isNotBlank()
                    if (connected) connectionPanel = false
                    discoveryStatus = if (vmixIp.isNotBlank()) "vMix подключен вручную: $vmixIp" else "Введи IP компьютера с vMix"
                },
                onDisconnect = {
                    connected = false
                    streaming = false
                    tallyLive = false
                    discoveryStatus = "Отключено"
                },
                onToggleConnectionPanel = { connectionPanel = !connectionPanel },
                onHideHud = { hudHidden = true }
            )
        } else {
            Button(
                onClick = { hudHidden = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA111827))
            ) { Text("Показать интерфейс") }
        }
    }
}

@Composable
private fun CameraPreview(lensFacing: Int, stabilizationMode: StabilizationMode, gyroState: GyroStabilizationState, onCameraReady: (Camera) -> Unit, onTap: (Float, Float, PreviewView) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val enabled = stabilizationMode != StabilizationMode.OFF
                scaleX = if (enabled) stabilizationMode.cropScale else 1f
                scaleY = if (enabled) stabilizationMode.cropScale else 1f
                translationX = if (enabled) gyroState.offsetX else 0f
                translationY = if (enabled) gyroState.offsetY else 0f
                rotationZ = if (enabled && stabilizationMode.lockHorizon) gyroState.roll else 0f
            },
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = if (stabilizationMode != StabilizationMode.OFF) PreviewView.ScaleType.FILL_CENTER else PreviewView.ScaleType.FIT_CENTER
            }
        },
        update = { view ->
            view.scaleType = if (stabilizationMode != StabilizationMode.OFF) PreviewView.ScaleType.FILL_CENTER else PreviewView.ScaleType.FIT_CENTER
            view.scaleX = 1f
            view.scaleY = 1f
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                provider.unbindAll()
                val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                onCameraReady(cam)
                view.setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) onTap(event.x, event.y, view)
                    true
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun TopStatusBar(streaming: Boolean, connected: Boolean, tally: Boolean, phoneIp: String, network: String, profile: String) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp).background(Color(0x77020617), RoundedCornerShape(24.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(if (streaming) "ЗАПИСЬ / ЭФИР" else "ГОТОВ", if (streaming) Color(0xFFE53935) else Color(0xFF334155))
            Chip(if (connected) "vMix подключен" else "нет подключения", if (connected) Color(0xFF16A34A) else Color(0xFF64748B))
            Chip(if (tally) "ЭФИР" else "ПРЕВЬЮ", if (tally) Color(0xFFD50000) else Color(0xFF0F766E))
        }
        Text("$network  •  IP телефона: $phoneIp  •  $profile", color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(text, color = Color.White, modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
private fun LeftPanel(
    micEnabled: Boolean,
    gridEnabled: Boolean,
    safeEnabled: Boolean,
    crossEnabled: Boolean,
    stabilizationMode: StabilizationMode,
    zebraEnabled: Boolean,
    peakingEnabled: Boolean,
    lowLightBoost: Boolean,
    onMic: () -> Unit,
    onGrid: () -> Unit,
    onSafe: () -> Unit,
    onCross: () -> Unit,
    onStabilization: () -> Unit,
    onZebra: () -> Unit,
    onPeaking: () -> Unit,
    onLowLight: () -> Unit,
    onFlip: () -> Unit
) {
    Column(Modifier.padding(start = 12.dp, top = 76.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        IconButtonCard(Icons.Default.Cameraswitch, "Камера", onFlip)
        IconButtonCard(if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff, "Микро", onMic, micEnabled)
        IconButtonCard(Icons.Default.GridOn, "Сетка", onGrid, gridEnabled)
        IconButtonCard(Icons.Default.CropFree, "Зона", onSafe, safeEnabled)
        IconButtonCard(Icons.Default.Add, "Центр", onCross, crossEnabled)
        IconButtonCard(Icons.Default.Settings, stabilizationMode.shortLabel, onStabilization, stabilizationMode != StabilizationMode.OFF)
        IconButtonCard(Icons.Default.Warning, "Зебра", onZebra, zebraEnabled)
        IconButtonCard(Icons.Default.Visibility, "Пикинг", onPeaking, peakingEnabled)
        IconButtonCard(Icons.Default.AutoFixHigh, "Ночь", onLowLight, lowLightBoost)
    }
}

@Composable
private fun IconButtonCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, active: Boolean = true) {
    Column(
        Modifier.width(78.dp).background(if (active) Color(0xCC111827) else Color(0x66111827), RoundedCornerShape(18.dp)).clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (active) Color.White else Color(0xFF94A3B8))
        Text(label, color = if (active) Color.White else Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BoxScope.RightControls(zoom: Float, exposure: Float, onZoom: (Float) -> Unit, onExposure: (Float) -> Unit) {
    Column(
        Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).width(230.dp)
            .background(Color(0xBB020617), RoundedCornerShape(26.dp)).padding(16.dp)
    ) {
        Text("Зум ${"%.1f".format(zoom)}x", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = zoom, onValueChange = onZoom, valueRange = 1f..8f)
        Spacer(Modifier.height(4.dp))
        Text("Экспозиция", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = exposure, onValueChange = onExposure, valueRange = 0f..1f)
        Spacer(Modifier.height(6.dp))
        Text("Фокус касанием включён", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BoxScope.CenterRecordButton(streaming: Boolean, connected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 126.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(88.dp).background(Color(0xAA020617), CircleShape).padding(9.dp)
                .background(if (streaming) Color(0xFFE11D48) else Color(0xFFF8FAFC), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(if (streaming) 34.dp else 54.dp).background(if (streaming) Color.White else Color(0xFFE11D48), if (streaming) RoundedCornerShape(8.dp) else CircleShape))
        }
        Text(if (connected) if (streaming) "Остановить" else "Начать запись/эфир" else "Сначала подключи vMix", color = Color.White, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun BoxScope.BottomPanel(
    profile: StreamProfile,
    vmixIp: String,
    connectionPanel: Boolean,
    connected: Boolean,
    onVmixIp: (String) -> Unit,
    discoveryStatus: String,
    discovering: Boolean,
    networkName: String,
    profileName: String,
    onProfile: (String) -> Unit,
    onChooseWifi: () -> Unit,
    onAutoDetect: () -> Unit,
    onManualConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleConnectionPanel: () -> Unit,
    onHideHud: () -> Unit
) {
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xEE020617)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onToggleConnectionPanel, colors = ButtonDefaults.buttonColors(containerColor = if (connected) Color(0xFF16A34A) else Color(0xFF2563EB))) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (connected) "Подключено" else "Подключение")
            }
            OutlinedButton(onClick = { onProfile("720p / 30fps / 4 Mbps") }) { Text("720p") }
            OutlinedButton(onClick = { onProfile("1080p / 30fps / 8 Mbps") }) { Text("1080p") }
            OutlinedButton(onClick = { onProfile("1080p / 60fps / 12 Mbps") }) { Text("60fps") }
            Text("Профиль: $profileName", color = Color.White, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onHideHud) { Text("Скрыть интерфейс") }
        }

        if (connectionPanel) {
            Column(Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(18.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (connected) "vMix подключен" else "Подключение к vMix", color = Color.White, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onToggleConnectionPanel) { Text("Закрыть") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onChooseWifi) {
                        Icon(Icons.Default.Wifi, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Выбрать Wi‑Fi")
                    }
                    Text("Сеть: $networkName", color = Color.White, modifier = Modifier.width(210.dp))
                    OutlinedTextField(value = vmixIp, onValueChange = onVmixIp, label = { Text("IP компьютера с vMix") }, singleLine = true, modifier = Modifier.width(220.dp))
                    Button(onClick = onAutoDetect, enabled = !discovering) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (discovering) "Поиск..." else "Найти vMix")
                    }
                    OutlinedButton(onClick = onManualConnect) { Text("Подключить") }
                    if (connected) OutlinedButton(onClick = onDisconnect) { Text("Отключить") }
                }
                Text(discoveryStatus, color = Color(0xFFE2E8F0))
                Text("Для vMix: ${profile.srtUrl}    |    ${profile.rtspUrl}", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GridOverlay() = Canvas(Modifier.fillMaxSize()) {
    val stroke = Stroke(width = 1.5f)
    drawLine(Color.White.copy(alpha = .35f), Offset(size.width / 3, 0f), Offset(size.width / 3, size.height), strokeWidth = stroke.width)
    drawLine(Color.White.copy(alpha = .35f), Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height), strokeWidth = stroke.width)
    drawLine(Color.White.copy(alpha = .35f), Offset(0f, size.height / 3), Offset(size.width, size.height / 3), strokeWidth = stroke.width)
    drawLine(Color.White.copy(alpha = .35f), Offset(0f, size.height * 2 / 3), Offset(size.width, size.height * 2 / 3), strokeWidth = stroke.width)
}

@Composable
private fun SafeZoneOverlay() = Canvas(Modifier.fillMaxSize()) {
    val w = size.width * .86f
    val h = size.height * .82f
    drawRect(Color.White.copy(alpha = .30f), topLeft = Offset((size.width - w) / 2, (size.height - h) / 2), size = Size(w, h), style = Stroke(2f))
}

@Composable
private fun CrosshairOverlay() = Canvas(Modifier.fillMaxSize()) {
    val c = Offset(size.width / 2, size.height / 2)
    drawLine(Color.White.copy(alpha = .55f), Offset(c.x - 28, c.y), Offset(c.x + 28, c.y), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = .55f), Offset(c.x, c.y - 28), Offset(c.x, c.y + 28), strokeWidth = 3f, cap = StrokeCap.Round)
}

@Composable
private fun ZebraOverlay() = Canvas(Modifier.fillMaxSize()) {
    var x = -size.height
    while (x < size.width) {
        drawLine(Color.Yellow.copy(alpha = .22f), Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = 3f)
        x += 34f
    }
}

@Composable
private fun FocusPeakingOverlay() = Canvas(Modifier.fillMaxSize()) {
    val c = Offset(size.width / 2, size.height / 2)
    drawCircle(Color(0xFF22C55E).copy(alpha = .38f), radius = 72f, center = c, style = Stroke(4f))
    drawCircle(Color(0xFF22C55E).copy(alpha = .25f), radius = 142f, center = c, style = Stroke(2f))
}

private enum class StabilizationMode(
    val shortLabel: String,
    val title: String,
    val cropScale: Float,
    val strength: Float,
    val rollStrength: Float,
    val maxOffset: Float,
    val smoothing: Float,
    val lockHorizon: Boolean
) {
    OFF("Стаб выкл", "Стабилизация выключена", 1.00f, 0f, 0f, 0f, 1f, false),
    NORMAL("Стаб", "Обычная стабилизация", 1.04f, 18f, 1.2f, 18f, 0.18f, false),
    STRONG("Стаб+", "Сильная стабилизация", 1.075f, 30f, 2.0f, 32f, 0.12f, false),
    ULTRA("Ультра", "Ультра-плавно + горизонт", 1.12f, 46f, 3.2f, 48f, 0.075f, true);

    fun next(): StabilizationMode = when (this) {
        OFF -> NORMAL
        NORMAL -> STRONG
        STRONG -> ULTRA
        ULTRA -> OFF
    }
}

private class GyroStabilizationState {
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var roll by mutableFloatStateOf(0f)
}

@Composable
private fun rememberGyroStabilizer(mode: StabilizationMode): GyroStabilizationState {
    val context = LocalContext.current
    val state = remember { GyroStabilizationState() }

    DisposableEffect(mode) {
        if (mode == StabilizationMode.OFF) {
            state.offsetX = 0f
            state.offsetY = 0f
            state.roll = 0f
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX = if (abs(event.values[1]) < 0.015f) 0f else -event.values[1] * mode.strength
                val rawY = if (abs(event.values[0]) < 0.015f) 0f else event.values[0] * mode.strength
                val rawRoll = if (abs(event.values[2]) < 0.01f) 0f else -event.values[2] * mode.rollStrength
                state.offsetX = smooth(state.offsetX, rawX.coerceIn(-mode.maxOffset, mode.maxOffset), mode.smoothing)
                state.offsetY = smooth(state.offsetY, rawY.coerceIn(-mode.maxOffset, mode.maxOffset), mode.smoothing)
                state.roll = smooth(state.roll, rawRoll.coerceIn(-2.6f, 2.6f), mode.smoothing)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (gyro != null) {
            sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
            state.offsetX = 0f
            state.offsetY = 0f
            state.roll = 0f
        }
    }

    return state
}

private fun smooth(current: Float, target: Float, factor: Float): Float = current + (target - current) * factor

private fun getPhoneIp(context: Context): String {
    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val ip = wm.connectionInfo.ipAddress
    return listOf(ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff).joinToString(".")
}
