package com.netscout.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.ExperimentalMaterial3Api
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.netscout.app.scanner.ProximityScanResult
import com.netscout.app.scanner.ProximityScanner
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netscout.app.model.Device
import com.netscout.app.scanner.NetworkInfo
import com.netscout.app.scanner.NetworkScanner
import com.netscout.app.scanner.WanScanResult
import com.netscout.app.scanner.WanTargetResult
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NetScoutScreen()
        }
    }
}

// =============================================================
// MAIN SCREEN
// =============================================================

@Composable
fun NetScoutScreen() {

    val context = LocalContext.current
    

    var lanMode by remember {
        mutableStateOf(true)
    }
    
    var proximityMode by remember {
        mutableStateOf(false)
    }

    var proximityScanning by remember {
        mutableStateOf(false)
    }

    var proximityResult by remember {
        mutableStateOf<ProximityScanResult?>(null)
    }

    var securityMode by remember {
        mutableStateOf(false)
    }

    var securityScanning by remember {
        mutableStateOf(false)
    }

    var securityResult by remember {
        mutableStateOf<SecurityAssessment?>(null)
    }
    
    var genMode by remember { 
        mutableStateOf(false)
    }
    var codeLabMode by remember { 
        mutableStateOf("APP BUILDER") 
    }
    var codeLabRequest by remember { 
        mutableStateOf("") 
    }
    var generatedProject by remember { 
        mutableStateOf<GeneratedProject?>(null) 
    }

    var devices by remember {
        mutableStateOf(emptyList<Device>())
    }

    var scanning by remember {
        mutableStateOf(false)
    }

    var showDetails by remember {
        mutableStateOf(false)
    }
    
    var selectedDevice by remember {
        mutableStateOf<Device?>(null)
    }

    var subnet by remember {
        mutableStateOf("Network not detected")
    }

    // ---------------------------------------------------------
    // WAN STATE
    // ---------------------------------------------------------

    var wanScanning by remember {
        mutableStateOf(false)
    }

    var wanResult by remember {
        mutableStateOf<WanScanResult?>(null)
    }

    var targetAddress by remember {
        mutableStateOf("")
    }

    var targetScanning by remember {
        mutableStateOf(false)
    }

    var targetResult by remember {
        mutableStateOf<WanTargetResult?>(null)
    }

    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }
    
    val proximityScanner =
        remember {
            ProximityScanner(context)
        }


    val permissionLauncher =
    rememberLauncherForActivityResult(
        contract =
            ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->

        val wifiAllowed =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val bluetoothAllowed =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionsResult[
                    Manifest.permission.BLUETOOTH_SCAN
                ] == true
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }

        if (wifiAllowed || bluetoothAllowed) {

            proximityScanning = true

            proximityScanner.scan { result ->

                mainHandler.post {

                    proximityResult = result

                    proximityScanning = false
                }
            }
        }
    }

fun startProximityScan() {

    val permissions = mutableListOf<String>()

    permissions.add(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(
            Manifest.permission.BLUETOOTH_SCAN
        )

        permissions.add(
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }

    val missing = permissions.filter {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) != PackageManager.PERMISSION_GRANTED
    }

    if (missing.isNotEmpty()) {

        permissionLauncher.launch(
            missing.toTypedArray()
        )

    } else {

        proximityScanning = true

        proximityScanner.scan { result ->

            mainHandler.post {

                proximityResult = result
                proximityScanning = false

            }
        }
    }
}

// =========================================================
// LAN SCAN
// =========================================================

