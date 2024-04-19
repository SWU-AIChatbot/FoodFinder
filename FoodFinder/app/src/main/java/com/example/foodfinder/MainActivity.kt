package com.example.foodfinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.foodfinder.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private val imageSize = 224
    private val REQUEST_CAMERA = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val foodIv = findViewById<ImageView>(R.id.food_iv)

        // 버튼 OnClickListener
        foodIv.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_CAMERA)
    }

    // 이미지 분류 함수
    private fun classifyImage(image: Bitmap): Pair<Bitmap, String> {
        var resultText = ""
        try {
            val model = Model.newInstance(applicationContext)

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++]
                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat((value and 0xFF) * (1f / 255f))
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.getOutputFeature0AsTensorBuffer()

            val confidences = outputFeature0.floatArray
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }

            // 음식 클래스 배열
            val classes = arrayOf("삼겹살", "불고기", "잡채", "물회", "육회", "짜장면", "비빔냉면", "물냉면",
                "파전", "주꾸미볶음", "떡볶이", "족발", "순대", "추어탕", "삼계탕", "순두부찌개", "김치찌개", "간장게장",
                "양념게장", "비빔밥", "김밥", "배추김치", "곱창전골", "약과", "김치볶음밥", "깍두기", "도토리묵", "꿀떡", "시금치나물",
                "제육볶음", "된장찌개", "수정과", "파김치", "만두", "라면", "장조림", "계란찜", "식혜", "유부초밥", "깻잎짱아찌")
            resultText = classes[maxPos]

            model.close()
        } catch (e: Exception) {
            Log.e("classifyImage", "Error during image classification: ${e.message}")
            e.printStackTrace()
            // 예외가 발생한 경우 이미지와 결과를 빈 값으로 반환하여 SubActivity로 전달하지 않음
            return Pair(image, "")
        }
        return Pair(image, resultText)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val image = data?.extras?.get("data") as? Bitmap
            image?.let {
                val dimension = it.width.coerceAtMost(it.height)
                val thumbnail = ThumbnailUtils.extractThumbnail(it, dimension, dimension)
                val scaledImage = Bitmap.createScaledBitmap(thumbnail, imageSize, imageSize, false)
                val resultPair = classifyImage(scaledImage)
                val resultText = resultPair.second

                // SubActivity로 이미지와 결과 전달
                val intent = Intent(this, FoodInfoActivity::class.java)
                intent.putExtra("image", resultPair.first)
                intent.putExtra("resultText", resultText)
                startActivity(intent)
            }
        }
    }
}