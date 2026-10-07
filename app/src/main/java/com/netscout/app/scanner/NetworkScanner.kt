package com.netscout.app.scanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// =============================================================
// WAN SCAN RESULT
// =============================================================

data class WanScanResult(
    val internetActive: Boolean,
    val publicIp: String = "Unknown",
    val publicIpv6: String = "Unknown",
    val latencyMs: Long = -1L,
    val externalHost: String = "Unknown",
    val localIp: String = "Unknown",
    val gateway: String = "Unknown",
    val connectionType: String = "Unknown",
    val dnsServers: List<String> = emptyList(),
    val message: String = ""
)

// =============================================================
// AUTHORIZED TARGET RESULT
// =============================================================

data class WanTargetResult(
    val target: String,
    val reachable: Boolean,
    val resolvedIp: String = "Unknown",
    val openPorts: List<Int> = emptyList(),
    val services: List<String> = emptyList(),
    val latencyMs: Long = -1L
)

// =============================================================
// LAN RESULT
// =============================================================

data class ScanResult(
    val ipAddress: String,
    val openPorts: List<Int>,
    val hostname: String = "Unknown",
    val macAddress: String = "Unknown",
    val deviceTypeHint: String = "Unknown",
    val services: List<String> = emptyList()
)

// =============================================================
// NETWORK SCANNER
// =============================================================

class NetworkScanner {

    // Specific authorized WAN target only.
    private val commonWanPorts = listOf(
        21, 22, 25, 53, 80, 110, 143, 443,
        554, 993, 995, 1883, 3306, 3389,
        5000, 8000, 8080, 8443
    )

    // Common LAN service fingerprints.
    private val commonLanPorts = listOf(
        21,      // FTP
        22,      // SSH
        23,      // Telnet
        53,      // DNS
        80,      // HTTP
        88,      // Kerberos / some enterprise devices
        139,     // NetBIOS
        443,     // HTTPS
        445,     // SMB
        554,     // RTSP
        631,     // IPP
        1883,    // MQTT
        5000,    // IoT/web
        8000,    // Camera/IoT
        8080,    // HTTP alternative
        8443,    // HTTPS alternative
        9100     // RAW printer
    )

    // =========================================================
    // WAN INTERNET CHECK
    // =========================================================

    fun checkWan(context: Context): WanScanResult {
        val networkInfo = getNetworkInfo(context)

        if (networkInfo == null) {
            return WanScanResult(
                internetActive = false,
                message = "No active network"
            )
        }

        val internetActive = hasInternetConnection(context)

        if (!internetActive) {
            return WanScanResult(
                internetActive = false,
                connectionType = getConnectionType(networkInfo),
                message = "Network connected, but Internet is unavailable"
            )
        }

        val externalHost = "1.1.1.1"

        val latency = measureTcpLatency(
            externalHost,
            443
        )

        val publicIp = getPublicIp("https://api.ipify.org")
        val publicIpv6 = getPublicIp("https://api64.ipify.org")
        val details = getNetworkDetails(context)

        return WanScanResult(
            internetActive = true,
            publicIp = if (publicIp.isBlank()) "Unknown" else publicIp,
            publicIpv6 = if (
                publicIpv6.isBlank() || publicIpv6 == publicIp
            ) "Unknown" else publicIpv6,
            latencyMs = latency,
            externalHost = externalHost,
            localIp = details.localIp,
            gateway = details.gateway,
            connectionType = details.connectionType,
            dnsServers = details.dnsServers,
            message = if (latency >= 0) {
                "WAN connection active"
            } else {
                "Internet active, latency unavailable"
            }
        )
    }

    // =========================================================
    // INTERNET CONNECTIVITY
    // =========================================================

