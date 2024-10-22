package com.example.opencv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.core.Mat
/*import org.opencv.android.CameraBridgeViewBase.CAMERA_ID
import org.opencv.android.CameraBridgeViewBase.CAMERA_FACING_BACK
import org.opencv.android.CameraBridgeViewBase.CAMERA_FACING_FRONT*/

class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CameraBridgeViewBase(context, attrs), CvCameraViewListener2 {

    private var paint: Paint = Paint()
    private var cameraCapture: Mat? = null

    init {
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 50f

        // Set the listener for camera events
        setCvCameraViewListener(this)
    }

  /*  override fun onCameraFrame(inputFrame: Mat): Mat {
        cameraCapture = inputFrame.clone() // Save the captured frame
        return inputFrame // Return the same frame (or apply processing here)
    }

    override fun onCameraOpened(cameraId: Int, cameraFrameWidth: Int, cameraFrameHeight: Int) {
        // Handle camera opened logic if necessary
    }

    override fun onCameraClosed() {
        // Handle camera closed logic if necessary
    }

    override fun onCameraStarted(cameraId: Int, cameraFrameWidth: Int, cameraFrameHeight: Int) {
        // Handle camera started logic if necessary
    }

    override fun onCameraStopped() {
        // Handle camera stopped logic if necessary
    }*/

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(android.graphics.Color.BLACK)

        cameraCapture?.let {
            // Here you can convert the Mat to a Bitmap or draw directly
            // For demonstration, I'm drawing a placeholder text.
            canvas.drawText("Camera Frame", 50f, 100f, paint)
        }
    }

    override fun connectCamera(width: Int, height: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun disconnectCamera() {
        TODO("Not yet implemented")
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onCameraViewStopped() {
        TODO("Not yet implemented")
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame?): Mat {
        TODO("Not yet implemented")
    }
}
