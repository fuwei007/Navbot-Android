package com.hyun.robot.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ficat.easyble.BleDevice
import com.ficat.easyble.gatt.callback.BleConnectCallback
import com.hyun.robot.MyApplication
import com.hyun.robot.R
import com.hyun.robot.utils.BaseBlueManager
import com.hyun.robot.utils.BleCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private var connectCallback: BleConnectCallback? = null
private var baseHeight: Byte = BleCommand.BASE_HEIGHT
private var roll: Byte = BleCommand.EMPTY_BYTE
private var linearVel: Byte = BleCommand.EMPTY_BYTE
private var angularVel: Byte = BleCommand.EMPTY_BYTE
private var joyX: Byte = BleCommand.EMPTY_BYTE
private var joyY: Byte = BleCommand.EMPTY_BYTE
private var stableStatus: Byte = BleCommand.BYTE_01
private var isToggleOn = false
private var showDialog = false
private var showLoading = true
private var bDevice: BluetoothDevice? = null
private var connectCount = 0
private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
private lateinit var activity: DeviceDetailActivity
private var isReady = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(content: DeviceDetailActivity) {
    activity = content
    var showSplash by remember { mutableStateOf(true) }
    val image = painterResource(R.drawable.bg_detail)
    var gotoHome by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(activity.deviceName ?: "") }
    var loadingText by remember { mutableStateOf("Connecting" ?: "") }
    var isConnectDevice by remember { mutableStateOf(false) }
    var isToggleReady by remember { mutableStateOf(false) }

    connectCallback = object : BleConnectCallback {
        override fun onFailure(
            failureCode: Int,
            info: String?,
            device: BleDevice?
        ) {
            showLoading = false
            MyApplication.bleManager.connectDev = null
            connectCount++
            showSplash = false
            isConnectDevice = false
            isToggleOn = false
        }

        override fun onStart(
            startSuccess: Boolean,
            info: String?,
            device: BleDevice?
        ) {
            loadingText = "Connecting"
            showSplash = true
        }

        override fun onConnected(device: BleDevice?) {
            showLoading = false
            if (device != null) {
                isConnectDevice = true
                isToggleOn = true
                bDevice = device.bluetoothDevice
                MyApplication.bleManager.connectingDevice = bDevice
                MyApplication.bleManager.connectDev = device
                MyApplication.bleManager.connectedDevices.remove(device)
                MyApplication.bleManager.connectedDevices.add(device)
            }
            showSplash = false
            connectCount = 0
            joyY = 0x05
            sendBleJoyXYData()
            showToast("Connected")
        }

        override fun onDisconnected(
            info: String?,
            status: Int,
            device: BleDevice?
        ) {
            isConnectDevice = false
            isToggleOn = false
            showToast("Disconnected")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                gotoHome = true
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(40.dp),
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "BACK"
                            )
                        }
                    },
                    title = {

                        Row(
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                modifier = Modifier.padding(top = 10.dp),
                                text = deviceName,
                                color = Color.Black,
                                fontSize = 18.sp,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 40.dp, end = 8.dp)
                            ) {
                                Text(
                                    fontSize = 14.sp,
                                    text = "ROBOT GO",
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            Switch(checked = isToggleReady, onCheckedChange = { status ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        isToggleReady = status
                                        isReady = status
                                        if (isToggleOn) {
                                            sendBleSettingData()
                                        } else {
                                            isToggleReady = false
                                            isReady = false
                                            showToast("Please Connect 'Navbot_en01' first.")
                                        }
                                    }
                                }
                            } , colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor =  Color(0xFF007AFF),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray,
                            ))
                        }
                    },
                    actions = {

                        Row(
                            modifier = Modifier.size(40.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            isToggleOn = !isToggleOn
                                            if (isToggleOn) {
                                                toConnectDevice()
                                            } else {
                                                MyApplication.bleManager.disconnectDevice(
                                                    activity.deviceAddress
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isConnectDevice) R.drawable.ic_close
                                        else R.drawable.ic_open
                                    ),
                                    contentDescription = "disconnect",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(7f)
                            .padding(16.dp)
                    ) {
                        Joystick()
                    }
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            VerticalSliderView("Base Height", "mm", 0.0f, 53, 32,true)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            VerticalSliderView("roll", "°", 0.5f, 60, -30,false)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            VerticalSliderView("Linear Vel", "mm/s", 0.5f, 400, -200,false)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            VerticalSliderView("Angular Vel", "°/s", 0.5f, 200, -100,false)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(bottom = 40.dp)
                            .weight(4f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        IconButton(
                            onClick = {
                                bDevice = MyApplication.bleManager.connectingDevice
                                if (!isReady) {
                                    showToast("Please turn on the ‘ROBOT GO’ button first.")
                                } else if (isToggleOn && bDevice != null) {
                                    Handler(Looper.getMainLooper()).postDelayed(
                                        {
                                            sendBleJumpData()
                                        },
                                        100
                                    )
                                } else {
                                    if (!isToggleOn) {
                                        toConnectDevice()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_reset),
                                contentDescription = "Reset",
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }
            }
        }
        if (gotoHome) {
            activity.BackToHome()
        }
        if (showDialog) {
            activity.WifiListDialog(onDismiss = { showDialog = false }, onConnect = {})
        }
    }
    if (showSplash) {
        if (MyApplication.bleManager.connectDev?.isConnected != true) {
            activity.LoadingConnectingScreen(
                isConnecting = showSplash,
                loadingText = loadingText,
                onTimeout = { showSplash = false })
            toConnectDevice()
        } else {
            bDevice = MyApplication.bleManager.connectDev!!.bluetoothDevice
            showLoading = false
            isToggleOn = true
            isConnectDevice = true
        }
    }
}

private fun sendBleJoyXYData() {
    MyApplication.bleManager.sendDataToDevice(
        device = bDevice!!,
        dataToSend = BleCommand.getBleCommand(
            height = baseHeight,
            roll = roll,
            linearH = linearVel,
            linearW = linearVel,
            angular = angularVel,
            stable = stableStatus,
            joyX = joyX,
            joyY = joyY
        )
    )
}

fun sendBleSettingData() {
    MyApplication.bleManager.sendDataToDevice(
        device = bDevice!!,
        dataToSend = BleCommand.getBleCommand(
            height = baseHeight,
            roll = roll,
            linearH = linearVel,
            linearW = linearVel,
            angular = angularVel,
            stable = stableStatus,
        )
    )
}

fun sendBleJumpData() {
    MyApplication.bleManager.sendDataToDevice(
        device = bDevice!!,
        dataToSend = BleCommand.getBleCommand(
            height = baseHeight,
            roll = roll,
            linearH = linearVel,
            linearW = linearVel,
            angular = angularVel,
            stable = stableStatus,
            dir = 0x01
        )
    )

}

private fun toConnectDevice() {
    scope.launch {
        withContext(Dispatchers.IO) {
            MyApplication.bleManager.connectToDevice(
                activity.deviceAddress,
                connectCallback!!
            )
        }
    }
}

private fun showToast(message: String) {
    CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.Main) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }
}

