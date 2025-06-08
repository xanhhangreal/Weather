package ie.weather;

import android.content.Context;
import android.util.Log;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * WeatherFetcher: Optimized class for fetching weather data from OpenWeatherMap API
 * Features: Singleton pattern, better error handling, caching, resource management
 */
public class WeatherFetcher {

    private static final String TAG = "WeatherFetcher";
    private static final String API_KEY = "8425462840818e1c815aa16663d1fedb"; // Consider moving to BuildConfig
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String ONE_CALL_URL = "https://api.openweathermap.org/data/2.5/onecall";

    // Singleton instance
    private static WeatherFetcher instance;
    private RequestQueue requestQueue;
    private final Context context;

    // Cache for coordinates to avoid repeated geocoding
    private static class CityCoords {
        final double lat, lon;
        final long timestamp;

        CityCoords(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TimeUnit.HOURS.toMillis(1);
        }
    }

    private CityCoords lastCityCoords;
    private String lastCityName;

    // Wind directions in Vietnamese
    private static final String[] WIND_DIRECTIONS = {
            "bắc", "đông bắc", "đông", "đông nam",
            "nam", "tây nam", "tây", "tây bắc"
    };

    // Date formatters
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    static {
        TIME_FORMAT.setTimeZone(TimeZone.getDefault());
        DATE_FORMAT.setTimeZone(TimeZone.getDefault());
    }

    // Callback interface
    public interface WeatherCallback {
        void onSuccess(String result);
        void onFailure(String errorMessage);
    }

    // Weather data models
    public static class CurrentWeather {
        public final String cityName;
        public final double temperature;
        public final double feelsLike;
        public final int humidity;
        public final int pressure;
        public final String description;
        public final double windSpeed;
        public final String windDirection;
        public final String updateTime;

        public CurrentWeather(String cityName, double temperature, double feelsLike,
                              int humidity, int pressure, String description,
                              double windSpeed, String windDirection, String updateTime) {
            this.cityName = cityName;
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.pressure = pressure;
            this.description = description;
            this.windSpeed = windSpeed;
            this.windDirection = windDirection;
            this.updateTime = updateTime;
        }
    }

    public static class ForecastWeather {
        public final String date;
        public final double dayTemp;
        public final double minTemp;
        public final double maxTemp;
        public final double feelsLike;
        public final String description;
        public final int humidity;
        public final int pressure;
        public final double windSpeed;
        public final String windDirection;

        public ForecastWeather(String date, double dayTemp, double minTemp, double maxTemp,
                               double feelsLike, String description, int humidity, int pressure,
                               double windSpeed, String windDirection) {
            this.date = date;
            this.dayTemp = dayTemp;
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.feelsLike = feelsLike;
            this.description = description;
            this.humidity = humidity;
            this.pressure = pressure;
            this.windSpeed = windSpeed;
            this.windDirection = windDirection;
        }
    }

    // Private constructor for singleton
    private WeatherFetcher(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }

