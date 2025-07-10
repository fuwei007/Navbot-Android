package com.hyun.robot.utils


class BleCommand {
    companion object {
        const val BASE_HEIGHT = 0X20.toByte()
        private const val HEADER_1 = 0x55.toByte()
        private const val HEADER_2 = 0xAA.toByte()
        const val EMPTY_BYTE = 0x00.toByte()
        const val BYTE_01 = 0x01.toByte()
        private const val MODE = BYTE_01
        private const val STABLE_OFF = EMPTY_BYTE
        private const val STABLE_ON = BYTE_01

        @JvmStatic
        fun intToUnsignedByte(value: Int): Byte {
            return (value and 0xFF).toByte()
        }

        @JvmStatic
        fun getBleCommand(
            roll: Byte = EMPTY_BYTE,
            height: Byte = EMPTY_BYTE,
            linearH: Byte = EMPTY_BYTE,
            linearW: Byte = EMPTY_BYTE,
            angular: Byte = EMPTY_BYTE,
            stable: Byte = STABLE_ON,
            dir: Byte = EMPTY_BYTE,
            joyY: Byte = EMPTY_BYTE,
            joyX: Byte = EMPTY_BYTE
        ): ByteArray {
            return byteArrayOf(
                HEADER_1, // 55
                HEADER_2, //AA
                EMPTY_BYTE,
                EMPTY_BYTE,
                EMPTY_BYTE,
                roll,//Rolling Angle, value range -30 to 30.
                height,//The height of the robot, with a value range of 32 to 85.
                linearH,//Linear speed, with a value range of -200 to 200.
                linearW,//Linear speed, with a value range of -200 to 200.
                angular,//The angular velocity of the robot, with a value range of -100 to 100
                stable,//The stable state of the robot: 1 on, 0 off
                MODE,
                dir,//The movement direction of the robot is defaulted to 0, jump:1
                joyY,//The Y-axis data of the joystick, with a value range of -100 to 100.
                joyX,//The X-axis data of the joystick, with a value range of -100 to 100.
                EMPTY_BYTE,
                EMPTY_BYTE,
                EMPTY_BYTE,
                EMPTY_BYTE,
                EMPTY_BYTE
            )
        }

        @JvmStatic
        fun getManeuverBleCommand(
            roll: Byte = EMPTY_BYTE,
            height: Byte = 0x26,
            maneuver: Byte = EMPTY_BYTE,
            linearH: Byte = EMPTY_BYTE,
            linearW: Byte = EMPTY_BYTE,
            angular: Byte = EMPTY_BYTE,
            stable: Byte = STABLE_ON,
            dir: Byte = EMPTY_BYTE,
            joyY: Byte = EMPTY_BYTE,
            joyX: Byte = EMPTY_BYTE
        ): ByteArray {
            return byteArrayOf(
                HEADER_1, // 55
                HEADER_2, //AA
                maneuver,
                EMPTY_BYTE,
                EMPTY_BYTE,
                roll,//Rolling Angle, value range -30 to 30.
                height,//The height of the robot, with a value range of 32 to 85.
                linearH,//Linear speed, with a value range of -200 to 200.
                linearW,//Linear speed, with a value range of -200 to 200.
                angular,//The angular velocity of the robot, with a value range of -100 to 100
                stable,//The stable state of the robot: 1 on, 0 off
                MODE,
                dir,//The movement direction of the robot is defaulted to 0, jump:1
                joyY,//The Y-axis data of the joystick, with a value range of -100 to 100.
                joyX,//The X-axis data of the joystick, with a value range of -100 to 100.
                EMPTY_BYTE,
                EMPTY_BYTE,
                EMPTY_BYTE,
                EMPTY_BYTE,
                EMPTY_BYTE
            )
        }


        @JvmStatic
        fun getWifiBleCommand(
            ssid:String,
            password:String,
        ): ByteArray {
            val ssidArr = hexStringToByteArray(ssid)
            val passwordArr = hexStringToByteArray(password)
            val header = byteArrayOf(
                0x55.toByte(),   // HEADER_1
                0xAA.toByte(),   // HEADER_2
                0x01.toByte(),   // BYTE_01
                0x00.toByte(),   // EMPTY_BYTE
                0x00.toByte()    // EMPTY_BYTE
            )
            return header + ssidArr!! + passwordArr!!
        }

        private fun hexStringToByteArray(hex: String): ByteArray? {
            val bytes = ByteArray(hex.length / 2)
            for (i in bytes.indices) {
                bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            return bytes
        }
    }
}