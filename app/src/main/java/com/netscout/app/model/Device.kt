package com.netscout.app.model

data class Device(
    val ipAddress: String,
    val hostname: String = "Unknown",

    // Network identity
    val macAddress: String = "Unknown",
    val manufacturer: String = "Unknown",

    // Classification
    val deviceType: String = "Unknown Device",
    val confidence: String = "Unknown",

    // Network information
    val openPorts: List<Int> = emptyList(),
    val services: List<String> = emptyList(),

    // Signal / discovery information
    val signalStrength: Int? = null,
    val discoverySources: List<String> = emptyList(),

    // State
    val isActive: Boolean = true,
    val isTrusted: Boolean = false,

    // Possible device indicators
    val possibleCamera: Boolean = false,
    val possibleIoT: Boolean = false,
    val possibleTracker: Boolean = false,

    // History
    val firstSeen: Long = 0L,
    val lastSeen: Long = 0L
)