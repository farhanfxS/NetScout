package com.netscout.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netscout.app.model.Device
import com.netscout.app.scanner.DeviceAnalysis
import com.netscout.app.scanner.NearbyBluetoothDevice
import com.netscout.app.scanner.NearbyIntelligence
import com.netscout.app.scanner.NearbyWifiDevice
import com.netscout.app.scanner.ProximityScanResult

@Composable
fun ProximityScreen(
    result: ProximityScanResult?,
    scanning: Boolean,
    lanDevices: List<Device>,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {

    val analyses =
        NearbyIntelligence.analyzeAll(
            lanDevices
        )

    val cameraDevices =
        analyses.filter {
            it.cameraScore >= 50
        }

    val iotDevices =
        analyses.filter {
            it.iotScore >= 50
        }

    val unknownDevices =
        analyses.filter {
            it.cameraScore < 50 &&
            it.iotScore < 50
        }

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        item {

            Text(
                text = "🛰️ PROXIMITY SCAN",
                color = Color(0xFF39FF14),
                fontSize = 20.sp
            )

            Text(
                text =
                    "Detecting Wi-Fi, Bluetooth/BLE and network device indicators",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onScan,
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF123D12)
                )
            ) {

                Text(
                    text =
                        if (scanning)
                            "SCANNING SURROUNDINGS..."
                        else
                            "SCAN SURROUNDINGS"
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            if (scanning) {

                StatusCard(
                    text =
                        "● SEARCHING FOR NEARBY SIGNALS"
                )
            }
        }

        // =====================================================
        // INTELLIGENCE SUMMARY
        // =====================================================

        if (result != null) {

            item {

                IntelligenceSummary(
                    result = result,
                    cameraCount = cameraDevices.size,
                    iotCount = iotDevices.size,
                    unknownCount = unknownDevices.size,
                    lanCount = lanDevices.size
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )
            }

            // =================================================
            // CAMERA INDICATORS
            // =================================================

            if (cameraDevices.isNotEmpty()) {

                item {

                    SectionTitle(
                        text =
                            "📷 POSSIBLE CAMERA / CCTV (${cameraDevices.size})"
                    )
                }

                items(cameraDevices) { analysis ->

                    AnalysisCard(
                        analysis = analysis
                    )
                }
            }

            // =================================================
            // IoT
            // =================================================

            if (iotDevices.isNotEmpty()) {

                item {

                    SectionTitle(
                        text =
                            "🏠 POSSIBLE IoT (${iotDevices.size})"
                    )
                }

                items(iotDevices) { analysis ->

                    AnalysisCard(
                        analysis = analysis
                    )
                }
            }

            // =================================================
            // WI-FI
            // =================================================

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                SectionTitle(
                    text =
                        "📡 WI-FI (${result.wifiDevices.size})"
                )
            }

            items(result.wifiDevices) { device ->

                WifiDeviceCard(
                    device = device
                )
            }

            // =================================================
            // BLUETOOTH
            // =================================================

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                SectionTitle(
                    text =
                        "📶 BLUETOOTH / BLE (${result.bluetoothDevices.size})"
                )
            }

            items(result.bluetoothDevices) { device ->

                BluetoothDeviceCard(
                    device = device
                )
            }

            // =================================================
            // UNKNOWN LAN DEVICES
            // =================================================

            if (unknownDevices.isNotEmpty()) {

                item {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    SectionTitle(
                        text =
                            "❓ NETWORK DEVICES (${unknownDevices.size})"
                    )
                }

                items(unknownDevices) { analysis ->

                    AnalysisCard(
                        analysis = analysis
                    )
                }
            }

            // =================================================
            // LIMITATION
            // =================================================

            item {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(12.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF081108)
                        )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                    ) {

                        Text(
                            text =
                                "⚠️ DETECTION LIMIT",

                            color =
                                Color(0xFFFFD54F),

                            fontSize = 15.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Camera and IoT labels are indicators based on observable network services. They are not proof that a device is a camera. Devices with no detectable radio or network signal may remain invisible.",

                            color =
                                Color.Gray,

                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}


// =============================================================
// INTELLIGENCE SUMMARY
// =============================================================

@Composable
fun IntelligenceSummary(
    result: ProximityScanResult,
    cameraCount: Int,
    iotCount: Int,
    unknownCount: Int,
    lanCount: Int
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(14.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFF081108)
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {

            Text(
                text =
                    "NEARBY ENVIRONMENT",

                color =
                    Color(0xFF39FF14),

                fontSize = 18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Metric(
                    value =
                        result.wifiDevices.size.toString(),

                    label =
                        "📡 Wi-Fi"
                )

                Metric(
                    value =
                        result.bluetoothDevices.size.toString(),

                    label =
                        "📶 BLE"
                )

                Metric(
                    value =
                        result.totalDevices.toString(),

                    label =
                        "📡 TOTAL"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Text(
                text =
                    "DEVICE INTELLIGENCE",

                color =
                    Color.Gray,

                fontSize = 11.sp
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            IndicatorLine(
                label =
                    "📷 Possible Camera / CCTV",

                count =
                    cameraCount
            )

            IndicatorLine(
                label =
                    "🏠 Possible IoT",

                count =
                    iotCount
            )

            IndicatorLine(
                label =
                    "❓ Unknown",

                count =
                    unknownCount
            )

            IndicatorLine(
                label =
                    "🌐 LAN Devices",

                count =
                    lanCount
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    result.message,

                color =
                    Color.Gray,

                fontSize = 11.sp
            )
        }
    }
}


// =============================================================
// METRIC
// =============================================================

@Composable
fun Metric(
    value: String,
    label: String
) {

    Column {

        Text(
            text =
                value,

            color =
                Color(0xFF39FF14),

            fontSize = 22.sp
        )

        Text(
            text =
                label,

            color =
                Color.White,

            fontSize = 11.sp
        )
    }
}


// =============================================================
// INDICATOR LINE
// =============================================================

@Composable
fun IndicatorLine(
    label: String,
    count: Int
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text =
                label,

            color =
                Color.White,

            fontSize = 12.sp
        )

        Text(
            text =
                count.toString(),

            color =
                if (count > 0)
                    Color(0xFF39FF14)
                else
                    Color.Gray,

            fontSize = 12.sp
        )
    }
}


// =============================================================
// ANALYSIS CARD
// =============================================================

@Composable
fun AnalysisCard(
    analysis: DeviceAnalysis
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),

        shape =
            RoundedCornerShape(12.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFF061006)
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
        ) {

            Text(
                text =
                    "● ${analysis.ipAddress}",

                color =
                    Color(0xFF39FF14),

                fontSize = 15.sp
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    analysis.type,

                color =
                    Color.White,

                fontSize = 13.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            if (analysis.cameraScore > 0) {

                ScoreLine(
                    label =
                        "CAMERA INDICATOR",

                    score =
                        analysis.cameraScore
                )
            }

            if (analysis.iotScore > 0) {

                ScoreLine(
                    label =
                        "IoT INDICATOR",

                    score =
                        analysis.iotScore
                )
            }

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    "CONFIDENCE: ${analysis.confidence}",

                color =
                    Color(0xFF39FF14),

                fontSize = 11.sp
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            analysis.reasons.forEach { reason ->

                Text(
                    text =
                        "✓ $reason",

                    color =
                        Color.Gray,

                    fontSize = 11.sp,

                    modifier =
                        Modifier.padding(
                            bottom = 3.dp
                        )
                )
            }
        }
    }
}


