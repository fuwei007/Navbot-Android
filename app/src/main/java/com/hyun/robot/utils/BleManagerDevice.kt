package com.hyun.robot.utils

class BleManagerDevice {
    var name: String? = null
    var robotName: String? = null
    var connState: Int? = 1
    var device: BluetoothManagerDevice? = null
    var address: String? = null
    var bluetoothDevice: BluetoothManagerDevice? =null

    class BluetoothManagerDevice{
        var robotName: String? = ""
        var name: String? = ""
        var address: String? = ""
        var connState: Int? = 1

    }

}