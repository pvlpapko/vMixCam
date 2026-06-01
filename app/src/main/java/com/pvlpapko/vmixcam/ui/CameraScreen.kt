package com.pvlpapko.vmixcam.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pvlpapko.vmixcam.stream.ConnectionMode
import com.pvlpapko.vmixcam.stream.ConnectionState
import com.pvlpapko.vmixcam.vmix.NetworkHelper
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private enum class StabilizationMode(val label: String, val crop: Float, val smooth: Float) {
    OFF("Стаб выкл", 1.00f, 0.00f),
    NORMAL("Стаб", 1.05f, 0.18f),
    STRONG("Стаб+", 1.09f, 0.28f),
    ULTRA("Ультра", 1.14f, 0.38f)
}

private enum class ColorPreset(val label: String, val overlay: Color) {
    NATURAL("Натуральный", Color.Transparent),
    CINEMA("Кино", Color(0x33214D8C)),
    VIVID("Яркий", Color(0x2210B981)),
    WARM("Тёплый", Color(0x22F59E0B)),
    COLD("Холодный", Color(0x2238BDF8)),
    NIGHT("Ночь", Color(0x33000000))
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isLive by remember { mutableStateOf(false) }
    var micEnabled by remember { mutableStateOf(hasMic) }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var connectionOpen by remember { mutableStateOf(false) }
    var connectionState by remember { mutableStateOf<ConnectionState?>(null) }
    var stabMode by remember { mutableStateOf(StabilizationMode.OFF) }
    var colorPreset by remember { mutableStateOf(ColorPreset.NATURAL) }
    var gridEnabled by remember { mutableStateOf(false) }
    var safeEnabled by remember { mutableStateOf(false) }
    var peakingEnabled by remember { mutableStateOf(false) }
    var zebraEnabled by remember { mutableStateOf(false) }
    var hideHud by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var gyroX by remember { mutableFloatStateOf(0f) }
    var gyroY by remember { mutableFloatStateOf(0f) }
    var gyroRoll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(stabMode) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val strength = stabMode.smooth
                if (strength > 0f) {
                    gyroX = (gyroX * 0.82f) + (-event.values[1] * 18f * strength)
                    gyroY = (gyroY * 0.82f) + (event.values[0] * 18f * strength)
                    gyroRoll = (gyroRoll * 0.86f) + (-event.values[2] * 2.2f * strength)
                } else {
                    gyroX = 0f; gyroY = 0f; gyroRoll = 0f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (stabMode != StabilizationMode.OFF && gyro != null) {
            sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCamera) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = stabMode.crop
                        scaleY = stabMode.crop
                        translationX = gyroX.coerceIn(-28f, 28f)
                        translationY = gyroY.coerceIn(-28f, 28f)
                        rotationZ = if (stabMode == StabilizationMode.ULTRA) gyroRoll.coerceIn(-2.5f, 2.5f) else 0f
                    }
                    .clickable {
                        val point = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f)
                        camera?.cameraControl?.startFocusAndMetering(
                            FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                        )
                        toast(context, "Фокус")
                    },
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    val providerFuture = ProcessCameraProvider.getInstance(previewView.context)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        runCatching {
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                            cameraReady = true
                            camera?.cameraControl?.setZoomRatio(zoomRatio)
                        }.onFailure {
                            cameraReady = false
                            toast(previewView.context, "Камера не запустилась")
                        }
                    }, ContextCompat.getMainExecutor(previewView.context))
                }
            )
        } else {
            Text("Нет доступа к камере", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        ColorOverlay(colorPreset.overlay)
        if (gridEnabled) GridOverlay()
        if (safeEnabled) SafeZoneOverlay()
        if (zebraEnabled) ZebraOverlay()
        if (peakingEnabled) FocusPeakingOverlay()

        if (!hideHud) {
            TopHud(cameraReady, isLive, micEnabled, stabMode.label, colorPreset.label, connectionState)
            SideQuickButtons(
                micEnabled = micEnabled,
                isLive = isLive,
                onMic = { micEnabled = !micEnabled; toast(context, if (micEnabled) "Микрофон включён" else "Микрофон выключен") },
                onConnect = { connectionOpen = true },
                onStab = { stabMode = nextStab(stabMode); toast(context, "Стабилизация: ${stabMode.label}") },
                onColor = { colorPreset = nextColor(colorPreset); toast(context, "Цвет: ${colorPreset.label}") },
                onMenu = { drawerOpen = true },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
            )
            LiveButton(isLive = isLive, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)) {
                isLive = !isLive
                toast(context, if (isLive) "Эфир включён" else "Эфир остановлен")
            }
            ZoomBar(zoomRatio = zoomRatio, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) { value ->
                zoomRatio = value
                camera?.cameraControl?.setZoomRatio(value)
            }
        }

        if (drawerOpen) {
            DrawerPanel(
                onClose = { drawerOpen = false },
                onSettings = { drawerOpen = false; settingsOpen = true },
                onConnection = { drawerOpen = false; connectionOpen = true },
                onSwitchCamera = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    drawerOpen = false
                    toast(context, "Камера переключена")
                },
                onHideHud = { hideHud = true; drawerOpen = false; toast(context, "HUD скрыт. Нажми экран два раза в будущей версии") }
            )
        }

        if (settingsOpen) {
            SettingsPanel(
                stabMode = stabMode,
                colorPreset = colorPreset,
                gridEnabled = gridEnabled,
                safeEnabled = safeEnabled,
                zebraEnabled = zebraEnabled,
                peakingEnabled = peakingEnabled,
                onStab = { stabMode = it; toast(context, "Стабилизация: ${it.label}") },
                onColor = { colorPreset = it; toast(context, "Цвет: ${it.label}") },
                onGrid = { gridEnabled = !gridEnabled },
                onSafe = { safeEnabled = !safeEnabled },
                onZebra = { zebraEnabled = !zebraEnabled },
                onPeaking = { peakingEnabled = !peakingEnabled },
                onClose = { settingsOpen = false }
            )
        }

        if (connectionOpen) {
            ConnectionPanel(
                currentState = connectionState,
                onStartStream = { state ->
                    connectionState = state.copy(isReady = true)
                    isLive = true
                    connectionOpen = false
                    toast(context, state.statusText())
                },
                onOpenWifi = { NetworkHelper.openWifiChooser(context) },
                onClose = { connectionOpen = false }
            )
        }

        if (hideHud) {
            IconButton(
                onClick = { hideHud = false },
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
            ) { Icon(Icons.Default.Visibility, null, tint = Color.White) }
        }
    }
}

