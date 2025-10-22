@file:Suppress("DEPRECATION", "PreviewAnnotationInFunctionWithParameters")

package com.hyun.robot.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ficat.easyble.BleDevice
import com.ficat.easyble.gatt.callback.BleConnectCallback
import com.ficat.easyble.scan.BleScanCallback
import com.hyun.robot.MyApplication.Companion.bleManager
import com.hyun.robot.R
import com.hyun.robot.utils.BleManagerDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
@Preview(
    showBackground = true, showSystemUi = true,
    device = "spec:parent=pixel_5,orientation=landscape"
)
fun AddDeviceScreen(activity: AddDeviceActivity) {
    var isScanning by remember { mutableStateOf(false) }
    var showEnableBtDialog by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    var isAddDeviceScreenVisible by remember { mutableStateOf(false) }

    fun showToast(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun scanBtnBleAction() {
        if (hasPermissions) {
            isScanning = !isScanning
            if (isScanning) bleManager.startDiscovery(object :
                BleScanCallback {
                override fun onLeScan(
                    bleDevice: BleDevice?,
                    rssi: Int,
                    scanRecord: ByteArray?
                ) {
                    var deiceFilterName = activity.getStringResource(
                        activity,
                        R.string.deice_filter_name
                    )
                    if (bleDevice != null && bleDevice.name.indexOf(
                            deiceFilterName
                        ) != -1
                    ) {
                        val bmDevice =
                            BleManagerDevice.BluetoothManagerDevice()
                        bmDevice.name = bleDevice.name
                        bmDevice.address = bleDevice.bluetoothDevice.address
                        bmDevice.connState = bleDevice.connectionState

                        if (bleManager.foundDevices.size == 0) {
                            var haveDevice = false
                            for (bluetoothManagerDevice in bleManager.persistedDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }
                            if (!haveDevice && bleDevice.name.indexOf(
                                    deiceFilterName
                                ) != -1
                            ) {
                                bleManager.foundDevices.add(bmDevice)
                            }
                        } else {
                            var haveDevice = false
                            for (bluetoothManagerDevice in bleManager.foundDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }

                            for (bluetoothManagerDevice in bleManager.pairedDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }

                            for (bluetoothManagerDevice in bleManager.persistedDevices) {
                                if (bmDevice.address.toString() == bluetoothManagerDevice.address.toString()) {
                                    haveDevice = true
                                }
                            }

                            if (!haveDevice && bleDevice.name.indexOf(
                                    deiceFilterName
                                ) != -1
                            ) {
                                bleManager.foundDevices.add(bmDevice)
                            }
                        }
                    }
                }

                override fun onStart(
                    startScanSuccess: Boolean,
                    info: String?
                ) {
                    isScanning = true
                    bleManager.foundDevices.clear()
                }

                override fun onFinish() {
                    isScanning = false
                }
            })
            else bleManager.stopDiscovery()
        } else {
            showEnableBtDialog = true
        }
    }

    PermissionHandler(
        onPermissionsGranted = {
            hasPermissions = true
            scanBtnBleAction()
        },
        onPermissionsDenied = { },
        activity
    )

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    "Add Robot", fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Left,
                    fontSize = 24.sp,
                )
            }, actions = {
                IconButton(onClick = {
                    isAddDeviceScreenVisible = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                    )
                }
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Devices  (${bleManager.foundDevices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 18.dp)
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { scanBtnBleAction() }
                ) {
                    Text(if (isScanning) "Stop Scanning" else "Start Scanning")
                }
            }
            LazyColumn {
                items(bleManager.foundDevices) { device ->
                    var isLoadingView = false
                    if (bleManager.connectingDevice != null) {
                        isLoadingView = bleManager.connectingDevice!!.address == device.address
                    }
                    DeviceItem(
                        device = device,
                        isLoading = isLoadingView,
                        haveClose = false
                    ) {
                        device.address?.let {
                            bleManager.connectDevice(it, object : BleConnectCallback {
                                override fun onFailure(
                                    failureCode: Int,
                                    info: String?,
                                    device: BleDevice?
                                ) {
                                    info?.let { it1 -> showToast(it1) }
                                }

                                override fun onStart(
                                    startSuccess: Boolean,
                                    info: String?,
                                    device: BleDevice?
                                ) {
                                }

                                override fun onConnected(device: BleDevice?) {
                                    isAddDeviceScreenVisible = true
                                    bleManager.connectDev = device
                                    showToast("Device Connected")
                                }

                                override fun onDisconnected(
                                    info: String?,
                                    status: Int,
                                    device: BleDevice?
                                ) {
                                    showToast("Device disConnected")
                                }
                            })
                            isLoadingView = false
                        }
                    }
                }
            }
            if (isScanning) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        if (isAddDeviceScreenVisible) {
            activity.BackToHome()
        }
    }
}


@Composable
fun DeviceItem(
    device: BleManagerDevice.BluetoothManagerDevice,
    isLoading: Boolean = false,
    haveClose: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isLoading,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BluetoothIcon()
            DeviceInfo(device)
            if (haveClose) {
                BluetoothDeleteIcon(device)
            }
            ConnectionIndicator(isLoading)
        }
    }
}


@Composable
private fun BluetoothDeleteIcon(device: BleManagerDevice.BluetoothManagerDevice) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    bleManager.removePersistedDevicesDevice(device)
                    bleManager.removePairedDevicesDevice(device)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Red
                )
            }
        }
        Spacer(Modifier.width(24.dp))
    }
}


@Composable
private fun BluetoothIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_robot),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
    Spacer(Modifier.width(16.dp))
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceInfo(device: BleManagerDevice.BluetoothManagerDevice) {
    Column(modifier = Modifier) {
        Text(
            text = device.name ?: "Unnamed Device",
            style = MaterialTheme.typography.bodyLarge
        )
        device.address?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit,
    activity: AddDeviceActivity
) {
    val context = LocalContext.current
    val currentActivity = LocalContext.current as Activity
    var showRationale by remember { mutableStateOf(false) }
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }

    val requiredPermissions = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ (API 31+)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-11 (API 29-30)
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }

            else -> {
                // Android 6.0-9 (API 23-28)
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            missingPermissions.isEmpty() -> onPermissionsGranted()
            missingPermissions.any { perm ->
                ActivityCompat.shouldShowRequestPermissionRationale(currentActivity, perm)
            } -> showRationale = true

            else -> permissionsLauncher.launch(requiredPermissions)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Bluetooth permission is required") },
            text = { Text("This feature requires Bluetooth permission to discover nearby devices") },
            confirmButton = {
                TextButton(onClick = {
                    permissionsLauncher.launch(requiredPermissions)
                    showRationale = false
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    if (bluetoothAdapter != null) {
        val isEnabled = bluetoothAdapter.isEnabled
        if (!isEnabled) {
            BluetoothHelper(activity).enableBluetooth()
        }
    }
}


class BluetoothHelper(private val activity: Activity) {
    companion object {
        const val REQUEST_ENABLE_BT = 1
    }

    fun enableBluetooth() {
        if (checkPermission().not()) return

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}

@Composable
private fun ConnectionIndicator(isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(
            color = Color.Black,
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
    }
}


