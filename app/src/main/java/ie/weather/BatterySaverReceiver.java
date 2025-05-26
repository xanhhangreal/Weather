package ie.weather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class BatterySaverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BatterySaver", "🔋 Tiết kiệm pin: " );
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) return;

        boolean isSaverOn = powerManager.isPowerSaveMode();
        Log.d("BatterySaver", "🔋 Tiết kiệm pin: " + isSaverOn);

        if (isSaverOn) {
            // ❌ Tắt cập nhật widget (huỷ alarm)
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(context, WeatherReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d("BatterySaver", "⛔ Đã huỷ alarm cập nhật widget");
            }
        } else {
            try {
                WeatherReminderReceiver.setAlarmIfNeeded(context);
            } catch (Exception e) {
                Log.e("BatterySaver", "❌ Lỗi khi khôi phục alarm: " + e.getMessage());
            }

        }
    }
}
