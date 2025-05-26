package ie.weather

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WeatherWidget : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("WeatherWidget", "📌 Widget được thêm vào màn hình chính")
        val intent = Intent(context, WeatherWidgetUpdater::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMillis = 15 * 60 * 1000L // 15 phút

        //val intervalMillis = 20 * 1000L // 20a
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            intervalMillis,
            pendingIntent
        )
        Log.d("WeatherWidget", "⏰ Đã đặt alarm cập nhật mỗi 15p")
    }
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("WeatherWidget", "🔄 Gọi cập nhật widget thủ công/bởi hệ thống")
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {

        val views = RemoteViews(context.packageName, R.layout.weather_widget)

        val sharedPref = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val temp = sharedPref.getString("temp", "--°C")
        val city = sharedPref.getString("city", "City")
        val iconCode = sharedPref.getString("icon", "01d")
        Log.d("WeatherWidget", "🧊 Đang cập nhật UI: $city - $temp - icon: $iconCode")
        views.setTextViewText(R.id.widget_temp, temp)
        views.setTextViewText(R.id.widget_city, city)

        // Load icon từ internet qua URL
        val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"
        // Load icon trên background thread
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load("https://openweathermap.org/img/w/$iconCode.png")
                    .submit()
                    .get()

                views.setImageViewBitmap(R.id.widget_icon, bitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                Log.e("Widget", "Error loading icon", e)
            }
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_temp, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d("WeatherWidget", "❌ Widget bị gỡ bỏ - huỷ alarm")
        val intent = Intent(context, WeatherWidgetUpdater::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}