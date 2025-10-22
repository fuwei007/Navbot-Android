package com.hyun.robot.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.withTimeout
import java.net.SocketTimeoutException

class WifiAutoConnector(context: Context) {
    private var wifiManager: WifiManager? = null
    private val context: Context

    init {
        this.context = context.applicationContext
        init()
    }

    fun init(context: Context) {
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private fun init() {
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private fun enableWifi(): Boolean {
        return if (!wifiManager!!.isWifiEnabled) {
            wifiManager!!.setWifiEnabled(true)
        } else true
    }

    private fun enableWifiAsync(callback: (Boolean) -> Unit) {
        if (wifiManager?.isWifiEnabled == true) {
            callback(true)
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            val result = try {
                wifiManager?.setWifiEnabled(true)
            } catch (e: SecurityException) {
                false
            }
            withContext(Dispatchers.Main) {
                if (result != null) {
                    callback(result)
                }
            }
        }
    }

    private fun connectToWifi(ssid: String, password: String): Boolean {
        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP or WifiConfiguration.GroupCipher.CCMP)
        }
        return wifiManager?.run {
            val netId = addNetwork(config).takeIf { it != -1 } ?: return false
            enableNetwork(netId, true) && saveConfiguration()
        } ?: false
    }

    @SuppressLint("MissingPermission")
    fun handleBluetoothConnection(
        device: BluetoothDevice,
        serviceUuid :String,
        onSuccess: (WifiConfiguration) -> Unit,
        onError: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (enableWifi()) {
                var socket: BluetoothSocket? = null
                try {
                    socket = device.createRfcommSocketToServiceRecord(
                        UUID.fromString(serviceUuid)
                    ).apply {
                        withTimeoutOrNull(5000) {
                            socket?.connect()
                        } ?: throw SocketTimeoutException("Connection timeout")
                    }

                    val configData = withTimeout(10000) {
                        socket.inputStream.use {
                            ByteArray(1024).apply {
                                val bytesRead = it.read(this)
                                takeIf { bytesRead > 0 }?.copyOf(bytesRead)
                                    ?: throw IOException("Empty config data")
                            }
                        }
                    }

                    parseWifiConfig(configData)?.let { config ->
                        if (connectToWifi(
                                config.SSID.trim('"'),
                                config.preSharedKey?.trim('"') ?: ""
                            )
                        ) {
                            onSuccess(config)
                        } else throw IOException("WiFi connection failed")
                    } ?: throw JSONException("Invalid config format")
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    runCatching { socket?.close() }
                }
            } else {
                enableWifiAsync { success ->
                    showToast("Wi-Fi ${if (success) "enable success" else "enable failed"}")
                }
            }
        }
    }

    private fun showToast(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun parseWifiConfig(json: ByteArray): WifiConfiguration? {
        return try {
            JSONObject(json.decodeToString()).run {
                WifiConfiguration().apply {
                    SSID = "\"${getString("SSID")}\""
                    when (getInt("authType")) {
                        0 -> allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        1 -> {
                            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                            preSharedKey = "\"${getString("password")}\""
                            allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                        }

                        2 -> {
                            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
                            allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                        }

                        else -> throw IllegalArgumentException("Unsupported auth type")
                    }
                    hiddenSSID = optBoolean("hidden", false)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}