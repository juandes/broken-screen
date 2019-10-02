package com.example.brokenscreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View

var MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT = 1

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // First, check if the storage permission has been granted
        if (ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Here, the user has not granted the permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Why? :(

            } else {
                // Here we are requesting both permissions even if we're only checking for the storage one.
                // Don't do this in production!
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                    MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_EXPORT)

                // Permission granted :D!
            }
        }
    }

    fun enterCamera(v: View) {
        Intent(this, CameraActivity::class.java).also { intent ->
            startActivity(intent)
        }
    }
}
