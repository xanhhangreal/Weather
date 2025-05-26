package ie.weather

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WeatherWidgetUpdater : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WeatherWidgetUpdater", "📥 Alarm nhận được - bắt đầu cập nhật dữ liệu")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Lấy toạ độ hoặc tên thành phố từ SharedPreferences
                val sharedPref = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                val city = sharedPref.getString("city", "Hanoi") ?: "Hanoi"

                // Gọi API OpenWeatherMap
                val apiKey = "485ec85551ded720ef8f68eccf7f96e0" //
                val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"
                Log.d("WeatherWidgetUpdater", "🌐 Gọi API: $url")
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.peekBody(Long.MAX_VALUE).string()

                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    val temp = json.getJSONObject("main").getDouble("temp").toInt().toString() + "°C"
                    val icon = json.getJSONArray("weather")
                        .getJSONObject(0).getString("icon")

                    // Lưu dữ liệu vào SharedPreferences
                    sharedPref.edit()
                        .putString("temp", temp)
                        .putString("icon", icon)
                        .apply()
                    Log.d("WeatherWidgetUpdater", "✅ Lưu SharedPreferences: temp=$temp, icon=$icon")
                    // Gọi cập nhật lại widget
                    Log.d("WeatherWidgetUpdater", "🔄 Gọi lại update Widget qua AppWidgetManager")
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidget::class.java))
                    WeatherWidget().onUpdate(context, manager, ids)
                }
            } catch (e: Exception) {
                Log.e("WeatherWidgetUpdater", "Failed to update weather", e)
            }
        }
    }
}
