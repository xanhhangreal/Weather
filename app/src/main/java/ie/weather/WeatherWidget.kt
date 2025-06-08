package ie.weather

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.*

class WeatherWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "WeatherWidget"
        private const val UPDATE_INTERVAL = 15 * 60 * 1000L // 15 phút
        private const val ACTION_UPDATE_WIDGET = "ie.weather.UPDATE_WIDGET"

        // Coroutine scope cho widget
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, " Widget được thêm vào màn hình chính")
        setupPeriodicUpdates(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "Gọi cập nhật widget thủ công/bởi hệ thống")
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_UPDATE_WIDGET -> {
                Log.d(TAG, " Nhận tín hiệu cập nhật từ alarm")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, WeatherWidget::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun setupPeriodicUpdates(context: Context) {
        val intent = Intent(context, WeatherWidget::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            // Cancel existing alarms first
            alarmManager.cancel(pendingIntent)

            // Check if we can schedule exact alarms (Android 12+)
            val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true // Trước Android 12 thì luôn được phép
            }

            if (canScheduleExactAlarms) {
                // Set exact alarm nếu có quyền
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + UPDATE_INTERVAL,
                        pendingIntent
                    )
                    Log.d(TAG, "Đã đặt exact alarm cập nhật sau ${UPDATE_INTERVAL / 60000}p")
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis(),
                        UPDATE_INTERVAL,
                        pendingIntent
                    )
                    Log.d(TAG, "Đã đặt repeating alarm cập nhật mỗi ${UPDATE_INTERVAL / 60000}p")
                }
            } else {
                // Fallback: sử dụng inexact alarm
                Log.w(TAG, " Không có quyền exact alarm, sử dụng inexact alarm")
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    UPDATE_INTERVAL,
                    pendingIntent
                )
                Log.d(TAG, " Đã đặt inexact alarm cập nhật khoảng ${UPDATE_INTERVAL / 60000}p")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, " SecurityException khi đặt alarm: ${e.message}")
            // Fallback với inexact alarm
            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    UPDATE_INTERVAL,
                    pendingIntent
                )
                Log.d(TAG, "Fallback: Đã đặt inexact alarm")
            } catch (e2: Exception) {
                Log.e(TAG, "Lỗi khi đặt fallback alarm: ${e2.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, " Lỗi tổng quát khi đặt alarm: ${e.message}")
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.weather_widget)

        // Get weather data from SharedPreferences
        val weatherData = getWeatherData(context)

        Log.d(TAG, " Đang cập nhật UI: ${weatherData.city} - ${weatherData.temp} - icon: ${weatherData.iconCode}")

        // Update text views
        views.setTextViewText(R.id.widget_temp, weatherData.temp)
        views.setTextViewText(R.id.widget_city, weatherData.city)

        // Set click intent to open main activity
        setupClickIntent(context, views)

        // Update widget first with text data
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Load weather icon asynchronously
        loadWeatherIcon(context, appWidgetManager, appWidgetId, views, weatherData.iconCode)
    }

    private fun getWeatherData(context: Context): WeatherData {
        val sharedPref = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        return WeatherData(
            temp = sharedPref.getString("temp", "--°C") ?: "--°C",
            city = sharedPref.getString("city", "City") ?: "City",
            iconCode = sharedPref.getString("icon", "01d") ?: "01d"
        )
    }

    private fun setupClickIntent(context: Context, views: RemoteViews) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        views.setOnClickPendingIntent(R.id.widget_temp, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_city, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_icon, pendingIntent)
    }

    private fun loadWeatherIcon(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews,
        iconCode: String
    ) {
        widgetScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadIconBitmap(context, iconCode)
                }

                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_icon, bitmap)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    Log.d(TAG, " Icon loaded successfully")
                } else {
                    Log.w(TAG, " Failed to load icon, using default")
                    // Set default icon if available
                    // views.setImageViewResource(R.id.widget_icon, R.drawable.default_weather_icon)
                }
            } catch (e: Exception) {
                Log.e(TAG, " Error loading weather icon: ${e.message}")
                // Handle error gracefully - maybe set a default icon
            }
        }
    }

    private suspend fun loadIconBitmap(context: Context, iconCode: String): Bitmap? {
        return try {
            val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"

            Glide.with(context.applicationContext)
                .asBitmap()
                .load(iconUrl)
                .apply(RequestOptions().apply {
                    timeout(10000) // 10 second timeout
                    override(64, 64) // Resize for widget
                })
                .submit()
                .get()
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadIconBitmap: ${e.message}")
            null
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Widget bị gỡ bỏ - huỷ alarm và coroutines")

        // Cancel alarm
        cancelPeriodicUpdates(context)

        // Cancel all coroutines
        widgetScope.cancel()
    }

    private fun cancelPeriodicUpdates(context: Context) {
        val intent = Intent(context, WeatherWidget::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
        )

        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Đã huỷ alarm")
        }
    }

    // Data class để chứa thông tin thời tiết
    private data class WeatherData(
        val temp: String,
        val city: String,
        val iconCode: String
    )
}