class JoystickFilter(private val alpha: Float = 0.3f) {
    private var filteredX = 0f
    private var filteredY = 0f

    fun filter(input: Offset): Offset {
        filteredX = alpha * input.x + (1 - alpha) * filteredX
        filteredY = alpha * input.y + (1 - alpha) * filteredY
        return Offset(filteredX, filteredY)
    }
}


@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
) {
    val context = LocalContext.current
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
    val currentActivity = LocalContext.current as Activity
    var showRationale by remember { mutableStateOf(false) }
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

    when (val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) {
        null -> {
        }

        else -> {
            val isEnabled = bluetoothAdapter.isEnabled
            if (!isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(enableBtIntent, BaseBlueManager.REQUEST_ENABLE_BT)
            }
        }
    }
}

@Composable
fun VerticalSlider(
    maxHeight: Dp = 200.dp,
    initialProgress: Float = 0.5f,
    onProgressChanged: (Float) -> Unit,
    canDrag: Boolean
) {
    val cornerRadius = 30.dp
    val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
    val cornerRadiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    var progressHeight by remember {
        mutableStateOf(
            initialProgress.coerceIn(
                0f,
                1f
            ) * maxHeightPx
        )
    }
    val animatedHeight by animateFloatAsState(targetValue = progressHeight, label = "")

    Box(
        modifier = Modifier
            .size(width = 30.dp, height = maxHeight)
            .background(Color(0xFFE1E9F0), RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (!isReady) {
                        showToast("Please turn on the ‘ROBOT GO’ button first.")
                    }else if(!canDrag){

                    }else {
                        val newHeight = (progressHeight - dragAmount).coerceIn(0f, maxHeightPx)
                        progressHeight = newHeight
                        onProgressChanged(newHeight / maxHeightPx)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFF007AFF),
                topLeft = Offset(0f, size.height - animatedHeight),
                size = Size(size.width, animatedHeight),
                cornerRadius = CornerRadius(cornerRadiusPx)
            )
        }
    }
}

