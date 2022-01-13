package com.boo.sample.sampledownloadmanager

import android.accounts.AccountManager.VISIBILITY_VISIBLE
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.boo.sample.sampledownloadmanager.databinding.ActivityMainBinding
import java.io.File
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    val TAG = "로그"
    var mDownloadManager: DownloadManager? = null

    val PERMISSIONS_REQUEST_STORAGE = 9

    private var mDownloadQueueId: Long? = null
    private var outputFilePath: String = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS
    ).toString() + "/downloadmanager" + "/example1.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            URLDownloading(Uri.parse(binding.editText.text.toString()))
        }
        val permissionCheck =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_STORAGE
            )
        } else {
            binding.button.setOnClickListener {
                val url = binding.editText.text.toString().toUri()
                Log.d(TAG, "url은 "+url.toString())
                URLDownloading(url)
            }

        }
    }

    private fun URLDownloading(url: Uri) {
        if (mDownloadManager == null) {
            mDownloadManager =
                baseContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        }

        var outputFile = File(outputFilePath)
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        var downloadUri = url
        val request: DownloadManager.Request = DownloadManager.Request(downloadUri)
        Log.d(TAG, "request = ${request}")
        val pathSegmentList: List<String> = downloadUri.pathSegments
        request.setTitle("다운로드 항목")
        request.setDestinationUri(Uri.fromFile(outputFile))
        request.setAllowedOverMetered(true)
        Log.d(TAG, "다운로드 경로는 ${outputFile.toString()}")

        mDownloadQueueId = mDownloadManager!!.enqueue(request)
    }

    private var downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var reference: Long? = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Log.d(TAG, "reference = $reference")
            if (mDownloadQueueId == reference) {
                Log.d(TAG, "mDownloadQueueId == reference")
                var query = DownloadManager.Query()
                if (reference != null) {
                    query.setFilterById(reference)
                } else {
                    Log.d(TAG, "reference가 널이다")
                }
                val cursor = mDownloadManager?.query(query)
                Log.d(TAG, "cursor = ${cursor}")

                cursor?.moveToFirst() ?: Log.d(TAG, "cursor가 널입니다")

                val columnIndex = cursor?.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val columnReason = cursor?.getColumnIndex(DownloadManager.COLUMN_REASON)
                Log.d(TAG, "columnIndex = $columnIndex , columnReason = $columnReason")

                val status = cursor?.getInt(columnIndex!!)
                val reason = cursor?.getString(columnReason!!)

                cursor?.close()

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> Toast.makeText(
                        baseContext,
                        "다운로드 완료하였습니다",
                        Toast.LENGTH_LONG
                    ).show()
                    DownloadManager.STATUS_PAUSED -> Toast.makeText(
                        baseContext,
                        "다운로드 중단하였습니다",
                        Toast.LENGTH_LONG
                    ).show()
                    DownloadManager.STATUS_FAILED -> {
                        Toast.makeText(
                            baseContext,
                            "다운로드 실패하였습니다, status = $status, reason = $reason",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "다운로드 실패하였습니다, status = $status, reason = $reason")
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val completeFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadCompleteReceiver, completeFilter)
        Log.d(TAG, "registerReceiver 실행")
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(downloadCompleteReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_STORAGE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.button.setOnClickListener {
                        val url = binding.editText.text.toString().toUri()
                        URLDownloading(url)
                    }
                }else {
                    finish()
                }
            }
        }
    }
}