package com.netscout.app.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult as WifiScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

// =============================================================
// NEARBY WI-FI DEVICE
// =============================================================

data class NearbyWifiDevice(

    val ssid: String,

    val bssid: String,

    val rssi: Int,

    val frequency: Int,

    val channel: Int,

    val security: String,

    // Intelligence
    val category: String = "Wi-Fi Access Point",

    val signalLevel: String = "Unknown",

    val band: String = "Unknown",

    val proximity: String = "Unknown"
)


// =============================================================
// NEARBY BLUETOOTH / BLE DEVICE
// =============================================================

data class NearbyBluetoothDevice(

    val name: String,

    val address: String,

    val rssi: Int,

    val deviceType: String,

    // Intelligence
    val category: String = "Unknown",

    val signalLevel: String = "Unknown",

    val proximity: String = "Unknown",

    val manufacturerData: String = "",

    val manufacturerName: String = "Unknown",

    val serviceUuids: String = ""
)


// =============================================================
// COMPLETE PROXIMITY RESULT
// =============================================================

data class ProximityScanResult(

    val wifiDevices:
        List<NearbyWifiDevice> = emptyList(),

    val bluetoothDevices:
        List<NearbyBluetoothDevice> = emptyList(),

    val message: String = ""
) {

    val totalDevices: Int
        get() =
            wifiDevices.size +
            bluetoothDevices.size


    val possibleCameraCount: Int
        get() =
            wifiDevices.count {
                it.category ==
                    "Possible Camera / CCTV"
            } +
            bluetoothDevices.count {
                it.category ==
                    "Possible Camera / CCTV"
            }


    val possibleIoTCount: Int
        get() =
            wifiDevices.count {
                it.category ==
                    "Possible IoT"
            } +
            bluetoothDevices.count {
                it.category ==
                    "Possible IoT"
            }


    val unknownCount: Int
        get() =
            wifiDevices.count {
                it.category ==
                    "Unknown"
            } +
            bluetoothDevices.count {
                it.category ==
                    "Unknown"
            }
}


// =============================================================
// PROXIMITY SCANNER
// =============================================================

