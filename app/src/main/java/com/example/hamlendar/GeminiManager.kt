package com.example.hamlendar

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

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
                    다, 나, 까 대신 햄이란 말투로 끝내줘
                    최대한 귀엽게 대답해줘 
                   
                   
                    
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

    fun generateEmpathyMessage(
        userName: String,
        callback: SummaryCallback // 기존 인터페이스 재사용 (혹은 이름이 신경 쓰인다면 별도 선언)
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = API_KEY
                )

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                val timeContext = when (hour) {
                    in 5..10 -> "아침 시간대입니다. 기상 응원이나 상쾌한 아침 인사를 해주세요."
                    in 11..16 -> "낮・오후 시간대입니다. 나른함을 쫓아내거나 달콤한 휴식을 권하는 위로를 해주세요."
                    in 17..21 -> "저녁 시간대입니다. 퇴근, 하교를 격려하거나 맛있는 저녁 식사를 응원해주세요."
                    else -> "밤・새벽 시간대입니다. 오늘 하루를 수고했다고 토닥이며 꿀잠을 기원해주세요."
                }

                val prompt = """
                    너는 사용자의 하루를 매 순간 귀엽게 응원해 주는 다정한 인공지능 햄스터 친구인 햄햄이야.
                    지금은 [ $timeContext ]
                    
                    위 시간대 상황에 딱 어울리는 귀여운 응원 메시지를 '오직 한 문장'만 생성해줘.
                    
                    [필수 규칙]
                    1. 다른 설명이나 예시(아침, 오후, 저녁 분류 등)는 절대로 출력하지 마. 오직 결과 메시지 1줄만 딱 보내야 해.
                    2. 메시지 안에서 사용자의 이름인 '$userName'을 자연스럽게 불러줘. ~님은 붙이지 마.
                    3. 문장 끝은 반드시 "~햄!", "~햄?" 처럼 '햄' 말투로 끝내야 해. (다, 나, 까, 요 금지)
                    4. 나, 내 등이 필요한 상황에 네 이름인 햄햄을 넣어야 해.
                    5. '어때햄'처럼 ~때로 끝나면 '어떠냐햄'처럼 야, 냐 말투에 '햄'을 붙여줘.
                    6.'~봐햄"보다는 "~봐라햄"처럼 끝내야 해.
                """.trimIndent()

                val response = model.generateContent(prompt)
                val message = response.text?.trim() ?: "햄햄과 함께 기분 좋은 하루 보내자햄!"

                callback.onSuccess(message)

            } catch (e: Exception) {
                e.printStackTrace()
                callback.onError(e.message ?: "오류 발생")
            }
        }
    }
}