    // Singleton getter
    public static synchronized WeatherFetcher getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherFetcher(context);
        }
        return instance;
    }

    /**
     * Fetch current weather data for a city
     * @param cityName City name (supports Vietnamese characters)
     * @param callback Callback to handle success/failure
     */
    public void fetchCurrentWeather(String cityName, WeatherCallback callback) {
        if (cityName == null || cityName.trim().isEmpty()) {
            callback.onFailure("Tên thành phố không được để trống");
            return;
        }

        String encodedCity = encodeUrl(cityName.trim());
        String url = BASE_URL + "weather?q=" + encodedCity +
                "&appid=" + API_KEY + "&units=metric&lang=vi";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        CurrentWeather weather = parseCurrentWeather(response);

                        // Cache coordinates for future forecast requests
                        if (response.has("coord")) {
                            JSONObject coord = response.getJSONObject("coord");
                            lastCityCoords = new CityCoords(coord.getDouble("lat"), coord.getDouble("lon"));
                            lastCityName = cityName;
                        }

                        callback.onSuccess(formatCurrentWeather(weather));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing current weather data", e);
                        callback.onFailure("Lỗi phân tích dữ liệu thời tiết");
                    }
                },
                error -> handleVolleyError(error, callback));

        // Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                2,     // 2 retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    /**
     * Fetch tomorrow's weather forecast
     * @param cityName City name (will use cached coordinates if available)
     * @param callback Callback to handle success/failure
     */
    public void fetchTomorrowForecast(String cityName, WeatherCallback callback) {
        // Use cached coordinates if available and not expired
        if (lastCityCoords != null && !lastCityCoords.isExpired() &&
                cityName.equals(lastCityName)) {
            fetchForecastByCoords(lastCityCoords.lat, lastCityCoords.lon, callback);
            return;
        }

        // First get coordinates, then fetch forecast
        fetchCoordinates(cityName, new WeatherCallback() {
            @Override
            public void onSuccess(String result) {
                String[] coords = result.split(",");
                double lat = Double.parseDouble(coords[0]);
                double lon = Double.parseDouble(coords[1]);
                fetchForecastByCoords(lat, lon, callback);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    /**
     * Fetch coordinates for a city name
     */
    private void fetchCoordinates(String cityName, WeatherCallback callback) {
        String encodedCity = encodeUrl(cityName.trim());
        String url = BASE_URL + "weather?q=" + encodedCity + "&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject coord = response.getJSONObject("coord");
                        double lat = coord.getDouble("lat");
                        double lon = coord.getDouble("lon");

                        // Cache coordinates
                        lastCityCoords = new CityCoords(lat, lon);
                        lastCityName = cityName;

                        callback.onSuccess(lat + "," + lon);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing coordinates", e);
                        callback.onFailure("Không thể lấy tọa độ thành phố");
                    }
                },
                error -> handleVolleyError(error, callback));

        request.setRetryPolicy(new DefaultRetryPolicy(5000, 1, 1.0f));
        requestQueue.add(request);
    }

    /**
     * Fetch forecast using coordinates
     */
    private void fetchForecastByCoords(double lat, double lon, WeatherCallback callback) {
        String url = ONE_CALL_URL + "?lat=" + lat + "&lon=" + lon +
                "&exclude=current,minutely,hourly,alerts" +
                "&units=metric&lang=vi&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray dailyArray = response.getJSONArray("daily");
                        if (dailyArray.length() < 2) {
                            callback.onFailure("Không có dữ liệu dự báo cho ngày mai");
                            return;
                        }

                        ForecastWeather forecast = parseForecastWeather(dailyArray.getJSONObject(1));
                        callback.onSuccess(formatForecastWeather(forecast));

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing forecast data", e);
                        callback.onFailure("Lỗi phân tích dữ liệu dự báo");
                    }
                },
                error -> handleVolleyError(error, callback));

        request.setRetryPolicy(new DefaultRetryPolicy(10000, 2, 1.0f));
        requestQueue.add(request);
    }

    /**
     * Parse current weather JSON response
     */
    private CurrentWeather parseCurrentWeather(JSONObject json) throws JSONException {
        String cityName = json.getString("name");

        JSONObject main = json.getJSONObject("main");
        double temperature = main.getDouble("temp");
        double feelsLike = main.getDouble("feels_like");
        int humidity = main.getInt("humidity");
        int pressure = main.getInt("pressure");

        String description = "";
        JSONArray weatherArray = json.getJSONArray("weather");
        if (weatherArray.length() > 0) {
            description = weatherArray.getJSONObject(0).getString("description");
        }

        JSONObject wind = json.getJSONObject("wind");
        double windSpeed = wind.getDouble("speed");
        int windDeg = wind.optInt("deg", 0);
        String windDirection = degreeToDirection(windDeg);

        long timestamp = json.getLong("dt");
        String updateTime = formatTime(timestamp);

        return new CurrentWeather(cityName, temperature, feelsLike, humidity,
                pressure, description, windSpeed, windDirection, updateTime);
    }

    /**
     * Parse forecast weather JSON response
     */
    private ForecastWeather parseForecastWeather(JSONObject json) throws JSONException {
        long timestamp = json.getLong("dt");
        String date = formatDate(timestamp);

        JSONObject temp = json.getJSONObject("temp");
        double dayTemp = temp.getDouble("day");
        double minTemp = temp.getDouble("min");
        double maxTemp = temp.getDouble("max");

        JSONObject feelsLike = json.getJSONObject("feels_like");
        double feelsLikeDay = feelsLike.getDouble("day");

        String description = "";
        JSONArray weatherArray = json.getJSONArray("weather");
        if (weatherArray.length() > 0) {
            description = weatherArray.getJSONObject(0).getString("description");
        }

        int humidity = json.getInt("humidity");
        int pressure = json.getInt("pressure");
        double windSpeed = json.getDouble("wind_speed");
        int windDeg = json.optInt("wind_deg", 0);
        String windDirection = degreeToDirection(windDeg);

        return new ForecastWeather(date, dayTemp, minTemp, maxTemp, feelsLikeDay,
                description, humidity, pressure, windSpeed, windDirection);
    }

    /**
     * Format current weather data into readable Vietnamese text
     */
    private String formatCurrentWeather(CurrentWeather weather) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thời tiết ở ").append(weather.cityName)
                .append(" hôm nay có nhiệt độ ").append(String.format("%.1f", weather.temperature))
                .append("°C và cảm giác như ").append(String.format("%.1f", weather.feelsLike)).append("°C. ");

        sb.append("Trời ").append(weather.description)
                .append(", độ ẩm ").append(weather.humidity)
                .append("% và áp suất ").append(weather.pressure).append(" hPa. ");

        sb.append("Gió ").append(String.format("%.1f", weather.windSpeed))
                .append(" m/s hướng ").append(weather.windDirection).append(". ");

        sb.append("\nCập nhật lúc ").append(weather.updateTime);

        return sb.toString();
    }

    /**
     * Format forecast weather data into readable Vietnamese text
     */
    private String formatForecastWeather(ForecastWeather forecast) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dự báo thời tiết ngày mai (").append(forecast.date).append("): ");
        sb.append("Nhiệt độ ").append(String.format("%.1f", forecast.dayTemp))
                .append("°C (").append(String.format("%.1f", forecast.minTemp))
                .append("°C - ").append(String.format("%.1f", forecast.maxTemp)).append("°C), ");

        sb.append("cảm giác như ").append(String.format("%.1f", forecast.feelsLike)).append("°C. ");

        sb.append("Trời ").append(forecast.description)
                .append(", độ ẩm ").append(forecast.humidity)
                .append("%, áp suất ").append(forecast.pressure).append(" hPa. ");

        sb.append("Gió ").append(String.format("%.1f", forecast.windSpeed))
                .append(" m/s hướng ").append(forecast.windDirection).append(".");

        return sb.toString();
    }

    /**
     * Convert wind degree to Vietnamese direction
     */
    private String degreeToDirection(int degree) {
        int index = Math.round(degree / 45f) % 8;
        return WIND_DIRECTIONS[index];
    }

    /**
     * Format timestamp to time string
     */
    private String formatTime(long epochSeconds) {
        return TIME_FORMAT.format(new Date(epochSeconds * 1000L));
    }

    /**
     * Format timestamp to date string
     */
    private String formatDate(long epochSeconds) {
        return DATE_FORMAT.format(new Date(epochSeconds * 1000L));
    }

    /**
     * URL encode with UTF-8
     */
    private String encodeUrl(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "UTF-8 encoding not supported, using fallback", e);
            return input.replace(" ", "%20");
        }
    }

    /**
     * Handle Volley errors with appropriate Vietnamese messages
     */
    private void handleVolleyError(VolleyError error, WeatherCallback callback) {
        String errorMessage;

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            switch (statusCode) {
                case 404:
                    errorMessage = "Không tìm thấy thành phố. Vui lòng kiểm tra tên thành phố.";
                    break;
                case 401:
                    errorMessage = "Lỗi xác thực API. Vui lòng thử lại sau.";
                    break;
                case 429:
                    errorMessage = "Quá nhiều yêu cầu. Vui lòng thử lại sau.";
                    break;
                default:
                    errorMessage = "Lỗi server (" + statusCode + "). Vui lòng thử lại.";
            }
        } else {
            errorMessage = "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối internet.";
        }

        Log.e(TAG, "Volley error: " + error.getMessage(), error);
        callback.onFailure(errorMessage);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}