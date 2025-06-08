package ie.weather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class BatterySaverReceiver extends BroadcastReceiver {
    private static final String TAG = "BatterySaver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;

        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm == null) return;

            boolean isSaverOn = pm.isPowerSaveMode();
            Log.d(TAG, "🔋 Tiết kiệm pin: " + isSaverOn);

            if (isSaverOn) {
                cancelAlarm(context);
            } else {
                WeatherReminderReceiver.setAlarmIfNeeded(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Lỗi: " + e.getMessage());
        }
    }

    private void cancelAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, WeatherReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);

        if (pi != null) {
            am.cancel(pi);
            Log.d(TAG, "⛔ Đã huỷ alarm");
        }
    }
}