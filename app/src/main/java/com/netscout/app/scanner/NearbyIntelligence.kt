package com.netscout.app.scanner

import com.netscout.app.model.Device

data class DeviceAnalysis(
    val ipAddress: String,
    val hostname: String,
    val type: String,
    val cameraScore: Int,
    val iotScore: Int,
    val confidence: String,
    val reasons: List<String>
)

object NearbyIntelligence {

    fun analyze(device: Device): DeviceAnalysis {

        val ports = device.openPorts.toSet()

        var cameraScore = 0
        var iotScore = 0

        val reasons = mutableListOf<String>()

        // =====================================================
        // CAMERA / VIDEO INDICATORS
        // =====================================================

        if (554 in ports) {
            cameraScore += 70
            reasons.add("RTSP service detected")
        }

        if (8000 in ports) {
            cameraScore += 20
            reasons.add("Camera/IoT web service port detected")
        }

        if (8080 in ports) {
            cameraScore += 10
            reasons.add("Alternative HTTP service detected")
        }

        // =====================================================
        // IoT INDICATORS
        // =====================================================

        if (1883 in ports) {
            iotScore += 70
            reasons.add("MQTT service detected")
        }

        if (8883 in ports) {
            iotScore += 70
            reasons.add("Secure MQTT service detected")
        }

        if (5000 in ports) {
            iotScore += 20
            reasons.add("IoT/web service indicator")
        }

        if (8000 in ports) {
            iotScore += 15
        }

        if (8080 in ports) {
            iotScore += 10
        }

        if (8443 in ports) {
            iotScore += 10
        }

        // =====================================================
        // EXISTING DEVICE TYPE HINT
        // =====================================================

        if (
            device.deviceType.contains(
                "camera",
                ignoreCase = true
            )
        ) {
            cameraScore += 30
            reasons.add("Device classification suggests camera")
        }

        if (
            device.deviceType.contains(
                "iot",
                ignoreCase = true
            )
        ) {
            iotScore += 30
            reasons.add("Device classification suggests IoT")
        }

        // =====================================================
        // CAP SCORES
        // =====================================================

        val finalCameraScore =
            cameraScore.coerceIn(0, 100)

        val finalIotScore =
            iotScore.coerceIn(0, 100)

        // =====================================================
        // CLASSIFICATION
        // =====================================================

        val type: String
        val confidence: String

        if (finalCameraScore >= 70) {

            type = "Possible Camera / CCTV"

            confidence =
                when {
                    finalCameraScore >= 90 ->
                        "HIGH INDICATOR"

                    finalCameraScore >= 70 ->
                        "STRONG INDICATOR"

                    else ->
                        "POSSIBLE"
                }

        } else if (finalIotScore >= 50) {

            type = "Possible IoT"

            confidence =
                if (finalIotScore >= 70)
                    "STRONG INDICATOR"
                else
                    "POSSIBLE"

        } else {

            type =
                if (device.deviceType != "Unknown Device")
                    device.deviceType
                else
                    "Unknown Device"

            confidence =
                if (ports.isNotEmpty())
                    "LOW INDICATOR"
                else
                    "NO STRONG INDICATOR"
        }

        if (reasons.isEmpty()) {
            reasons.add("No strong camera or IoT service indicators")
        }

        return DeviceAnalysis(
            ipAddress = device.ipAddress,
            hostname = device.hostname,
            type = type,
            cameraScore = finalCameraScore,
            iotScore = finalIotScore,
            confidence = confidence,
            reasons = reasons
        )
    }

    fun analyzeAll(
        devices: List<Device>
    ): List<DeviceAnalysis> {

        return devices.map {
            analyze(it)
        }
    }
}