package com.pvlpapko.vmixcam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraScreen() {

    var isStreaming by remember { mutableStateOf(false) }
    var micEnabled by remember { mutableStateOf(true) }
    var gridEnabled by remember { mutableStateOf(false) }
    var safeEnabled by remember { mutableStateOf(false) }
    var crossEnabled by remember { mutableStateOf(false) }
    var zebraEnabled by remember { mutableStateOf(false) }

    var stabilizationMode by remember {
        mutableStateOf("Ультра")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        )

        LeftPanel(
            micEnabled = micEnabled,
            gridEnabled = gridEnabled,
            safeEnabled = safeEnabled,
            crossEnabled = crossEnabled,
            stabilizationMode = stabilizationMode,
            zebraEnabled = zebraEnabled,
            onMicToggle = {
                micEnabled = !micEnabled
            },
            onGridToggle = {
                gridEnabled = !gridEnabled
            },
            onSafeToggle = {
                safeEnabled = !safeEnabled
            },
            onCrossToggle = {
                crossEnabled = !crossEnabled
            },
            onZebraToggle = {
                zebraEnabled = !zebraEnabled
            },
            onStabilizationChange = {
                stabilizationMode = it
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                onClick = {
                    isStreaming = !isStreaming
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                    if (isStreaming) Color.Red
                    else Color.DarkGray
                ),
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text =
                if (isStreaming) "ЭФИР"
                else "НАЧАТЬ",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun LeftPanel(
    micEnabled: Boolean,
    gridEnabled: Boolean,
    safeEnabled: Boolean,
    crossEnabled: Boolean,
    zebraEnabled: Boolean,
    stabilizationMode: String,
    onMicToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onSafeToggle: () -> Unit,
    onCrossToggle: () -> Unit,
    onZebraToggle: () -> Unit,
    onStabilizationChange: (String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Button(onClick = onMicToggle) {
            Text(if (micEnabled) "Микрофон: ВКЛ" else "Микрофон: ВЫКЛ")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onGridToggle) {
            Text(if (gridEnabled) "Сетка: ВКЛ" else "Сетка: ВЫКЛ")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onSafeToggle) {
            Text(if (safeEnabled) "Safe Zone: ВКЛ" else "Safe Zone: ВЫКЛ")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onCrossToggle) {
            Text(if (crossEnabled) "Центр: ВКЛ" else "Центр: ВЫКЛ")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onZebraToggle) {
            Text(if (zebraEnabled) "Zebra: ВКЛ" else "Zebra: ВЫКЛ")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Стабилизация",
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {

            listOf(
                "Выкл",
                "Стаб",
                "Стаб+",
                "Ультра"
            ).forEach { mode ->

                Button(
                    onClick = {
                        onStabilizationChange(mode)
                    },
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(mode)
                }
            }
        }
    }
}