fun startScan() {

    if (scanning) {
        return
    }

    val prefix =
        NetworkInfo.getSubnetPrefix(context)

    if (prefix == null) {

        subnet = "Wi-Fi network not detected"
        devices = emptyList()

        return
    }

    subnet = "$prefix.x"

    devices = emptyList()

    scanning = true

    Thread {

        NetworkScanner().scan(
            subnetPrefix = prefix
        ) { result ->

            val baseType =
                if (
                    result.deviceTypeHint.isNotBlank() &&
                    result.deviceTypeHint != "Unknown"
                ) {
                    result.deviceTypeHint
                } else {
                    classifyDevice(
                        result.openPorts
                    )
                }

            // =================================================
            // LAN DEVICE INTELLIGENCE
            // =================================================

            val intelligence =
                analyzeLanDevice(
                    hostname = result.hostname,
                    macAddress = result.macAddress,
                    openPorts = result.openPorts,
                    services = result.services,
                    existingType = baseType
                )

            val device =
                Device(

                    ipAddress =
                        result.ipAddress,

                    hostname =
                        result.hostname,

                    macAddress =
                        result.macAddress,

                    openPorts =
                        result.openPorts
                            .distinct()
                            .sorted(),

                    services =
                        result.services
                            .distinct(),

                    deviceType =
                        intelligence.deviceType,

                    manufacturer =
                        if (
                            result.macAddress.isBlank() ||
                            result.macAddress == "Unknown"
                        ) {
                            "Unknown"
                        } else {
                            "Detected from MAC"
                        },

                    confidence =
                        intelligence.confidence,

                    discoverySources =
                        intelligence.discoverySources,

                    isActive = true
                )

            mainHandler.post {

                val existingIndex =
                    devices.indexOfFirst {
                        it.ipAddress ==
                            device.ipAddress
                    }

                if (existingIndex >= 0) {

                    val updated =
                        devices.toMutableList()

                    updated[existingIndex] =
                        device

                    devices = updated

                } else {

                    devices =
                        devices + device
                }
            }
        }

        mainHandler.post {

            scanning = false
        }

    }.start()
}


    // =========================================================
    // WAN SCAN
    // =========================================================

    fun startWanScan() {

        if (wanScanning) {
            return
        }

        wanScanning = true
        wanResult = null

        Thread {

            val result =
                NetworkScanner().checkWan(context)

            mainHandler.post {

                wanResult = result
                wanScanning = false
            }

        }.start()
    }


    // =========================================================
    // AUTHORIZED TARGET SCAN
    // =========================================================

    fun startTargetScan() {

        val target =
            targetAddress.trim()

        if (target.isEmpty() || targetScanning) {
            return
        }

        targetScanning = true
        targetResult = null

        NetworkScanner().scanAuthorizedTarget(
            target = target
        ) { result ->

            mainHandler.post {

                targetResult = result
                targetScanning = false
            }
        }
    }


    // =========================================================
    // SECURITY ASSESSMENT
    // =========================================================

    fun startSecurityAssessment() {

        val target = targetAddress.trim()

        if (target.isEmpty() || securityScanning) {
            return
        }

        securityScanning = true
        securityResult = null

        NetworkScanner().scanAuthorizedTarget(
            target = target
        ) { result ->

            val findings = mutableListOf<SecurityFinding>()

            result.openPorts.forEach { port ->

                when (port) {
                    23 -> findings.add(
                        SecurityFinding(
                            "CRITICAL",
                            "Telnet exposed",
                            "Port 23 is reachable; Telnet provides weak legacy remote administration."
                        )
                    )
                    21 -> findings.add(
                        SecurityFinding(
                            "HIGH",
                            "FTP exposed",
                            "Port 21 is reachable; review whether unencrypted FTP is required."
                        )
                    )
                    3389 -> findings.add(
                        SecurityFinding(
                            "HIGH",
                            "RDP exposed",
                            "Remote Desktop is reachable; verify access controls, MFA and network restrictions."
                        )
                    )
                    445 -> findings.add(
                        SecurityFinding(
                            "HIGH",
                            "SMB exposed",
                            "SMB is reachable; verify authentication, patching and exposure boundaries."
                        )
                    )
                    80 -> findings.add(
                        SecurityFinding(
                            "MEDIUM",
                            "HTTP service exposed",
                            "Unencrypted HTTP is reachable; prefer HTTPS where supported."
                        )
                    )
                    554 -> findings.add(
                        SecurityFinding(
                            "MEDIUM",
                            "RTSP service detected",
                            "RTSP is commonly used by cameras and media devices; verify authentication and exposure."
                        )
                    )
                    1883 -> findings.add(
                        SecurityFinding(
                            "MEDIUM",
                            "MQTT service detected",
                            "MQTT is commonly used by IoT systems; verify authentication and transport security."
                        )
                    )
                    3306 -> findings.add(
                        SecurityFinding(
                            "HIGH",
                            "Database service exposed",
                            "MySQL is reachable; database services should normally be restricted to trusted hosts."
                        )
                    )
                    22 -> findings.add(
                        SecurityFinding(
                            "INFO",
                            "SSH service detected",
                            "SSH is reachable; review keys, authentication policy and exposure."
                        )
                    )
                }
            }

            if (result.openPorts.isEmpty()) {
                findings.add(
                    SecurityFinding(
                        "INFO",
                        "No tested services exposed",
                        "No ports from the current authorized service set were detected."
                    )
                )
            }

            val score = 
                findings.fold(0) { total, finding ->
                    total + when (finding.severity) {
                    "CRITICAL" -> 40
                    "HIGH" -> 20
                    "MEDIUM" -> 10
                    else -> 0
                }
            }.coerceAtMost(100)

            val risk = when {
                score >= 70 -> "CRITICAL"
                score >= 40 -> "HIGH"
                score >= 20 -> "MEDIUM"
                else -> "LOW"
            }

            mainHandler.post {
                securityResult = SecurityAssessment(
                    target = result.target,
                    resolvedIp = result.resolvedIp,
                    reachable = result.reachable,
                    openPorts = result.openPorts,
                    risk = risk,
                    score = score,
                    findings = findings
                )
                securityScanning = false
            }
        }
    }
    // =========================================================
    // MAIN UI
    // =========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020602))
            .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "NETSCOUT",
            color = Color(0xFF39FF14),
            fontSize = 28.sp
        )

        Text(
            text =
                when {
                    securityMode -> "CYBERSECURITY ASSESSMENT CONSOLE"
                    genMode -> "AI DEVELOPMENT & SECURITY LAB"
                    lanMode -> "NEARBY DEVICE SCANNER"
                    else -> "WIDE AREA NETWORK MONITOR"
                },

            color = Color.Gray,
            fontSize = 12.sp
        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =====================================================
        // MAIN MODE SWITCH
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Button(
                onClick = {
                    lanMode = true
                    proximityMode = false
                    securityMode = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (lanMode && !proximityMode && !securityMode)
                            Color(0xFF1D661D)
                        else
                            Color(0xFF081108)
                )
            ) {
                Text("LAN")
            }

            Button(
                onClick = {
                    lanMode = false
                    proximityMode = false
                    securityMode = false
                    genMode = false
                    if (wanResult == null) {
                        startWanScan()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (!lanMode && !proximityMode && !securityMode && !genMode)
                            Color(0xFF1D661D)
                        else
                            Color(0xFF081108)
                )
            ) {
                Text("WAN")
            }

            Button(
                onClick = {
                    lanMode = false
                    proximityMode = true
                    securityMode = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (proximityMode)
                            Color(0xFF1D661D)
                        else
                            Color(0xFF081108)
                )
            ) {
                Text("NEAR")
            }

            Button(
                onClick = {
                    lanMode = false
                    proximityMode = false
                    securityMode = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (securityMode)
                            Color(0xFF1D661D)
                        else
                            Color(0xFF081108)
                )
            ) {
                Text("SEC")
            }
            Button(
                onClick = {
                    lanMode = false
                    proximityMode = false
                    securityMode = false
                    genMode = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                containerColor = 
                    if (genMode) 
                       Color(0xFF1B5E20) 
                    else 
                       Color(0xFF001005)
               )
           ) {
               Text("GEN")
           }
           
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        if (lanMode) {

            // =================================================
            // LAN MODE
            // =================================================

            Radar(
                devices = devices,

                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Text(
                text =
                    if (scanning)
                        "● SCANNING NETWORK"
                    else
                        "● LAN SCAN READY",

                color = Color(0xFF39FF14),
                fontSize = 16.sp
            )


            Spacer(
                modifier = Modifier.height(5.dp)
            )


            Text(
                text =
                    "${devices.size} DEVICES DETECTED",

                color = Color.White,
                fontSize = 14.sp
            )


            Text(
                text = subnet,
                color = Color.Gray,
                fontSize = 12.sp
            )


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = {
                        startScan()
                    },

                    enabled = !scanning,

                    modifier =
                        Modifier.weight(1f),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF123D12)
                        )
                ) {

                    Text(
                        if (scanning)
                            "SCANNING..."
                        else
                            "SCAN AGAIN"
                    )
                }


                Button(
                    onClick = {
                        showDetails = true
                    },

                    modifier =
                        Modifier.weight(1f),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF123D12)
                        )
                ) {

                    Text("DETAILS")
                }
            }

        } else if (proximityMode) {

    ProximityScreen(
        result = proximityResult,
        scanning = proximityScanning,
        lanDevices = emptyList(),
        onScan = {
            startProximityScan()
        },
        modifier = Modifier.weight(1f)
    )

} else if (securityMode) {

            SecurityScreen(
                targetAddress = targetAddress,
                scanning = securityScanning,
                assessment = securityResult,
                onTargetChange = {
                    targetAddress = it
                },
                onAssess = {
                    startSecurityAssessment()
                },
                modifier = Modifier.weight(1f)
            )
            
            } else if (genMode) {
    CodeLabScreen(
        request = codeLabRequest,
        mode = codeLabMode,
        project = generatedProject,
        onRequestChange = { codeLabRequest = it },
        onModeChange = { codeLabMode = it },
        onGenerate = {
            generatedProject = CodeLabEngine.generate(
                mode = codeLabMode,
                request = codeLabRequest
            )
        },
        modifier = Modifier.weight(1f)
    )

        } else {

            // =================================================
            // WAN MODE
            // =================================================

            WanScreen(
                wanScanning = wanScanning,
                wanResult = wanResult,
                targetAddress = targetAddress,
                targetScanning = targetScanning,
                targetResult = targetResult,

                onWanScan = {
                    startWanScan()
                },

                onTargetChange = {
                    targetAddress = it
                },

                onTargetScan = {
                    startTargetScan()
                },

                modifier =
                    Modifier.weight(1f)
            )
        }
    }
    
    if (selectedDevice != null) {

    val device = selectedDevice!!

    AlertDialog(
        onDismissRequest = {
            selectedDevice = null
        },

        containerColor =
            Color(0xFF050A05),

        title = {
            Text(
                text = "DEVICE ANALYSIS",
                color = Color(0xFF39FF14),
                fontSize = 21.sp
            )
        },

        text = {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(500.dp)
            ) {

                item {

                    Text(
                        text = "NETWORK IDENTITY",
                        color = Color(0xFF39FF14),
                        fontSize = 15.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    WanInfoRow(
                        label = "IP ADDRESS",
                        value = device.ipAddress
                    )

                    WanInfoRow(
                        label = "MAC ADDRESS",
                        value =
                            if (
                                device.macAddress.isBlank() ||
                                device.macAddress == "Unknown"
                            ) {
                                "Unknown"
                            } else {
                                device.macAddress
                            }
                    )

                    WanInfoRow(
                        label = "HOSTNAME",
                        value =
                            if (device.hostname.isBlank()) {
                                "Unknown"
                            } else {
                                device.hostname
                            }
                    )

                    WanInfoRow(
                        label = "DEVICE TYPE",
                        value = device.deviceType
                    )

                    WanInfoRow(
                        label = "OPEN PORTS",
                        value =
                            if (device.openPorts.isEmpty()) {
                                "None detected"
                            } else {
                                device.openPorts
                                    .distinct()
                                    .sorted()
                                    .joinToString(", ")
                            }
                    )

                    WanInfoRow(
                        label = "SERVICES",
                        value =
                            if (device.services.isEmpty()) {
                                "None detected"
                            } else {
                                device.services
                                    .distinct()
                                    .joinToString(", ")
                            }
                    )

                    WanInfoRow(
                        label = "STATUS",
                        value =
                            if (device.isActive) {
                                "● ACTIVE"
                            } else {
                                "● INACTIVE"
                            }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "This analysis is based on information observable from the local network. Device classification is an estimate and may not identify the exact hardware.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {
                    selectedDevice = null
                },

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF123D12)
                    )
            ) {

                Text(
                    text = "CLOSE",
                    color = Color(0xFF39FF14)
                )
            }
        }
    )
}

    // =========================================================
    // LAN DETAILS POPUP
    // =========================================================

    if (showDetails) {

        AlertDialog(

            onDismissRequest = {
                showDetails = false
            },

            containerColor =
                Color(0xFF050A05),

            title = {

                Text(
                    text = "DETECTED DEVICES",

                    color =
                        Color(0xFF39FF14),

                    fontSize = 22.sp
                )
            },

            text = {

                if (devices.isEmpty()) {

                    Text(
                        text = "No devices detected.",

                        color =
                            Color.White,

                        fontSize = 15.sp
                    )

                } else {

                    LazyColumn(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(430.dp)
                    ) {

                        val networkDevices =
                            devices.filter {
                                it.deviceType ==
                                    "Network Device"
                            }

                        if (networkDevices.isNotEmpty()) {

                            item {

                                CategoryTitle(
                                    text =
                                        "📡 NETWORK (${networkDevices.size})"
                                )
                            }

                            items(networkDevices) { device ->

                                DeviceCard(
                                    device = device,
                                    onClick = {
                                        selectedDevice = device
                                    }
                                )
                            }
                        }


                        val cameras =
                            devices.filter {
                                it.deviceType ==
                                    "Possible IP Camera"
                            }

                        if (cameras.isNotEmpty()) {

                            item {

                                CategoryTitle(
                                    text =
                                        "📷 CAMERAS (${cameras.size})"
                                )
                            }

                            items(cameras) { device ->

                                DeviceCard(
                                    device = device
                                )
                            }
                        }


                        val computers =
                            devices.filter {
                                it.deviceType ==
                                    "Linux / Network Device"
                            }

                        if (computers.isNotEmpty()) {

                            item {

                                CategoryTitle(
                                    text =
                                        "💻 COMPUTERS (${computers.size})"
                                )
                            }

                            items(computers) { device ->

                                DeviceCard(
                                    device = device,
                                    onClick = {
                                        selectedDevice = device
                                    }
                                )
                            }
                        }


                        val phones =
                            devices.filter {
                                it.deviceType ==
                                    "Phone"
                            }

                        if (phones.isNotEmpty()) {

                            item {

                                CategoryTitle(
                                    text =
                                        "📱 PHONES (${phones.size})"
                                )
                            }

                            items(phones) { device ->

                                DeviceCard(
                                    device = device,
                                    onClick = {
                                        selectedDevice = device
                                    }
                                )
                            }
                        }


                        val unknownDevices =
                            devices.filter {
                                it.deviceType ==
                                    "Unknown Device"
                            }

                        if (unknownDevices.isNotEmpty()) {

                            item {

                                CategoryTitle(
                                    text =
                                        "❓ UNKNOWN (${unknownDevices.size})"
                                )
                            }

                            items(unknownDevices) { device ->

                                DeviceCard(
                                    device = device,
                                    onClick = {
                                        selectedDevice = device
                                    }
                                )
                            }
                        }
                    }
                }
            },

            confirmButton = {

                Button(

                    onClick = {
                        showDetails = false
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF123D12)
                        )
                ) {

                    Text(
                        text = "CLOSE",

                        color =
                            Color(0xFF39FF14)
                    )
                }
            }
        )
    }
}


