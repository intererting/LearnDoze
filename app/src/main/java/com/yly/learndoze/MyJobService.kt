package com.yly.learndoze

import android.app.job.JobParameters
import android.app.job.JobService
import android.app.job.JobWorkItem
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author    yiliyang
 * @date      2021/9/24 上午11:26
 * @version   1.0
 * @since     1.0
 */
class MyJobService : JobService() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartJob(params: JobParameters): Boolean {
        println("onStartJob $this ${Thread.currentThread()}")

        //如果 params.dequeueWork()返回null，JobServie会被自动关闭
        var workItem: JobWorkItem? = params.dequeueWork()
        while (null != workItem) {
            workItem.intent.getStringExtra("haha")
            params.completeWork(workItem)
            workItem = params.dequeueWork()
        }


        //主线程
//        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
////                jobFinished(params, false)
//            println("job continue")
//        }, 0, 5, TimeUnit.SECONDS)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        println("onStopJob")
        return false
//        return true
    }
}