package com.example.foodfinder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

        val imageView: ImageView = findViewById(R.id.menu_img_iv)
        val resultTextView: TextView = findViewById(R.id.menuname_kr_tv)

        // FoodActivity에서 전달한 파일 경로를 받아옴
        val photoFilePath = intent.getStringExtra("image")
        val resultText: String? = intent.getStringExtra("resultText")

        // 파일 경로를 사용하여 이미지를 로드하고 이미지뷰에 설정
        if (!photoFilePath.isNullOrEmpty()) {
            val file = File(photoFilePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(photoFilePath)
                imageView.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Photo file path is empty", Toast.LENGTH_SHORT).show()
        }
        resultTextView.text = resultText
    }
}