// =============================================================
// SCORE
// =============================================================

@Composable
fun ScoreLine(
    label: String,
    score: Int
) {

    Column {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text =
                    label,

                color =
                    Color.Gray,

                fontSize = 9.sp
            )

            Text(
                text =
                    "$score%",

                color =
                    Color(0xFF39FF14),

                fontSize = 10.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(3.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(6.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF102410)
                )
        ) {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(
                            score / 100f
                        )
                        .height(7.dp),

                shape =
                    RoundedCornerShape(6.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF39FF14)
                    )
            ) {}
        }
    }
}


// =============================================================
// WI-FI CARD
// =============================================================

@Composable
fun WifiDeviceCard(
    device: NearbyWifiDevice
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),

        shape =
            RoundedCornerShape(10.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFF061006)
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(13.dp)
        ) {

            Text(
                text =
                    "● ${device.ssid}",

                color =
                    Color(0xFF39FF14),

                fontSize = 15.sp
            )

            Text(
                text =
                    classifyWifi(device),

                color =
                    Color.White,

                fontSize = 12.sp
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            InfoLine(
                "SIGNAL",
                "${signalLabel(device.rssi)} • ${device.rssi} dBm"
            )

            InfoLine(
                "BSSID",
                device.bssid
            )

            InfoLine(
                "FREQUENCY",
                "${device.frequency} MHz"
            )

            InfoLine(
                "CHANNEL",
                if (device.channel >= 0)
                    device.channel.toString()
                else
                    "Unknown"
            )

            InfoLine(
                "SECURITY",
                device.security
            )
        }
    }
}


