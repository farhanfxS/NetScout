package com.netscout.app.scanner

/**
 * Offline LAN device intelligence.
 *
 * This does NOT claim to know the exact hardware.
 * It evaluates observable evidence:
 *
 * - hostname
 * - MAC availability
 * - open TCP ports
 * - detected services
 *
 * The result is an estimate with a confidence level.
 */
data class LanDeviceAnalysis(
    val deviceType: String,
    val confidence: String,
    val reasons: List<String>,
    val discoverySources: List<String>
)

object LanDeviceIntelligence {

    fun analyze(
        hostname: String,
        macAddress: String,
        openPorts: List<Int>,
        services: List<String>,
        existingType: String = "Network Device"
    ): LanDeviceAnalysis {

        val name = hostname
            .trim()
            .lowercase()

        val ports = openPorts.toSet()

        val serviceText = services
            .joinToString(" ")
            .lowercase()

        val reasons = mutableListOf<String>()
        val sources = mutableListOf<String>()

        var androidScore = 0
        var appleScore = 0
        var windowsScore = 0
        var linuxScore = 0
        var cameraScore = 0
        var printerScore = 0
        var routerScore = 0
        var iotScore = 0
        var tvScore = 0

        // =====================================================
        // HOSTNAME EVIDENCE
        // =====================================================

        if (name.isNotBlank() && name != "unknown") {

            sources.add("Hostname resolution")

            when {

                name.contains("android") ||
                name.contains("vivo") ||
                name.contains("oppo") ||
                name.contains("realme") ||
                name.contains("redmi") ||
                name.contains("xiaomi") ||
                name.contains("oneplus") ||
                name.contains("pixel") -> {

                    androidScore += 60

                    reasons.add(
                        "Hostname suggests an Android/mobile device"
                    )
                }

                name.contains("iphone") ||
                name.contains("ipad") ||
                name.contains("apple") -> {

                    appleScore += 70

                    reasons.add(
                        "Hostname suggests an Apple device"
                    )
                }

                name.contains("windows") ||
                name.contains("desktop") ||
                name.contains("laptop") ||
                name.contains("pc") -> {

                    windowsScore += 40

                    reasons.add(
                        "Hostname suggests a computer"
                    )
                }

                name.contains("router") ||
                name.contains("gateway") ||
                name.contains("fiber") ||
                name.contains("jio") ||
                name.contains("tp-link") ||
                name.contains("tplink") ||
                name.contains("netgear") ||
                name.contains("dlink") -> {

                    routerScore += 60

                    reasons.add(
                        "Hostname suggests a router/network gateway"
                    )
                }

                name.contains("camera") ||
                name.contains("cam") ||
                name.contains("cctv") ||
                name.contains("hikvision") ||
                name.contains("dahua") -> {

                    cameraScore += 70

                    reasons.add(
                        "Hostname suggests a camera/CCTV device"
                    )
                }

                name.contains("printer") ||
                name.contains("epson") ||
                name.contains("canon") ||
                name.contains("hp-") ||
                name.contains("brother") -> {

                    printerScore += 60

                    reasons.add(
                        "Hostname suggests a printer"
                    )
                }

                name.contains("tv") ||
                name.contains("androidtv") ||
                name.contains("smarttv") ||
                name.contains("bravia") ||
                name.contains("roku") -> {

                    tvScore += 60

                    reasons.add(
                        "Hostname suggests a smart TV/media device"
                    )
                }
            }
        }

        // =====================================================
        // MAC EVIDENCE
        // =====================================================

        if (
            macAddress.isNotBlank() &&
            macAddress != "Unknown"
        ) {

            sources.add("MAC address")

            reasons.add(
                "MAC address was observed"
            )
        }

        // =====================================================
        // PORT EVIDENCE
        // =====================================================

        if (openPorts.isNotEmpty()) {

            sources.add("TCP service probe")

            // Camera / CCTV

            if (554 in ports) {

                cameraScore += 75

                reasons.add(
                    "RTSP service detected on port 554"
                )
            }

            if (8000 in ports) {

                cameraScore += 15
                iotScore += 15

                reasons.add(
                    "Port 8000 is commonly used by camera/IoT web services"
                )
            }

            // Printer

            if (631 in ports) {

                printerScore += 70

                reasons.add(
                    "IPP printing service detected on port 631"
                )
            }

            if (9100 in ports) {

                printerScore += 70

                reasons.add(
                    "RAW printing service detected on port 9100"
                )
            }

            // Windows / SMB

            if (445 in ports) {

                windowsScore += 70

                reasons.add(
                    "SMB service detected on port 445"
                )
            }

            if (139 in ports) {

                windowsScore += 40

                reasons.add(
                    "NetBIOS/SMB service detected on port 139"
                )
            }

            if (3389 in ports) {

                windowsScore += 70

                reasons.add(
                    "RDP service detected on port 3389"
                )
            }

            // Linux / SSH

            if (22 in ports) {

                linuxScore += 45

                reasons.add(
                    "SSH service detected on port 22"
                )
            }

            // IoT

            if (1883 in ports) {

                iotScore += 75

                reasons.add(
                    "MQTT service detected on port 1883"
                )
            }

            if (8883 in ports) {

                iotScore += 75

                reasons.add(
                    "Secure MQTT service detected on port 8883"
                )
            }

            // Router / network device

            if (53 in ports) {

                routerScore += 35

                reasons.add(
                    "DNS service detected on port 53"
                )
            }

            // Web service

            if (80 in ports) {

                reasons.add(
                    "HTTP service detected on port 80"
                )
            }

            if (443 in ports) {

                reasons.add(
                    "HTTPS service detected on port 443"
                )
            }
        }

        // =====================================================
        // SERVICE EVIDENCE
        // =====================================================

        if (
            serviceText.contains("rtsp") ||
            serviceText.contains("camera")
        ) {

            cameraScore += 25

            reasons.add(
                "Detected service information suggests camera functionality"
            )
        }

        if (
            serviceText.contains("mqtt") ||
            serviceText.contains("iot")
        ) {

            iotScore += 25

            reasons.add(
                "Detected service information suggests IoT functionality"
            )
        }

        if (
            serviceText.contains("smb") ||
            serviceText.contains("rdp")
        ) {

            windowsScore += 25

            reasons.add(
                "Detected services suggest Windows/network file sharing"
            )
        }

        if (
            serviceText.contains("ssh")
        ) {

            linuxScore += 20

            reasons.add(
                "Detected SSH service suggests a Unix/Linux/network appliance"
            )
        }

        if (
            serviceText.contains("ipp") ||
            serviceText.contains("printer")
        ) {

            printerScore += 25

            reasons.add(
                "Detected services suggest a printer"
            )
        }

        // =====================================================
        // DETERMINE WINNER
        // =====================================================

        val candidates =
            listOf(
                "Android Device" to androidScore,
                "Apple Device" to appleScore,
                "Windows Computer" to windowsScore,
                "Linux / Unix Device" to linuxScore,
                "Possible IP Camera" to cameraScore,
                "Printer" to printerScore,
                "Router / Network Device" to routerScore,
                "Possible IoT Device" to iotScore,
                "Smart TV / Media Device" to tvScore
            )

        val best =
            candidates.maxByOrNull {
                it.second
            }

        val bestScore =
            best?.second ?: 0

        val finalType =
            if (bestScore >= 60) {
                best!!.first
            } else {
                existingType
                    .ifBlank {
                        "Network Device"
                    }
            }

        // =====================================================
        // CONFIDENCE
        // =====================================================

        val confidence =
            when {
                bestScore >= 80 ->
                    "HIGH"

                bestScore >= 50 ->
                    "MEDIUM"

                bestScore >= 25 ->
                    "LOW"

                else ->
                    "UNKNOWN"
            }

        // =====================================================
        // FALLBACK EVIDENCE
        // =====================================================

        if (reasons.isEmpty()) {

            reasons.add(
                "Only basic LAN reachability information was observed"
            )
        }

        if (sources.isEmpty()) {

            sources.add(
                "LAN discovery"
            )
        }

        return LanDeviceAnalysis(
            deviceType = finalType,
            confidence = confidence,
            reasons = reasons.distinct(),
            discoverySources = sources.distinct()
        )
    }
}