// =============================================================
// WAN SCREEN
// =============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WanScreen(
    wanScanning: Boolean,
    wanResult: WanScanResult?,
    targetAddress: String,
    targetScanning: Boolean,
    targetResult: WanTargetResult?,
    onWanScan: () -> Unit,
    onTargetChange: (String) -> Unit,
    onTargetScan: () -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {

        item {

            Text(
                text = "🌐 WAN STATUS",

                color =
                    Color(0xFF39FF14),

                fontSize = 20.sp
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            WanStatusCard(
                result = wanResult,
                scanning = wanScanning
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            Button(
                onClick = onWanScan,

                enabled = !wanScanning,

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF123D12)
                    )
            ) {

                Text(
                    if (wanScanning)
                        "CHECKING WAN..."
                    else
                        "REFRESH WAN STATUS"
                )
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            Text(
                text = "AUTHORIZED TARGET",

                color =
                    Color(0xFF39FF14),

                fontSize = 18.sp
            )


            Text(
                text =
                    "Scan only systems you own or are authorized to test.",

                color = Color.Gray,

                fontSize = 11.sp
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            OutlinedTextField(

                value = targetAddress,

                onValueChange = onTargetChange,

                modifier =
                    Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),         
                singleLine = true,

                label = {
                    Text("IP address or domain")
                },

                placeholder = {
                    Text("example.com")
                }
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Button(
                onClick = onTargetScan,

                enabled =
                    targetAddress.trim().isNotEmpty() &&
                    !targetScanning,

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF123D12)
                    )
            ) {

                Text(
                    if (targetScanning)
                        "SCANNING TARGET..."
                    else
                        "SCAN AUTHORIZED TARGET"
                )
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            if (targetResult != null) {

                TargetResultCard(
                    result = targetResult
                )
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            Text(
                text = "WAN DISCOVERY NOTE",

                color =
                    Color(0xFF39FF14),

                fontSize = 16.sp
            )


            Spacer(
                modifier = Modifier.height(5.dp)
            )


            Text(
                text =
                    "WAN does not provide a global list of nearby internet devices. NAT, firewalls and private networks normally prevent that. NetScout can monitor your WAN connection and inspect specific authorized targets.",

                color = Color.Gray,

                fontSize = 12.sp
            )
        }
    }
}


// =============================================================
// WAN STATUS CARD
// =============================================================

@Composable
fun WanStatusCard(
    result: WanScanResult?,
    scanning: Boolean
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

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {

            Text(
                text =
                    if (scanning)
                        "● CHECKING CONNECTION..."
                    else if (result?.internetActive == true)
                        "● INTERNET ACTIVE"
                    else if (result != null)
                        "● INTERNET INACTIVE"
                    else
                        "● WAN STATUS UNKNOWN",

                color =
                    if (result?.internetActive == true)
                        Color(0xFF39FF14)
                    else
                        Color.Gray,

                fontSize = 16.sp
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            WanInfoRow(
                label = "PUBLIC IP",
                value =
                    result?.publicIp ?: "Not checked"
            )


            WanInfoRow(
                label = "LATENCY",
                value =
                    if (result?.latencyMs ?: -1L >= 0)
                        "${result?.latencyMs} ms"
                    else
                        "Unavailable"
            )


            WanInfoRow(
                label = "EXTERNAL HOST",
                value =
                    result?.externalHost ?: "Not checked"
            )


            WanInfoRow(
                label = "MESSAGE",
                value =
                    result?.message ?: "Press refresh to check WAN"
            )
        }
    }
}


// =============================================================
// WAN INFO ROW
// =============================================================

@Composable
fun WanInfoRow(
    label: String,
    value: String
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
    ) {

        Text(
            text = label,

            color = Color.Gray,

            fontSize = 10.sp
        )

        Text(
            text = value,

            color = Color.White,

            fontSize = 14.sp
        )
    }
}


