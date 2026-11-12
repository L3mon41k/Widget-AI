package com.leemon41k.widgetai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leemon41k.widgetai.data.DeviceData
import com.leemon41k.widgetai.data.WidgetApi
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF090909), surface = Color(0xFF171717), primary = Color.White
            )) { WidgetHome(this) }
        }
    }
}

@Composable
private fun WidgetHome(context: Context) {
    var prompt by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Опиши виджет, который хочешь создать") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF090909)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("WidgetAI", color = Color.White, fontSize = 30.sp)
        Text("Твои идеи. Твои виджеты.", color = Color.LightGray, fontSize = 14.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
            shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Создать виджет", color = Color.White, fontSize = 19.sp)
                OutlinedTextField(value = prompt, onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например: часы и заряд батареи") }, minLines = 3)
                Button(onClick = {
                    if (prompt.isBlank()) message = "Сначала опиши свой виджет"
                    else if (!hasInternet(context)) message = "Нет подключения к интернету"
                    else scope.launch {
                        busy = true
                        message = try {
                            WidgetApi.generate(prompt, DeviceData.read(context))
                        } catch (e: Exception) {
                            "Ошибка сервера: ${e.message ?: "неизвестная ошибка"}"
                        }
                        busy = false
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Создать с помощью ИИ")
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))) {
            Column(Modifier.padding(18.dp)) {
                Text("Состояние", color = Color.White, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                Text(message, color = Color.LightGray)
            }
        }
        Text("WidgetAI · 0.1.0 alpha", color = Color.Gray, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

private fun hasInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
