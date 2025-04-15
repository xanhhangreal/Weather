package ie.weather;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PlaceSuggester {

    private final Context context;
    private final String placesApiKey;
    private final String weatherApiKey;

    public PlaceSuggester(Context context, String placesApiKey, String weatherApiKey) {
        this.context = context;
        this.placesApiKey = placesApiKey;
        this.weatherApiKey = weatherApiKey;
    }

    public void suggestNearby(Location userLocation, String type) {
        double lat = userLocation.getLatitude();
        double lon = userLocation.getLongitude();

        Log.d("PlaceSuggester", "📍 Gợi ý địa điểm xung quanh: type = " + type);
        Log.d("PlaceSuggester", "📍 Tọa độ hiện tại: " + lat + ", " + lon);

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + lat + "," + lon +
                "&radius=10000&type=" + type +
                "&key=" + placesApiKey;

        new Thread(() -> {
            ArrayList<SuggestedPlace> suggestions = new ArrayList<>();

            try {
                Log.d("PlaceSuggester", "🔗 Gọi Google Places API...");
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONArray results = new JSONObject(sb.toString()).getJSONArray("results");
                int count = Math.min(5, results.length());
                Log.d("PlaceSuggester", "✅ Lấy được " + count + " địa điểm");

                ExecutorService executor = Executors.newFixedThreadPool(5);
                ArrayList<Future<SuggestedPlace>> futures = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    JSONObject obj = results.getJSONObject(i);
                    String name = obj.getString("name");
                    JSONObject location = obj.getJSONObject("geometry").getJSONObject("location");
                    double placeLat = location.getDouble("lat");
                    double placeLon = location.getDouble("lng");

                    String placeInfo = name + " (" + placeLat + ", " + placeLon + ")";
                    Log.d("PlaceSuggester", "🌍 Gọi thời tiết cho: " + placeInfo);

                    futures.add(executor.submit(() -> getWeather(placeLat, placeLon, name)));
                }

                for (Future<SuggestedPlace> future : futures) {
                    try {
                        SuggestedPlace sp = future.get(5, TimeUnit.SECONDS);
                        if (sp != null) {
                            suggestions.add(sp);
                            Log.d("PlaceSuggester", "✅ Địa điểm được chọn: " + sp.name);
                        }
                    } catch (Exception e) {
                        Log.e("PlaceSuggester", "⛔ Lỗi hoặc timeout khi lấy thời tiết: " + e.getMessage());
                    }
                }

                executor.shutdown();

            } catch (Exception e) {
                Log.e("PlaceSuggester", "❌ Lỗi gọi Google Places API: " + e.getMessage());
            }

            Log.d("PlaceSuggester", "🎯 Tổng địa điểm đẹp: " + suggestions.size());

            new Handler(Looper.getMainLooper()).post(() -> {
                if (suggestions.isEmpty()) {
                    Log.w("PlaceSuggester", "⚠️ Không tìm thấy địa điểm phù hợp");
                }
                showFragment(suggestions);
            });
        }).start();
    }

    private SuggestedPlace getWeather(double lat, double lon, String name) {
//        try {
//            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
//                    "&lon=" + lon + "&units=metric&appid=" + weatherApiKey;
//
//            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
//            conn.connect();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            StringBuilder sb = new StringBuilder(); String line;
//            while ((line = reader.readLine()) != null) sb.append(line);
//
//            JSONObject res = new JSONObject(sb.toString());
//            double temp = res.getJSONObject("main").getDouble("temp");
//            String condition = res.getJSONArray("weather").getJSONObject(0).getString("main");
//            String icon = res.getJSONArray("weather").getJSONObject(0).getString("icon");
//
//            Log.d("PlaceSuggester", "⛅ " + name + ": " + temp + "°C, " + condition);
//
//            if (temp >= 20 && temp <= 32 && !condition.toLowerCase().contains("rain")) {
//                String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@2x.png";
//                return new SuggestedPlace(name, temp, condition, iconUrl);
//            } else {
//                Log.d("PlaceSuggester", "❌ Bỏ qua " + name + " do thời tiết không phù hợp");
//            }
//
//        } catch (Exception e) {
//            Log.e("PlaceSuggester", "Lỗi getWeather(" + name + "): " + e.getMessage());
//        }
//
//        return null;
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                    "&lon=" + lon + "&units=metric&appid=" + weatherApiKey;

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject res = new JSONObject(sb.toString());
            double temp = res.getJSONObject("main").getDouble("temp");
            String condition = res.getJSONArray("weather").getJSONObject(0).getString("main");
            String icon = res.getJSONArray("weather").getJSONObject(0).getString("icon");

            String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@2x.png";

            // ✅ Bỏ điều kiện lọc thời tiết – luôn trả về địa điểm
            return new SuggestedPlace(name, temp, condition, iconUrl);

        } catch (Exception e) {
            Log.e("PlaceSuggester", "getWeather() failed: " + e.getMessage());
        }

        return null;
    }

    private void showFragment(ArrayList<SuggestedPlace> suggestions) {
        if (suggestions.isEmpty()) {
            Toast.makeText(context, "Không tìm thấy địa điểm thời tiết đẹp gần bạn!", Toast.LENGTH_SHORT).show();
            return; // ⛔ Không mở fragment rỗng nữa
        }

        if (!(context instanceof AppCompatActivity)) return;

        AppCompatActivity activity = (AppCompatActivity) context;
        activity.getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, PlaceSuggestionFragment.newInstance(suggestions))
                .addToBackStack(null)
                .commit();
    }
}
