package com.leemon41k.widgetai.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WidgetApi {
    // Replace with the deployed HTTPS backend URL. Provider secrets stay server-side.
    private const val BASE_URL = "https://api.widgetai.example"

    suspend fun generate(prompt: String, device: DeviceSnapshot): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val connection = (URL("$BASE_URL/v1/widgets/generate").openConnection() as HttpURLConnection)
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            try {
                val deviceJson = JSONObject()
                    .put("batteryPercent", device.batteryPercent ?: JSONObject.NULL)
                    .put("charging", device.charging)
                    .put("time", device.time)
                    .put("networkConnected", device.networkConnected)
                val body = JSONObject().put("prompt", prompt).put("device", deviceJson)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("HTTP $code: ${response.take(160)}")
                JSONObject(response).optString("message",
                    "Запрос принят сервером. Подключим рендеринг виджета следующим этапом.")
            } finally {
                connection.disconnect()
            }
        }
}
