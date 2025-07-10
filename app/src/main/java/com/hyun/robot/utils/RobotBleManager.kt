package com.hyun.robot.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ficat.easyble.BleDevice
import com.ficat.easyble.BleManager
import com.ficat.easyble.gatt.BleHandlerThread
import com.ficat.easyble.gatt.callback.BleCallback
import com.ficat.easyble.gatt.callback.BleConnectCallback
import com.ficat.easyble.gatt.callback.BleNotifyCallback
import com.ficat.easyble.gatt.callback.BleReadCallback
import com.ficat.easyble.gatt.callback.BleWriteCallback
import com.ficat.easyble.scan.BleScanCallback
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hyun.robot.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask


@SuppressLint("MissingPermission")
class RobotBleManager(context: Context) {
    private val context: Context
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: BleScanCallback? = null
    var pairedDevices = mutableStateListOf<BleManagerDevice.BluetoothManagerDevice>()
    var foundDevices = mutableStateListOf<BleManagerDevice.BluetoothManagerDevice>()
    var connectDev: BleDevice? = null
    var connectingDevice by mutableStateOf<BluetoothDevice?>(null)
    private val prefs = context.getSharedPreferences("BT_DEVICES1", Context.MODE_PRIVATE)
    private val gson = GsonBuilder().serializeNulls().create()
    var connectedDevices = mutableStateListOf<BleDevice>()

    var persistedDevices = mutableSetOf<BleManagerDevice>().apply {
        prefs.getStringSet("PERSISTED_DEVICES1", mutableSetOf())?.forEach { json ->
            gson.fromJson(json, BleManagerDevice::class.java)?.let { add(it) }
        }
    }

//    private var sendCommandArray = BleCommand.getBleCommand(
//        height = BleCommand.BASE_HEIGHT,
//        roll = BleCommand.EMPTY_BYTE,
//        linearH = BleCommand.EMPTY_BYTE,
//        linearW = BleCommand.EMPTY_BYTE,
//        angular = BleCommand.EMPTY_BYTE,
//        stable = BleCommand.EMPTY_BYTE,
//    )
    var isOnline = true
    private val timer = Timer()
    private var timerTask = object : TimerTask() {
        override fun run() {
            isOnline = false
        }
    }


    fun saveDevice(device: BleDevice) {
        var bleManagerDevice: BleManagerDevice = BleManagerDevice()
        bleManagerDevice.address = device.address
        bleManagerDevice.name = device.name
        bleManagerDevice.connState = device.connectionState
        var bluetoothManagerDevice: BleManagerDevice.BluetoothManagerDevice =
            BleManagerDevice.BluetoothManagerDevice()
        bluetoothManagerDevice.address = device.bluetoothDevice.address
        bluetoothManagerDevice.name = device.bluetoothDevice.name
        bleManagerDevice.device = bluetoothManagerDevice
        persistedDevices.add(bleManagerDevice)
        val jsonSet = persistedDevices.map { gson.toJson(it) }.toSet()
        prefs.edit().putStringSet("PERSISTED_DEVICES1", jsonSet).apply()
    }

    fun removeDevice(device: BleManagerDevice) {
        persistedDevices.remove(device)
        val deviceStrings = persistedDevices.map { device ->
            Gson().toJson(device)
        }.toSet()
        prefs.edit().putStringSet("PERSISTED_DEVICES1", deviceStrings).apply()
    }

    fun updateDeviceName( device: BleManagerDevice.BluetoothManagerDevice,name:String){
        var needEditDevice = BleManagerDevice()
        var currentDevice = BleManagerDevice()
        for (bleManagerDevice in persistedDevices) {
            if(bleManagerDevice.address == device.address)
            {
                needEditDevice = bleManagerDevice
                currentDevice = bleManagerDevice
                needEditDevice.robotName = name
                needEditDevice.connState = device.connState
                if(needEditDevice.bluetoothDevice!=null) {
                    needEditDevice.bluetoothDevice!!.robotName = name
                    needEditDevice.bluetoothDevice!!.connState = needEditDevice.connState
                }
                if(needEditDevice.device!=null) {
                    needEditDevice.device!!.robotName = name
                    needEditDevice.device!!.connState = needEditDevice.connState
                }
            }
        }
        if(needEditDevice != null) {
            persistedDevices.remove(currentDevice)
            persistedDevices.add(needEditDevice)
        }

        val deviceStrings = persistedDevices.map { device ->
            Gson().toJson(device)
        }.toSet()
        prefs.edit().putStringSet("PERSISTED_DEVICES1", deviceStrings).apply()
        showToast("Save successful")

    }


