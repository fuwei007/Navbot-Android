package com.hyun.robot.ui

import android.bluetooth.BluetoothDevice

class DeviceState {
   var isToggleOn :Boolean = false
    var bDevice: BluetoothDevice? = null
    var showLoading :Boolean = true
}


class DeviceInfo {
    var deviceID :String = ""
    var deviceAddress: String = ""
    var cloudToken :String = ""
    var openAIToken: String = ""
}