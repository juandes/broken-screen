package com.example.brokenscreen

import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
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
        if (params?.supportedFocusModes!!.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
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
                dialog.setMessage("Predicting...")
                dialog.show()
            }

            Log.d("CameraActivity", "Picture Taken")

            // create image file
            val pictureFile: File = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
                Log.d("CameraActivity", ("Error creating media file, check storage permissions"))
                return@PictureCallback
            }


            // write image
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

            // build API request's body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("modelId", "07efed96-937c-4824-8649-b17f55934842")
                .addFormDataPart(
                    "file",
                    "REPLACE_IMAGE_PATH.jpg",
                    RequestBody.create(MEDIA_TYPE_JPG, File(pictureFile.canonicalPath))
                )
                .build()

            // build request
            val request = Request.Builder()
                .url("https://app.nanonets.com/api/v2/ImageCategorization/LabelFile/")
                .post(requestBody)
                .addHeader("Authorization", Credentials.basic("", ""))
                .build()

            // define the thread that will execute the API call
            val thread = Thread(Runnable {
                try {
                    val response = client.newCall(request).execute()
                    val body = response.body()?.string()
                    val json = JSONObject(body)
                    val predictions = json.getJSONArray("result").getJSONObject(0).getJSONArray("prediction")
                    Log.d(TAG, predictions.toString())


                    runOnUiThread {
                        val sb = StringBuilder()
                        dialog.dismiss()
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
                    Log.e(TAG, e.message)
                }

            })

            // execute the thread
            thread.start()
            // restore the camera preview
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

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
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
