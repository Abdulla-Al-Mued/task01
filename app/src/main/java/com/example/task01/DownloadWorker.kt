import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {


    override fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()


        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "downloaded_video.mp4"
        val outputFile = File(directory, fileName)

        try {

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            val inputStream: InputStream = connection.inputStream
            val fileOutput = FileOutputStream(outputFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalBytes = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOutput.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
                val progress = (totalBytes * 100 / connection.contentLength).toInt()
                //viewModel.updateProgress(progress)

                sendProgressUpdate(progress)

            }

            fileOutput.close()
            inputStream.close()

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            val exceptionMessage = e.message ?: "An unknown error occurred during download."
            val outputData = Data.Builder().putString("exceptionMessage", exceptionMessage).build()
            return Result.failure(outputData)
        }
    }

    private fun sendProgressUpdate(progress: Int) {
        val intent = Intent("progress_update")
        intent.putExtra("progress", progress)
        applicationContext.sendBroadcast(intent)
    }


}