class ProximityScanner(
    private val context: Context
) {

    private val appContext =
        context.applicationContext


    private val wifiManager =
        appContext.getSystemService(
            Context.WIFI_SERVICE
        ) as WifiManager


    private val bluetoothManager =
        appContext.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager


    // =========================================================
    // WI-FI PERMISSION
    // =========================================================

    private fun hasWifiPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) ==
            PackageManager.PERMISSION_GRANTED
    }


    // =========================================================
    // BLUETOOTH PERMISSION
    // =========================================================

    private fun hasBluetoothPermission(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) ==
                PackageManager.PERMISSION_GRANTED

        } else {

            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                PackageManager.PERMISSION_GRANTED
        }
    }


    // =========================================================
    // SIGNAL LEVEL
    // =========================================================

    private fun signalLevel(
        rssi: Int
    ): String {

        return when {

            rssi >= -50 ->
                "VERY STRONG"

            rssi >= -65 ->
                "STRONG"

            rssi >= -75 ->
                "MODERATE"

            rssi >= -85 ->
                "WEAK"

            else ->
                "VERY WEAK"
        }
    }


    // =========================================================
    // APPROXIMATE PROXIMITY
    //
    // RSSI is NOT a precise distance measurement.
    // This is only a signal-strength based indication.
    // =========================================================

    private fun proximityLevel(
        rssi: Int
    ): String {

        return when {

            rssi >= -50 ->
                "VERY NEAR"

            rssi >= -65 ->
                "NEAR"

            rssi >= -75 ->
                "MEDIUM"

            rssi >= -85 ->
                "FAR"

            else ->
                "VERY FAR / WEAK"
        }
    }


    // =========================================================
    // WI-FI BAND
    // =========================================================

    private fun wifiBand(
        frequency: Int
    ): String {

        return when {

            frequency in 2400..2500 ->
                "2.4 GHz"

            frequency in 4900..5900 ->
                "5 GHz"

            frequency in 5925..7125 ->
                "6 GHz"

            else ->
                "Unknown"
        }
    }


    // =========================================================
    // WI-FI SCAN
    // =========================================================

    fun scanWifi(
        onComplete:
            (List<NearbyWifiDevice>) -> Unit
    ) {

        if (!hasWifiPermission()) {

            onComplete(
                emptyList()
            )

            return
        }


        val handler =
            Handler(
                Looper.getMainLooper()
            )


        var finished = false


        lateinit var receiver:
            BroadcastReceiver


        fun finish(
            results:
                List<NearbyWifiDevice>
        ) {

            if (finished) {
                return
            }


            finished = true


            try {

                appContext.unregisterReceiver(
                    receiver
                )

            } catch (_: Exception) {
            }


            handler.removeCallbacksAndMessages(
                null
            )


            onComplete(
                results
            )
        }


        receiver =
            object : BroadcastReceiver() {

                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {

                    if (
                        intent?.action ==
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
                    ) {

                        finish(
                            getWifiDevices()
                        )
                    }
                }
            }


        try {

            val filter =
                IntentFilter(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
                )


            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                appContext.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )

            } else {

                @Suppress("DEPRECATION")

                appContext.registerReceiver(
                    receiver,
                    filter
                )
            }


            val started =
                wifiManager.startScan()


            if (!started) {

                handler.postDelayed({

                    finish(
                        getWifiDevices()
                    )

                }, 500)

            } else {

                handler.postDelayed({

                    finish(
                        getWifiDevices()
                    )

                }, 6000)
            }

        } catch (_: SecurityException) {

            finish(
                emptyList()
            )

        } catch (_: Exception) {

            finish(
                getWifiDevices()
            )
        }
    }


    // =========================================================
    // CURRENT WI-FI RESULTS
    // =========================================================

    fun getWifiDevices():
        List<NearbyWifiDevice> {

        if (!hasWifiPermission()) {
            return emptyList()
        }


        return try {

            wifiManager.scanResults

                .distinctBy {
                    it.BSSID
                }

                .map {
                    convertWifiResult(it)
                }

                .sortedByDescending {
                    it.rssi
                }

        } catch (_: SecurityException) {

            emptyList()

        } catch (_: Exception) {

            emptyList()
        }
    }


    // =========================================================
    // CONVERT WI-FI RESULT
    // =========================================================

    private fun convertWifiResult(
        result: WifiScanResult
    ): NearbyWifiDevice {

        val ssid =
            if (result.SSID.isBlank()) {

                "<HIDDEN SSID>"

            } else {

                result.SSID
            }


        val rssi =
            result.level


        return NearbyWifiDevice(

            ssid =
                ssid,

            bssid =
                result.BSSID
                    ?: "Unknown",

            rssi =
                rssi,

            frequency =
                result.frequency,

            channel =
                frequencyToChannel(
                    result.frequency
                ),

            security =
                detectSecurity(
                    result.capabilities
                ),

            category =
                classifyWifi(
                    ssid,
                    result.capabilities
                ),

            signalLevel =
                signalLevel(
                    rssi
                ),

            band =
                wifiBand(
                    result.frequency
                ),

            proximity =
                proximityLevel(
                    rssi
                )
        )
    }


    // =========================================================
    // WI-FI CLASSIFICATION
    // =========================================================

    private fun classifyWifi(
        ssid: String,
        capabilities: String
    ): String {

        val text =
            "$ssid $capabilities"
                .lowercase()


        return when {

            text.contains("camera") ||
            text.contains("cctv") ||
            text.contains("ipcam") ||
            text.contains("ipc") ||
            text.contains("dvr") ||
            text.contains("nvr") ->
                "Possible Camera / CCTV"


            text.contains("iot") ||
            text.contains("smart") ||
            text.contains("plug") ||
            text.contains("sensor") ||
            text.contains("doorbell") ||
            text.contains("home") ->
                "Possible IoT"


            text.contains("tv") ||
            text.contains("androidtv") ||
            text.contains("chromecast") ||
            text.contains("firetv") ->
                "Smart TV / Media"


            text.contains("printer") ||
            text.contains("print") ||
            text.contains("hp-") ||
            text.contains("canon") ||
            text.contains("epson") ->
                "Printer"


            text.contains("router") ||
            text.contains("gateway") ||
            text.contains("airtel") ||
            text.contains("jio") ||
            text.contains("tp-link") ||
            text.contains("zyxel") ||
            text.contains("netgear") ->
                "Network / Router"


            else ->
                "Wi-Fi Access Point"
        }
    }


    // =========================================================
    // WI-FI CHANNEL
    // =========================================================

    private fun frequencyToChannel(
        frequency: Int
    ): Int {

        return when {

            frequency in 2412..2472 ->
                (frequency - 2407) / 5

            frequency == 2484 ->
                14

            frequency in 5170..5895 ->
                (frequency - 5000) / 5

            frequency in 5955..7115 ->
                (frequency - 5950) / 5

            else ->
                -1
        }
    }


    // =========================================================
    // WI-FI SECURITY
    // =========================================================

    private fun detectSecurity(
        capabilities: String
    ): String {

        return when {

            capabilities.contains(
                "WPA3",
                ignoreCase = true
            ) ->
                "WPA3"


            capabilities.contains(
                "WPA2",
                ignoreCase = true
            ) ->
                "WPA2"


            capabilities.contains(
                "WPA",
                ignoreCase = true
            ) ->
                "WPA"


            capabilities.contains(
                "WEP",
                ignoreCase = true
            ) ->
                "WEP"


            else ->
                "OPEN"
        }
    }


    // =========================================================
    // BLUETOOTH / BLE SCAN
    // =========================================================

    fun scanBluetooth(
        durationMs: Long = 8000L,

        onComplete:
            (List<NearbyBluetoothDevice>) -> Unit
    ) {

        if (!hasBluetoothPermission()) {

            onComplete(
                emptyList()
            )

            return
        }


        val adapter:
            BluetoothAdapter? =
            bluetoothManager.adapter


        if (
            adapter == null ||
            !adapter.isEnabled
        ) {

            onComplete(
                emptyList()
            )

            return
        }


        val scanner:
            BluetoothLeScanner =
            adapter.bluetoothLeScanner
                ?: run {

                    onComplete(
                        emptyList()
                    )

                    return
                }


        val results =
            mutableMapOf<
                String,
                NearbyBluetoothDevice
            >()


        val callback =
            object : ScanCallback() {

                override fun onScanResult(
                    callbackType: Int,
                    result: ScanResult
                ) {

                    try {

                        val device =
                            result.device


                        val address =
                            device.address


                        val name =
                            try {

                                device.name
                                    ?: "Unknown BLE Device"

                            } catch (
                                _: SecurityException
                            ) {

                                "Unknown BLE Device"
                            }


                        val type =
                            when (
                                device.type
                            ) {

                                BluetoothDevice
                                    .DEVICE_TYPE_CLASSIC ->
                                    "Bluetooth Classic"


                                BluetoothDevice
                                    .DEVICE_TYPE_LE ->
                                    "Bluetooth LE"


                                BluetoothDevice
                                    .DEVICE_TYPE_DUAL ->
                                    "Bluetooth Dual"


                                else ->
                                    "Unknown Bluetooth"
                            }


                        val manufacturerData =
                            extractManufacturerData(
                                result
                            )


                        val manufacturerName =
                            identifyManufacturer(
                                result
                            )


                        val serviceUuids =
                            extractServiceUuids(
                                result
                            )


                        val category =
                            classifyBluetooth(
                                name,
                                manufacturerName,
                                serviceUuids
                            )


                        results[address] =
                            NearbyBluetoothDevice(

                                name =
                                    name,

                                address =
                                    address,

                                rssi =
                                    result.rssi,

                                deviceType =
                                    type,

                                category =
                                    category,

                                signalLevel =
                                    signalLevel(
                                        result.rssi
                                    ),

                                proximity =
                                    proximityLevel(
                                        result.rssi
                                    ),

                                manufacturerData =
                                    manufacturerData,

                                manufacturerName =
                                    manufacturerName,

                                serviceUuids =
                                    serviceUuids
                            )

                    } catch (
                        _: SecurityException
                    ) {
                    } catch (
                        _: Exception
                    ) {
                    }
                }


                override fun onScanFailed(
                    errorCode: Int
                ) {
                    // Timeout handles completion.
                }
            }


        val settings =
            ScanSettings.Builder()

                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )

                .build()


        try {

            scanner.startScan(
                 null,
                settings,
                callback
            )


            Thread {

                try {

                    Thread.sleep(
                        durationMs
                    )

                } catch (
                    _: InterruptedException
                ) {

                    Thread.currentThread()
                        .interrupt()
                }


                try {

                    scanner.stopScan(
                        callback
                    )

                } catch (_: Exception) {
                }


                onComplete(

                    results.values

                        .sortedByDescending {
                            it.rssi
                        }
                )

            }.start()

        } catch (
            _: SecurityException
        ) {

            onComplete(
                emptyList()
            )

        } catch (
            _: Exception
        ) {

            onComplete(
                emptyList()
            )
        }
    }


    // =========================================================
    // BLUETOOTH CLASSIFICATION
    // =========================================================

    private fun classifyBluetooth(
        name: String,
        manufacturer: String,
        services: String
    ): String {

        val text =
            "$name $manufacturer $services"
                .lowercase()


        return when {

            text.contains("camera") ||
            text.contains("cctv") ||
            text.contains("ipcam") ||
            text.contains("video") ->
                "Possible Camera / CCTV"


            text.contains("airpods") ||
            text.contains("buds") ||
            text.contains("earbud") ||
            text.contains("headphone") ||
            text.contains("headset") ->
                "Audio Device"


            text.contains("watch") ||
            text.contains("band") ||
            text.contains("fit") ->
                "Wearable"


            text.contains("tile") ||
            text.contains("tracker") ||
            text.contains("tag") ||
            text.contains("beacon") ->
                "Possible Tracker / Beacon"


            text.contains("sensor") ||
            text.contains("iot") ||
            text.contains("smart") ||
            text.contains("lock") ||
            text.contains("door") ->
                "Possible IoT"


            text.contains("phone") ||
            text.contains("android") ||
            text.contains("iphone") ||
            text.contains("redmi") ||
            text.contains("vivo") ||
            text.contains("realme") ||
            text.contains("samsung") ||
            text.contains("pixel") ->
                "Possible Phone / Tablet"


            else ->
                "Unknown"
        }
    }


    // =========================================================
    // BLE MANUFACTURER DATA
    // =========================================================

    private fun extractManufacturerData(
        result: ScanResult
    ): String {

        return try {

            val record =
                result.scanRecord
                    ?: return ""


            val data =
                record.manufacturerSpecificData


            if (data.size() == 0) {
                return ""
            }


            val output =
                StringBuilder()


            for (
                index in
                0 until data.size()
            ) {

                val manufacturerId =
                    data.keyAt(index)


                val bytes =
                    data.valueAt(index)


                output.append(
                    "ID "
                )

                output.append(
                    manufacturerId
                )

                output.append(
                    ": "
                )


                bytes.forEach {

                    output.append(
                        String.format(
                            "%02X ",
                            it.toInt() and 0xFF
                        )
                    )
                }


                if (
                    index <
                    data.size() - 1
                ) {

                    output.append(
                        " | "
                    )
                }
            }


            output
                .toString()
                .trim()

        } catch (_: Exception) {

            ""
        }
    }

