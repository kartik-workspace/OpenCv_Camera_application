package com.example.opencv

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import android.hardware.camera2.CameraCaptureSession

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize OpenCV
        if (OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialized successfully")
        } else {
            Log.e("OpenCV", "OpenCV initialization failed")
        }

        surfaceView = findViewById(R.id.surface_view)
        surfaceView.holder.addCallback(this)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        // Request Camera Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            setupCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCamera() {
        try {
            // Setup the first camera (back camera)
            cameraId = cameraManager.cameraIdList[0]
            openCamera()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        try {
            // Check for camera permission
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
                return
            }

            // Open the camera
            cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startPreview() {
        try {
            val surface = surfaceView.holder.surface
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.setRepeatingRequest(previewRequestBuilder!!.build(), null, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(this@MainActivity, "Failed to configure camera preview", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface is created, now start the camera preview
        openCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Cleanup when the surface is destroyed
        cameraDevice?.close()
    }
}
