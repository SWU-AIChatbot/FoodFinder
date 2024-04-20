package com.example.foodfinder

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.IOException

class MenuInfoActivity : AppCompatActivity() {
    private lateinit var koreanText: String // 인식된 한국어 텍스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_info)

        val menuname_us_tv = findViewById<TextView>(R.id.menuname_us_tv)    // 번역 후 텍스트뷰(외국어 텍스트뷰)
        val menuname_kr_tv = findViewById<TextView>(R.id.menuname_kr_tv)    // 번역 전 텍스트뷰(이미지 인식 후 한글 텍스트뷰)

        val imageUri: Uri

        if(intent.hasExtra("image_uri")) {      // intent로 받아온 uri가 있을 경우
            val imageUriString = intent.getStringExtra("image_uri")
            imageUri = Uri.parse(imageUriString)    // String -> Uri

            // 이미지 텍스트 인식
            val ocrImage: InputImage? = imageFromPath(this, imageUri)   // 이미지 uri 사용하여 InputImage 객체 만들기
            if (ocrImage != null) {
                recognizeText(ocrImage) { recognizedResult ->   // 텍스트 인식
                    koreanText = recognizedResult    // 인식된 한국어 텍스트
                    menuname_kr_tv.text = recognizedResult
                    Log.d("Translation", "Text recognition succeeded1: $recognizedResult")

                    translateText(koreanText, menuname_us_tv)   // 번역
                }
            }
        }
    }

    private fun imageFromPath(context: Context, uri: Uri): InputImage? {    // 파일 uri 사용하여 InputImage 객체 만들기
        // [START image_from_path]
        val image: InputImage

        try {
            image = InputImage.fromFilePath(context, uri)
            return image
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        // [END image_from_path]
    }

    private fun recognizeText(image: InputImage, onComplete: (String) -> Unit) {        // 텍스트 인식
        // [START get_detector_default]
        // When using Korean script library - 한국어
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())       // 한국어 라이
        // [END get_detector_default]

        // [START run_detector]
        recognizer.process(image).addOnSuccessListener { visionText ->          // 인식 성공
            val resultText = visionText.text
            Log.d("Translation", "Text recognition succeeded3: $resultText")
            onComplete(resultText)
        }.addOnFailureListener { e ->                                             // 인식 실패
            Log.e("Translation", "Text recognition failed: ${e.message}")
            onComplete("Unrecognizable")
            runOnUiThread {
                Toast.makeText(this, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun translateText(resultText: String, menuname_us_tv: TextView) {       // DeepL을 이용한 번역(한국어 -> 외국어)
        // translateText(번역할 텍스트, 원본 언어, 번역할 언어)
        DeepLApiService().translateText(resultText, "ko", "en-US",
            onComplete = { translatedText ->
                runOnUiThread { menuname_us_tv.text = translatedText }    // 번역 성공
            },
            onError = { unTranslatedText ->
                runOnUiThread { menuname_us_tv.text = unTranslatedText }    // 번역 실패
            }
        )
    }
}