    fun removePersistedDevicesDevice(device: BleManagerDevice.BluetoothManagerDevice) {
        var needRemoveDevice: BleManagerDevice? = null
        for (bleManagerDevice in persistedDevices) {
            bleManagerDevice.address == device.address
            needRemoveDevice = bleManagerDevice
        }
        if (needRemoveDevice != null) {
            persistedDevices.remove(needRemoveDevice)
        }
        val deviceStrings = persistedDevices.map { device ->
            Gson().toJson(device)
        }.toSet()
        prefs.edit().putStringSet("PERSISTED_DEVICES1", deviceStrings).apply()
    }

    fun removeFoundDevicesDevice(device: BleManagerDevice.BluetoothManagerDevice) {
        foundDevices.remove(device)
    }

    fun removePairedDevicesDevice(device: BleManagerDevice.BluetoothManagerDevice) {
        pairedDevices.remove(device)
    }

    var uuid_service: String? = "6E400011-B5A3-F393-E0A9-E50E24DCCA9E"
    var uuid_notify: String? = "6E400012-B5A3-F393-E0A9-E50E24DCCA9E"
    var uuid_notify_write: String? = "6E400012-B5A3-F393-E0A9-E50E24DCCA9E"
    var TAG = "RobotBleManager"
    lateinit var bleManager: BleManager
    var bluetoothEnabled = true
    private val receivingBuffer = mutableListOf<Byte>()

    private fun initBleManager() {
        //check if this android device supports ble
        if (!BleManager.supportBle(context)) {
            return
        }
        val scanOptions = BleManager.ScanOptions
            .newInstance()
            .scanPeriod(8000)
            .scanDeviceName(null)
        val connectionOptions = BleManager.ConnectionOptions
            .newInstance()
            .connectionPeriod(15000)
        bleManager = BleManager
            .getInstance()
            .setScanOptions(scanOptions)
            .setConnectionOptions(connectionOptions)
            .setLog(true, "RobotBle")
            .init(context)

        timer.schedule(timerTask, 5000)
    }

    private var isFirst: Boolean = true

    fun getStringResource(context: Context, resId: Int): String {
        return context.resources.getString(resId)
    }

