package com.hyun.robot.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BaseBlueManager(context: Context) {
    companion object {
        const val REQUEST_ENABLE_BT = 1001
    }
    private val context: Context = context.applicationContext
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }
    var connectingDevice by mutableStateOf<BluetoothDevice?>(null)
    val pairedDevices = mutableStateListOf<BluetoothDevice>()
    val foundDevices = mutableStateListOf<BluetoothDevice>()
    private val prefs = context.getSharedPreferences("BT_DEVICES", Context.MODE_PRIVATE)
    private val persistedDevices = mutableSetOf<String>().apply {
        addAll(prefs.getStringSet("PERSISTED_DEVICES", mutableSetOf()) ?: mutableSetOf())
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?.let { device ->
                            if (!foundDevices.any { it.address == device.address }) {
                                foundDevices.add(device)
                            }
                        }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    handleBondStateChange(intent)
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    updatePairedDevices()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        checkPermissions()
        updatePairedDevices()
        restorePersistedDevices()
    }

    private fun restorePersistedDevices() {
        persistedDevices.forEach { address ->
            bluetoothAdapter?.getRemoteDevice(address)?.let { device ->
                if (!pairedDevices.any { it.address == address }) {
                    pairedDevices.add(device)
                }
            }
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun requestEnableBluetooth(activity: Activity) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    fun startDiscovery() {
        foundDevices.clear()
        bluetoothAdapter?.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) {
        connectingDevice = device
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    device.createBond()
                }

                synchronized(persistedDevices) {
                    persistedDevices.add(device.address)
                    prefs.edit().putStringSet("PERSISTED_DEVICES", persistedDevices).apply()
                }

                withContext(Dispatchers.Main) {
                    foundDevices.removeAll { it.address == device.address }
                    if (!pairedDevices.any { it.address == device.address }) {
                        pairedDevices.add(device)
                    }
                }
            } finally {
                connectingDevice = null
            }
        }
    }

    private fun handleBondStateChange(intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)

        device?.let {
            when (bondState) {
                BluetoothDevice.BOND_BONDED -> {
                    updatePairedDevices()
                }

                BluetoothDevice.BOND_NONE -> {
                    persistedDevices.remove(it.address)
                    prefs.edit().putStringSet("PERSISTED_DEVICES", persistedDevices).apply()
                    updatePairedDevices()
                }
            }
        }
    }

    private fun updatePairedDevices() {
        pairedDevices.clear()

        val systemPaired = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        val combinedDevices = (systemPaired + persistedDevices.mapNotNull { address ->
            bluetoothAdapter?.getRemoteDevice(address)
        }).distinctBy { it.address }

        pairedDevices.addAll(combinedDevices)
    }

    @SuppressLint("MissingPermission")
    fun sendTestDataToDevice(device: BluetoothDevice, dataToSend: ByteArray) {
        connectingDevice = device
        CoroutineScope(Dispatchers.IO).launch {
            var socket: BluetoothSocket? = null
            try {
                // 1. Verify Bluetooth adapter status
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter?.isEnabled != true) {
                    showToast("Bluetooth is not enabled")
                    return@launch
                }

                // 2. Stop discovery to improve connection stability
                adapter.cancelDiscovery()

                // 3. Handle device pairing
                if (!ensureDeviceBonded(device)) {
                    showToast("Pairing failed")
                    return@launch
                }

                // 4. Establish Bluetooth connection
                socket = createBluetoothSocket(device).apply {
                    withTimeoutOrNull(5000) {
                        connect()
                    } ?: throw IOException("Connection timeout")
                }

                // 5. Send data through established connection
                sendDataThroughSocket(socket!!, dataToSend)

                // 6. Wait for response
                receiveResponse(socket!!)

            } catch (e: SecurityException) {
                showToast("Permission error: ${e.message}")
            } catch (e: IOException) {
                handleSocketError(e, socket, "Connection failed: ${e.message}")
            } catch (e: Exception) {
                showToast("Error: ${e.javaClass.simpleName}")
            } finally {
                closeSocketSafely(socket)
                withContext(Dispatchers.Main) {
                    connectingDevice = null
                }
            }
        }
    }

    /**
     * Ensures device is bonded/pairing complete
     * Returns true if already bonded or pairing successful
     */
    private suspend fun ensureDeviceBonded(device: BluetoothDevice): Boolean {
        if (device.bondState == BluetoothDevice.BOND_BONDED) return true

        // Register pairing state receiver
        val pairingResult = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        when (intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.ERROR
                        )) {
                            BluetoothDevice.BOND_BONDED -> pairingResult.complete(true)
                            BluetoothDevice.BOND_NONE -> pairingResult.complete(false)
                        }
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))

        try {
            // Initiate pairing
            device.createBond()

            // Wait for pairing result with 15s timeout
            return withTimeout(15000) {
                pairingResult.await()
            }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    /**
     * Creates Bluetooth socket with fallback method
     * First tries standard UUID, then reflection method
     */
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        return try {
            // Standard SPP UUID
            val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            device.createRfcommSocketToServiceRecord(sppUUID)
        } catch (e: IOException) {
            try {
                // Fallback method for some devices
                val createMethod = device.javaClass.getMethod(
                    "createRfcommSocket",
                    Int::class.javaPrimitiveType
                )
                createMethod.invoke(device, 1) as BluetoothSocket
            } catch (e: Exception) {
                throw IOException("Socket creation failed: ${e.message}")
            }
        }
    }

    /** Handles data transmission */
    private fun sendDataThroughSocket(socket: BluetoothSocket, data: ByteArray) {
        socket.outputStream.use { stream ->
            stream.write(data)
            stream.flush()
        }
    }

    /** Handles response reception */
    private suspend fun receiveResponse(socket: BluetoothSocket) {
        withTimeoutOrNull(3000) {
            socket.inputStream.use { stream ->
                val buffer = ByteArray(1024)
                val bytesRead = stream.read(buffer)
                if (bytesRead > 0) {
                    String(buffer, 0, bytesRead).also { response ->
                        showToast("Received response: $response")
                    }
                }
            }
        } ?: showToast("Response timeout")
    }

    /** Gracefully closes socket connection */
    private fun closeSocketSafely(socket: BluetoothSocket?) {
        try {
            socket?.close()
        } catch (e: IOException) {
            // Log error if needed
        }
    }

    /** Handles socket-related errors */
    private suspend fun handleSocketError(
        e: IOException,
        socket: BluetoothSocket?,
        message: String
    ) {
        showToast(message)
        try {
            socket?.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    /** Shows toast messages on UI thread */
    private suspend fun showToast(text: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }


    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {

    }


    @SuppressLint("MissingPermission")
    fun disconnectToDevice(device: BluetoothDevice) {

    }


    fun getDeviceByAddress(address: String): BluetoothDevice {
        lateinit var addressDevice: BluetoothDevice
        bluetoothAdapter?.getRemoteDevice(address)?.let { device ->
            addressDevice = device
        }
        return addressDevice
    }

}
