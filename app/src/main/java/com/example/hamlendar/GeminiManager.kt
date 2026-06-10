package com.example.hamlendar

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GeminiManager {

    private const val API_KEY = "AQ.Ab8RN6Js4AP7cVBVLbcebGuk6jnGL38w2Y3ycOGlN_4y2tJILw"

    interface SummaryCallback {
        fun onSuccess(summary: String)
        fun onError(error: String)
    }

    fun generateSummary(
        diaryContent: String,
        callback: SummaryCallback
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val model = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = API_KEY
                )

                val prompt = """
                    다음 일기를 한 문장으로 간단히 요약해줘.
                    핵심 내용만 20~30자 정도로 작성해.
                    
                    일기:
                    $diaryContent
                """.trimIndent()

                val response = model.generateContent(prompt)

                val summary =
                    response.text ?: "요약 결과 없음"

                println("Gemini 응답 = $summary")
                callback.onSuccess(summary)

            } catch (e: Exception) {

                e.printStackTrace()

                callback.onError(
                    e.message ?: "오류 발생"
                )
            }
        }
    }
}