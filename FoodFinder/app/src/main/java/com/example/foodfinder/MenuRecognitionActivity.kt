package com.example.foodfinder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.theartofdev.edmodo.cropper.CropImage
import java.io.File

class MenuRecognitionActivity : AppCompatActivity() {

    private lateinit var imagePathString: String
    private lateinit var bitmap: Bitmap

    private var canvas: Canvas = Canvas()  // 캔바스
    private val rectPaintInit: Paint = Paint() // 기본 페인트
    private val rectPaintSelect: Paint = Paint() // 선택된 페인트

    init {
        rectPaintInit.style = Paint.Style.FILL
        rectPaintInit.color = Color.argb(70, 240, 240, 240)   // #FFFFFF
        rectPaintSelect.style = Paint.Style.FILL
        rectPaintSelect.color = Color.argb(85, 255, 124, 82)   // #FF7C52
    }

    // 텍스트 박스 정보 저장
    private val recognizeMap = mutableMapOf<RectF, String>()        // key: 인식된 텍스트 박스 좌표, value: 인식된 텍스트
    private val boxSelectMap = mutableMapOf<RectF, Boolean>()       // key: 인식된 텍스트 박스 좌표,value: 텍스트 박스 선택 여부

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_recognition)

        val back_btn = findViewById<ImageView>(R.id.back_iv)
        val crop_btn = findViewById<ImageView>(R.id.crop_iv)
        val menu_img_iv = findViewById<ImageView>(R.id.menu_img_iv)

        if(intent.hasExtra("imagePath")) {
            imagePathString = intent.getStringExtra("imagePath").toString()

            // 촬영된 이미지 파일을 비트맵으로 디코딩
            bitmap = BitmapFactory.decodeFile(imagePathString)
            //val rotatedBitmap = bitmapRotate(bitmap);  // 비트맵 이미지 회전
            bitmap = bitmapRotate(bitmap);  // 비트맵 이미지 회전

            canvas = Canvas(bitmap) // 이미지 캔바스 설정

            // 이미지 텍스트 인식
            val ocrImage: InputImage? = imageFromBitmap(bitmap)   // 비트맵 이미지를 사용하여 InputImage 객체 만들기
            if (ocrImage != null) {
                recognizeText(ocrImage, bitmap) // 이미지 텍스트 인식
            }
        }

        // ImageView에 터치 리스너 추가
        var recognizedText: String = "" // 선택한 박스의 인식된 텍스트
        var beforeRect: RectF? = null   // 움직이기 이전 텍스트 박스 좌표
        var isTouch: Boolean = false    // 이미지 텍스트 박스 터치 여부

        menu_img_iv.setOnTouchListener { v: View, event: MotionEvent ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("테스트1", "event: $event")
                    Log.d("테스트1", "event.x: ${event.x}")
                    Log.d("테스트1", "event.y: ${event.y}")
                    for (r in recognizeMap) {   // 모든 텍스트 박스에 대해 클릭 여부 확인
                        // 클릭된 위치가 텍스트 박스ㅅ 내부일 때
                        if (event.x >= r.key.left && event.x <= r.key.right && event.y >= r.key.top && event.y <= r.key.bottom) {
                            isTouch = true  // 이미지 텍스트 박스 터치 true로 변경
                            boxSelectMap[r.key] = true      // 텍스트 박스 선택하면 true로 변경
                            recognizedText = r.value    // 인식된 텍스트
                            beforeRect = r.key      // 선택한 텍스트 박스 좌표

                            canvas.drawRect(r.key, rectPaintSelect)
                            menu_img_iv.setImageBitmap(bitmap)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    for (r in recognizeMap) {   // 모든 텍스트 박스에 대해 클릭 여부 확인
                        // 클릭된 위치가 텍스트 박스 내부일 때
                        if (event.x >= r.key.left && event.x <= r.key.right && event.y >= r.key.top && event.y <= r.key.bottom) {
                            if(boxSelectMap[r.key] == false) {   // 선택하고 있는 텍스트 박스가 이전에 선택한 텍스트 박스가 아닌 경우
                                boxSelectMap[r.key] = true      // 텍스트 박스 선택하면 true로 변경
                                recognizedText = recognizedText + r.value    // 인식된 텍스트(텍스트 합치기)

                                canvas.drawRect(r.key, rectPaintSelect)
                                menu_img_iv.setImageBitmap(bitmap)

                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if(isTouch == true) {   // 이미지 텍스트 박스 터치 여부가 true인 경우 (텍스트 박스 선택한 경우)
                        nextActivity(imagePathString, recognizedText)   // MenuInfo 화면으로 전환
                    }
                    true
                }
                else -> false
            }
        }

        back_btn.setOnClickListener {
            // MainActivity로 이동하는 Intent 생성
            val intent = Intent(this, MainActivity::class.java)
            // Intent로 새 액티비티 시작
            startActivity(intent)
        }

        crop_btn.setOnClickListener {
            Log.d("테스트2", "크롭")
            // 이미지 crop - 이미지가 성공적으로 찍혔으므로 크롭 액티비티를 시작
            startCrop(imagePathString)
        }
    }

    // 비트맵 이미지 회전
    private fun bitmapRotate(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix,true)
    }

    // 비트맵 이미지를 사용하여 InputImage 객체 만들기
    private fun imageFromBitmap(bitmap: Bitmap): InputImage? {
        val rotationDegrees = 0
        return InputImage.fromBitmap(bitmap, rotationDegrees)
    }

    private fun recognizeText(image: InputImage, bitmap: Bitmap) {
        // When using Korean script library - 한국어
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    val blockText = block.text
                    Log.d("테스트 - 블럭", "$blockText")
                    for (line in block.lines) {
                        val lineText = line.text
                        Log.d("테스트 - 라인", "$lineText")
                        for (element in line.elements) {
                            val elementText = element.text      // 텍스트
                            val rect = RectF(element.boundingBox)   // 텍스트 박스 좌표 (left, top, right, bottom)

                            Log.d("테스트2 - element", "$elementText")
                            Log.d("테스트2", "rect: $rect")

                            // 텍스트 박스 정보 저장
                            recognizeMap.put(rect, elementText)
                            boxSelectMap.put(rect, false)

                            // 텍스트 박스 표시
                            canvas.drawRect(rect, rectPaintInit)
                        }
                    }
                }

                Log.d("테스트 - 블럭 비전", "${visionText.textBlocks}")

                // 이미지 뷰에 변경된 비트맵 설정
                (findViewById<ImageView>(R.id.menu_img_iv)).setImageBitmap(bitmap)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@MenuRecognitionActivity, "Text is not recognized", Toast.LENGTH_SHORT).show()
                Log.e("RecognizeText", "텍스트 에러")
            }
    }

    private fun recognizeCrop(resultUri: Uri) {
        val context: Context = this

        // When using Korean script library - 한국어
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

        recognizer.process(InputImage.fromFilePath(context, resultUri))
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                nextActivity(imagePathString, resultText)   // MenuInfo 화면으로 전환
            }
    }

    // 이미지 crop
    private fun startCrop(imagePathString: String) {
        val file = File(imagePathString)
        val uri = Uri.fromFile(file)
        CropImage.activity(uri).start(this@MenuRecognitionActivity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {   // 이미지 자르기 누른 후
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result: CropImage.ActivityResult = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri: Uri = result.uri

                // 크롭한 이미지 텍스트 인식
                recognizeCrop(resultUri)
            }
        }
    }

    // MenuInfo 화면으로 전환
    private fun nextActivity(imagePath: String, recognizedText: String) {
        val intent = Intent(this, MenuInfoActivity::class.java)   // 다음 화면으로 이동하기 위한 인텐트 객체 생성
        intent.putExtra("imagePath", imagePath)
        intent.putExtra("recognizedText", recognizedText)
        startActivity(intent)  // 화면 전환
        finish()
    }
}