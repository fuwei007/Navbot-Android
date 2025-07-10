package com.hyun.robot.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.hyun.robot.MyApplication

class DeviceDetailActivity : BaseActivity() {
    private lateinit var context: Context
    var deviceAddress = ""
    var deviceName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS").toString() ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME").toString() ?: ""

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var hasPermissions by remember { mutableStateOf(false) }
                PermissionHandler(
                    onPermissionsGranted = { hasPermissions = true },
                    onPermissionsDenied = { }
                )
                ShowDetailScreen(this)
            }
        }
    }
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        ContextCompat.startActivity(this, intent, bundle)
        super.onBackPressed()
    }


}



