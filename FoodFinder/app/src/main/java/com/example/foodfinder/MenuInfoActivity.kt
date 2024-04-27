package com.example.foodfinder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MenuInfoActivity : AppCompatActivity() {

    private lateinit var koreanText: String // 인식된 한국어 텍스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_info)

        val menunameUsTv = findViewById<TextView>(R.id.menuname_us_tv)    // 번역 후 텍스트뷰(외국어 텍스트뷰)
        val menunameKrTv = findViewById<TextView>(R.id.menuname_kr_tv)    // 번역 전 텍스트뷰(이미지 인식 후 한글 텍스트뷰)

        val imageUri: Uri

        val back_btn = findViewById<ImageView>(R.id.back_iv)

        val usdTv = findViewById<TextView>(R.id.usd_tv)
        val kwrEt = findViewById<EditText>(R.id.kwr_et)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.freecurrencyapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val exchangeRateApiService = retrofit.create(ExchangeRateApiService::class.java)

        if(intent.hasExtra("image_uri")) {      // intent로 받아온 uri가 있을 경우
            val imageUriString = intent.getStringExtra("image_uri")
            imageUri = Uri.parse(imageUriString)    // String -> Uri

            // 이미지 텍스트 인식
            val ocrImage: InputImage? =
                imageFromPath(this, imageUri)   // 이미지 uri 사용하여 InputImage 객체 만들기
            if (ocrImage != null) {
                recognizeText(ocrImage) { recognizedResult ->   // 텍스트 인식
                    koreanText = recognizedResult    // 인식된 한국어 텍스트
                    menunameKrTv.text = recognizedResult
                    Log.d("Translation", "Text recognition succeeded1: $recognizedResult")

                    translateText(koreanText, menunameUsTv)   // 번역
                }
            }
        }

        //원화 입력 시 달러 자동 변환
        kwrEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val kwrAmount = s.toString().toDoubleOrNull()

                if (kwrAmount != null) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val response = exchangeRateApiService.getLatestExchangeRates(
                                "fca_live_UEh7ozfhJgsNKf9gpGgUGRxSdqEYQL1bEbDR66vA",
                                "USD",
                                "KRW"
                            )
                            val usdRate = response.data.USD ?: 0.0 // USD 환율을 가져옴

                            val usdAmount = kwrAmount * usdRate

                            // 계산된 환율을 usd_tv에 표시
                            withContext(Dispatchers.Main) {
                                usdTv.text = String.format("%.2f", usdAmount)
                                Log.d("환율계산","usd -> ${usdTv.text}")
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MenuInfoActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            e.printStackTrace()
                        }
                    }
                } else {
                    // kwr_et에 유효한 숫자가 입력되지 않은 경우 사용자에게 메시지 표시
                    Toast.makeText(this@MenuInfoActivity, "유효한 숫자를 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        })


        back_btn.setOnClickListener {
            // FoodActivity로 이동하는 Intent 생성
            val intent = Intent(this, MainActivity::class.java)
            // Intent로 새 액티비티 시작
            startActivity(intent)
        }
    }

    // DeepL을 이용한 번역(한국어 -> 외국어)
    private fun translateText(resultText: String, menunameUsTv: TextView) {
        // translateText(번역할 텍스트, 원본 언어, 번역할 언어)
        DeepLApiService().translateText(resultText, "ko", "en-US",
            onComplete = { translatedText ->
                runOnUiThread { menunameUsTv.text = translatedText }    // 번역 성공
            },
            onError = { unTranslatedText ->
                runOnUiThread { menunameUsTv.text = unTranslatedText }    // 번역 실패
            }
        )
    }

    // 텍스트 인식
    private fun recognizeText(image: InputImage, onComplete: (String) -> Unit) {
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

    // 파일 uri 사용하여 InputImage 객체 만들기
    private fun imageFromPath(context: Context, uri: Uri): InputImage? {
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
}