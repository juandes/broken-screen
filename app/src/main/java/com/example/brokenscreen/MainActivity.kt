package com.example.brokenscreen

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

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
