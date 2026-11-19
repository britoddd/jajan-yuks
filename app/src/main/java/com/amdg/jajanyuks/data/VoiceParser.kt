package com.amdg.jajanyuks.data

import com.amdg.jajanyuks.domain.MAX_STATUS_LENGTH
import com.amdg.jajanyuks.domain.MenuItem
import com.amdg.jajanyuks.domain.VoiceUpdate
import com.amdg.jajanyuks.domain.parseVoiceUpdate
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
// Turns a seller's Indonesian sentence ("cilok tinggal lima, es teh habis") into menu changes.
// Gemini Developer API through Firebase AI Logic: no API key in the app, Firebase holds it.
class VoiceParser {
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        // Flash-Lite: fast and free-tier; plenty for pulling numbers out of one sentence.
        // (gemini-3.8-flash did not answer within 60 s on 2026-11-19.)
        modelName = "gemini-3.5-flash-lite",
        generationConfig = generationConfig {
            // A response schema makes Gemini answer with JSON in exactly this shape, never free text.
            responseMimeType = "application/json"
            responseSchema = Schema.obj(
                mapOf(
                    "stock" to Schema.array(Schema.obj(mapOf("itemId" to Schema.string(), "stock" to Schema.integer()))),
                    "statusText" to Schema.string(),
                ),
                optionalProperties = listOf("statusText"),
            )
            // Simple extraction needs no long "thinking"; minimal keeps the answer fast.
            thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.MINIMAL }
        },
        // A seller on the street shouldn't wait minutes: give up after 30 s and show "try again".
        requestOptions = RequestOptions(timeoutInMillis = 30_000),
    )

    // `suspend`: waits for the network without blocking the UI thread. Throws on network/AI errors.
    suspend fun parse(text: String, menu: List<MenuItem>): VoiceUpdate {
        val menuLines = menu.joinToString("\n") { "- id=${it.id}, name=${it.name}, current stock=${it.stock}" }
        val prompt = """
            You help an Indonesian street-food seller update their stall by voice.
            Menu:
            $menuLines

            The seller said: "$text"

            For each menu item the seller mentioned, return its NEW total stock in "stock"
            (e.g. "tinggal lima" = 5, "habis" = 0, "tambah 10" = current stock + 10).
            Use only ids from the menu. Leave out items that were not mentioned.
            If the seller describes where they are or what they are doing (e.g. "lagi di depan SD Harapan",
            "istirahat 10 menit"), put a short Indonesian status for buyers in "statusText"
            (at most $MAX_STATUS_LENGTH characters). Otherwise leave "statusText" out.
        """.trimIndent()
        val raw = model.generateContent(prompt).text ?: error("Empty Gemini response")
        return parseVoiceUpdate(raw, menu)
    }
}
