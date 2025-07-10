@file:Suppress("DEPRECATION", "PreviewAnnotationInFunctionWithParameters")

package com.hyun.robot.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ficat.easyble.BleDevice
import com.ficat.easyble.gatt.callback.BleConnectCallback
import com.hyun.robot.MyApplication
import com.hyun.robot.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private var connectCallback: BleConnectCallback? = null
private var showLoading = true
private var bDevice: BluetoothDevice? = null

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
@Preview(
    showBackground = true, showSystemUi = true, device = "spec:parent=pixel_5,orientation=landscape"
)
fun DeviceSettingScreen(activity: DeviceSettingActivity = DeviceSettingActivity()) {
    val image = painterResource(R.drawable.bg_robot_list)
    var isConnectDevice by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Connecting" ?: "") }
//    var deviceName by remember { mutableStateOf(activity.deviceName ?: "") }
    var isClient by remember { mutableStateOf(true) }
    var wifiName  by remember { mutableStateOf("Wifi-10001") }
    var password  by remember { mutableStateOf("test") }
    var isShowWifiDialog  by remember { mutableStateOf(true) }
    var deviceName by remember { mutableStateOf("NavBot-EN01") }
    var deviceID by remember { mutableStateOf("xxxxxxxxxxx") }
    var deviceInfo by remember { mutableStateOf(DeviceInfo())}
    deviceInfo.deviceID ="xxxxxxxxxxxx"
    deviceInfo.deviceAddress ="xxxx.xxxx.xxxx.xxxx"
    deviceInfo.cloudToken ="xxxx-xxxx-xxxxx-xxxx"
    deviceInfo.openAIToken ="xxxxxxx-xxxxxxxx-xxxxxxxxx"
    var isDeviceSettingScreenVisible by remember { mutableStateOf(false) }

    fun showToast(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    connectCallback = object : BleConnectCallback {
        override fun onFailure(
            failureCode: Int, info: String?, device: BleDevice?
        ) {
            showLoading = false
            MyApplication.bleManager.connectDev = null
        }

        override fun onStart(
            startSuccess: Boolean, info: String?, device: BleDevice?
        ) {
            loadingText = "Connecting"
        }

        override fun onConnected(device: BleDevice?) {
            showLoading = false
            if (device != null) {
                isConnectDevice = true
                bDevice = device.bluetoothDevice
                MyApplication.bleManager.connectingDevice = bDevice
                MyApplication.bleManager.connectDev = device
                MyApplication.bleManager.connectedDevices.remove(device)
                MyApplication.bleManager.connectedDevices.add(device)
            }
        }

        override fun onDisconnected(
            info: String?, status: Int, device: BleDevice?
        ) {
            isConnectDevice = false
        }
    }
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

        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent, topBar = {
            TopAppBar(colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ), title = {
                Text(
                    "Settings", fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Left,
                    fontSize = 24.sp,
                )
            }, actions = {
//                IconButton(onClick = {
//                    isDeviceSettingScreenVisible = true
//                }) {
//                    Icon(
//                        imageVector = Icons.Default.Close,
//                        contentDescription = "Close",
//                    )
//                }
            })
        }) { padding ->
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_robot_item),
                                contentDescription = "Device Image",
                                modifier = Modifier.size(165.dp),
                                contentScale = ContentScale.Fit,
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = 24.dp,),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = deviceName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp
                                )
                                IconButton(onClick = { },modifier = Modifier.size(22.dp),) {
                                    Icon(
                                        modifier = Modifier.size(14.dp),
                                        painter = painterResource(id = R.drawable.ic_edit),
                                        contentDescription = "",
                                        tint = Color.LightGray
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
//                                .height(16.dp)
                            ,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Device ID:",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color(0xFF949799)
                            )
                            Text(
                                text = deviceID,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color(0xFF949799)
                            )
                        }
                    }


                    Box(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
//                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                            .wrapContentHeight(Alignment.Bottom),
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(Color.White),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    modifier = Modifier
                                        .offset(y = 18.dp)
                                        .weight(0.5f),
                                    text = "Wifi Mode",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp
                                )

                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .background(Color.White)
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xF000000)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .padding(start = 4.dp, end = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(
                                            contentPadding = PaddingValues(4.dp),
                                            onClick = { isClient = false },
                                            modifier = Modifier
                                                .height(38.dp)
                                                .padding(top = 4.dp, bottom = 4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (!isClient) Color.White else Color.Transparent,
                                            )
                                        ) {
                                            Text(
                                                text = "Server",
                                                fontSize = 14.sp,
                                                color = if (!isClient) Color.Black else Color(
                                                    0xff949799
                                                ),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .height(16.dp)
                                                    .width(54.dp)
                                                    .offset(x = 8.dp)
                                            )
                                        }
                                        Button(
                                            contentPadding = PaddingValues(4.dp),
                                            onClick = { isClient = true },
                                            modifier = Modifier
                                                .height(38.dp)
                                                .padding(top = 4.dp, bottom = 4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isClient) Color.White else Color.Transparent,
                                            )
                                        ) {
                                            Text(
                                                text = "Client",
                                                fontSize = 14.sp,
                                                color = if (isClient) Color.Black else Color(
                                                    0xff949799
                                                ),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .height(16.dp)
                                                    .width(54.dp)
                                                    .offset(x = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (!isClient) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        textStyle = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 17.sp
                                        ),
                                        value = deviceInfo.deviceAddress,
                                        readOnly = true,
                                        onValueChange = { deviceInfo.deviceAddress = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .offset(y = 30.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0x34A1A3A8),
                                            unfocusedBorderColor = Color(0x34A1A3A8),
                                        )
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .offset(y = 30.dp)
                                        .wrapContentHeight(Alignment.Top)
                                        .border(
                                            width = 1.dp,
                                            color = Color(0x34A1A3A8),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, start = 14.dp, end = 14.dp)
                                                .clickable {
                                                    isShowWifiDialog = true
                                                }
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(0.3f),
                                                text = "Wifi Name",
                                                fontSize = 15.sp,
                                                color = Color(0xFF000000)
                                            )
                                            Row(
                                                modifier = Modifier.weight(0.7f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = wifiName,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF000000),
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_next),
                                                    contentDescription = "next",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 14.dp, start = 14.dp, end = 14.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(0.3f),
                                                text = "Password",
                                                fontSize = 15.sp,
                                                color = Color(0xFF000000)
                                            )
                                            Row(
                                                modifier = Modifier.weight(0.7f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = password,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF000000),
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_next),
                                                    contentDescription = "next",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 14.dp, start = 14.dp, end = 14.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(0.3f),
                                                text = "Cloud Token",
                                                fontSize = 15.sp,
                                                color = Color(0xFF000000)
                                            )
                                            Row(
                                                modifier = Modifier.weight(0.7f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = deviceInfo.cloudToken,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF000000),
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_next),
                                                    contentDescription = "next",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = 14.dp,
                                                    start = 14.dp,
                                                    end = 14.dp,
                                                    bottom = 14.dp
                                                )
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(0.3f),
                                                text = "OpenAI Token",
                                                fontSize = 15.sp,
                                                color = Color(0xFF000000)
                                            )
                                            Row(
                                                modifier = Modifier.weight(0.7f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = deviceInfo.openAIToken,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF000000),
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_next),
                                                    contentDescription = "next",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = 16.dp,
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    bottom = 16.dp
                                                )
                                        ) {
                                                    Button(
                                                        shape = RoundedCornerShape(12.dp),
                                                        onClick = {

                                                        },
                                                        modifier = Modifier
                                                            .width(240.dp)
                                                            .height(40.dp),
//                                                            .padding(top = 30.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF007AFF)
                                                        )
                                                    ) {
                                                        Text(
                                                            "OK",
                                                            fontSize = 14.sp,
                                                            color = Color.White
                                                        )
                                                    }
                                                }

                                    }
                                }

                            }
                        }
                    }
                }
            }

            //show wifi dialog
            if(isShowWifiDialog){
//                activity.WifiListDialog(onDismiss = { isShowWifiDialog = false }, onConnect = {})
            }

            if (isDeviceSettingScreenVisible) {
                activity.BackToHome()
            }
        }
    }
}



