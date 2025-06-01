package ie.weather;
import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * WeatherFetcher: chứa các hàm lấy dữ liệu thời tiết (current + daily forecast) từ OpenWeatherMap.
 */
public class WeatherFetcher {
    public static final String API_KEY = "8425462840818e1c815aa16663d1fedb";

    public interface WeatherCallback {
        void onSuccess(String reply);
        void onFailure(String errorMsg);
    }

    /**
     * Lấy dữ liệu thời tiết hiện tại (current) với rất nhiều trường: temp, feels_like, humidity, pressure,
     * description, wind speed & direction, v.v.
     *
     * @param context  Context để tạo RequestQueue
     * @param cityName Tên thành phố người dùng nhập (có thể chứa dấu)
     * @param callback Callback nhận kết quả hoặc lỗi
     */
    public static void fetchCurrentWeather(Context context, String cityName, WeatherCallback callback) {
        // Mã hóa tên thành phố vào URL (UTF-8)
        String encodedCity;
        try {
            encodedCity = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            // fallback đơn giản: thay khoảng trắng
            encodedCity = cityName.replace(" ", "%20");
        }

        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + encodedCity
                + "&appid=" + API_KEY
                + "&units=metric&lang=vi";

        // Khởi tạo RequestQueue
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);

                            // 1) Tên thành phố (server trả về thường chuẩn theo DB)
                            String city = json.getString("name");

                            // 2) main: temp, feels_like, temp_min, temp_max, pressure, humidity
                            JSONObject main = json.getJSONObject("main");
                            double temp = main.getDouble("temp");
                            double feelsLike = main.getDouble("feels_like");
                            int humidity = main.getInt("humidity");
                            int pressure = main.getInt("pressure");

                            // 3) weather: mảng, chỉ lấy phần tử đầu (index 0).description
                            JSONArray weatherArr = json.getJSONArray("weather");
                            String description = "";
                            if (weatherArr.length() > 0) {
                                description = weatherArr.getJSONObject(0).getString("description");
                            }

                            // 4) wind: speed + deg
                            JSONObject wind = json.getJSONObject("wind");
                            double windSpeed = wind.getDouble("speed");
                            int windDeg = wind.getInt("deg");
                            String windDir = degToDirection(windDeg);

                            // 5) Thời gian bản tin (timestamp) – nếu muốn convert sang HH:mm:ss
                            long dt = json.getLong("dt"); // thời gian Unix (giây)
                            String updatedTime = epochToHuman(dt);

                            // 6) Kết hợp tất cả vào một chuỗi dài:
                            // Ví dụ:
                            // "Thời tiết ở Hà Nội hôm nay có nhiệt độ khoảng 39°C (312.13 K) và cảm giác như 43°C (316.01 K).
                            // Trời có nhiều mây, với độ ẩm là 34% và áp suất không khí là 995 hPa.
                            // Tốc độ gió khoảng 4.96 m/s từ hướng tây nam.
                            // Nếu bạn có kế hoạch ra ngoài, hãy mặc đồ nhẹ và nhớ uống đủ nước để tránh bị nóng."
                            StringBuilder sb = new StringBuilder();
                            sb.append("Thời tiết ở ").append(city)
                                    .append(" hôm nay có nhiệt độ khoảng ")
                                    .append(String.format("%.1f", temp)).append("°C (")
                                    .append(String.format("%.2f", temp + 273.15)).append(" K) và cảm giác như ")
                                    .append(String.format("%.1f", feelsLike)).append("°C (")
                                    .append(String.format("%.2f", feelsLike + 273.15)).append(" K). ");

                            sb.append("Trời ").append(description)
                                    .append(", với độ ẩm là ").append(humidity).append("% và áp suất không khí là ")
                                    .append(pressure).append(" hPa. ");

                            sb.append("Tốc độ gió khoảng ").append(String.format("%.2f", windSpeed))
                                    .append(" m/s từ hướng ").append(windDir).append(". ");

                            sb.append("\nCập nhật: ").append(updatedTime).append(". ");

