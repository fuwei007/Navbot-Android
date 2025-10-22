package com.hyun.robot.ui

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
import androidx.core.content.ContextCompat
import com.hyun.robot.MyApplication
import com.hyun.robot.ui.theme.RobotTheme

class AddDeviceActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        setContent {
            RobotTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var showSplash by remember { mutableStateOf(true) }
                    if (showSplash) {
                        LoadingScreen(onTimeout = { showSplash = false })
                    } else {
                        AddDeviceScreen(this)
                        showSplash = false
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        ContextCompat.startActivity(this, intent, bundle)
        MyApplication.isFirstDeviceDetail = false
        super.onBackPressed()
    }

}