    // Initialization method
    private fun init() {
        try {
            initBleManager()
            var deiceFilterName = getStringResource(context, R.string.deice_filter_name)
            scanCallback = object : BleScanCallback {
                @SuppressLint("SuspiciousIndentation")
                override fun onLeScan(bleDevice: BleDevice?, rssi: Int, scanRecord: ByteArray?) {
                    if (bleDevice != null && bleDevice.name.indexOf(deiceFilterName) != -1) {
                        var bmDevice = BleManagerDevice.BluetoothManagerDevice()
                        bmDevice.name = bleDevice.name
                        bmDevice.address = bleDevice.bluetoothDevice.address
                        bmDevice.connState = bleDevice.connectionState

                        if (foundDevices.size == 0) {
                            var haveDevice = false
                            for (bluetoothManagerDevice in persistedDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }
                            if (haveDevice && bleDevice.name.indexOf(deiceFilterName) != -1) {
                                foundDevices.add(bmDevice)
                            }
                        } else {
                            var haveDevice = false
                            for (bluetoothManagerDevice in foundDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }

                            for (bluetoothManagerDevice in pairedDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }

                            for (bluetoothManagerDevice in persistedDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }

                            if (!haveDevice && bleDevice.name.indexOf(deiceFilterName) != -1) {
                                foundDevices.add(bmDevice)
                            }
                        }
                    }
                }

                override fun onStart(startScanSuccess: Boolean, info: String?) {
                    foundDevices.clear()
                    bluetoothEnabled = false
                }

                override fun onFinish() {
                    bluetoothEnabled = true
                }
            }
            pairedDevices.clear()
            persistedDevices.forEach { dev ->
                var list: List<BleDevice> = bleManager.connectedDevices;
                for (bleDevice in list) {
                    if (dev.address == bleDevice.address) {
                        dev.device?.let { pairedDevices.add(it) }
                    }
                }
                if (isFirst) {
                    var a = dev
                    dev.device?.let { pairedDevices.add(it) }
                }
            }
            isFirst = false
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }

    // Method to start scanning for BLE devices
    fun startDiscovery() {
        foundDevices.clear()
        bleManager.startScan(scanCallback)
    }

    fun startDiscovery(callback: BleScanCallback) {
        foundDevices.clear()
        bleManager.startScan(callback)
    }

    // Method to stop scanning for BLE devices
    fun stopDiscovery() {
        bleManager.stopScan()
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothEnabled
    }

    fun disconnectDevice(address: String) {
        if (bleManager.isConnected(address)) {
            bleManager.disconnect(address)
            showToast("Disconnect")
        }
    }

    fun connectToDevice(device: BluetoothDevice, callback: BleConnectCallback) {
        try {
            connectDev = null
            bleManager.connect(device.address, object : BleConnectCallback {
                override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                    when (failureCode) {
                        BleCallback.FAILURE_CONNECTION_NOT_ESTABLISHED -> {}
                        BleCallback.FAILURE_SERVICE_NOT_FOUND -> {}
                        BleCallback.FAILURE_CHARACTERISTIC_NOT_FOUND_IN_SERVICE -> {}
                        BleCallback.FAILURE_WRITE_UNSUPPORTED -> {}
                        BleCallback.FAILURE_OTHER -> {}
                    }
                    callback.onFailure(failureCode, info, device)
                }

                override fun onStart(startSuccess: Boolean, info: String?, device: BleDevice?) {
                }

                override fun onConnected(device: BleDevice?) {
                    device?.let {
                        connectDev = device
                        connectedDevices.remove(device)
                        connectedDevices.add(device)
                        Handler(Looper.getMainLooper()).postDelayed({
                            val gatt = bleManager.getBluetoothGatt(device.address)
                            gatt?.let {
                                val targetService = it.services?.find { service ->
                                    service.uuid.toString()
                                        .equals(uuid_service, ignoreCase = true)
                                }

                                targetService?.let { service ->
                                    uuid_service = service.uuid.toString()

                                    val writeCharacteristic =
                                        service.characteristics.find { characteristic ->
                                            (characteristic.properties and
                                                    (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                                        }

                                    writeCharacteristic?.let {
                                        uuid_notify_write = it.uuid.toString()
                                        setupWriteCharacteristic(it)
                                    } ?: run {
                                        Log.e(TAG, "not found ")
                                        showToast("device can not write")
                                        disconnect()
                                        return@postDelayed
                                    }

                                    val notifyCharacteristic =
                                        service.characteristics.find { characteristic ->
                                            (characteristic.properties and
                                                    (BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                                                            BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                                        }
                                    notifyCharacteristic?.let {
                                        uuid_notify = it.uuid.toString()
                                        setNotify(device)
                                    }
                                } ?: run {
                                    Log.e(TAG, "not found")
                                    showToast("service error")
                                    disconnect()
                                    return@postDelayed
                                }

                                gatt.requestMtu(517)
                                callback.onConnected(device)
                            }
                        }, 500)
                    }
                }

                override fun onDisconnected(info: String?, status: Int, device: BleDevice?) {
                    callback.onDisconnected(info, status, device)
                }
            }, BleHandlerThread("connectToDevice"))
        } catch (e: Exception) {
            showToast(e.message.toString())
        }
    }

    var isConnecting = false
    fun connectToDevice(mac: String, callback: BleConnectCallback) {
        try {
            if (isConnecting) {
                return
            }
            isConnecting = true
            connectDev = null
            if (bleManager.isConnected(mac)) {
                bleManager.disconnect(mac)
            }

            bleManager.connect(mac, object : BleConnectCallback {
                override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                    info?.let { showToast(it) }
                    when (failureCode) {
                        BleCallback.FAILURE_CONNECTION_NOT_ESTABLISHED -> {}
                        BleCallback.FAILURE_SERVICE_NOT_FOUND -> {}
                        BleCallback.FAILURE_CHARACTERISTIC_NOT_FOUND_IN_SERVICE -> {}
                        BleCallback.FAILURE_WRITE_UNSUPPORTED -> {}
                        BleCallback.FAILURE_OTHER -> {}
                    }
                    callback.onFailure(failureCode, info, device)
                    isConnecting = false
                }

                override fun onStart(startSuccess: Boolean, info: String?, device: BleDevice?) {
                    callback.onStart(startSuccess, info, device)
                    isConnecting = false
                }

                override fun onConnected(device: BleDevice?) {
                    device?.let {
                        connectDev = device
                        connectedDevices.remove(device)
                        connectedDevices.add(device)
                        Handler(Looper.getMainLooper()).postDelayed({
                            val gatt = bleManager.getBluetoothGatt(device.address)
                            gatt?.let {
                                val targetService = it.services?.find { service ->
                                    service.uuid.toString()
                                        .equals(uuid_service, ignoreCase = true)
                                }

                                targetService?.let { service ->
                                    uuid_service = service.uuid.toString()

                                    val writeCharacteristic =
                                        service.characteristics.find { characteristic ->
                                            (characteristic.properties and
                                                    (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                                        }

                                    writeCharacteristic?.let {
                                        uuid_notify_write = it.uuid.toString()
                                        setupWriteCharacteristic(it)
                                    } ?: run {
                                        Log.e(TAG, "not found ")
                                        showToast("device can not write")
                                        disconnect()
                                        return@postDelayed
                                    }

                                    val notifyCharacteristic =
                                        service.characteristics.find { characteristic ->
                                            (characteristic.properties and
                                                    (BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                                                            BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                                        }
                                    notifyCharacteristic?.let {
                                        uuid_notify = it.uuid.toString()
                                        setNotify(device)
                                    }
                                } ?: run {
                                    Log.e(TAG, "not found")
                                    showToast("service error")
                                    disconnect()
                                    return@postDelayed
                                }

                                gatt.requestMtu(517)
                                callback.onConnected(device)
                            }
                        }, 500)
                        isConnecting = false
                    }
                }

                override fun onDisconnected(info: String?, status: Int, device: BleDevice?) {
                    callback.onDisconnected(info, status, device)
                    isConnecting = false
                }
            }, BleHandlerThread("connectToDevice"))
        } catch (e: Exception) {
            showToast(e.message.toString())
            isConnecting = false
        }
    }

    // Other BLE operations (connect, disconnect, send data) will be added in subsequent parts
    // Additional methods for BLE connection and disconnection
    // Method to connect to a BLE device
    fun connectDevice(mac: String) {
        try {
            if (bleManager.isConnected(mac)) {
                bleManager.disconnect(mac)
            }
            bleManager.connect(mac, object : BleConnectCallback {
                override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                    if (info != null) {
                        showToast(info)
                    }
                    when (failureCode) {
                        BleCallback.FAILURE_CONNECTION_TIMEOUT -> {}
                        BleCallback.FAILURE_CONNECTION_CANCELED -> {}
                        BleCallback.FAILURE_CONNECTION_FAILED -> {}
                        else -> {}
                    }
                }

                override fun onStart(startSuccess: Boolean, info: String?, device: BleDevice?) {
                    Log.d(TAG, "Start connecting")
                }

                override fun onConnected(bleDevice: BleDevice?) {
                    Log.d(TAG, "Connect successfully")
                    showToast("Connect successfully")
                    var bmDevice = BleManagerDevice.BluetoothManagerDevice()
                    bmDevice.name = bleDevice!!.bluetoothDevice.name
                    bmDevice.address = bleDevice!!.bluetoothDevice.address
                    bmDevice.connState = bleDevice.connectionState

                    connectingDevice = bleDevice.bluetoothDevice
                    connectDev = bleDevice
                    connectedDevices.remove(bleDevice)
                    connectedDevices.add(bleDevice)
                    if (bleDevice != null) {
                        if (bleDevice.bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
                            bleDevice.bluetoothDevice.createBond()
                        }
                        saveDevice(bleDevice)
                        foundDevices.removeAll { it.address == bleDevice.address }
                        if (!pairedDevices.any { it.address == bleDevice.address }) {
                            pairedDevices.add(bmDevice)
                        }
                    }
                    connectingDevice = null

                    bleDevice?.let {
                        val gatt = bleManager.getBluetoothGatt(bleDevice.address)
                        gatt?.let {
                            val targetService = it.services?.find { service ->
                                service.uuid.toString()
                                    .equals(uuid_service, ignoreCase = true)
                            }

                            targetService?.let { service ->
                                uuid_service = service.uuid.toString()

                                val writeCharacteristic =
                                    service.characteristics.find { characteristic ->
                                        (characteristic.properties and
                                                (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                                    }

                                writeCharacteristic?.let {
                                    uuid_notify_write = it.uuid.toString()
                                    setupWriteCharacteristic(it)
                                } ?: run {
                                    Log.e(TAG, "not found ")
                                    showToast("device can not write")
                                    disconnect()
                                }

                                val notifyCharacteristic =
                                    service.characteristics.find { characteristic ->
                                        (characteristic.properties and
                                                (BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                                                        BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                                    }
                                notifyCharacteristic?.let {
                                    uuid_notify = it.uuid.toString()
                                    setNotify(bleDevice)
                                }
                            } ?: run {
                                Log.e(TAG, "not found")
                                showToast("service error")
                                disconnect()
                            }

                            gatt.requestMtu(517)
                        }
                    }
                }

                override fun onDisconnected(info: String?, status: Int, device: BleDevice?) {
                    Log.d(TAG, "Disconnect")
                    showToast("Disconnect")
                }
            }, BleHandlerThread("connectDevice"))
        } catch (e: Exception) {
            showToast(e.message.toString())
        }
    }

    fun connectDevice(mac: String,callback: BleConnectCallback) {
        try {
            if (bleManager.isConnected(mac)) {
                bleManager.disconnect(mac)
            }
            bleManager.connect(mac, object : BleConnectCallback {
                override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                    if (info != null) {
                        showToast(info)
                    }
                    when (failureCode) {
                        BleCallback.FAILURE_CONNECTION_TIMEOUT -> {}
                        BleCallback.FAILURE_CONNECTION_CANCELED -> {}
                        BleCallback.FAILURE_CONNECTION_FAILED -> {}
                        else -> {}
                    }
                    callback.onFailure(failureCode,info,device)
                }

                override fun onStart(startSuccess: Boolean, info: String?, device: BleDevice?) {
                    Log.d(TAG, "Start connecting")
                }

                override fun onConnected(bleDevice: BleDevice?) {
                    Log.d(TAG, "Connect successfully")
                    showToast("Connect successfully")
                    var bmDevice = BleManagerDevice.BluetoothManagerDevice()
                    bmDevice.name = bleDevice!!.bluetoothDevice.name
                    bmDevice.address = bleDevice!!.bluetoothDevice.address
                    bmDevice.connState = bleDevice.connectionState

                    connectingDevice = bleDevice.bluetoothDevice
                    connectDev = bleDevice
                    connectedDevices.remove(bleDevice)
                    connectedDevices.add(bleDevice)
                    if (bleDevice != null) {
                        if (bleDevice.bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
                            bleDevice.bluetoothDevice.createBond()
                        }
                        saveDevice(bleDevice)
                        foundDevices.removeAll { it.address == bleDevice.address }
                        if (!pairedDevices.any { it.address == bleDevice.address }) {
                            pairedDevices.add(bmDevice)
                        }
                    }
                    connectingDevice = null

                    bleDevice?.let {
                        val gatt = bleManager.getBluetoothGatt(bleDevice.address)
                        gatt?.let {
                            val targetService = it.services?.find { service ->
                                service.uuid.toString()
                                    .equals(uuid_service, ignoreCase = true)
                            }

                            targetService?.let { service ->
                                uuid_service = service.uuid.toString()

                                val writeCharacteristic =
                                    service.characteristics.find { characteristic ->
                                        (characteristic.properties and
                                                (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                                    }

                                writeCharacteristic?.let {
                                    uuid_notify_write = it.uuid.toString()
                                    setupWriteCharacteristic(it)
                                } ?: run {
                                    Log.e(TAG, "not found ")
                                    showToast("device can not write")
                                    disconnect()
                                }

                                val notifyCharacteristic =
                                    service.characteristics.find { characteristic ->
                                        (characteristic.properties and
                                                (BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                                                        BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                                    }
                                notifyCharacteristic?.let {
                                    uuid_notify = it.uuid.toString()
                                    setNotify(bleDevice)
                                }
                            } ?: run {
                                Log.e(TAG, "not found")
                                showToast("service error")
                                disconnect()
                            }

                            gatt.requestMtu(517)
                        }
                    }
                    callback.onConnected(bleDevice)
                }

                override fun onDisconnected(info: String?, status: Int, device: BleDevice?) {
                    Log.d(TAG, "Disconnect")
                    showToast("Disconnect")
                    callback.onDisconnected(info,status,device)

                }
            }, BleHandlerThread("connectDevice"))
        } catch (e: Exception) {
            showToast(e.message.toString())
        }
    }


    fun writeData(device: BleDevice, data: ByteArray, callback: BleWriteCallback) {
        bleManager.write(
            device,
            uuid_service,
            uuid_notify_write,
            data,
            callback
        )
    }

    fun readData(mCurrentDevice: BleDevice) {
        var scanCallback1 = object : BleReadCallback {
            override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
            }

            override fun onReadSuccess(data: ByteArray?, device: BleDevice?) {
            }
        }
        bleManager.read(
            mCurrentDevice!!,
            uuid_service,
            uuid_notify,
            scanCallback1
        )
    }

    fun setNotify(mCurrentDevice: BleDevice?) {
        if (mCurrentDevice == null) {
            return
        }
        Thread.sleep(200)
        Log.d(TAG, "Notify Start")
        bleManager.notify(mCurrentDevice,
            uuid_service,
            uuid_notify,
            object : BleNotifyCallback {
                override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                    when (failureCode) {
                        BleCallback.FAILURE_CONNECTION_NOT_ESTABLISHED -> {}
                        BleCallback.FAILURE_SERVICE_NOT_FOUND -> {}
                        BleCallback.FAILURE_CHARACTERISTIC_NOT_FOUND_IN_SERVICE -> {}
                        BleCallback.FAILURE_NOTIFICATION_OR_INDICATION_UNSUPPORTED -> {}
                        BleCallback.FAILURE_OTHER -> {}
                    }
                }

                override fun onCharacteristicChanged(data: ByteArray?, device: BleDevice?) {
                    data?.let {
                        Log.d(TAG, "Response : ${data.HextoString()}")
                        receivingBuffer.addAll(it.toList())
                        var index = 0
                        while (index < receivingBuffer.size - 1) {
                            if (receivingBuffer[index] == 0xFF.toByte() && receivingBuffer[index + 1] == 0xAA.toByte()) {
                                val startIndex = index
                                index += 2 // Move past FF AA

                                while (index < receivingBuffer.size - 1) {
                                    if (receivingBuffer[index] == 0xFF.toByte() && receivingBuffer[index + 1] == 0xBB.toByte()) {
                                        // Found FF BB
                                        val endIndex = index + 2
                                        val block = receivingBuffer.subList(startIndex, endIndex)
                                            .toByteArray()
                                        parseReceivedData(block)
                                        // Remove processed bytes from buffer
                                        receivingBuffer.subList(0, endIndex).clear()
                                        index = endIndex // Move index past FF BB
                                        break // Exit inner loop
                                    }
                                    index++
                                }
                            } else if (receivingBuffer[index] == 0xAA.toByte() && receivingBuffer[index + 1] == 0x03.toByte()) {
                                val startIndex = index
                                index += 2 // Move past AA 03

                                while (index < receivingBuffer.size) {
                                    if (receivingBuffer[index] == 0xBB.toByte()) {
                                        val endIndex = index
                                        if (endIndex < receivingBuffer.size) {
                                            val block =
                                                receivingBuffer.subList(startIndex, endIndex + 1)
                                                    .toByteArray()
                                            parseAA03Data(block)
                                            receivingBuffer.subList(0, endIndex + 1).clear()
                                            index = startIndex
                                        }
                                        break
                                    }
                                    index++
                                }
                            } else {
                                index++
                            }
                        }
                    }
                }

                override fun onNotifySuccess(notifySuccessUuid: String?, device: BleDevice?) {
                    Log.d(TAG, "Notification set successfully")
                }
            })
    }

    fun parseReceivedData(data: ByteArray) {
//        showToast(data.HextoString())
    }

    fun parseAA03Data(data: ByteArray) {
//        showToast(data.HextoString())
    }


    private fun sendCommand(command: ByteArray, mCurrentDevice: BleDevice) {
        isOnline = true
        if (mCurrentDevice == null) {
            Log.e(TAG, "No connected device")
            return
        }
        if (!bleManager.isConnected(mCurrentDevice.address)) {
            Log.e(TAG, "Device is not connected")
            return
        }
        writeData(mCurrentDevice!!, command, object : BleWriteCallback {
            override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                when (failureCode) {
                    BleCallback.FAILURE_CONNECTION_NOT_ESTABLISHED -> Log.e(
                        TAG,
                        "Connection not established"
                    )

                    BleCallback.FAILURE_SERVICE_NOT_FOUND -> Log.e(TAG, "Service not found")
                    BleCallback.FAILURE_CHARACTERISTIC_NOT_FOUND_IN_SERVICE -> Log.e(
                        TAG,
                        "Characteristic not found in service"
                    )

                    BleCallback.FAILURE_WRITE_UNSUPPORTED -> Log.e(TAG, "Write unsupported")
                    BleCallback.FAILURE_OTHER -> Log.e(TAG, "Other failure: $info")
                }
            }

            override fun onWriteSuccess(justWrite: ByteArray?, device: BleDevice?) {
                if (justWrite != null) {
                    val dataString = justWrite.HextoString()
                    sendDataCallback.onWriteSuccess(justWrite, device)
                    Log.d(TAG, "Command send successfully: $dataString")
                }
            }
        })
    }

    fun ByteArray.HextoString(): String {
        return this.joinToString(separator = " ") { byte ->
            String.format(
                "%02X",
                byte
            )
        }
    }

    // Method to disconnect from a BLE device
    fun disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt!!.disconnect()
            bluetoothGatt = null
        }
    }

    // Constructor
    init {
        this.context = context.applicationContext
        init()
    }

    // Helper method to show Toast messages
    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private var sendDataCallback = object : BleWriteCallback {
        override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
        }

        override fun onWriteSuccess(data: ByteArray?, device: BleDevice?) {
            val byteValue = data?.getOrNull(12)
            val staleString = String.format("%02X", byteValue)

            if (staleString == "01") {
                data!!?.set(12, BleCommand.EMPTY_BYTE)
                if (device != null) {
                    sendDataToDevice(device.bluetoothDevice, data)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendDataToDevice(device: BluetoothDevice, dataToSend: ByteArray) {
        try {
            connectingDevice = device
            if (connectDev != null && bleManager.isConnected(device.address)) {
                sendCommand(dataToSend, connectDev!!)
            } else if (connectDev != null && !bleManager.isConnected(device.address)) {
                connectToDevice(device, object : BleConnectCallback {
                    override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                    }

                    override fun onStart(startSuccess: Boolean, info: String?, device: BleDevice?) {
                    }

                    override fun onConnected(device: BleDevice?) {
                        sendCommand(dataToSend, connectDev!!)
                    }

                    override fun onDisconnected(info: String?, status: Int, device: BleDevice?) {
                    }
                })
            }
        } catch (e: Exception) {
            showToast(e.message.toString())
        }
    }


    @SuppressLint("MissingPermission")
    fun sendDataToDevice(device: String, dataToSend: ByteArray) {
        try {
            connectToDevice(device, object : BleConnectCallback {
                override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
                }

                override fun onStart(startSuccess: Boolean, info: String?, device: BleDevice?) {
                }

                override fun onConnected(bleDevice: BleDevice?) {
                    if (bleDevice != null) {
                        connectingDevice = bleDevice.bluetoothDevice
                        connectDev = bleDevice
                    }
                    sendCommand(dataToSend, connectDev!!)
                }

                override fun onDisconnected(info: String?, status: Int, device: BleDevice?) {
                }
            })

        } catch (e: Exception) {
            showToast(e.message.toString())
        }
    }


    private fun setupWriteCharacteristic(characteristic: BluetoothGattCharacteristic) {
        characteristic.writeType = when {
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            else -> {
                Log.e(TAG, "can not write")
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
        }
    }
}