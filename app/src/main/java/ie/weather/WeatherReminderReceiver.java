package ie.weather;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class WeatherReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Reminder", "Receiver chạy!");

        // 🔹 Lấy dữ liệu thời tiết từ SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
        String tempStr = prefs.getString("temp", "0").replace("°C", "").trim();
        String condition = prefs.getString("condition", "Không rõ");

        float temp = 0f;
        try {
            temp = Float.parseFloat(tempStr);
        } catch (NumberFormatException e) {
            Log.e("Reminder", "Lỗi chuyển đổi nhiệt độ: " + tempStr);
        }

        // 🔹 Tạo nội dung thông báo theo điều kiện
        String message = "🌤️ Chúc bạn một ngày tốt lành!";
        if (condition.toLowerCase().contains("rain")) {
            message = "☔ Hôm nay có mưa. Đừng quên mang ô!";
        } else if (temp < 18) {
            message = "🥶 Trời lạnh đấy. Nhớ mặc ấm nha!";
        } else if (temp > 33) {
            message = "🔥 Nóng thế này nhớ uống nhiều nước nhé!";
        }

        // 🔹 Tạo thông báo
        String channelId = "weather_channel";
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Thông báo thời tiết",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_compass)  // hoặc ic_weather_clear
                .setContentTitle("⏰ Nhắc nhở thời tiết")
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify(1002, builder.build());

        // 🔁 Đặt lại báo thức cho ngày hôm sau lúc 7h
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        Intent newIntent = new Intent(context, WeatherReminderReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_IMMUTABLE);
//
//        Calendar next = Calendar.getInstance();
//        next.add(Calendar.DAY_OF_YEAR, 1);
//        next.set(Calendar.HOUR_OF_DAY, 7);
//        next.set(Calendar.MINUTE, 0);
//        next.set(Calendar.SECOND, 0);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (alarmManager.canScheduleExactAlarms()) {
//                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
//            }
//        } else {
//            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
//        }
//
//        Log.d("Reminder", "Đã lên lịch lại cho: " + next.getTime());
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent newIntent = new Intent(context, WeatherReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_IMMUTABLE);

        Calendar next = Calendar.getInstance();
        next.add(Calendar.SECOND, 10); // 💥 Lặp lại sau 10s

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
        }

        Log.d("Reminder", "⏱️ Lặp lại sau 10s tại: " + next.getTime());
    }
}
