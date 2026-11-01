package com.example.data.local.entities

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.json.JSONArray
import org.json.JSONObject

enum class SpanType {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    COLOR,
    HIGHLIGHT
}

data class RichSpan(
    val start: Int,
    val end: Int,
    val type: SpanType,
    val value: String? = null // For COLOR and HIGHLIGHT (hex string, e.g. "#5B61D6")
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("start", start)
            put("end", end)
            put("type", type.name)
            if (value != null) put("value", value)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RichSpan {
            val start = json.optInt("start", 0)
            val end = json.optInt("end", 0)
            val typeStr = json.optString("type", SpanType.BOLD.name)
            val type = runCatching { SpanType.valueOf(typeStr) }.getOrDefault(SpanType.BOLD)
            val value = if (json.has("value")) json.optString("value") else null
            return RichSpan(start, end, type, value)
        }

        fun listToJson(spans: List<RichSpan>): String {
            val array = JSONArray()
            spans.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String?): List<RichSpan> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<RichSpan>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun toAnnotatedString(text: String, spans: List<RichSpan>): AnnotatedString {
            if (text.isEmpty()) return AnnotatedString("")
            val builder = AnnotatedString.Builder(text)
            val textLength = text.length

            for (span in spans) {
                val safeStart = span.start.coerceIn(0, textLength)
                val safeEnd = span.end.coerceIn(safeStart, textLength)
                if (safeStart >= safeEnd) continue

                val style = when (span.type) {
                    SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                    SpanType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    SpanType.COLOR -> {
                        val color = parseHexColor(span.value) ?: Color.Unspecified
                        SpanStyle(color = color)
                    }
                    SpanType.HIGHLIGHT -> {
                        val color = parseHexColor(span.value) ?: Color(0xFFFFF176)
                        SpanStyle(background = color)
                    }
                }
                builder.addStyle(style, safeStart, safeEnd)
            }
            return builder.toAnnotatedString()
        }

        private fun parseHexColor(hex: String?): Color? {
            if (hex.isNullOrBlank()) return null
            return try {
                val clean = hex.removePrefix("#")
                val parsed = when (clean.length) {
                    6 -> 0xFF000000 or clean.toLong(16)
                    8 -> clean.toLong(16)
                    else -> return null
                }
                Color(parsed)
            } catch (e: Exception) {
                null
            }
        }
    }
}
