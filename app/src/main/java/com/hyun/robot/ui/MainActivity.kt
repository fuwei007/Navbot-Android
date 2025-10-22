package com.hyun.robot.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.ficat.easyble.BleManager
import com.hyun.robot.MyApplication
import com.hyun.robot.MyApplication.Companion.bleManager
import com.hyun.robot.R
import com.hyun.robot.ui.theme.RobotTheme
import com.hyun.robot.utils.BleManagerDevice
import com.hyun.robot.utils.RobotBleManager


class MainActivity : BaseActivity() {
    private lateinit var context: Context

    companion object {
        private const val REQUEST_CODE: Int = 0x01
    }

    private fun requestBLEPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ), REQUEST_CODE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        context = this
        MyApplication.isFirstDeviceDetail = true
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            if (showSplash) {
                LoadingScreen(onTimeout = { showSplash = false })
            } else {
                MainScreen()
                showSplash = false
            }
        }
        requestBLEPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.stopDiscovery()
    }
}

@Composable
fun AddDevice() {
    val intent = Intent(LocalContext.current, AddDeviceActivity::class.java)
    var bundle = Bundle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        startActivity(context, intent, bundle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(
    showBackground = true, showSystemUi = true,
    device = "spec:parent=pixel_5,orientation=landscape"
)
fun MainScreen() {
    bleManager = RobotBleManager(LocalContext.current)
    var test = BleManagerDevice.BluetoothManagerDevice()
    test.robotName = "111"
    test.address = "2222"
    bleManager.pairedDevices.add(test)

    val image = painterResource(R.drawable.bg_robot_list)
    var isAddDeviceScreenVisible by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
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
                    title = {
                        Text(
                            "My Robot", fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Left,
                            fontSize = 24.sp,
                            style = MaterialTheme.typography.bodySmall

                        )
                    }
                )
            },
            floatingActionButton = {
                IconButton(
                    onClick = {
                        isAddDeviceScreenVisible = true
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Add",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bleManager.pairedDevices) { device ->
                        GridItem(
                            device = device,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                        )
                    }
                }
            }
            if (isAddDeviceScreenVisible) {
                AddDevice()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun showDetail(device: BleManagerDevice.BluetoothManagerDevice, deviceName: String) {
    val intent = Intent(LocalContext.current, DeviceDetailActivity::class.java).apply {
        putExtra("DEVICE_ADDRESS", device.address)
        putExtra("DEVICE_NAME", deviceName)
    }
    var bundle = Bundle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        startActivity(context, intent, bundle)
    }
}

fun getFirst8CharsOfMac(macAddress: String): String {
    val cleanMac = macAddress.replace(":", "")
    return cleanMac.substring(0, 4)
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
private fun GridItem(
    device: BleManagerDevice.BluetoothManagerDevice,
    modifier: Modifier = Modifier
) {
    var isShowDetail by remember { mutableStateOf(false) }
    val backgroundColor = Color.White
    var deviceName by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable {
                isShowDetail = true
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            deviceName =
                (device.name + device.address?.let { getFirst8CharsOfMac(it) }) ?: "Unknown"
            if (device.robotName != null) {
                deviceName = device.robotName!!
            }
            RobotItemTopView(
                initialText = deviceName,
                device = device,
            )

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .border(Dp(1f), Color(0xC8CBD0D6), RoundedCornerShape(Dp(16f))),
                contentAlignment = Alignment.TopStart
            ) {
                BatteryIndicator(
                    level = 10,
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = 10.dp, y = 4.dp)
                )
                Image(
                    painter = painterResource(R.drawable.ic_robot_item),
                    contentDescription = "Device Image",
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.Center)
                        .clickable {
                            isShowDetail = true
                        },
                    contentScale = ContentScale.Fit,

                    )
            }

            if (isShowDetail) {
                showDetail(device, deviceName)
            }
        }
    }
}


