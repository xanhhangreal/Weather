package ie.weather;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private ProgressBar progressBar;
    private RelativeLayout rLHome;
    private TextView textCityName, textTemp, textConditions, textWindSpeed, textLastTime;
    private RecyclerView rvWeather, rvFavs;
    private ImageView imgBG, imgSearch, imgWeather, imgRefresh;
    private TextInputEditText editCityName;
    private ArrayList<WeatherModel> arr;
    private ArrayList<FavCityModel> favArr;
    private WeatherModelAdapter weatherModelAdapter;
    private FavCityAdapter favCityAdapter;
    private int PERMISSION_CODE = 1;
    private String cityName;
    //    double lat, lon;
    double lat, lon;
    private LocationManager locationManager;
    private Location location;
    String apiKey = "485ec85551ded720ef8f68eccf7f96e0";
    String apiKeyGoogle = "AIzaSyD-i0ZFOUsYRlsalHGg8YX2qUBhcicFzF4";
    String[] saveKey = {
            "CurrentWeatherData",
            "ForecastWeatherData",
            "New York",
            "Singapore",
            "Mumbai",
            "Delhi",
            "Sydney",
            "Melbourne"
    };
    Spinner spinner;
    Button btnSuggest;
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentLat, currentLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.pBarLoading);
        rLHome = findViewById(R.id.RLHome);
        textCityName = findViewById(R.id.textCityName);
        textTemp = findViewById(R.id.textTemp);
        textConditions = findViewById(R.id.textConditions);
        rvWeather = findViewById(R.id.rvWeather);
        rvFavs = findViewById(R.id.rvFavs);
        imgBG = findViewById(R.id.imgBG);
        imgWeather = findViewById(R.id.imgWeather);
        imgSearch = findViewById(R.id.imgSearch);
        imgRefresh = findViewById(R.id.imgRefresh);
        editCityName = findViewById(R.id.editCityName);
        textWindSpeed = findViewById(R.id.textWindSpeed);
        textLastTime = findViewById(R.id.textLastTime);
        Button btnShare = findViewById(R.id.btnShareWeather);
        btnShare.setOnClickListener(view -> shareWeatherInfo());
        Button btnMap = findViewById(R.id.btnWeatherMap);
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WeatherMapActivity.class);
            startActivity(intent);
        });
        arr = new ArrayList<>();
        favArr = new ArrayList<>();

        ///  Nếu đã có quyền truy cập vị trí thì bỏ qua else yêu ầu quyền truy cập vị trí
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            Toast.makeText(this,"Fetching Last known location",Toast.LENGTH_SHORT).show(); // visit cuối cùng
            lat = location.getLatitude();
            lon = location.getLongitude();
        }
        getCurrentWeather(lat, lon);
        getForecastWeather(lat, lon);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        weatherModelAdapter = new WeatherModelAdapter(this, arr);
        rvWeather.setAdapter(weatherModelAdapter);

        favCityAdapter = new FavCityAdapter(this,favArr);
        rvFavs.setAdapter(favCityAdapter);

        for(int i=2;i<saveKey.length;i++){
            getFavCoord(saveKey[i],i);
        }

        imgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isInternetAvailable = NetworkCheck.isNetworkAvailable(getApplicationContext());
                if(isInternetAvailable){
                    String city = editCityName.getText().toString();
                    if (city.equals("")) {
                        Toast.makeText(MainActivity.this, "Please enter city Name", Toast.LENGTH_SHORT).show();
                        editCityName.requestFocus();
                        editCityName.setError("Please Enter City Name");
                    }else{
                        updateWeather(city);
                    }
                }else {
                    Toast.makeText(MainActivity.this,"No Internet Connection",Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isInternetAvailable = NetworkCheck.isNetworkAvailable(getApplicationContext());
                if(isInternetAvailable){
                    Toast.makeText(MainActivity.this,"Refreshing...",Toast.LENGTH_SHORT).show();
                    getCurrentWeather(lat,lon);
                    getForecastWeather(lat,lon);
                    favArr.clear();
                    for(int i=2;i<saveKey.length;i++){
                        getFavCoord(saveKey[i],i);
                    }
                }else {
                    Toast.makeText(MainActivity.this,"No Internet Connection",Toast.LENGTH_SHORT).show();
                }
            }
        });
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        spinner = findViewById(R.id.spinnerPlaceType);
        btnSuggest = findViewById(R.id.btnSuggestPlaces);

        btnSuggest.setOnClickListener(v -> {
//            String placeType = spinner.getSelectedItem().toString();
//            getLastLocationAndSuggest(placeType);
            ArrayList<SuggestedPlace> mockPlaces = new ArrayList<>();
            mockPlaces.add(new SuggestedPlace("Highlands Coffee Nguyễn Trãi", 29.5, "Clear", "https://openweathermap.org/img/wn/01d@2x.png"));
            mockPlaces.add(new SuggestedPlace("The Coffee House Vincom", 31.0, "Clouds", "https://openweathermap.org/img/wn/03d@2x.png"));
            mockPlaces.add(new SuggestedPlace("Starbucks Keangnam", 28.2, "Sunny", "https://openweathermap.org/img/wn/01d@2x.png"));
            mockPlaces.add(new SuggestedPlace("Aha Cafe Trung Kính", 27.4, "Partly Cloudy", "https://openweathermap.org/img/wn/02d@2x.png"));
            mockPlaces.add(new SuggestedPlace("Trill Rooftop Cafe", 30.1, "Few Clouds", "https://openweathermap.org/img/wn/02d@2x.png"));

            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, PlaceSuggestionFragment.newInstance(mockPlaces))
                    .addToBackStack(null)
                    .commit();
        });
        Button btnToggle = findViewById(R.id.btnToggleFeatures);
        LinearLayout layoutFeatures = findViewById(R.id.layoutFeatures);

        btnToggle.setOnClickListener(v -> {
            if (layoutFeatures.getVisibility() == View.VISIBLE) {
                layoutFeatures.setVisibility(View.GONE);
            } else {
                layoutFeatures.setVisibility(View.VISIBLE);
            }
        });

    }
    private void getLastLocationAndSuggest(String type) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                PlaceSuggester suggester = new PlaceSuggester(
                        MainActivity.this,
                        apiKeyGoogle,
                        apiKey
                );
                String selectedType = spinner.getSelectedItem().toString();
                suggester.suggestNearby(location, selectedType);
            }
        });
    }
    private void scheduleDailyReminder() {
        // Thông báo vào lúc 7h sáng.
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(this, WeatherReminderReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
//
//        // Hẹn lúc 7h sáng ngày kế tiếp
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.HOUR_OF_DAY, 7);
//        calendar.set(Calendar.MINUTE, 0);
//        calendar.set(Calendar.SECOND, 0);
//
//        // Nếu giờ đã qua thì hẹn cho ngày hôm sau
//        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
//            calendar.add(Calendar.DAY_OF_YEAR, 1);
//        }
//
//        long triggerTime = calendar.getTimeInMillis();
//
//        // Android 12+ cần kiểm tra quyền trước
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (!alarmManager.canScheduleExactAlarms()) {
//                Log.d("Alarm", "Chưa có quyền SCHEDULE_EXACT_ALARM");
//                return;
//            }
//        }
//
//        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
//        Log.d("Alarm", "Alarm đã đặt lúc: " + new Date(triggerTime));
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, WeatherReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = System.currentTimeMillis() + 10 * 1000; // sau 10s

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(i);
                return;
            }
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        Log.d("Reminder", "🚀 Đặt lần đầu sau 10s tại " + new Date(triggerTime));
    }
    private void shareWeatherInfo() {
        Bitmap bmp = createWeatherSnapshot();
        shareBitmap(bmp);
    }
    private String getRandomQuote() {
        String[] quotes = {
                "🌞 Trời đẹp thế này, ai rủ đi chơi không?",
                "☁️ Thời tiết thế này chỉ muốn ở nhà ôm gối xem phim 🍿",
                "🌧️ Mưa thì mưa, mình vẫn chill như thường 😎",
                "❄️ Gió lạnh đầu mùa, nhớ mặc ấm nha bạn!",
                "🔥 Nóng thế này có ai bán kem không?"
        };
        int randomIndex = new Random().nextInt(quotes.length);
        return quotes[randomIndex];
    }
    private void shareBitmap(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "weather_snapshot.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            String content = getRandomQuote();
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ thời tiết"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private Bitmap createWeatherSnapshot() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.weather_share_snapshot, null);

        ImageView bgView = view.findViewById(R.id.snapshot_background);
        ImageView iconView = view.findViewById(R.id.snapshot_icon);
        TextView cityView = view.findViewById(R.id.snapshot_city);
        TextView tempView = view.findViewById(R.id.snapshot_temp);

        SharedPreferences sharedPreferences = getSharedPreferences("weather_prefs", MODE_PRIVATE);
        String city = sharedPreferences.getString("city", "City");
        String temp = sharedPreferences.getString("temp", "--°C");
        String condition = textConditions.getText().toString();
        String iconName = sharedPreferences.getString("icon_name", "ic_weather_clear");

        cityView.setText(city);
        tempView.setText(temp + " – " + condition);
        int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
        iconView.setImageResource(iconResId);


        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour < 6 || hour >= 18);

        int bgResId = isNight ? R.drawable.night_image : R.drawable.day_image;
        bgView.setImageResource(bgResId);

        // Tạo ảnh bitmap từ view
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }
    private void updateWeather(String city) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                +city
                +"&appid="
                +apiKey
                +"&units=metric";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getString("cod").equals("404")){
                        Toast.makeText(MainActivity.this, "Please enter correct city name", Toast.LENGTH_LONG).show();
                    }else {
                        textLastTime.setText(getCurrentTime());
                        lon = response.getJSONObject("coord").getDouble("lon");
                        lat = response.getJSONObject("coord").getDouble("lat");
                        getCurrentWeather(lat, lon);
                        getForecastWeather(lat, lon);
                    }
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, "Please enter correct city name", Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error","city not found");
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private String getCurrentTime() {
        LocalDateTime dateTime = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return dateTime.format(formatter);
        }
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date());
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission Granted...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied...", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void getCurrentWeather(double lat, double lon) {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        String urlCurrent = "https://api.openweathermap.org/data/2.5/weather?lat="
                + lat
                + "&lon="
                + lon
                + "&appid="
                + apiKey
                + "&units=metric";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlCurrent, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                textLastTime.setText(getCurrentTime());
                saveLastResponse(response,0);
                progressBar.setVisibility(View.GONE);
                rLHome.setVisibility(View.VISIBLE);
                updateCurrentWeather(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error",error.getMessage());
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void updateCurrentWeather(JSONObject response) {
        try{
            String city = response.getString("name");
            textCityName.setText(city);

            String temperature = response.getJSONObject("main").getString("temp");
            textTemp.setText(temperature + "°C");

            String img = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("icon");
            Picasso.get().load("https://openweathermap.org/img/w/" + img + ".png").into(imgWeather);

            String condition = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");
            textConditions.setText(condition);

            double wSpeed = response.getJSONObject("wind")
                    .getDouble("speed");
            textWindSpeed.setText(""+wSpeed+"Km/h");

            SharedPreferences sharedPreferences = getSharedPreferences("weather_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("temp", temperature + "°C");
            editor.putString("city", city);
            editor.putString("icon", img);
            editor.apply();
            SharedPreferences prefs = getSharedPreferences("weather_prefs", MODE_PRIVATE);
            boolean isScheduled = prefs.getBoolean("reminder_scheduled", false);
            Log.e("Remind: ","isScheduled");
            if (!isScheduled) {

                scheduleDailyReminder();
                prefs.edit().putBoolean("reminder_scheduled", true).apply();
            }


        }catch (Exception e){
            Log.d("Update Res",e.getMessage());
        }

    }

    private void getForecastWeather(double lat, double lon) {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        String urlForecast = "https://api.openweathermap.org/data/2.5/forecast?lat="
                + lat
                + "&lon="
                + lon
                + "&appid="
                + apiKey
                + "&units=metric";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlForecast, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                textLastTime.setText(getCurrentTime());
                saveLastResponse(response,1);
                arr.clear();
                updateForecastWeather(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error",error.getMessage());
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void updateForecastWeather(JSONObject response) {
        try{
            int loop = response.getInt("cnt");
            JSONArray forecast = response.getJSONArray("list");

            for (int i = 0; i < loop; i += 1) {
                String time = forecast.getJSONObject(i)
                        .getString("dt_txt");
                double temp = forecast.getJSONObject(i)
                        .getJSONObject("main")
                        .getDouble("temp");
                String condition = forecast.getJSONObject(i)
                        .getJSONArray("weather")
                        .getJSONObject(0)
                        .getString("icon");
                double wSpeed = forecast.getJSONObject(i)
                        .getJSONObject("wind")
                        .getDouble("speed");
                String pod = forecast.getJSONObject(i)
                        .getJSONObject("sys")
                        .getString("pod");
                arr.add(new WeatherModel(temp, condition, wSpeed,time,pod));
                if(i==0){
                    if(pod.equals("n")){
                        imgBG.setImageResource(R.drawable.night_image);
                    }else {
                        imgBG.setImageResource(R.drawable.day_image);
                    }
                }
            }
            weatherModelAdapter.notifyDataSetChanged();
        }catch (Exception e){
            Log.d("Update Res",e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location loca) {
        location = loca;

        lat = location.getLatitude();
        lon = location.getLongitude();

        Log.d("Latitude", String.valueOf(lat));
        Log.d("Longitude", String.valueOf(lon));

        // Stop receiving location updates if needed
        locationManager.removeUpdates(this);
    }

    private void saveLastResponse(JSONObject res, int time) {

        SharedPreferences sharedPreferences = getSharedPreferences("WeatherData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try{
            String save = res.toString();
            editor.putString(saveKey[time], save);
            editor.putString("Time", getCurrentTime());
            editor.apply();
        }catch (Exception e){
            Log.d("Save Res",e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (int i=0;i<saveKey.length;i++) {
            retrieveLastResponse(i);
        }
    }

    private void retrieveLastResponse(int time) {
        SharedPreferences sharedPreferences = getSharedPreferences("WeatherData", Context.MODE_PRIVATE);
        String weatherData = sharedPreferences.getString(saveKey[time], "");
        String dateTime = sharedPreferences.getString("Time", "");
        textLastTime.setText(dateTime);
        try {
            JSONObject response = new JSONObject(weatherData);
            switch (time){
                case 0:
                    updateCurrentWeather(response);
                    break;
                case 1:
                    updateForecastWeather(response);
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    updateFavWeather(response);
                    break;
                default:
                    Log.d("retrieveLastResponse","Wrong time");
                    break;
            }
        }catch (Exception ex){
            Log.d("Load Res",ex.getMessage());
        }
    }

    public void getFavCoord(String name,int i){
        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                +name
                +"&appid="
                +apiKey
                +"&units=metric";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    textLastTime.setText(getCurrentTime());
                    double lon = response.getJSONObject("coord").getDouble("lon");
                    double lat = response.getJSONObject("coord").getDouble("lat");
                    getFavWeather(lat, lon, i);
                } catch (Exception ex) {
                    Log.d("getFavCoord","Favorite City Fetch Failed");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error",error.getMessage());
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void getFavWeather(double lat, double lon, int i) {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        String urlCurrent = "https://api.openweathermap.org/data/2.5/weather?lat="
                + lat
                + "&lon="
                + lon
                + "&appid="
                + apiKey
                + "&units=metric";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlCurrent, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                saveLastResponse(response,i);
                updateFavWeather(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error",error.getMessage());
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void updateFavWeather(JSONObject response) {
        try{
            String city = response.getString("name");
            String temperature = response.getJSONObject("main").getString("temp");
            String img = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("icon");
            String condition = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");
            double wSpeed = response.getJSONObject("wind")
                    .getDouble("speed");
            favArr.add(new FavCityModel(0,city,temperature,condition,String.valueOf(wSpeed),img));
        }catch (Exception e){
            Log.d("Update Res",e.getMessage());
        }
        favCityAdapter.notifyDataSetChanged();
    }
}
