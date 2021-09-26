package com.yly.learndoze

import android.Manifest
import android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.*
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.Permissions

/**
 * 息屏后操作,doze模式，低消耗模式
 *  adb shell dumpsys deviceidle force-idle
 *
 *  adb shell dumpsys deviceidle unforce
 *
 *  adb shell dumpsys battery reset
 *
 *  待机模式
 *  adb shell dumpsys battery unplug
 *  adb shell am set-inactive com.yly.learndoze true
 *
 *
 * adb shell am set-inactive com.yly.learndoze false
 * adb shell am get-inactive com.yly.learndoze
 *
 * 在以上两种模式下
 *
 * doze阶段
 * 设备会进入低电耗模式并应用第一部分限制：关闭应用网络访问、推迟作业和同步。
 *
 * 待机模式阶段
 * 如果进入低电耗模式后设备处于静止状态达到一定时间，系统则会对 PowerManager.WakeLock、AlarmManager 闹铃、GPS 和 WLAN 扫描应用余下的低电耗模式限制
 *
 *
 * 1：网络异常
 * 2：job不执行,将会调用onStopJob,当onStopJob返回true，解除doze模式会重新调用onStartJob
 * (job里面的子线程不会停止，就算是打开了doze模式，所以只要内存足够，这个是可以一直执行的）
 * 3:handler只要系统没杀死还是可以运行的
 *
 */
class MainActivity : AppCompatActivity() {
    val myHandler = MyHandler()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            println("RequestPermission   $it")
        }
        findViewById<Button>(R.id.network).setOnClickListener {
            myHandler.sendEmptyMessage(100)
        }
        findViewById<Button>(R.id.job).setOnClickListener {
            val jobScheduler: JobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobs = jobScheduler.allPendingJobs
            for (info in jobs) {
                if (info.id == 100) {
                    println("job alive")
                    return@setOnClickListener
                }
            }

            val componentName = ComponentName(this, MyJobService::class.java)
            val jobInfo = JobInfo.Builder(100, componentName)
                .setRequiresCharging(true)
                .setPersisted(true)
                .build()
            jobScheduler.schedule(jobInfo)

//            val componentName = ComponentName(this, MyJobService::class.java)
//            val jobInfo = JobInfo.Builder(100, componentName)
//                .build()
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                jobScheduler.enqueue(jobInfo, JobWorkItem(Intent(this, JobWorkService::class.java)))
//            }

        }

        findViewById<Button>(R.id.alarm).setOnClickListener {
            val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(this, MyAlarmReceiver::class.java).let { intent ->
                PendingIntent.getBroadcast(this, 0, intent, 0)
            }
//时间不准
//            alarmMgr.setRepeating(
//                AlarmManager.RTC_WAKEUP,
//                System.currentTimeMillis() + 5000,
//                1000 * 5,
//                alarmIntent
//            )
            //RTC_WAKEUP会唤醒，待机模式阶段就不行了
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20_000, alarmIntent)
            //AlarmManager.RTC在doze模式不会执行，但是解除后马上执行
//            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
//            println(powerManager.isIgnoringBatteryOptimizations("com.yly.learndoze"))
//            val lock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "yly:test")
//            lock.acquire(100_000)
//            try {
//                alarmMgr.setExact(AlarmManager.RTC, System.currentTimeMillis() + 20_000, alarmIntent)
//            } finally {
////                lock.release()
//            }

        }

        findViewById<Button>(R.id.keepAlive).setOnClickListener {
//            if (checkSelfPermission(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) != PackageManager.PERMISSION_GRANTED) {
//                launcher.launch(REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
//            }
            //加入白名单
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    addCategory(Intent.CATEGORY_DEFAULT)
                })
            }
            val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(this, MyAlarmReceiver::class.java).let { intent ->
                PendingIntent.getBroadcast(this, 0, intent, 0)
            }
            alarmMgr.setExact(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10_000,
                alarmIntent
            )
        }

        findViewById<Button>(R.id.cancelKeepAlive).setOnClickListener {
            val alarmIntent = Intent(this, MyAlarmReceiver::class.java).let { aintent ->
                PendingIntent.getBroadcast(this, 0, aintent, 0)
            }
            val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmMgr.cancel(alarmIntent)
        }

        findViewById<Button>(R.id.handler).setOnClickListener {
            myHandler.sendEmptyMessage(101)
        }
    }


    class MyHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                100 -> {
                    Thread {
                        try {
                            val client = OkHttpClient();
                            val request = Request.Builder()
                                .url("https://www.baidu.com")
                                .build();
                            val response = client.newCall(request).execute()
                            println(response.code)
                        } catch (e: Exception) {
                            //doze状态和待机模式这里会报异常
                            println("doze 异常 ${e.message}")
                        }
                    }.start()
                    sendEmptyMessageDelayed(100, 5000)
                }
                101 -> {
                    println("handler loop 10 secs")
                    sendEmptyMessageDelayed(101, 10_000)
                }
            }
        }
    }
}