    private fun hasInternetConnection(context: Context): Boolean {
        return try {
            val manager =
                context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as ConnectivityManager

            val network = manager.activeNetwork ?: return false

            val capabilities =
                manager.getNetworkCapabilities(network)
                    ?: return false

            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) &&
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
        } catch (_: Exception) {
            false
        }
    }

    // =========================================================
    // ACTIVE NETWORK
    // =========================================================

    private fun getNetworkInfo(context: Context): NetworkInfo? {
        return try {
            val manager =
                context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as ConnectivityManager

            @Suppress("DEPRECATION")
            manager.activeNetworkInfo
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================
    // CONNECTION TYPE
    // =========================================================

    private fun getConnectionType(networkInfo: NetworkInfo): String {
        return try {
            when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                ConnectivityManager.TYPE_MOBILE -> "Cellular"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                else -> "Other"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    // =========================================================
    // NETWORK DETAILS
    // =========================================================

    private data class NetworkDetails(
        val localIp: String = "Unknown",
        val gateway: String = "Unknown",
        val connectionType: String = "Unknown",
        val dnsServers: List<String> = emptyList()
    )

    private fun getNetworkDetails(context: Context): NetworkDetails {
        return try {
            val manager =
                context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as ConnectivityManager

            val network = manager.activeNetwork
                ?: return NetworkDetails()

            val capabilities =
                manager.getNetworkCapabilities(network)
                    ?: return NetworkDetails()

            val properties =
                manager.getLinkProperties(network)
                    ?: return NetworkDetails()

            val type = when {
                capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI
                ) -> "Wi-Fi"

                capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_CELLULAR
                ) -> "Cellular"

                capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_ETHERNET
                ) -> "Ethernet"

                else -> "Other"
            }

            NetworkDetails(
                localIp = getLocalIp(properties),
                gateway = getGateway(properties),
                connectionType = type,
                dnsServers = properties.dnsServers
                    .mapNotNull { it.hostAddress }
                    .distinct()
            )
        } catch (_: Exception) {
            NetworkDetails()
        }
    }

    // =========================================================
    // LOCAL IP
    // =========================================================

    private fun getLocalIp(properties: LinkProperties): String {
        return try {
            properties.linkAddresses
                .firstOrNull {
                    it.address is Inet4Address &&
                    !it.address.isLoopbackAddress
                }
                ?.address
                ?.hostAddress
                ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    // =========================================================
    // DEFAULT GATEWAY
    // =========================================================

    private fun getGateway(properties: LinkProperties): String {
        return try {
            properties.routes
                .firstOrNull { it.isDefaultRoute }
                ?.gateway
                ?.hostAddress
                ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    // =========================================================
    // PUBLIC IP
    // =========================================================

    private fun getPublicIp(endpoint: String): String {
        return try {
            val connection =
                URL(endpoint)
                    .openConnection() as HttpURLConnection

            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            connection.inputStream
                .bufferedReader()
                .use { it.readText().trim() }
        } catch (_: Exception) {
            ""
        }
    }

    // =========================================================
    // TCP LATENCY
    // =========================================================

    private fun measureTcpLatency(
        host: String,
        port: Int
    ): Long {
        return try {
            val start = System.currentTimeMillis()

            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(host, port),
                    3000
                )
            }

            System.currentTimeMillis() - start
        } catch (_: Exception) {
            -1L
        }
    }

    // =========================================================
    // AUTHORIZED WAN TARGET SCAN
    // =========================================================

    fun scanAuthorizedTarget(
        target: String,
        onComplete: (WanTargetResult) -> Unit
    ) {
        Thread {
            val cleanTarget = target
                .trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .substringBefore(":")

            var resolvedIp = "Unknown"
            var latency = -1L

            val openPorts = mutableListOf<Int>()
            val services = mutableListOf<String>()

            try {
                val address = InetAddress.getByName(cleanTarget)

                resolvedIp =
                    address.hostAddress ?: "Unknown"

                latency = measureTcpLatency(
                    cleanTarget,
                    443
                )

                val executor =
                    Executors.newFixedThreadPool(8)

                commonWanPorts.forEach { port ->
                    executor.execute {
                        try {
                            Socket().use { socket ->
                                socket.connect(
                                    InetSocketAddress(
                                        cleanTarget,
                                        port
                                    ),
                                    800
                                )

                                synchronized(openPorts) {
                                    openPorts.add(port)
                                    services.add(serviceName(port))
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                executor.shutdown()

                executor.awaitTermination(
                    15,
                    TimeUnit.SECONDS
                )
            } catch (_: Exception) {
            }

            onComplete(
                WanTargetResult(
                    target = cleanTarget,
                    reachable =
                        latency >= 0 ||
                        openPorts.isNotEmpty(),
                    resolvedIp = resolvedIp,
                    openPorts = openPorts
                        .distinct()
                        .sorted(),
                    services = services
                        .distinct()
                        .sorted(),
                    latencyMs = latency
                )
            )
        }.start()
    }

    // =========================================================
    // SERVICE IDENTIFICATION
    // =========================================================

    private fun serviceName(port: Int): String {
        return when (port) {
            21 -> "FTP"
            22 -> "SSH"
            23 -> "Telnet"
            25 -> "SMTP"
            53 -> "DNS"
            80 -> "HTTP"
            88 -> "Kerberos / Enterprise"
            110 -> "POP3"
            139 -> "NetBIOS"
            143 -> "IMAP"
            443 -> "HTTPS"
            445 -> "SMB"
            554 -> "RTSP / Possible IP Camera"
            631 -> "IPP Printer"
            993 -> "IMAPS"
            995 -> "POP3S"
            1883 -> "MQTT / IoT"
            3306 -> "MySQL"
            3389 -> "RDP"
            5000 -> "Possible IoT / Web Service"
            8000 -> "Possible Camera / IoT"
            8080 -> "HTTP Alternative"
            8443 -> "HTTPS Alternative"
            9100 -> "RAW Printer"
            else -> "Port $port"
        }
    }

    // =============================================================
    // LAN SCANNER
    //
    // Detection sources:
    // 1. Existing neighbour/ARP cache
    // 2. Host reachability
    // 3. TCP service probes
    // 4. MAC lookup after probing
    // 5. Reverse hostname lookup
    //
    // IMPORTANT:
    // Android does not guarantee access to the complete LAN ARP
    // table. Some phones/Android builds therefore legitimately
    // report MAC = Unknown.
    // =============================================================

    fun scan(
        subnetPrefix: String,
        onDeviceFound: (ScanResult) -> Unit
    ) {
        val prefix = subnetPrefix.trim().removeSuffix(".")

        if (!prefix.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){2}$"))) {
            return
        }

        val discovered =
            ConcurrentHashMap<String, ScanResult>()

        // Seed immediately from whatever neighbour information
        // Android currently exposes.
        readNeighbourTable()
            .filter {
                it.ipAddress.startsWith("$prefix.")
            }
            .forEach { entry ->
                val result = ScanResult(
                    ipAddress = entry.ipAddress,
                    openPorts = emptyList(),
                    hostname = resolveHostname(entry.ipAddress),
                    macAddress = entry.macAddress,
                    deviceTypeHint = "Network Device",
                    services = emptyList()
                )

                discovered[entry.ipAddress] = result
                onDeviceFound(result)
            }

        val executor =
            Executors.newFixedThreadPool(32)

        for (host in 1..254) {
            val ip = "$prefix.$host"

            executor.execute {
                try {
                    // Do not waste time on obviously invalid addresses.
                    val address = InetAddress.getByName(ip)

                    var reachable = false

                    try {
                        reachable = address.isReachable(600)
                    } catch (_: Exception) {
                    }

                    val openPorts =
                        mutableListOf<Int>()

                    // TCP probes are useful even when ICMP/Java
                    // reachability is blocked by the target.
                    commonLanPorts.forEach { port ->
                        try {
                            Socket().use { socket ->
                                socket.connect(
                                    InetSocketAddress(
                                        ip,
                                        port
                                    ),
                                    450
                                )
                            }

                            openPorts.add(port)
                        } catch (_: Exception) {
                        }
                    }

                    // A probe can cause the Android/Linux network
                    // stack to learn the neighbour MAC.
                    val mac = findMacAddress(ip)

                    val hasMac =
                        mac != "Unknown"

                    val hasPorts =
                        openPorts.isNotEmpty()

                    // Only perform reverse DNS when there is evidence
                    // that the host exists. This keeps scans faster.
                    val hostname =
                        if (
                            reachable ||
                            hasPorts ||
                            hasMac
                        ) {
                            resolveHostname(ip)
                        } else {
                            "Unknown"
                        }

                    if (
                        reachable ||
                        hasPorts ||
                        hasMac
                    ) {
                        val sortedPorts =
                            openPorts
                                .distinct()
                                .sorted()

                        val type =
                            classifyLanDevice(
                                hostname = hostname,
                                openPorts = sortedPorts
                            )

                        val result =
                            ScanResult(
                                ipAddress = ip,
                                openPorts = sortedPorts,
                                hostname = hostname,
                                macAddress = mac,
                                deviceTypeHint = type,
                                services = sortedPorts.map {
                                    serviceName(it)
                                }
                            )

                        // Avoid duplicate callbacks for a host that
                        // was already found in the neighbour table.
                        val previous =
                            discovered.put(
                                ip,
                                result
                            )

                        if (previous == null) {
                            onDeviceFound(result)
                        } else {
                            // Send the richer result if new information
                            // was learned during active probing.
                            if (previous != result) {
                                onDeviceFound(result)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // One failed host must never stop the scan.
                }
            }
        }

        executor.shutdown()

        try {
            executor.awaitTermination(
                60,
                TimeUnit.SECONDS
            )
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    // =============================================================
    // MAC / NEIGHBOUR DISCOVERY
    // =============================================================

    private data class ArpEntry(
        val ipAddress: String,
        val macAddress: String
    )

    private val macRegex =
        Regex("(?i)^([0-9a-f]{2}:){5}[0-9a-f]{2}$")

    private fun isValidMac(mac: String): Boolean {
        return macRegex.matches(mac) &&
            mac != "00:00:00:00:00:00" &&
            mac != "ff:ff:ff:ff:ff:ff"
    }

    // =========================================================
    // LINUX IP NEIGHBOUR TABLE
    // =========================================================

    private fun readIpNeighbourTable(): List<ArpEntry> {
        val entries = mutableListOf<ArpEntry>()

        val commands = listOf(
            listOf("/system/bin/ip", "neigh"),
            listOf("ip", "neigh")
        )

        for (command in commands) {
            try {
                val process =
                    ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .start()

                BufferedReader(
                    InputStreamReader(
                        process.inputStream
                    )
                ).use { reader ->
                    reader.forEachLine { line ->
                        val trimmed = line.trim()

                        if (trimmed.isBlank()) {
                            return@forEachLine
                        }

                        val ipMatch =
                            Regex(
                                "^((?:\\d{1,3}\\.){3}\\d{1,3})\\s+"
                            ).find(trimmed)

                        val macMatch =
                            Regex(
                                "(?i)([0-9a-f]{2}(?::[0-9a-f]{2}){5})"
                            ).find(trimmed)

                        if (
                            ipMatch != null &&
                            macMatch != null
                        ) {
                            val ip =
                                ipMatch.groupValues[1]

                            val mac =
                                macMatch.groupValues[1]
                                    .lowercase()

                            if (
                                isValidIpv4(ip) &&
                                isValidMac(mac)
                            ) {
                                entries.add(
                                    ArpEntry(
                                        ipAddress = ip,
                                        macAddress = mac
                                    )
                                )
                            }
                        }
                    }
                }

                process.waitFor(
                    2,
                    TimeUnit.SECONDS
                )

                if (entries.isNotEmpty()) {
                    break
                }
            } catch (_: Exception) {
                // Try next source.
            }
        }

        return entries.distinctBy {
            it.ipAddress
        }
    }

    // =========================================================
    // /proc/net/arp
    // =========================================================

    private fun readArpTable(): List<ArpEntry> {
        val entries = mutableListOf<ArpEntry>()

        try {
            val file = File("/proc/net/arp")

            if (!file.exists()) {
                return emptyList()
            }

            file.forEachLine { line ->
                val parts =
                    line.trim()
                        .split(Regex("\\s+"))

                if (parts.size >= 4) {
                    val ip = parts[0]
                    val mac = parts[3].lowercase()

                    if (
                        isValidIpv4(ip) &&
                        isValidMac(mac)
                    ) {
                        entries.add(
                            ArpEntry(
                                ipAddress = ip,
                                macAddress = mac
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        return entries.distinctBy {
            it.ipAddress
        }
    }

    // =========================================================
    // COMBINED NEIGHBOUR SOURCES
    // =========================================================

    private fun readNeighbourTable(): List<ArpEntry> {
        val combined =
            mutableMapOf<String, ArpEntry>()

        readIpNeighbourTable()
            .forEach { entry ->
                combined[entry.ipAddress] = entry
            }

        readArpTable()
            .forEach { entry ->
                if (!combined.containsKey(entry.ipAddress)) {
                    combined[entry.ipAddress] = entry
                }
            }

        return combined.values.toList()
    }

    // =========================================================
    // MAC ADDRESS LOOKUP FOR ONE HOST
    // =========================================================

    private fun findMacAddress(ipAddress: String): String {
        // Method 1: ip neigh
        try {
            val process =
                ProcessBuilder(
                    "ip",
                    "neigh",
                    "show",
                    ipAddress
                )
                    .redirectErrorStream(true)
                    .start()

            val output =
                process.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            process.waitFor()

            val mac =
                Regex(
                    "(?i)\\b[0-9a-f]{2}(:[0-9a-f]{2}){5}\\b"
                )
                    .find(output)
                    ?.value

            if (
                mac != null &&
                isValidMac(mac)
            ) {
                return mac.lowercase()
            }
        } catch (_: Exception) {
        }

        // Method 2: /proc/net/arp
        try {
            val file = File("/proc/net/arp")

            if (file.exists()) {
                file.forEachLine { line ->
                    val parts =
                        line.trim()
                            .split(Regex("\\s+"))

                    if (
                        parts.size >= 4 &&
                        parts[0] == ipAddress
                    ) {
                        val mac = parts[3].trim()

                        if (isValidMac(mac)) {
                            throw MacFoundException(
                                mac.lowercase()
                            )
                        }
                    }
                }
            }
        } catch (e: MacFoundException) {
            return e.mac
        } catch (_: Exception) {
        }

        // Method 3: arp fallback
        try {
            val process =
                ProcessBuilder(
                    "arp",
                    "-n",
                    ipAddress
                )
                    .redirectErrorStream(true)
                    .start()

            val output =
                process.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            process.waitFor()

            val mac =
                Regex(
                    "(?i)\\b[0-9a-f]{2}(:[0-9a-f]{2}){5}\\b"
                )
                    .find(output)
                    ?.value

            if (
                mac != null &&
                isValidMac(mac)
            ) {
                return mac.lowercase()
            }
        } catch (_: Exception) {
        }

        return "Unknown"
    }

    private class MacFoundException(
        val mac: String
    ) : Exception()

    // =========================================================
    // HOSTNAME RESOLUTION
    // =========================================================

    private fun resolveHostname(ipAddress: String): String {
        return try {
            val address =
                InetAddress.getByName(ipAddress)

            val hostname =
                address.hostName

            if (
                hostname.isNullOrBlank() ||
                hostname == ipAddress
            ) {
                "Unknown"
            } else {
                hostname
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    // =========================================================
    // IPV4 VALIDATION
    // =========================================================

    private fun isValidIpv4(ip: String): Boolean {
        val parts = ip.split(".")

        if (parts.size != 4) {
            return false
        }

        return parts.all {
            val value = it.toIntOrNull()
            value != null && value in 0..255
        }
    }

    // =========================================================
    // LAN DEVICE CLASSIFICATION
    // =========================================================

    private fun classifyLanDevice(
        hostname: String,
        openPorts: List<Int>
    ): String {
        val name = hostname.lowercase()
        val ports = openPorts.toSet()

        return when {
            554 in ports ->
                "Possible IP Camera"

            631 in ports || 9100 in ports ->
                "Printer"

            445 in ports || 139 in ports ->
                "Computer / Network Storage"

            1883 in ports || 8883 in ports ->
                "Possible IoT Device"

            3389 in ports ->
                "Windows Computer"

            name.contains("iphone") ||
            name.contains("ipad") ->
                "Apple Device"

            name.contains("redmi") ||
            name.contains("xiaomi") ->
                "Xiaomi Device"

            name.contains("vivo") ->
                "Vivo Device"

            name.contains("realme") ->
                "Realme Device"

            name.contains("samsung") ||
            name.contains("galaxy") ->
                "Samsung Device"

            name.contains("tv") ||
            name.contains("androidtv") ->
                "Smart TV"

            name.contains("router") ||
            name.contains("gateway") ->
                "Router"

            else ->
                "Network Device"
        }
    }
}
