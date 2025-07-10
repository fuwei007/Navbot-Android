package com.hyun.robot.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.util.Date

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    private var defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    companion object{
        lateinit var mContext: Context
        @Volatile
        private var instance: CrashHandler? = null

        @Synchronized
        fun getInstance(): CrashHandler {
            return instance ?: CrashHandler().also { instance = it }
        }

    }


    fun init(context: Context) {
        Thread.setDefaultUncaughtExceptionHandler(this)
        mContext = context.applicationContext
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val crashInfo = collectCrashInfo(ex)

        saveCrashLog(crashInfo)

//        uploadCrashLog(crashInfo)

        defaultHandler?.uncaughtException(thread, ex)

//        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun collectCrashInfo(ex: Throwable): String {
        return buildString {
            append("Brand: ${Build.BRAND}\n")
            append("Model: ${Build.MODEL}\n")
            append("Android SDK: ${Build.VERSION.SDK_INT}\n")
            append("Exception: ${ex.javaClass.name}\n")
            append("Message: ${ex.message}\n")
            append("Stack Trace:\n${Log.getStackTraceString(ex)}")
        }
    }

    private fun saveCrashLog(content: String) {
        try {
            val file = File(mContext.getExternalFilesDir(null), "navbot_01_crash.log")
            FileWriter(file, true).use { it.write("${Date()}\n$content\n\n") }
        } catch (e: Exception) {
        }
    }

}
