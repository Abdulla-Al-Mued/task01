package com.example.task01

import DownloadWorker
import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.task01.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

//    private lateinit var viewModel: FileDownloadViewModel
    private var notificationId = 1
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationBuilder : NotificationCompat.Builder
    private lateinit var notificationManager : NotificationManagerCompat
    private var currentProgress = 0
    val REQUEST_CODE_PERMISSIONS = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()



        binding.downloadBtn.setOnClickListener {
            val url = "https://file-examples.com/storage/fe1134defc6538ed39b8efa/2017/04/file_example_MP4_1920_18MG.mp4"
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

                                Toast.makeText(applicationContext, exceptionMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })

            Toast.makeText(applicationContext, "download started", Toast.LENGTH_SHORT).show()

            WorkManager.getInstance(this).enqueue(downloadRequest)


        }


    }

    private fun checkAndRequestPermissions(activity: Activity, vararg permissions: String): Boolean {
        val permissionsList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            val permissionState = activity.checkSelfPermission(permission)
            if (permissionState == PackageManager.PERMISSION_DENIED) {
                permissionsList.add(permission)
            }
        }
        if (!permissionsList.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsList.toTypedArray<String>(),
                REQUEST_CODE_PERMISSIONS
            )
            return false
        }
        return true
    }
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestPermissions(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("progress_update")
        registerReceiver(progressReceiver, filter)
    }
    override fun onPause() {
        super.onPause()

        notificationBuilder = NotificationCompat.Builder(this, "download_channel")
        notificationManager = NotificationManagerCompat.from(this)

        showNotification(currentProgress)

    }
    private fun updateDownloadProgress(progress: Int) {
        Log.d("Progress", "$progress%")
        binding.progressBar.progress = progress
        binding.progress.text = progress.toString()+"%"

        if (progress == 100){
            binding.progress.text = "Download Complete"
        }


    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra("progress", 0)
            if (progress != null) {
                updateDownloadProgress(progress)


                currentProgress = progress
            }
        }
    }

    private fun showNotification(progress: Int) {

        notificationBuilder.setContentTitle("Download in Progress")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentText("Downloading... $currentProgress%")
            .setProgress(100, currentProgress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        val notificationId = 1


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(notificationId, notificationBuilder.build())

    }

}