@Composable
private fun TopHud(
    cameraReady: Boolean,
    isLive: Boolean,
    micEnabled: Boolean,
    stab: String,
    color: String,
    connection: ConnectionState?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip(if (cameraReady) "КАМЕРА" else "ЗАПУСК")
        Chip(if (isLive) "● В ЭФИРЕ" else "ГОТОВО", if (isLive) Color(0xCCDC2626) else Color(0x99000000))
        Chip(if (micEnabled) "МИК ВКЛ" else "МИК ВЫКЛ")
        Chip(stab)
        Chip(color)
        Chip(connection?.statusText() ?: "НЕ ПОДКЛЮЧЕНО")
    }
}

@Composable
private fun Chip(text: String, bg: Color = Color(0x99000000)) {
    Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(40.dp)).background(bg).padding(horizontal = 10.dp, vertical = 7.dp))
}

@Composable
private fun SideQuickButtons(
    micEnabled: Boolean,
    isLive: Boolean,
    onMic: () -> Unit,
    onConnect: () -> Unit,
    onStab: () -> Unit,
    onColor: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RoundIcon(if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff, onMic)
        RoundIcon(Icons.Default.SettingsInputAntenna, onConnect)
        RoundIcon(Icons.Default.MotionPhotosAuto, onStab)
        RoundIcon(Icons.Default.Palette, onColor)
        RoundIcon(Icons.Default.Tune, onMenu)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0x99000000))) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun LiveButton(isLive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(82.dp).clip(CircleShape).background(if (isLive) Color(0xFFE11D48) else Color(0xAA111827))) {
            Icon(if (isLive) Icons.Default.Stop else Icons.Default.FiberManualRecord, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Text(if (isLive) "СТОП" else "В ЭФИР", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ZoomBar(zoomRatio: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    Column(modifier = modifier.width(52.dp).clip(RoundedCornerShape(30.dp)).background(Color(0x77000000)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ZoomIn, null, tint = Color.White)
        Slider(value = zoomRatio, onValueChange = onChange, valueRange = 1f..6f, modifier = Modifier.height(140.dp))
        Text("${String.format("%.1f", zoomRatio)}x", color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun DrawerPanel(onClose: () -> Unit, onSettings: () -> Unit, onConnection: () -> Unit, onSwitchCamera: () -> Unit, onHideHud: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
        Column(Modifier.align(Alignment.CenterStart).width(280.dp).fillMaxHeight().background(Color(0xF0111827)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Меню", color = Color.White, style = MaterialTheme.typography.titleLarge)
            MenuButton("Подключение к vMix", Icons.Default.SettingsInputAntenna, onConnection)
            MenuButton("Расширенные настройки", Icons.Default.Tune, onSettings)
            MenuButton("Переключить камеру", Icons.Default.Cameraswitch, onSwitchCamera)
            MenuButton("Скрыть интерфейс", Icons.Default.VisibilityOff, onHideHud)
            Spacer(Modifier.weight(1f))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
        }
    }
}

@Composable
private fun MenuButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(text) }
}

@Composable
private fun SettingsPanel(
    stabMode: StabilizationMode,
    colorPreset: ColorPreset,
    gridEnabled: Boolean,
    safeEnabled: Boolean,
    zebraEnabled: Boolean,
    peakingEnabled: Boolean,
    onStab: (StabilizationMode) -> Unit,
    onColor: (ColorPreset) -> Unit,
    onGrid: () -> Unit,
    onSafe: () -> Unit,
    onZebra: () -> Unit,
    onPeaking: () -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
        Column(Modifier.align(Alignment.Center).width(560.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xF0111827)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Расширенные настройки", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("Стабилизация", color = Color.White, fontWeight = FontWeight.Bold)
            FlowRowLike(StabilizationMode.entries.map { it.label }, stabMode.label) { label -> onStab(StabilizationMode.entries.first { it.label == label }) }
            Text("Цветокоррекция", color = Color.White, fontWeight = FontWeight.Bold)
            FlowRowLike(ColorPreset.entries.map { it.label }, colorPreset.label) { label -> onColor(ColorPreset.entries.first { it.label == label }) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleButton("Сетка", gridEnabled, onGrid)
                ToggleButton("Safe zone", safeEnabled, onSafe)
                ToggleButton("Zebra", zebraEnabled, onZebra)
                ToggleButton("Peaking", peakingEnabled, onPeaking)
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Готово") }
        }
    }
}

@Composable
private fun FlowRowLike(items: List<String>, selected: String, onClick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item -> Button(onClick = { onClick(item) }, colors = ButtonDefaults.buttonColors(containerColor = if (item == selected) Color(0xFFE11D48) else Color.DarkGray)) { Text(item) } }
    }
}

@Composable
private fun ToggleButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = if (selected) Color(0xFF0EA5E9) else Color.DarkGray)) { Text(text) }
}

