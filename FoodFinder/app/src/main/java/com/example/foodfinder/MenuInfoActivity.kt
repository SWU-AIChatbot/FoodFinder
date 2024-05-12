package com.example.foodfinder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MenuInfoActivity : AppCompatActivity() {

    private lateinit var koreanText: String // 인식된 한국어 텍스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_info)

        val menuname_us_tv = findViewById<TextView>(R.id.menuname_us_tv)    // 번역 후 텍스트뷰(외국어 텍스트뷰)
        val menuname_kr_tv = findViewById<TextView>(R.id.menuname_kr_tv)    // 번역 전 텍스트뷰(이미지 인식 후 한글 텍스트뷰)

        val imageUri: Uri

        val back_btn = findViewById<ImageView>(R.id.back_iv)

        val usdTv = findViewById<TextView>(R.id.usd_tv)
        val kwrEt = findViewById<EditText>(R.id.kwr_et)

        val searchIv = findViewById<ImageView>(R.id.img_search_iv)

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
                    menuname_kr_tv.text = recognizedResult
                    Log.d("Translation", "Text recognition succeeded1: $recognizedResult")

                    translateText(koreanText, menuname_us_tv)   // 번역
                    CoroutineScope(Dispatchers.Main).launch {
                        translateToRomanized(koreanText)
                    }

                    // 검색엔진 이미지
                    searchIv.setOnClickListener {
                        val keyword = koreanText
                        val intent = Intent(this, NewActivity::class.java).apply {
                            putExtra("keyword", keyword)
                        }
                        startActivity(intent)
                    }
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

    private suspend fun translateToRomanized(koreanText: String) {
        val menuname_kr2_tv = findViewById<TextView>(R.id.menuname_kr2_tv)

        val apiService = NaverApiService.create()

        try {
            val response = withContext(Dispatchers.IO) {
                apiService.getRomanization(koreanText)
            }
            Log.d("로마자", "$koreanText")
            Log.d("로마자", "Response: $response") // 추가한 로그


            if (response.aResult.isNotEmpty()) {
                val romanizedName = response.aResult[0].aItems[0].name
                withContext(Dispatchers.Main) {
                    menuname_kr2_tv.text = romanizedName
                }
                Log.d("로마자", "$romanizedName")
            } else {
                Log.e("로마자", "No result found")
            }
        } catch (e: Exception) {
            Log.e("로마자", "Failed to get Romanized name", e)
        }
    }



    private fun translateText(resultText: String, menuname_us_tv: TextView) {       // DeepL을 이용한 번역(한국어 -> 외국어)
        // translateText(번역할 텍스트, 원본 언어, 번역할 언어)
        DeepLApiService().translateText(resultText, "ko", "en-US",
            onComplete = { translatedText ->
                runOnUiThread { menuname_us_tv.text = translatedText }    // 번역 성공
                adjustTextSize(menuname_us_tv, translatedText) // 번역된 텍스트의 크기 조정
            },
            onError = { unTranslatedText ->
                runOnUiThread { menuname_us_tv.text = unTranslatedText }    // 번역 실패
                adjustTextSize(menuname_us_tv, unTranslatedText) // 번역되지 않은 텍스트의 크기 조정
            }
        )
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

    // 글자 크기
    private fun adjustTextSize(textView: TextView, text: String) {
        val textWidth = textView.paint.measureText(text) // 텍스트의 폭 측정
        val textViewWidth = textView.width - textView.paddingLeft - textView.paddingRight
        val textViewHeight = textView.height - textView.paddingTop - textView.paddingBottom

        val textSize = textView.textSize // 현재 텍스트 크기
        val newTextSize = if (textWidth > textViewWidth || text.lines().size > 1) {
            // 텍스트가 너무 길거나 여러 줄인 경우 텍스트 크기 조정
            (textSize * textViewWidth / textWidth).coerceAtMost(textViewHeight.toFloat()) // 더 작은 값으로 크기 조정
        } else {
            // 텍스트가 적절한 크기인 경우 현재 크기 유지
            textSize
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize)
    }

}