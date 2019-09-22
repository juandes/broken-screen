package com.example.brokenscreen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import com.example.brokenscreen.CameraActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun enterCamera(v: View) {
        Intent(this, CameraActivity::class.java).also { intent ->
            startActivity(intent)
        }
    }
}