// =============================================================
// TARGET RESULT CARD
// =============================================================

@Composable
fun TargetResultCard(
    result: WanTargetResult
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

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {

            Text(
                text = "TARGET RESULT",

                color =
                    Color(0xFF39FF14),

                fontSize = 17.sp
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            WanInfoRow(
                label = "TARGET",
                value = result.target
            )


            WanInfoRow(
                label = "RESOLVED IP",
                value = result.resolvedIp
            )


            WanInfoRow(
                label = "STATUS",
                value =
                    if (result.reachable)
                        "● REACHABLE"
                    else
                        "● NOT REACHABLE"
            )


            WanInfoRow(
                label = "LATENCY",
                value =
                    if (result.latencyMs >= 0)
                        "${result.latencyMs} ms"
                    else
                        "Unavailable"
            )


            WanInfoRow(
                label = "OPEN PORTS",
                value =
                    if (result.openPorts.isEmpty())
                        "None detected"
                    else
                        result.openPorts.joinToString(", ")
            )


            WanInfoRow(
                label = "SERVICES",
                value =
                    if (result.services.isEmpty())
                        "None detected"
                    else
                        result.services.joinToString(", ")
            )
        }
    }
}


// =============================================================
// CATEGORY TITLE
// =============================================================

@Composable
fun CategoryTitle(
    text: String
) {

    Text(
        text = text,

        color =
            Color(0xFF39FF14),

        fontSize = 15.sp,

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                    bottom = 8.dp
                )
    )
}