// =============================================================
// BLUETOOTH CARD
// =============================================================

@Composable
fun BluetoothDeviceCard(
    device: NearbyBluetoothDevice
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),

        shape =
            RoundedCornerShape(10.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                     Color(0xFF061006)
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(13.dp)
        ) {

            Text(
                text =
                    "● ${device.name}",

                color =
                    Color(0xFF39FF14),

                fontSize = 15.sp
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            InfoLine(
                "SIGNAL",
                "${signalLabel(device.rssi)} • ${device.rssi} dBm"
            )

            InfoLine(
                "ADDRESS",
                device.address
            )

            InfoLine(
                "TYPE",
                device.deviceType
            )
        }
    }
}

// =============================================================
// SIGNAL LABEL
// =============================================================

fun signalLabel(
    rssi: Int
): String {

    return when {

        rssi >= -55 ->
            "VERY STRONG"

        rssi >= -67 ->
            "STRONG"

        rssi >= -75 ->
            "MODERATE"

        rssi >= -85 ->
            "WEAK"

        else ->
            "VERY WEAK"
    }
}


// =============================================================
// WI-FI CLASSIFICATION
// =============================================================

fun classifyWifi(
    device: NearbyWifiDevice
): String {

    val name =
        device.ssid.lowercase()

    return when {

        name.contains("camera") ||
        name.contains("cctv") ||
        name.contains("ipc") ->
            "Possible Camera Network"

        name.contains("iot") ||
        name.contains("smart") ||
        name.contains("sensor") ->
            "Possible IoT Network"

        name.contains("router") ||
        name.contains("airtel") ||
        name.contains("jio") ||
        name.contains("tp-link") ||
        name.contains("zyxel") ->
            "Network / Router"

        else ->
            "Wi-Fi Access Point"
    }
}


// =============================================================
// STATUS
// =============================================================

@Composable
fun StatusCard(
    text: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(12.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFF081108)
            )
    ) {

        Text(
            text =
                text,

            color =
                Color(0xFF39FF14),

            fontSize = 14.sp,

            modifier =
                Modifier.padding(16.dp)
        )
    }
}


// =============================================================
// SECTION
// =============================================================

@Composable
fun SectionTitle(
    text: String
) {

    Text(
        text =
            text,

        color =
            Color(0xFF39FF14),

        fontSize = 16.sp,

        modifier =
            Modifier.padding(
                top = 8.dp,
                bottom = 8.dp
            )
    )
}


// =============================================================
// INFO
// =============================================================

@Composable
fun InfoLine(
    label: String,
    value: String
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
    ) {

        Text(
            text =
                label,

            color =
                Color.Gray,

            fontSize = 9.sp
        )

        Text(
            text =
                value,

            color =
                Color.White,

            fontSize = 12.sp
        )
    }
}