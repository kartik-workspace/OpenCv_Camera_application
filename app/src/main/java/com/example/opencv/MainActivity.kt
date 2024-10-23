package com.example.opencv

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraId: String? = null
    private var isBackCamera: Boolean = true
    private lateinit var previewSize: Size
    private lateinit var imageReader: ImageReader
    private lateinit var capturedImageView: ImageView
    private lateinit var captureButton: ImageView
    private lateinit var buttonContainer: LinearLayout
    private lateinit var saveButton: Button
    private var capturedImage: Bitmap? = null // Define capturedImage here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceView = findViewById(R.id.surface_view)
        surfaceView.holder.addCallback(this)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        capturedImageView = findViewById(R.id.captured_image_view)
        captureButton = findViewById(R.id.btn_capture)
        buttonContainer = findViewById(R.id.button_container)
        saveButton = findViewById(R.id.btn_save)

        saveButton.setOnClickListener {
            capturedImage?.let { bitmap ->
                saveImage(bitmap)
            } ?: run {
                Toast.makeText(this, "No image to save!", Toast.LENGTH_SHORT).show()
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            setupCamera()
        }

        findViewById<Button>(R.id.btn_switch_camera).setOnClickListener {
            toggleCamera()
        }
        // Aspect ratio buttons
        findViewById<Button>(R.id.btn_ratio_1_1).setOnClickListener {
            setAspectRatio(1, 1)
        }
        findViewById<Button>(R.id.btn_ratio_4_3).setOnClickListener {
            setAspectRatio(4, 3)
        }
        findViewById<Button>(R.id.btn_ratio_16_9).setOnClickListener {
            setAspectRatio(16, 9)
        }

        findViewById<Button>(R.id.btn_retake).setOnClickListener {
            retakeImage() // Call the retake function on button click
        }

        captureButton.setOnClickListener {
            captureImage() // Call the capture function on button click
        }
    }



    private fun saveImage(bitmap: Bitmap) {

        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // For Android 10+
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        try {
            if (uri != null) {
                val outputStream = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                outputStream?.close()
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun captureImage() {
        try {
            // Initialize ImageReader
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                image.close()

                // Rotate the captured image
                val rotation = getImageRotation()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val rotatedBitmap = rotateImage(bitmap, rotation)

                // Store the captured image
                capturedImage = rotatedBitmap // Assign to capturedImage here

                // Display the rotated bitmap
                capturedImageView.setImageBitmap(rotatedBitmap)

                // Update visibility of views
                surfaceView.visibility = SurfaceView.GONE
                capturedImageView.visibility = ImageView.VISIBLE
                captureButton.visibility = Button.GONE
                buttonContainer.visibility = LinearLayout.VISIBLE
            }, null)

            // Set up capture request
            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder?.addTarget(imageReader.surface)

            // Create the capture session
            cameraDevice?.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.capture(captureRequestBuilder!!.build(), null, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(this@MainActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun getImageRotation(): Int {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
        val rotation = windowManager.defaultDisplay.rotation
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        return when (rotation) {
            Surface.ROTATION_0 -> sensorOrientation
            Surface.ROTATION_90 -> (sensorOrientation + 90) % 360
            Surface.ROTATION_180 -> (sensorOrientation + 180) % 360
            Surface.ROTATION_270 -> (sensorOrientation + 270) % 360
            else -> sensorOrientation
        }
    }

    private fun rotateImage(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(angle.toFloat())
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun retakeImage() {
        // Reset visibility for retaking the image
        surfaceView.visibility = SurfaceView.VISIBLE
        capturedImageView.visibility = ImageView.GONE
        captureButton.visibility = Button.VISIBLE
        buttonContainer.visibility = LinearLayout.GONE

        // Close the camera if it is open before reopening it
        cameraDevice?.close()
        cameraDevice = null // Set cameraDevice to null after closing

        // Restart the camera preview
        setupCamera() // Ensure setupCamera reopens the camera
    }

    // Method to set aspect ratio
    private fun setAspectRatio(widthRatio: Int, heightRatio: Int) {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.constraint_layout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        // Adjust for portrait or landscape mode
        val aspectRatio = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            "$widthRatio:$heightRatio" // Landscape mode
        } else {
            "$heightRatio:$widthRatio" // Portrait mode
        }

        // Set the aspect ratio dynamically for SurfaceView
        constraintSet.setDimensionRatio(R.id.surface_view, aspectRatio)
        constraintSet.applyTo(constraintLayout)

        // Update camera preview size based on the new aspect ratio
        updateCameraPreviewSize(widthRatio, heightRatio)
    }

    // Update the camera preview size to match the aspect ratio
    private fun updateCameraPreviewSize(widthRatio: Int, heightRatio: Int) {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(SurfaceHolder::class.java)

            // Find the best matching size based on the aspect ratio
            previewSize = chooseOptimalSize(outputSizes!!, widthRatio, heightRatio)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // Method to choose the best preview size based on the aspect ratio
    private fun chooseOptimalSize(choices: Array<Size>, widthRatio: Int, heightRatio: Int): Size {
        val targetRatio = widthRatio.toDouble() / heightRatio.toDouble()
        var optimalSize: Size? = null
        var minDiff = Double.MAX_VALUE

        for (size in choices) {
            val aspectRatio = size.width.toDouble() / size.height.toDouble()
            val diff = abs(aspectRatio - targetRatio)
            if (diff < minDiff) {
                minDiff = diff
                optimalSize = size
            }
        }
        return optimalSize ?: choices[0] // Fallback to the first size if no match is found
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        openCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        cameraDevice?.close()
    }

    private fun setupCamera() {
        try {
            cameraId = if (isBackCamera) {
                cameraManager.cameraIdList[0] // Back camera
            } else {
                cameraManager.cameraIdList.last() // Use the last camera ID for front camera
            }

            updateCameraPreviewSize(4, 3) // Set default aspect ratio 4:3
            openCamera() // Open the camera
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
                return
            }

            cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice?.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice?.close()
                    cameraDevice = null
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startPreview() {
        if (cameraDevice == null) {
            Log.e("CameraError", "CameraDevice is null, cannot start preview.")
            return
        }

        try {
            val surfaceHolder = surfaceView.holder
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surfaceHolder.surface)

            cameraDevice?.createCaptureSession(listOf(surfaceHolder.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(previewRequestBuilder!!.build(), null, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(this@MainActivity, "Failed to start camera preview", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun toggleCamera() {
        // Close the current camera if it is open
        cameraDevice?.close()
        cameraDevice = null

        // Toggle camera state
        isBackCamera = !isBackCamera

        // Re-setup the camera with the new camera ID
        setupCamera()
    }
}
