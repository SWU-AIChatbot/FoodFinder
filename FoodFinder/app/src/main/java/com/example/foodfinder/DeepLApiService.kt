package com.example.foodfinder

import android.util.Log
import com.deepl.api.Translator

class DeepLApiService {
    private val deepLApiKey = "3c031c53-4afe-4cfd-86ea-459b311659a1:fx"      // DeepL Api 키

    fun translateText(text: String, sourceLang: String, targetLang: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val translator = Translator(deepLApiKey)
                val result = translator.translateText(text, sourceLang, targetLang)
                Log.d("Translation", "Original: $text, Translated: ${result.text}")
                onComplete(result.text)
            } catch (e: Exception) {
                Log.e("Translation", "Translation failed: ${e.message}")
                onError("Untranslatable")
            }
        }.start()
    }
}