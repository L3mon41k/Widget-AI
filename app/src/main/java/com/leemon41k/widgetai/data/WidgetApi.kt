package com.leemon41k.widgetai.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WidgetElement(
    val type: String, val label: String, val value: String,
    val size: Int, val color: String
)

data class WidgetDefinition(
    val name: String, val description: String,
    val refreshSeconds: Int, val elements: List<WidgetElement>
) {
    fun toJson(): JSONObject = JSONObject().put("name", name).put("description", description)
        .put("refreshSeconds", refreshSeconds).put("elements", JSONArray().also { array ->
            elements.forEach { e -> array.put(JSONObject().put("type", e.type).put("label", e.label)
                .put("value", e.value).put("size", e.size).put("color", e.color)) }
        })

    companion object {
        fun fromJson(json: JSONObject): WidgetDefinition {
            val array = json.optJSONArray("elements") ?: JSONArray()
            val elements = (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                WidgetElement(
                    item.optString("type", "text").takeIf { it in setOf("text", "battery", "clock", "progress") } ?: "text",
                    item.optString("label").take(80), item.optString("value").take(120),
                    item.optInt("size", 14).coerceIn(10, 32),
                    item.optString("color", "#FFFFFF").takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}$")) } ?: "#FFFFFF"
                )
            }.take(16)
            return WidgetDefinition(json.optString("name", "Новый виджет").take(80),
                json.optString("description").take(240), json.optInt("refreshSeconds", 900).coerceIn(60, 3600), elements)
        }
    }
}

data class WidgetGenerationResult(val message: String, val widget: WidgetDefinition)

object WidgetApi {
    suspend fun generate(baseUrl: String, prompt: String, device: DeviceSnapshot): WidgetGenerationResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val connection = URL("${baseUrl.trimEnd('/')}/v1/widgets/generate").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 45000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            try {
                val deviceJson = JSONObject().put("batteryPercent", device.batteryPercent ?: JSONObject.NULL)
                    .put("charging", device.charging).put("time", device.time)
                    .put("networkConnected", device.networkConnected)
                val body = JSONObject().put("prompt", prompt).put("device", deviceJson)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("HTTP $code: ${response.take(160)}")
                val json = JSONObject(response)
                val widgetJson = json.optJSONObject("widget") ?: error("Сервер не вернул описание виджета")
                WidgetGenerationResult(json.optString("message", "Виджет подготовлен"), WidgetDefinition.fromJson(widgetJson))
            } finally { connection.disconnect() }
        }
}
