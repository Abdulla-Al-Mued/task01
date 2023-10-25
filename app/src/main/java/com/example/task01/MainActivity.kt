package com.example.task01

import DownloadWorker
import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.task01.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

//    private lateinit var viewModel: FileDownloadViewModel
    private var notificationId = 1
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationBuilder : NotificationCompat.Builder
    private lateinit var notificationManager : NotificationManagerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationBuilder = NotificationCompat.Builder(this, "download_channel")
        notificationManager = NotificationManagerCompat.from(this)

        //val downloadButton = findViewById<Button>(R.id.downloadBtn)

//        viewModel = ViewModelProvider(this)[FileDownloadViewModel::class.java]

//        viewModel.downloadProgress.observe(this) { progress ->
//            //updateNotificationProgress(progress)
//            Log.d("NOTIFICATION", " $progress")
//        }


//        viewModel.downloadProgress.observe(
//            this
//        ) {
//            Log.d("NOTIFICATION", it.toString())
//        }
//
//        viewModel.updateProgress(0)


        binding.downloadBtn.setOnClickListener {
            val url = "https://file-examples.com/storage/fee82f638f6538df295a1ac/2017/04/file_example_MP4_1920_18MG.mp4"
            val workData = Data.Builder().putString("url", url).build()

            val downloadRequest = OneTimeWorkRequest.Builder(DownloadWorker::class.java)
                .setInputData(workData)
                .build()


            WorkManager.getInstance(this).getWorkInfoByIdLiveData(downloadRequest.id)
                .observe(this, Observer { workInfo ->
                    if (workInfo != null) {
                        if (workInfo.state == WorkInfo.State.FAILED) {
                            val outputData = workInfo.outputData
                            val exceptionMessage = outputData.getString("exceptionMessage")
                            if (exceptionMessage != null) {
                                // Handle the exception message, e.g., show a toast with the error
                                Toast.makeText(applicationContext, exceptionMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })

            Toast.makeText(applicationContext, "download started", Toast.LENGTH_SHORT).show()

            WorkManager.getInstance(this).enqueue(downloadRequest)


        }


    }


    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("progress_update")
        registerReceiver(progressReceiver, filter)
    }



    override fun onPause() {
        super.onPause()
//        notificationBuilder.setContentTitle("Download in Progress")
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        //showNotification(progressReceiver.resultData.length)
        //unregisterReceiver(progressReceiver)
        notificationBuilder = NotificationCompat.Builder(this, "download_channel")
        notificationManager = NotificationManagerCompat.from(this)

        notificationBuilder.setContentTitle("Download in Progress")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
        val notificationId = 1


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(notificationId, notificationBuilder.build())

    }

    private fun updateDownloadProgress(progress: Int) {
        Log.d("Progress", "$progress%")
        binding.progressBar.progress = progress
        binding.progress.text = progress.toString()+"%"

        if (progress == 100){
            binding.progress.text = "Download Complete"
        }

        notificationBuilder
            .setProgress(100, progress, false)
            .setContentText("Downloading... $progress%")

    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra("progress", 0)
            if (progress != null) {
                updateDownloadProgress(progress)

                showNotification(progress)
            }
        }
    }

    private fun showNotification(progress: Int) {
        notificationBuilder
            .setProgress(100, progress, false)
            .setContentText("Downloading... $progress%")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)


        // Use a unique notification ID to ensure each notification is displayed
    }

}