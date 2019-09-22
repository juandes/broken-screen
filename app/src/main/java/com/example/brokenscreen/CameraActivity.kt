package com.example.brokenscreen

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.example.brokenscreen.CameraPreview
import okhttp3.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.lang.StringBuilder


class CameraActivity : AppCompatActivity() {
    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private var TAG = "CameraActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Create an instance of Camera
        mCamera = getCameraInstance()

        val params = mCamera?.parameters
        if (params?.supportedFocusModes!!.contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            )
        ) {
            params?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        mCamera?.setDisplayOrientation(90)
        mCamera?.parameters = params

        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(this, it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            val preview: FrameLayout = findViewById(R.id.camera_preview)
            preview.addView(it)
        }


        val mPicture = Camera.PictureCallback { data, _ ->
            val builder = AlertDialog.Builder(this!!)
            val dialog = builder.create()

            runOnUiThread {
                //builder.setMessage("Predicting...") //R.string.dialog_fire_missiles
                // Create the AlertDialog object and return it
                dialog.setMessage("Predicting...")
                dialog.show()
            }

            Log.d("CameraActivity", "Picture Taken")
            val pictureFile: File = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
                Log.d("CameraActivity", ("Error creating media file, check storage permissions"))
                return@PictureCallback
            }

            try {
                val fos = FileOutputStream(pictureFile)
                fos.write(data)
                fos.close()
            } catch (e: FileNotFoundException) {
                Log.d(TAG, "File not found: ${e.message}")
            } catch (e: IOException) {
                Log.d(TAG, "Error accessing file: ${e.message}")
            }


            val client = OkHttpClient()
            val MEDIA_TYPE_JPG = MediaType.parse("image/jpeg")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("modelId", "07efed96-937c-4824-8649-b17f55934842")
                .addFormDataPart(
                    "file",
                    "REPLACE_IMAGE_PATH.jpg",
                    RequestBody.create(MEDIA_TYPE_JPG, File(pictureFile.canonicalPath))
                )
                .build()

            val request = Request.Builder()
                .url("https://app.nanonets.com/api/v2/ImageCategorization/LabelFile/")
                .post(requestBody)
                .addHeader("Authorization", Credentials.basic("_TyJF0Ox5DIY-2wlU7Axmf2qOdiOLgh7", "_TyJF0Ox5DIY-2wlU7Axmf2qOdiOLgh7"))
                .build()

                val thread = Thread(Runnable {
                    try {
                        val response = client.newCall(request).execute()
                        val body = response.body()?.string()
                        Log.d("CameraActivity", body)
                        val json = JSONObject(body)
                        //val predictions = json.getJSONObject("result").getJSONArray("prediction")
                        //val predictions = json.getJSONArray("result")
                        val predictions = json.getJSONArray("result").getJSONObject(0).getJSONArray("prediction")
                        Log.d("CameraActivity", predictions.toString())


                        runOnUiThread {
                            val sb = StringBuilder()
                            dialog.dismiss()
                            //dialog.setMessage(body)
                            for(i in 0 until predictions.length()) {
                                predictions.getJSONObject(i).getString("label")
                                sb.append(predictions.getJSONObject(i).getString("label")).
                                        append(": ").
                                        append(predictions.getJSONObject(i).getString("probability"))

                                if(i != predictions.length() - 1) {
                                    sb.append(System.getProperty("line.separator"))
                                }
                            }
                            dialog.setMessage(sb)
                            // iterate to retrieve the labels

                            // Create the AlertDialog object and return it
                            dialog.show()
                        }
                    } catch (e: Exception) {
                        Log.e("CameraActivity", e.message)
                    }

                })
                thread.start()
            mCamera?.startPreview()
        }


        val captureButton: Button = findViewById(R.id.button_capture)
        captureButton.setOnClickListener {
            // get an image from the camera
            mCamera?.takePicture(null, null, mPicture)
        }
    }



    private fun stopPreviewAndFreeCamera() {
        mCamera?.apply {
            // Call stopPreview() to stop updating the preview surface.
            stopPreview()

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            release()

            mCamera = null
        }
    }

    override fun onPause() {
        super.onPause()
        //stopPreviewAndFreeCamera()
    }

    fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }

    private fun getOutputMediaFile(type: Int): File? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Date())
        val filename = String.format("%s_%s.jpg", "image", dateFormat)


        val f = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            ), "/BrokenScreen"
        )

        if (!f.exists()) {
            f.mkdirs()
        }

        Log.d(TAG, "Directory created")

        val file = File(f, filename)

        // Create a media file name
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                file
            }
            else -> null
        }
    }
}
