package com.example.foodfinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.foodfinder.databinding.ActivityMenuBinding
import com.theartofdev.edmodo.cropper.CropImage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MenuActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMenuBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    // 변수 추가 - 사진 한번만 찍을 수 있도록
    private var photoClick: Boolean = false

    private lateinit var photoFile: File // 파일 변수 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions - 카메라 권한 요청
        if (allPermissionsGranted()) {  // 권한 0
            startCamera()   // 카메라 시작 함수 호출
        } else {
            ActivityCompat.requestPermissions(  // 권한 X
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // 이미지 임시 파일 생성 -> 캐시 생성(data/data/패키지/cache)
        photoFile = File(applicationContext.cacheDir, "foodImage.jpg")

        // Set up the listeners for take photo and video capture buttons
        viewBinding.captureBtn.setOnClickListener { takePhoto() }   // 카메라 찍는 버튼 클릭 시, takePhoto 함수 호출

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // 사진 찍기 함수
    private fun takePhoto() {
        if(!photoClick) {       // 카메라 찍는 버튼을 여러번 연속 클릭하는 것을 방지하기 위해 if문 작성(photoClick 변수가 false일 때만 실행 이후 true로 변경됨)
            // Get a stable reference of the modifiable image capture use case
            val imageCapture = imageCapture ?: return
            // ImageCapture.OutputFileOptions는 새로 캡처한 이미지를 저장하기 위한 옵션
            // 저장 위치 및 메타데이터를 구성하는데 사용
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture.takePicture(           // 이미지 찍음
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed : ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: return

                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // MenuRecognition 화면으로 전환 (이미지 파일 경로를 넘김)
                        nextActivity(photoFile.absolutePath)

                        // 이미지 crop - 이미지가 성공적으로 찍혔으므로 크롭 액티비티를 시작
                        // startCrop(savedUri)
                    }
                }
            )

        }
        photoClick = true
    }

    // 카메라 시작 함수 호출
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        // private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // 이미지 crop
    private fun startCrop(imageUri: Uri) {
        CropImage.activity(imageUri).start(this@MenuActivity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {   // 이미지 자르기 누른 후
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result: CropImage.ActivityResult = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri: Uri = result.uri

                // MenuInfo 화면으로 전환
                //nextActivity(resultUri)
            }
        }
    }

    // MenuRecognition 화면으로 전환
    private fun nextActivity(imagePath: String) {
        val intent = Intent(this, MenuRecognitionActivity::class.java)   // 다음 화면으로 이동하기 위한 인텐트 객체 생성
        intent.putExtra("imagePath", imagePath)
        startActivity(intent)  // 화면 전환
        finish()
    }
}