@Composable
private fun ConnectionPanel(currentState: ConnectionState?, onStartStream: (ConnectionState) -> Unit, onOpenWifi: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val phoneIp = remember { NetworkHelper.getLocalIpv4(context) ?: getLocalIpFallback() }
    var mode by remember { mutableStateOf(currentState?.mode ?: ConnectionMode.SRT_LISTENER) }
    var vmixIp by remember { mutableStateOf(currentState?.vmixIp ?: "") }
    var portText by remember { mutableStateOf((currentState?.port ?: 9999).toString()) }
    var latencyText by remember { mutableStateOf((currentState?.latency ?: 120).toString()) }
    val port = portText.toIntOrNull() ?: 9999
    val latency = latencyText.toIntOrNull() ?: 120
    val state = ConnectionState(mode, phoneIp, vmixIp, port, latency)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
        Column(Modifier.align(Alignment.Center).width(560.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xF0111827)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Подключение к vMix", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton("SRT Listener", mode == ConnectionMode.SRT_LISTENER) { mode = ConnectionMode.SRT_LISTENER }
                ModeButton("SRT Caller", mode == ConnectionMode.SRT_CALLER) { mode = ConnectionMode.SRT_CALLER }
                ModeButton("RTSP", mode == ConnectionMode.RTSP) { mode = ConnectionMode.RTSP }
            }
            Text("IP телефона: $phoneIp", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(portText, { portText = it.filter(Char::isDigit) }, label = { Text("Порт") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(latencyText, { latencyText = it.filter(Char::isDigit) }, label = { Text("Задержка SRT") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            if (mode == ConnectionMode.SRT_CALLER) {
                OutlinedTextField(vmixIp, { vmixIp = it }, label = { Text("IP компьютера с vMix") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Text("Для vMix:", color = Color.White, fontWeight = FontWeight.Bold)
            OutlinedTextField(state.vmixUrl(), {}, readOnly = true, modifier = Modifier.fillMaxWidth())
            Text(connectionHelp(mode), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenWifi, modifier = Modifier.weight(1f)) { Text("Выбрать Wi‑Fi") }
                Button(onClick = { copy(context, state.vmixUrl()) }, modifier = Modifier.weight(1f)) { Text("Скопировать") }
            }
            Button(onClick = { onStartStream(state) }, modifier = Modifier.fillMaxWidth()) { Text("Запустить поток") }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = if (selected) Color(0xFFE11D48) else Color.DarkGray)) { Text(text) }
}

private fun connectionHelp(mode: ConnectionMode): String = when (mode) {
    ConnectionMode.SRT_LISTENER -> "В vMix: Add Input → Stream/SRT → SRT Caller → Host = IP телефона, Port = порт выше."
    ConnectionMode.SRT_CALLER -> "В vMix: Add Input → Stream/SRT → SRT Listener → Port = порт выше. IP vMix вводится на телефоне."
    ConnectionMode.RTSP -> "В vMix: Add Input → Stream/RTSP и вставь адрес RTSP выше."
}

@Composable
private fun ColorOverlay(color: Color) { if (color != Color.Transparent) Box(Modifier.fillMaxSize().background(color)) }

@Composable
private fun GridOverlay() { Canvas(Modifier.fillMaxSize()) { val w=size.width; val h=size.height; val c=Color.White.copy(alpha=.35f); drawLine(c, Offset(w/3,0f), Offset(w/3,h), 1.5f); drawLine(c, Offset(2*w/3,0f), Offset(2*w/3,h), 1.5f); drawLine(c, Offset(0f,h/3), Offset(w,h/3), 1.5f); drawLine(c, Offset(0f,2*h/3), Offset(w,2*h/3), 1.5f) } }
@Composable
private fun SafeZoneOverlay() { Canvas(Modifier.fillMaxSize()) { val padX=size.width*.08f; val padY=size.height*.08f; drawRect(Color.White.copy(alpha=.32f), Offset(padX,padY), androidx.compose.ui.geometry.Size(size.width-padX*2,size.height-padY*2), style=androidx.compose.ui.graphics.drawscope.Stroke(2f)) } }
@Composable
private fun ZebraOverlay() { Canvas(Modifier.fillMaxSize()) { val c=Color.Yellow.copy(alpha=.18f); var x=-size.height; while (x<size.width) { drawLine(c, Offset(x,0f), Offset(x+size.height,size.height), 3f); x+=34f } } }
@Composable
private fun FocusPeakingOverlay() { Canvas(Modifier.fillMaxSize()) { val c=Color.Green.copy(alpha=.45f); drawCircle(c, 4f, Offset(size.width*.48f,size.height*.45f)); drawCircle(c, 4f, Offset(size.width*.52f,size.height*.53f)); drawCircle(c, 4f, Offset(size.width*.57f,size.height*.47f)) } }

private fun nextStab(current: StabilizationMode): StabilizationMode = when (current) { StabilizationMode.OFF -> StabilizationMode.NORMAL; StabilizationMode.NORMAL -> StabilizationMode.STRONG; StabilizationMode.STRONG -> StabilizationMode.ULTRA; StabilizationMode.ULTRA -> StabilizationMode.OFF }
private fun nextColor(current: ColorPreset): ColorPreset = when (current) { ColorPreset.NATURAL -> ColorPreset.CINEMA; ColorPreset.CINEMA -> ColorPreset.VIVID; ColorPreset.VIVID -> ColorPreset.WARM; ColorPreset.WARM -> ColorPreset.COLD; ColorPreset.COLD -> ColorPreset.NIGHT; ColorPreset.NIGHT -> ColorPreset.NATURAL }
private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
private fun copy(context: Context, text: String) { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("vMix URL", text)); toast(context, "Скопировано") }
private fun getLocalIpFallback(): String = "0.0.0.0"
