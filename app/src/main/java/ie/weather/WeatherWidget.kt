package ie.weather

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

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
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
}