// =========================================================
    // MANUFACTURER IDENTIFICATION
    //
    // Identification is based on Bluetooth company IDs when
    // available. Unknown IDs remain Unknown.
    // =========================================================

    private fun identifyManufacturer(
        result: ScanResult
    ): String {

        return try {

            val record =
                result.scanRecord
                    ?: return "Unknown"


            val data =
                record.manufacturerSpecificData


            if (data.size() == 0) {
                return "Unknown"
            }


            val id =
                data.keyAt(0)


            when (id) {

                0x004C ->
                    "Apple"


                0x0075 ->
                    "Samsung"


                0x00E0 ->
                    "Google"


                0x0006 ->
                    "Microsoft"


                0x0059 ->
                    "Nordic Semiconductor"


                0x000F ->
                    "Broadcom"


                0x000D ->
                    "Texas Instruments"


                0x0131 ->
                    "Xiaomi / Mijia"


                else ->
                    "Company ID $id"
            }

        } catch (_: Exception) {

            "Unknown"
        }
    }


    // =========================================================
    // BLE SERVICE UUIDS
    // =========================================================

    private fun extractServiceUuids(
        result: ScanResult
    ): String {

        return try {

            result.scanRecord
                ?.serviceUuids
                ?.joinToString(", ") {
                    it.toString()
                }
                ?: ""

        } catch (_: Exception) {

            ""
        }
    }


    // =========================================================
    // COMPLETE NEAR SCAN
    // =========================================================

    fun scan(
        onComplete:
            (ProximityScanResult) -> Unit
    ) {

        scanWifi { wifi ->

            scanBluetooth { bluetooth ->

                onComplete(

                    ProximityScanResult(

                        wifiDevices =
                            wifi,

                        bluetoothDevices =
                            bluetooth,

                        message =
                            "Wi-Fi + Bluetooth/BLE proximity scan completed"
                    )
                )
            }
        }
    }
}