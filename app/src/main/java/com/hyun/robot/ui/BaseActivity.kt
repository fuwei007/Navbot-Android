package com.hyun.robot.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hyun.robot.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

open class BaseActivity : ComponentActivity(){


    @Composable
    fun LoadingScreen(onTimeout: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(id = R.drawable.ic_loading),
                    contentScale = ContentScale.FillBounds
                ),
            contentAlignment = Alignment.Center
        ) {
        }

        LaunchedEffect(Unit) {
            delay(100)
            onTimeout()
        }

    }

    fun getStringResource(context: Context, resId: Int): String {
        return context.resources.getString(resId)
    }

    @Composable
    fun LoadingConnectingScreen(
        isConnecting: Boolean,
        loadingText :String,
        onTimeout: () -> Unit
    ) {
        var dotCount by remember { mutableStateOf(1) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable { }
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isConnecting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = loadingText,
                        fontSize = 16.sp,
                        color = Color.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    repeat(dotCount) {
                        Text(
                            text = ".",
                            color = Color.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            LaunchedEffect(Unit) {
                while (isConnecting) {
                    delay(500)
                    dotCount = if (dotCount < 3) dotCount + 1 else 1
                }
            }

        }
    }

    class KalmanFilterProcessor {
        private var lastEstimate = Offset.Zero
        private var errorCovariance = Offset(1f, 1f)

        fun process(measurement: Offset): Offset {
            val predictedEstimate = lastEstimate
            val predictedErrorX = errorCovariance.x + 0.1f
            val predictedErrorY = errorCovariance.y + 0.1f
            val kalmanGainX = predictedErrorX / (predictedErrorX + 0.5f)
            val kalmanGainY = predictedErrorY / (predictedErrorY + 0.5f)
            val delta = measurement - predictedEstimate
            val newEstimateX = predictedEstimate.x + kalmanGainX * delta.x
            val newEstimateY = predictedEstimate.y + kalmanGainY * delta.y
            lastEstimate = Offset(newEstimateX, newEstimateY)
            val newErrorCovarianceX = (1f - kalmanGainX) * predictedErrorX
            val newErrorCovarianceY = (1f - kalmanGainY) * predictedErrorY
            errorCovariance = Offset(newErrorCovarianceX, newErrorCovarianceY)

            return lastEstimate
        }
    }

    @Composable
    fun BackToHome() {
        val intent = Intent(LocalContext.current, MainActivity::class.java)
        val bundle = Bundle()
        ContextCompat.startActivity(LocalContext.current, intent, bundle)
        finish()
    }

    fun Offset.applyDeadZone(threshold: Float = 2f): Offset {
        val distance = getDistance()
        return when {
            distance < threshold -> Offset.Zero
            distance < threshold * 2 -> this * 0.5f
            else -> this
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

  // wifi
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("LaunchDuringComposition")
    @Composable
    fun WifiListDialog(
        onDismiss: () -> Unit,
        onConnect: (ScanResult) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        var wifiList by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var showWifiDisabledWarning by remember { mutableStateOf(false) }
        var showPermissionWarning by remember { mutableStateOf(false) }
        val wifiManager = remember { context.getSystemService(WIFI_SERVICE) as WifiManager }
        var showPasswordAlert by remember { mutableStateOf(false) }
        var wifiPassword by remember { mutableStateOf("") }
        val wifiSsid by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        var lastScanTime by remember { mutableStateOf(0L) }
        fun canScanWifi(): Boolean {
            return (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) && wifiManager.isWifiEnabled
        }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startWifiScan(wifiManager, scope, context, ::canScanWifi,
                    { isLoading = true }) { showPermissionWarning = false }
            } else {
                showPermissionWarning = true
            }
        }

        fun startWifiScan() {
            if (!canScanWifi()) {
                if (!wifiManager.isWifiEnabled) {
                    showWifiDisabledWarning = true
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                return
            }

            scope.launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main) { isLoading = true }
                    val success = wifiManager.startScan()
                    if (!success) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Scan startup failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SecurityException) {
                    withContext(Dispatchers.Main) {
                        showPermissionWarning = true
                    }
                }
            }
        }

        val wifiReceiver = remember {
            object : BroadcastReceiver() {
                @SuppressLint("MissingPermission")
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                            wifiList = wifiManager.scanResults
                                ?.filter { !it.SSID.isNullOrBlank() }
                                ?.sortedByDescending { it.level }
                                ?: emptyList()
                            isLoading = false
                            lastScanTime = System.currentTimeMillis()
                        }

                        WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                            when (intent.getIntExtra(
                                WifiManager.EXTRA_WIFI_STATE,
                                WifiManager.WIFI_STATE_UNKNOWN
                            )) {
                                WifiManager.WIFI_STATE_ENABLED -> {
                                    showWifiDisabledWarning = false
                                    startWifiScan()
                                }
                            }
                        }
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            val filter = IntentFilter().apply {
                addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            context.registerReceiver(wifiReceiver, filter)
            onDispose { context.unregisterReceiver(wifiReceiver) }
        }

        LaunchedEffect(Unit) {
            startWifiScan()
        }

        XXPermissions.with(context)
            .permission(Permission.ACCESS_FINE_LOCATION)
            .permission(Permission.ACCESS_COARSE_LOCATION)
            .permission(Permission.NEARBY_WIFI_DEVICES)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    if (!allGranted) {
                        showWifiDisabledWarning = false
                        return
                    }
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        showWifiDisabledWarning = true
                        XXPermissions.startPermissionActivity(context, permissions)
                    }
                }
            })

        if (showPasswordAlert) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(wifiSsid) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        Text(
                            "Connect Wifi Password",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPasswordAlert
                        },
                    ) {
                        Text(
                            "Connect", fontSize = 14.sp,
                        )
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showPasswordAlert = false
                            wifiPassword = ""
                        }
                    ) {
                        Text("Close", fontSize = 14.sp)
                    }
                },
                modifier = modifier
            )
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("WLAN") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        if (wifiList.isEmpty()) {
                            Text(
                                "No Wifi found",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            LazyColumn {
                                items(wifiList) { result ->
                                    WifiListItem(
                                        context = context,
                                        result = result,
                                        onClick = {
                                            onConnectWifiToBlue(result)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canScanWifi()) {
                            if (System.currentTimeMillis() - lastScanTime > 15000) {
                                isLoading = true
                                wifiManager.startScan()
                                lastScanTime = System.currentTimeMillis()
                            }
                        } else {
                            showPermissionWarning = true
                        }
                    },
                    enabled = wifiManager.isWifiEnabled
                ) {
                    Text(
                        "Scan", fontSize = 14.sp,
                    )
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Close", fontSize = 14.sp)
                }
            },
            modifier = modifier
        )
    }

    fun onConnectWifiToBlue(result: ScanResult) {
    }

    private fun startWifiScan(
        wifiManager: WifiManager,
        scope: CoroutineScope,
        context: Context,
        canScanCheck: () -> Boolean,
        onScanStart: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (!canScanCheck()) {
            onPermissionDenied()
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onScanStart() }
                val success = wifiManager.startScan()
                if (!success) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Scan startup failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    onPermissionDenied()
                }
            }
        }
    }

    fun getConnectedWifiBssid(context: Context): String? {
        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
        return wifiManager.connectionInfo?.bssid?.takeIf { it != "02:00:00:00:00:00" }
    }

    @Composable
    fun WifiListItem(
        context: Context,
        result: ScanResult,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val (signalStrength, securityType) = remember(result) {
            Pair(
                when {
                    result.level > -50 -> 4
                    result.level > -60 -> 3
                    result.level > -70 -> 2
                    else -> 1
                },
                when {
                    result.capabilities.contains("WPA2") -> "WPA2"
                    result.capabilities.contains("WPA") -> "WPA"
                    result.capabilities.contains("WEP") -> "WEP"
                    else -> "Open"
                }
            )
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = when (signalStrength) {
                            4 -> painterResource(R.drawable.wifi_4)
                            3 -> painterResource(R.drawable.wifi_3)
                            2 -> painterResource(R.drawable.wifi_2)
                            else -> painterResource(R.drawable.wifi_1)
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = when (signalStrength) {
                            4 -> Color.Green
                            3 -> Color(0xFF4CAF50)
                            2 -> Color(0xFFFFC107)
                            else -> Color.Red
                        }
                    )
                    Text(
                        text = "${abs(result.level)}dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.SSID ?: "Hide Wifi",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = securityType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (result.BSSID == getConnectedWifiBssid(context)) {
                            Text(
                                text = "Connected",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }


}