// =============================================================
// DEVICE CARD
// =============================================================

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit = {}
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 10.dp
                 )   
                 .clickable(
                      onClick = onClick,
                ),

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

            // -------------------------------------------------
            // IP ADDRESS
            // -------------------------------------------------

            Text(
                text =
                    "● ${device.ipAddress}",

                color =
                    Color(0xFFE0FFE0),

                fontSize = 16.sp
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // -------------------------------------------------
            // DEVICE TYPE
            // -------------------------------------------------

            Text(
                text = "TYPE",

                color =
                    Color.Gray,

                fontSize = 11.sp
            )

            Text(
                text =
                    device.deviceType,

                color =
                    Color(0xFF39FF14),

                fontSize = 14.sp
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // -------------------------------------------------
            // MAC ADDRESS
            // -------------------------------------------------

            Text(
                text = "MAC ADDRESS",

                color =
                    Color.Gray,

                fontSize = 11.sp
            )

            Text(
                text =
                    if (
                        device.macAddress.isBlank() ||
                        device.macAddress == "Unknown"
                    ) {
                        "Unknown"
                    } else {
                        device.macAddress
                    },

                color =
                    Color.White,

                fontSize = 14.sp
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // -------------------------------------------------
            // HOSTNAME
            // -------------------------------------------------

            Text(
                text = "HOSTNAME",

                color =
                    Color.Gray,

                fontSize = 11.sp
            )

            Text(
                text =
                    if (device.hostname.isBlank()) {
                        "Unknown"
                    } else {
                        device.hostname
                    },

                color =
                    Color.White,

                fontSize = 14.sp
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // -------------------------------------------------
            // OPEN PORTS
            // -------------------------------------------------

            Text(
                text = "OPEN PORTS",

                color =
                    Color.Gray,

                fontSize = 11.sp
            )

            Text(
                text =
                    if (device.openPorts.isEmpty()) {
                        "None"
                    } else {
                        device.openPorts
                            .distinct()
                            .sorted()
                            .joinToString(", ")
                    },

                color =
                    Color.White,

                fontSize = 14.sp
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // -------------------------------------------------
            // SERVICES
            // -------------------------------------------------

            Text(
                text = "SERVICES",

                color =
                    Color.Gray,

                fontSize = 11.sp
            )

            Text(
                text =
                    if (device.services.isEmpty()) {
                        "None detected"
                    } else {
                        device.services
                            .distinct()
                            .joinToString(", ")
                    },

                color =
                    Color(0xFF8CFF8C),

                fontSize = 14.sp
            )


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            // -------------------------------------------------
            // STATUS
            // -------------------------------------------------

            Text(
                text = "STATUS",

                color =
                    Color.Gray,

                fontSize = 11.sp
            )

            Text(
                text =
                    if (device.isActive) {
                        "● ACTIVE"
                    } else {
                        "● INACTIVE"
                    },

                color =
                    if (device.isActive) {
                        Color(0xFF39FF14)
                    } else {
                        Color.Red
                    },

                fontSize = 14.sp
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            // -------------------------------------------------
            // ANALYSIS HINT
            // -------------------------------------------------

            Text(
                text =
                    "TAP FOR FULL DEVICE ANALYSIS",

                color =
                    Color.Gray,

                fontSize = 10.sp
            )
        }
    }
}



// =============================================================
data class LanDeviceAnalysis(
    val deviceType: String,
    val confidence: String,
    val discoverySources: List<String>
)

fun analyzeLanDevice(
    hostname: String,
    macAddress: String,
    openPorts: List<Int>,
    services: List<String>,
    existingType: String
): LanDeviceAnalysis {
    val ports = openPorts.toSet()
    val sources = mutableListOf("LAN scan")

    if (macAddress.isNotBlank() && !macAddress.equals("Unknown", ignoreCase = true)) {
        sources.add("MAC address")
    }

    if (hostname.isNotBlank() && !hostname.equals("Unknown", ignoreCase = true)) {
        sources.add("hostname")
    }

    if (openPorts.isNotEmpty()) {
        sources.add("TCP service scan")
    }

    if (services.isNotEmpty()) {
        sources.add("service identification")
    }

    val normalizedHost = hostname.lowercase()

    val inferredType = when {
        554 in ports || 8554 in ports || 37777 in ports || 37778 in ports ->
            "📷 Possible IP Camera"

        631 in ports || 9100 in ports || normalizedHost.contains("printer") ->
            "🖨️ Network Printer"

        445 in ports || 139 in ports ->
            "💻 Computer / File Server"

        3389 in ports ->
            "💻 Windows Computer"

        22 in ports ->
            "🖥️ Linux / Server"

        1883 in ports || 8883 in ports ->
            "🏠 Possible IoT Device"

        3306 in ports ->
            "🗄️ Database Server"

        53 in ports && (443 in ports || 80 in ports) ->
            "📡 Router / Network Device"

        80 in ports || 443 in ports || 8080 in ports || 8443 in ports ->
            existingType

        existingType.isNotBlank() ->
            existingType

        else ->
            "❓ Unknown Device"
    }

    val confidence = when {
        !macAddress.equals("Unknown", ignoreCase = true) &&
            !hostname.equals("Unknown", ignoreCase = true) &&
            openPorts.isNotEmpty() ->
            "HIGH"

        !macAddress.equals("Unknown", ignoreCase = true) ||
            !hostname.equals("Unknown", ignoreCase = true) ||
            openPorts.isNotEmpty() ->
            "MEDIUM"

        else ->
            "LOW"
    }

    return LanDeviceAnalysis(
        deviceType = inferredType,
        confidence = confidence,
        discoverySources = sources.distinct()
    )
}
// DEVICE CLASSIFICATION
// =============================================================

fun classifyDevice(
    ports: List<Int>
): String {

    return when {

        // =====================================================
        // CAMERA / VIDEO DEVICES
        // =====================================================

        554 in ports ->
            "📷 Possible IP Camera"

        8554 in ports ->
            "📷 Possible IP Camera"

        37777 in ports ->
            "📷 Possible CCTV / Camera"

        37778 in ports ->
            "📷 Possible CCTV / Camera"

        8000 in ports ->
            "📷 Possible Camera / IoT"

        // =====================================================
        // PRINTERS
        // =====================================================

        631 in ports ->
            "🖨️ Printer"

        9100 in ports ->
            "🖨️ Network Printer"

        // =====================================================
        // WINDOWS / FILE SHARING
        // =====================================================

        445 in ports ->
            "💻 Computer / File Server"

        139 in ports ->
            "💻 Computer / Network Device"

        // =====================================================
        // REMOTE DESKTOP
        // =====================================================

        3389 in ports ->
            "💻 Windows Computer"

        // =====================================================
        // SSH / LINUX / SERVER
        // =====================================================

        22 in ports ->
            "🖥️ Linux / Server"

        23 in ports ->
            "🖥️ Telnet Device"

        // =====================================================
        // IoT / SMART HOME
        // =====================================================

        1883 in ports ->
            "🏠 Possible IoT Device"

        5000 in ports ->
            "🏠 Possible IoT / Web Device"

        8080 in ports ->
            "🌐 Web / IoT Device"

        8443 in ports ->
            "🌐 Secure Web / IoT Device"

        // =====================================================
        // DATABASE / SERVER
        // =====================================================

        3306 in ports ->
            "🗄️ Database Server"

        // =====================================================
        // ROUTER / NETWORK SERVICES
        // =====================================================

        53 in ports && 443 in ports ->
            "📡 Router / Network Device"

        53 in ports ->
            "📡 DNS / Network Device"

        // =====================================================
        // WEB DEVICE
        // =====================================================

        80 in ports && 443 in ports ->
            "🌐 Web / Network Device"

        80 in ports ->
            "🌐 HTTP Device"

        443 in ports ->
            "🌐 HTTPS Device"

        // =====================================================
        // NOTHING IDENTIFIED
        // =====================================================

        else ->
            "❓ Unknown Device"
    }
}

// =============================================================
// SECURITY MODE DATA
// =============================================================

data class SecurityFinding(
    val severity: String,
    val title: String,
    val evidence: String
)

data class SecurityAssessment(
    val target: String,
    val resolvedIp: String,
    val reachable: Boolean,
    val openPorts: List<Int>,
    val risk: String,
    val score: Int,
    val findings: List<SecurityFinding>
)

// =============================================================
// SECURITY SCREEN
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    targetAddress: String,
    scanning: Boolean,
    assessment: SecurityAssessment?,
    onTargetChange: (String) -> Unit,
    onAssess: () -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {

        item {
            Text(
                text = "🛡️ SECURITY ASSESSMENT",
                color = Color(0xFF39FF14),
                fontSize = 20.sp
            )

            Text(
                text = "Authorized, non-destructive security analysis",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetAddress,
                onValueChange = onTargetChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                singleLine = true,
                label = { Text("Authorized target") },
                placeholder = { Text("192.168.1.7 or example.com") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAssess,
                enabled = targetAddress.trim().isNotEmpty() && !scanning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF123D12)
                )
            ) {
                Text(if (scanning) "ASSESSING TARGET..." else "RUN SECURITY ASSESSMENT")
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (assessment != null) {

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF081108)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "RISK: ${assessment.risk}",
                            color = when (assessment.risk) {
                                "CRITICAL", "HIGH" -> Color(0xFFFF5252)
                                "MEDIUM" -> Color(0xFFFFD54F)
                                else -> Color(0xFF39FF14)
                            },
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("SCORE: ${assessment.score}/100", color = Color.White, fontSize = 14.sp)
                        Text("TARGET: ${assessment.target}", color = Color.White, fontSize = 13.sp)
                        Text("IP: ${assessment.resolvedIp}", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            "STATUS: ${if (assessment.reachable) "REACHABLE" else "NOT REACHABLE"}",
                            color = if (assessment.reachable) Color(0xFF39FF14) else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "FINDINGS (${assessment.findings.size})",
                    color = Color(0xFF39FF14),
                    fontSize = 17.sp
                )
            }

            items(assessment.findings) { finding ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF061006)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(13.dp)
                    ) {
                        Text(
                            text = "${finding.severity} • ${finding.title}",
                            color = when (finding.severity) {
                                "CRITICAL", "HIGH" -> Color(0xFFFF5252)
                                "MEDIUM" -> Color(0xFFFFD54F)
                                else -> Color(0xFF39FF14)
                            },
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = finding.evidence,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF081108)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "🧪 ATTACK LAB",
                            color = Color(0xFF39FF14),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Controlled exploit demonstrations will run only against an isolated lab target. This assessment currently performs reconnaissance and non-destructive checks.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// =============================================================
// RADAR
// =============================================================

@Composable
fun Radar(
    devices: List<Device>,
    modifier: Modifier = Modifier
) {

    val transition =
        rememberInfiniteTransition()

    val sweepAngle =
        transition.animateFloat(

            initialValue = 0f,

            targetValue = 360f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        tween(
                            durationMillis = 3000,
                            easing = LinearEasing
                        ),

                    repeatMode =
                        RepeatMode.Restart
                )
        )


    Canvas(
        modifier = modifier
    ) {

        val center =
            Offset(
                size.width / 2f,
                size.height / 2f
            )


        val radius =
            minOf(
                size.width,
                size.height
            ) * 0.43f


        // =====================================================
        // RADAR RINGS
        // =====================================================

        for (i in 1..5) {

            drawCircle(

                color =
                    Color(0xFF39FF14)
                        .copy(alpha = 0.18f),

                radius =
                    radius * i / 5f,

                center =
                    center,

                style =
                    Stroke(
                        width = 2f
                    )
            )
        }


        // =====================================================
        // HORIZONTAL CROSSHAIR
        // =====================================================

        drawLine(

            color =
                Color(0xFF39FF14)
                    .copy(alpha = 0.20f),

            start =
                Offset(
                    center.x - radius,
                    center.y
                ),

            end =
                Offset(
                    center.x + radius,
                    center.y
                ),

            strokeWidth = 2f
        )


        // =====================================================
        // VERTICAL CROSSHAIR
        // =====================================================

        drawLine(

            color =
                Color(0xFF39FF14)
                    .copy(alpha = 0.20f),

            start =
                Offset(
                    center.x,
                    center.y - radius
                ),

            end =
                Offset(
                    center.x,
                    center.y + radius
                ),

            strokeWidth = 2f
        )


        // =====================================================
        // RADAR SWEEP
        // =====================================================

        val radians =
            Math.toRadians(
                sweepAngle.value.toDouble()
            )


        val sweepEnd =
            Offset(

                center.x +
                    cos(radians).toFloat() *
                    radius,

                center.y +
                    sin(radians).toFloat() *
                    radius
            )


        drawLine(

            color =
                Color(0xFF39FF14),

            start =
                center,

            end =
                sweepEnd,

            strokeWidth = 5f,

            cap =
                StrokeCap.Round
        )


        // =====================================================
        // DETECTED DEVICES
        // =====================================================

        devices.forEachIndexed { index, _ ->

            val angle =
                index * 1.35f


            val distance =
                radius *
                    (
                        0.25f +
                        (index % 4) * 0.18f
                    )


            val devicePosition =
                Offset(

                    center.x +
                        cos(angle) *
                        distance,

                    center.y +
                        sin(angle) *
                        distance
                )


            drawCircle(

                color =
                    Color(0xFF39FF14)
                        .copy(alpha = 0.20f),

                radius = 18f,

                center =
                    devicePosition
            )


            drawCircle(

                color =
                    Color(0xFF39FF14),

                radius = 5f,

                center =
                    devicePosition
            )
        }


        // =====================================================
        // CENTER
        // =====================================================

        drawCircle(

            color =
                Color(0xFF39FF14),

            radius = 7f,

            center =
                center
        )
    }
}
