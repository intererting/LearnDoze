package com.yly.learndoze

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.getSystemService

/**
 * @author    yiliyang
 * @date      21-2-26 下午3:09
 * @version   1.0
 * @since     1.0
 */
class MyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        //关机后重启会失效，所以必须在开机后重新启动
        println("MyAlarmReceiver onReceive $context")
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, MyAlarmReceiver::class.java).let { aintent ->
            PendingIntent.getBroadcast(context, 0, aintent, 0)
        }
        alarmMgr.setExact(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10_000,
            alarmIntent
        )
    }
}