@Composable
fun BatteryIndicator(
    level: Int, // 0-100
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        var canvasColor = when {
            level < 20 -> Color.Red
            level < 50 -> Color.Yellow
            else -> Color.Green
        }
        Image(
            painter = painterResource(R.drawable.ic_battery),
            contentDescription = "",
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(canvasColor)

        )

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .alpha(0.4f)
                .padding(start = 2.dp, end = 2.dp, top = 10.dp, bottom = 10.dp)
        ) {
            val fillWidth = size.width * (level / 100f) * 0.8f
            drawRect(
                color = canvasColor,
                size = Size(fillWidth, size.height * 0.7f),
                topLeft = Offset(size.width * 0.05f, size.height * 0.15f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowRobotSettingModal(
    showSettings: Boolean,
    device: BleManagerDevice.BluetoothManagerDevice,
    onDismiss: () -> Unit,
    onShowDialog: (Boolean) -> Unit,
    onShowSettingDialog: (Boolean) -> Unit,
) {
    var showDialog by remember { mutableStateOf(showSettings) }
    if (showDialog) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .wrapContentSize()
                    .height(360.dp)
                    .wrapContentHeight(Alignment.Bottom),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp)
                ) {
                    IconButton(modifier = Modifier.size(70.dp), onClick = {
                        showDialog = false
                        onDismiss()
                        onShowDialog(true)
                    }) {
                        Icon(
                            tint = Color.Black,
                            painter = painterResource(R.drawable.ic_item_rename),
                            contentDescription = "Rename",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }

                    IconButton(modifier = Modifier.size(70.dp), onClick = {
                        showDialog = false
                        onShowSettingDialog(true)
                        onDismiss()
                    }) {
                        Icon(
                            tint = Color.Black,
                            painter = painterResource(R.drawable.ic_item_settings),
                            contentDescription = "Rename",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }

                    IconButton(modifier = Modifier.size(70.dp), onClick = {
                        showDialog = false
                        bleManager.removePersistedDevicesDevice(device)
                        bleManager.removePairedDevicesDevice(device)
                        onDismiss()
                    }) {
                        Icon(
                            tint = Color.Red,
                            painter = painterResource(R.drawable.ic_item_remove),
                            contentDescription = "Rename",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowRenameModal(
    device: BleManagerDevice.BluetoothManagerDevice,
    onDismiss: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    val editText = remember { mutableStateOf(device.robotName.toString()) }

    if (showDialog) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .wrapContentSize()
                    .width(330.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(
                        top = 24.dp,
                        bottom = 24.dp,
                        start = 40.dp,
                        end = 40.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rename",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            modifier = Modifier.height(30.dp)
                        )
                        IconButton(
                            modifier = Modifier.size(18.dp),
                            onClick = {
                                showDialog = false
                                onDismiss()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    TextField(
                        value = editText.value,
                        onValueChange = { editText.value = it },
                        singleLine = true,
                        modifier = Modifier
                            .width(240.dp)
                            .height(70.dp)
                            .padding(top = 20.dp)
                        ,
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color(0x4FC4BEBE),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Button(
                        onClick = {
                            bleManager.updateDeviceName(device, editText.value)
                            showDialog = false
                            onDismiss()
                        },
                        modifier = Modifier
                            .width(240.dp)
                            .height(70.dp)
                            .padding(top = 30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        )
                    ) {
                        Text("OK", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun showDeviceSettingPage(device: BleManagerDevice.BluetoothManagerDevice) {
    val intent = Intent(LocalContext.current, DeviceSettingActivity::class.java).apply {
        putExtra("DEVICE_ADDRESS", device.address)
        putExtra("DEVICE_NAME", device.robotName)
    }
    var bundle = Bundle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        startActivity(context, intent, bundle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotItemTopView(
    initialText: String,
    device: BleManagerDevice.BluetoothManagerDevice,
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(initialText) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSettingPage by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .clickable { isEditing = true }) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .clickable { isEditing = true },
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
            )
        }

        IconButton(
            onClick = {
                isEditing = !isEditing
            },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Setting",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        if (isEditing) {
            ShowRobotSettingModal(
                isEditing,
                device,
                onDismiss = { isEditing = false },
                onShowDialog = { showRenameDialog = it },
                onShowSettingDialog = {showSettingPage = it}
            )
        }

        if (showRenameDialog) {
            device.robotName?.let {
                ShowRenameModal(device = device, onDismiss = {
                    showRenameDialog = false
                })
            }
        }

        if (showSettingPage) {
            showDeviceSettingPage(device)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RobotTheme {
        MainScreen()
    }
}






