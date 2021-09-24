package com.yly.learndoze

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @author    yiliyang
 * @date      2021/9/24 下午2:55
 * @version   1.0
 * @since     1.0
 */
class JobWorkService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("JobWorkService onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        println(println("JobWorkService onDestroy"))
        super.onDestroy()
    }
}