@Composable
fun VerticalSliderView(
    verticalName: String,
    verticalUnit: String,
    baseVolume: Float,
    verticalPlusValue: Int,
    verticalAddValue: Int,
    canDrag:Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        var volume by remember { mutableStateOf(baseVolume) }

        VerticalSlider(
            initialProgress = volume,
            onProgressChanged = {
                if (!isReady) {
                    showToast("Please turn on the ‘ROBOT GO’ button first.")
                }else{
                    volume = it
                    baseHeight =
                        BleCommand.intToUnsignedByte((volume * verticalPlusValue + verticalAddValue).toInt())
                    if (!isReady) {
                        showToast("Please turn on the ‘ROBOT GO’ button first.")
                    } else if (isToggleOn) {
                        if (showLoading) {
                            toConnectDevice()
                        } else {
                            bDevice = MyApplication.bleManager.connectingDevice
                            sendBleSettingData()
                        }
                    } else {
                        toConnectDevice()
                    }
                }
            },
            canDrag = canDrag
        )

        Text(
            text = "${(volume * verticalPlusValue + verticalAddValue).toInt()}${verticalUnit}",
            textAlign = TextAlign.Center,
            fontSize = 8.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )

        Text(
            text = verticalName,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

fun Offset.applyDeadZone(threshold: Float = 2f): Offset {
    val distance = getDistance()
    return when {
        distance < threshold -> Offset.Zero
        distance < threshold * 2 -> this * 0.5f
        else -> this
    }
}

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    joystickSize: Dp = 130.dp
) {
    val radius = joystickSize / 2
    val center = remember { Offset(0.5f, 0.5f) }
    var buttonOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var lastSendTime by remember { mutableStateOf(0L) }
    var isJoystickActive = false
    val kalmanFilter = remember { BaseActivity.KalmanFilterProcessor() }
    var direction by remember { mutableStateOf(Offset(0f, 0f)) }
    val filter = remember { JoystickFilter() }
    bDevice = MyApplication.bleManager.connectingDevice

    fun positionChange(raw1: Offset) {
        if (isToggleOn && bDevice != null) {
            val raw = kalmanFilter.process(raw1)
            direction = raw
            val directionAdjusted = Offset(x = raw.y, y = -raw.x)
            val normalizedDirection = directionAdjusted.normalize()
            val processed = directionAdjusted
                .applyDeadZone(5f)
                .let { filter.filter(it) }
            val clamped = Offset(
                x = processed.x.coerceIn(-100f, 100f),
                y = processed.y.coerceIn(-100f, 100f)
            )
            val directionAdjusted1 = Offset(x = -raw1.y, y = -raw1.x)
            val normalizedDirection1 = directionAdjusted1.normalize()

            if (normalizedDirection1 != Offset(
                    0f,
                    0f
                ) && raw1.y != 0.0f && raw1.x != 0.0f
            ) {
                isJoystickActive = true
                sendJoystickData(clamped)
                lastSendTime = System.currentTimeMillis()
            } else {
                if (isJoystickActive) {
                    isJoystickActive = false
                }
            }
            if (isJoystickActive) {
                sendJoystickData(direction)
            }
        }
    }

    Box(
        modifier = modifier
            .size(joystickSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        if (!isReady) {
                            showToast("Please turn on the ‘ROBOT GO’ button first.")
                        } else {
                            val newOffset = Offset(
                                x = (buttonOffset.x + dragAmount.x).coerceIn(
                                    -radius.toPx(),
                                    radius.toPx()
                                ),
                                y = (buttonOffset.y + dragAmount.y).coerceIn(
                                    -radius.toPx(),
                                    radius.toPx()
                                )
                            )
                            buttonOffset = newOffset
                            val normalizedX =
                                (buttonOffset.x / radius.toPx()).coerceIn(-1f..1f) * 100
                            val normalizedY =
                                (buttonOffset.y / radius.toPx()).coerceIn(-1f..1f) * 100
                            positionChange(Offset(normalizedX, normalizedY))
                        }
                    },
                    onDragEnd = {
                        if (!isReady) {
                            showToast("Please turn on the ‘ROBOT GO’ button first.")
                        } else{
                            buttonOffset = Offset(0f, 0f)
                            joyX = 0X00
                            joyY = 0X00
                            isJoystickActive = false
                            if (!showLoading && bDevice != null) {
                                Handler(Looper.getMainLooper()).postDelayed(
                                    {
                                        sendBleJoyXYData()
                                    },
                                    100
                                )
                            } else {
                                toConnectDevice()
                            }
                        }
                    }
                )
            }
            .border(Dp(2f), Color(0xFF007AFF), CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val absoluteCenter = Offset(
                x = canvasSize.width * center.x,
                y = canvasSize.height * center.y
            )
            val finalPosition = absoluteCenter + buttonOffset
            drawCircle(
                color = Color(0xFF007AFF),
                radius = radius.toPx() * 0.4f,
                center = finalPosition
            )
        }
    }
}

private fun Offset.normalize(): Offset {
    val length = kotlin.math.hypot(x.toDouble(), y.toDouble()).toFloat()
    return Offset(x / length, y / length)
}

private fun sendJoystickData(direction: Offset) {
    val (joyDataX, joyDataY) = when {
        direction.isSpecified -> {
            val clampedY = (-direction.y).coerceIn(-100f, 100f)
            val clampedX = (-direction.x).coerceIn(-100f, 100f)
            clampedY.toInt().toByte() to clampedX.toInt().toByte()
        }

        else -> 0.toByte() to 0.toByte()
    }
    joyX = joyDataX
    joyY = joyDataY
    if (!showLoading && bDevice != null) {
        sendBleJoyXYData()
    } else {
        toConnectDevice()
    }

}
