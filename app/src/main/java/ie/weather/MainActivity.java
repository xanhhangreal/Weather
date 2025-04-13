package ie.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;

import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

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
                        imgBG.setImageResource(R.drawable.night);
                    }else {
                        imgBG.setImageResource(R.drawable.day);
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
