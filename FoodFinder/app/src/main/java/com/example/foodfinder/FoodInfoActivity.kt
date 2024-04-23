package com.example.foodfinder

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class FoodInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_info)

        // 이미지뷰, 텍스트뷰 초기화
        val imageView: ImageView = findViewById(R.id.menu_img_iv)
        val resultTextView: TextView = findViewById(R.id.menuname_kr_tv)

        // FoodActivity로부터 전달받은 파일 경로와 분류 결과 텍스트 가져오기
        val photoFilePath = intent.getStringExtra("image")
        val resultText: String? = intent.getStringExtra("resultText")

        val back_btn = findViewById<ImageView>(R.id.back_iv)

        back_btn.setOnClickListener {
            // FoodActivity로 이동하는 Intent 생성
            val intent = Intent(this, MainActivity::class.java)
            // Intent로 새 액티비티 시작
            startActivity(intent)
        }


        // 파일 경로가 비어 있지 않은 경우 이미지를 로드하고 회전시켜서 이미지뷰에 설정
        if (!photoFilePath.isNullOrEmpty()) {
            val file = File(photoFilePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(photoFilePath)

                // 이미지의 회전 메타데이터 확인
                val exif = ExifInterface(photoFilePath)
                val rotation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )

                // 회전된 이미지를 원래의 방향으로 회전시키는 함수 호출
                val rotatedBitmap = rotateBitmap(bitmap, rotation)

                // 회전된 이미지를 이미지뷰에 설정
                imageView.setImageBitmap(rotatedBitmap)
            } else {
                // 파일이 존재하지 않는 경우 에러 메시지 표시
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 파일 경로가 비어있는 경우 에러 메시지 표시
            Toast.makeText(this, "Photo file path is empty", Toast.LENGTH_SHORT).show()
        }

        // 분류 결과 텍스트를 텍스트뷰에 설정
        resultTextView.text = resultText
    }

    // 비트맵을 주어진 각도로 회전시키는 함수
    private fun rotateBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
        val matrix = Matrix()
        // 회전 각도에 따라 행렬에 회전 변환 추가
        when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        }
        // 회전된 비트맵 반환
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