                            // Gọi onSuccess với chuỗi kết quả
                            callback.onSuccess(sb.toString());

                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onFailure("Lỗi phân tích dữ liệu thời tiết.");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Khi server trả về lỗi (404 nếu thành phố không tồn tại, v.v.)
                        callback.onFailure("Không thể lấy dữ liệu thời tiết. Vui lòng thử lại.");
                    }
                }
        );

        queue.add(request);
    }

    /**
     * Lấy dự báo ngày mai (tomorrow) dựa trên lat/lon.
     * Đây sẽ gọi API One Call (daily forecast) để lấy thông tin của [1] (ngày mai).
     *
     * @param context Context để tạo RequestQueue
     * @param lat     Vĩ độ thành phố
     * @param lon     Kinh độ thành phố
     * @param callback Callback nhận chuỗi mô tả forecast ngày mai
     */
    public static void fetchTomorrowForecast(Context context, double lat, double lon, WeatherCallback callback) {
        // One Call: lấy daily forecast (0 = hôm nay, 1 = ngày mai, …)
        String url = "https://api.openweathermap.org/data/2.5/onecall?lat="
                + lat + "&lon=" + lon
                + "&exclude=current,minutely,hourly,alerts"
                + "&units=metric&lang=vi&appid=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            JSONArray dailyArr = json.getJSONArray("daily");

                            if (dailyArr.length() < 2) {
                                callback.onFailure("Không có dữ liệu dự báo cho ngày mai.");
                                return;
                            }

                            JSONObject tomorrow = dailyArr.getJSONObject(1); // index 1 = ngày mai

                            // Lấy ngày (dt), temp: day, min, max, feels_like
                            long dt = tomorrow.getLong("dt");
                            String date = epochToShortDate(dt); // vd. “2023-07-31”

                            JSONObject tempObj = tomorrow.getJSONObject("temp");
                            double tempDay = tempObj.getDouble("day");
                            double tempMin = tempObj.getDouble("min");
                            double tempMax = tempObj.getDouble("max");

                            JSONObject feelsLikeObj = tomorrow.getJSONObject("feels_like");
                            double feelsDay = feelsLikeObj.getDouble("day");

                            // weather[0].description
                            JSONArray weatherArr = tomorrow.getJSONArray("weather");
                            String description = "";
                            if (weatherArr.length() > 0) {
                                description = weatherArr.getJSONObject(0).getString("description");
                            }

                            // độ ẩm + áp suất
                            int humidity = tomorrow.getInt("humidity");
                            int pressure = tomorrow.getInt("pressure");

                            // gió: speed + deg
                            double windSpeed = tomorrow.getDouble("wind_speed");
                            int windDeg = tomorrow.getInt("wind_deg");
                            String windDir = degToDirection(windDeg);

                            // Kết hợp thành chuỗi dài:
                            StringBuilder sb = new StringBuilder();
                            sb.append("Thời tiết ở ").append(date).append(" sẽ có nhiệt độ khoảng ")
                                    .append(String.format("%.1f", tempDay)).append("°C (")
                                    .append(String.format("%.2f", tempDay + 273.15)).append(" K) trong suốt cả ngày, ")
                                    .append("với nhiệt độ cao nhất lên tới ")
                                    .append(String.format("%.2f", tempMax)).append("°C (")
                                    .append(String.format("%.2f", tempMax + 273.15)).append(" K) ")
                                    .append("và thấp nhất là ")
                                    .append(String.format("%.2f", tempMin)).append("°C (")
                                    .append(String.format("%.2f", tempMin + 273.15)).append(" K). ");

                            sb.append("Cảm giác như khoảng ")
                                    .append(String.format("%.2f", feelsDay)).append("°C (")
                                    .append(String.format("%.2f", feelsDay + 273.15)).append(" K) vào ban ngày. ");

                            sb.append("Trời sẽ ").append(description)
                                    .append(", độ ẩm ").append(humidity).append("% và áp suất không khí là ")
                                    .append(pressure).append(" hPa. ");

                            sb.append("Tốc độ gió khoảng ")
                                    .append(String.format("%.2f", windSpeed)).append(" m/s từ hướng ")
                                    .append(windDir).append(". ");

                            // Lời khuyên/ngắn gọn
                            sb.append("\nHãy mặc đồ nhẹ và uống đủ nước khi ra ngoài nhé!");

                            callback.onSuccess(sb.toString());

                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onFailure("Lỗi phân tích dữ liệu forecast.");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onFailure("Không thể lấy dữ liệu forecast.");
                    }
                }
        );

        queue.add(request);
    }

    /**
     * Chuyển độ (0-360) thành chuỗi hướng gió (N, NE, E, SE, S, SW, W, NW).
     */
    private static String degToDirection(int deg) {
        String[] dirs = { "bắc", "đông bắc", "đông", "đông nam", "nam", "tây nam", "tây", "tây bắc" };
        int idx = Math.round(deg / 45f) % 8;
        return dirs[idx];
    }

    /**
     * Chuyển epoch (giây) sang HH:mm:ss (theo local timezone).
     */
    private static String epochToHuman(long epochSeconds) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(new java.util.Date(epochSeconds * 1000L));
    }

    /**
     * Chuyển epoch (giây) sang yyyy-MM-dd.
     */
    private static String epochToShortDate(long epochSeconds) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(new java.util.Date(epochSeconds * 1000L));
    }
}
