package com.example.foodfinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.foodfinder.databinding.ActivityFoodBinding
import com.example.foodfinder.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FoodActivity : AppCompatActivity() {

    private val imageSize = 224

    private lateinit var viewBinding: ActivityFoodBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    // 변수 추가 - 사진 한번만 찍을 수 있도록
    private var photoClick: Boolean = false

    private lateinit var photoFile: File // 파일 변수 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityFoodBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions - 카메라 권한 요청
        if (allPermissionsGranted()) {  // 권한 0
            startCamera()   // 카메라 시작 함수 호출
        } else {
            ActivityCompat.requestPermissions(  // 권한 X
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // 이미지 임시 파일 생성 -> 캐시 생성(data/data/패키지/cache) -> 실험중
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
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: return

                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // 촬영된 이미지 파일을 비트맵으로 디코딩
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                        // 이미지 분류 함수 호출
                        val resultText = classifyImage(bitmap)

                        // FoodInfoActivity로 결과 전달 Intent 설정
                        val intent = Intent(this@FoodActivity, FoodInfoActivity::class.java)
                        intent.putExtra("image", photoFile.absolutePath)
                        intent.putExtra("resultText", resultText)
                        startActivity(intent)
                        finish()
                    }
                }
            )

        }
        photoClick = true
    }

    // 이미지 분류 함수
    private fun classifyImage(image: Bitmap): String {
        var resultText = ""
        try {
            // 이미지 리사이즈
            val resizedBitmap = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)

            Log.d(TAG, "Resized image size: ${resizedBitmap.width} x ${resizedBitmap.height}")

            // TensorFlow Lite 모델을 초기화하여 새로운 인스턴스를 가져옴
            val model = Model.newInstance(applicationContext)

            // 입력 이미지의 픽셀 값을 ByteBuffer에 저장
            val byteBuffer = ByteBuffer.allocateDirect(1 * imageSize * imageSize * 3 * 4) // 이미지 크기 * 3(RGB 채널) * 4(Byte)
            byteBuffer.order(ByteOrder.nativeOrder())

            // 입력 이미지의 픽셀 값을 int 배열로 저장
            val intValues = IntArray(imageSize * imageSize)
            resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

            // 픽셀 값을 byteBuffer에 저장하여 입력 이미지를 TensorBuffer에 로드
            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++]
                    // RGB 채널 값을 정규화하여 byteBuffer에 저장
                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f)) // Red 채널
                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))  // Green 채널
                    byteBuffer.putFloat((value and 0xFF) * (1f / 255f))           // Blue 채널
                }
            }

            // byteBuffer의 데이터를 TensorBuffer로 로드
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, imageSize, imageSize, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

            // 모델에 입력 이미지를 전달하여 결과를 가져옴
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.getOutputFeature0AsTensorBuffer()

            // 결과로부터 가장 높은 확률의 클래스를 찾아냄
            val confidences = outputFeature0.floatArray
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }

            Log.d(TAG, "Max confidence: $maxConfidence, Max position: $maxPos")

            // 각 클래스에 해당하는 음식 이름 배열
            val classes = arrayOf("삼겹살", "불고기", "잡채", "물회", "육회", "짜장면", "비빔냉면", "물냉면",
                "파전", "주꾸미볶음", "떡볶이", "족발", "순대", "추어탕", "삼계탕", "순두부찌개", "김치찌개", "간장게장",
                "양념게장", "비빔밥", "김밥", "배추김치", "곱창전골", "약과", "김치볶음밥", "깍두기", "도토리묵", "꿀떡", "시금치나물",
                "제육볶음", "된장찌개", "수정과", "파김치", "만두", "라면", "장조림", "계란찜", "식혜", "유부초밥", "깻잎짱아찌")
            // 결과 텍스트를 해당 클래스의 음식 이름으로 설정
            resultText = classes[maxPos]

            // 모델 인스턴스를 닫음
            model.close()
        } catch (e: Exception) {
            // 예외가 발생한 경우 에러 로그 출력
            Log.e("classifyImage", "Error during image classification: ${e.message}")
            e.printStackTrace()
            // 예외가 발생한 경우 빈 문자열 반환
            return ""
        }
        // 분류된 결과를 반환
        return resultText
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
                //, Manifest.permission.RECORD_AUDIO
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
}
