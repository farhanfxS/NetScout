package com.netscout.app.scanner

import android.content.Context
import android.net.wifi.WifiManager

object NetworkInfo {

    fun getLocalIpv4Address(context: Context): String? {

        return try {
            val wifiManager =
                context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager

            val ip = wifiManager.connectionInfo.ipAddress

            if (ip == 0) {
                return null
            }

            val address =
                "${ip and 0xff}." +
                "${ip shr 8 and 0xff}." +
                "${ip shr 16 and 0xff}." +
                "${ip shr 24 and 0xff}"

            address

        } catch (e: Exception) {
            null
        }
    }

    fun getSubnetPrefix(context: Context): String? {

        val ip = getLocalIpv4Address(context)
            ?: return null

        val lastDot = ip.lastIndexOf('.')

        if (lastDot == -1) {
            return null
        }

        return ip.substring(0, lastDot)
    }
}