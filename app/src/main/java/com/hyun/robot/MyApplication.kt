package com.hyun.robot

import android.app.Application
import com.hyun.robot.utils.CrashHandler
import com.hyun.robot.utils.RobotBleManager
import com.hyun.robot.utils.WifiAutoConnector

class MyApplication: Application() {
    companion object{
        lateinit var bleManager: RobotBleManager
        lateinit var wifiAutoConnector: WifiAutoConnector
        var isFirst = true
        var isFirstDeviceDetail = true

    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.getInstance().init(this)
        bleManager = RobotBleManager(applicationContext)
        wifiAutoConnector = WifiAutoConnector(applicationContext)
    }


}