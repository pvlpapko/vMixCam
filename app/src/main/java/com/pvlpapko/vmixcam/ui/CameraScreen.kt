package com.pvlpapko.vmixcam.ui

import android.content.Context
import android.net.wifi.WifiManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pvlpapko.vmixcam.stream.StreamProfile
import com.pvlpapko.vmixcam.vmix.VmixAutoDiscovery
import com.pvlpapko.vmixcam.vmix.VmixTallyClient
import com.pvlpapko.vmixcam.vmix.NetworkHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var exposure by remember { mutableFloatStateOf(0f) }
    var micEnabled by remember { mutableStateOf(true) }
    var gridEnabled by remember { mutableStateOf(true) }
    var safeEnabled by remember { mutableStateOf(true) }
    var crossEnabled by remember { mutableStateOf(true) }
    var streaming by remember { mutableStateOf(false) }
    var vmixIp by remember { mutableStateOf("auto") }
    var tallyLive by remember { mutableStateOf(false) }
    var discoveryStatus by remember { mutableStateOf("Choose Wi-Fi or auto detect vMix") }
    var currentNetwork by remember { mutableStateOf(NetworkHelper.getCurrentNetworkName(context)) }
    var discovering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val phoneIp = remember { getPhoneIp(context) }

    LaunchedEffect(Unit) {
        discovering = true
        discoveryStatus = "Searching vMix..."
        val found = VmixAutoDiscovery.findVmixHost(context)
        if (found != null) {
            vmixIp = found
            discoveryStatus = "vMix found: $found"
        } else {
            vmixIp = ""
            discoveryStatus = "vMix not found. Enter IP manually."
        }
        discovering = false
    }

    LaunchedEffect(vmixIp) {
        if (vmixIp.isBlank() || vmixIp == "auto") return@LaunchedEffect
        val client = VmixTallyClient(vmixIp)
        while (true) {
            tallyLive = client.isProgramLive()
            delay(1500)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            lensFacing = lensFacing,
            onCameraReady = { camera = it },
            onTap = { x, y, view ->
                val point = view.meteringPointFactory.createPoint(x, y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera?.cameraControl?.startFocusAndMetering(action)
            }
        )
        if (gridEnabled) GridOverlay()
        if (safeEnabled) SafeZoneOverlay()
        if (crossEnabled) CrosshairOverlay()
        TopStatusBar(streaming, tallyLive, phoneIp)
        LeftPanel(
            micEnabled = micEnabled,
            gridEnabled = gridEnabled,
            safeEnabled = safeEnabled,
            crossEnabled = crossEnabled,
            onMic = { micEnabled = !micEnabled },
            onGrid = { gridEnabled = !gridEnabled },
            onSafe = { safeEnabled = !safeEnabled },
            onCross = { crossEnabled = !crossEnabled },
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
        BottomPanel(
            profile = StreamProfile(phoneIp = phoneIp),
            vmixIp = vmixIp,
            onVmixIp = { vmixIp = it },
            discoveryStatus = discoveryStatus,
            discovering = discovering,
            networkName = currentNetwork,
            onChooseWifi = {
                NetworkHelper.openWifiChooser(context)
                currentNetwork = NetworkHelper.getCurrentNetworkName(context)
                discoveryStatus = "Choose the same Wi-Fi as the vMix PC, then press AUTO."
            },
            onAutoDetect = {
                scope.launch {
                    discovering = true
                    discoveryStatus = "Searching vMix..."
                    currentNetwork = NetworkHelper.getCurrentNetworkName(context)
                    val found = VmixAutoDiscovery.findVmixHost(context)
                    if (found != null) {
                        vmixIp = found
                        discoveryStatus = "vMix found: $found"
                    } else {
                        discoveryStatus = "vMix not found. Check Wi-Fi and vMix Web Controller."
                    }
                    discovering = false
                }
            },
            streaming = streaming,
            onStreamToggle = { streaming = !streaming }
        )
    }
}

@Composable
private fun CameraPreview(lensFacing: Int, onCameraReady: (Camera) -> Unit, onTap: (Float, Float, PreviewView) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        },
        update = { view ->
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
private fun TopStatusBar(streaming: Boolean, tally: Boolean, phoneIp: String) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(if (streaming) "STREAM" else "READY", if (streaming) Color(0xFFE53935) else Color(0xFF334155))
            Chip(if (tally) "LIVE IN VMIX" else "PREVIEW", if (tally) Color(0xFFD50000) else Color(0xFF0F766E))
        }
        Text("IP: $phoneIp", color = Color.White)
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(text, color = Color.White, modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
private fun LeftPanel(micEnabled: Boolean, gridEnabled: Boolean, safeEnabled: Boolean, crossEnabled: Boolean, onMic: () -> Unit, onGrid: () -> Unit, onSafe: () -> Unit, onCross: () -> Unit, onFlip: () -> Unit) {
    Column(Modifier.padding(start = 12.dp, top = 70.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IconButtonCard(Icons.Default.Cameraswitch, "Flip", onFlip)
        IconButtonCard(if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff, "Mic", onMic)
        IconButtonCard(Icons.Default.GridOn, "Grid", onGrid, gridEnabled)
        IconButtonCard(Icons.Default.CropFree, "Safe", onSafe, safeEnabled)
        IconButtonCard(Icons.Default.Add, "Center", onCross, crossEnabled)
    }
}

@Composable
private fun IconButtonCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, active: Boolean = true) {
    Column(
        Modifier.width(76.dp).background(if (active) Color(0xAA111827) else Color(0x66111827), RoundedCornerShape(18.dp)).clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RightControls(zoom: Float, exposure: Float, onZoom: (Float) -> Unit, onExposure: (Float) -> Unit) {
    Column(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).width(220.dp).background(Color(0x99111827), RoundedCornerShape(22.dp)).padding(14.dp)) {
        Text("Zoom ${"%.1f".format(zoom)}x", color = Color.White)
        Slider(value = zoom, onValueChange = onZoom, valueRange = 1f..8f)
        Text("Exposure", color = Color.White)
        Slider(value = exposure, onValueChange = onExposure, valueRange = 0f..1f)
    }
}

@Composable
private fun BoxScope.BottomPanel(profile: StreamProfile, vmixIp: String, onVmixIp: (String) -> Unit, discoveryStatus: String, discovering: Boolean, networkName: String, onChooseWifi: () -> Unit, onAutoDetect: () -> Unit, streaming: Boolean, onStreamToggle: () -> Unit) {
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xCC020617)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onChooseWifi) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("WI-FI")
            }
            Text("Network: $networkName", color = Color.White, modifier = Modifier.width(180.dp))
            OutlinedTextField(value = vmixIp, onValueChange = onVmixIp, label = { Text("vMix PC IP") }, singleLine = true, modifier = Modifier.width(190.dp))
            OutlinedButton(onClick = onAutoDetect, enabled = !discovering) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (discovering) "SCAN..." else "AUTO")
            }
            Text(discoveryStatus, color = Color.White, modifier = Modifier.width(260.dp))
            Text("SRT: ${profile.srtUrl}", color = Color.White, modifier = Modifier.weight(1f))
            Text("RTSP: ${profile.rtspUrl}", color = Color.White, modifier = Modifier.weight(1f))
            Button(onClick = onStreamToggle, colors = ButtonDefaults.buttonColors(containerColor = if (streaming) Color(0xFFE53935) else Color(0xFF16A34A))) {
                Text(if (streaming) "STOP" else "START")
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
    drawRect(Color.White.copy(alpha = .30f), topLeft = Offset((size.width - w)/2, (size.height - h)/2), size = Size(w, h), style = Stroke(2f))
}

@Composable
private fun CrosshairOverlay() = Canvas(Modifier.fillMaxSize()) {
    val c = Offset(size.width / 2, size.height / 2)
    drawLine(Color.White.copy(alpha = .55f), Offset(c.x - 28, c.y), Offset(c.x + 28, c.y), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = .55f), Offset(c.x, c.y - 28), Offset(c.x, c.y + 28), strokeWidth = 3f, cap = StrokeCap.Round)
}

private fun getPhoneIp(context: Context): String {
    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val ip = wm.connectionInfo.ipAddress
    return listOf(ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff).joinToString(".")
}
