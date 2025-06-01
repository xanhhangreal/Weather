package ie.weather;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
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

public class ChatActivity extends AppCompatActivity {

    private EditText inputMessage;
    private Button sendButton;
    private TextView chatResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inputMessage = findViewById(R.id.inputMessage);
        sendButton   = findViewById(R.id.sendButton);
        chatResult   = findViewById(R.id.chatResult);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userText = inputMessage.getText().toString().trim();
                if (userText.isEmpty()) {
                    chatResult.setText("Bot: Hãy nhập tên thành phố hoặc câu hỏi.");
                    return;
                }

                // Chuyển về lowercase để dễ check
                String lower = userText.toLowerCase();

                // 1) Kiểm tra ngày: nếu có "ngày mai" hoặc "tomorrow" → askTomorrow = true
                boolean askTomorrow = false;
                if (lower.contains("ngày mai") || lower.contains("tomorrow") || lower.contains("mai")) {
                    askTomorrow = true;
                }

                // 2) Xóa bỏ các từ khoá để lấy đúng tên thành phố
                String cityName = lower
                        .replaceAll("thời tiết", "")
                        .replaceAll("hôm nay", "")
                        .replaceAll("today", "")
                        .replaceAll("ngày mai", "")
                        .replaceAll("tomorrow", "")
                        .trim();

                if (cityName.isEmpty()) {
                    chatResult.setText("Bot: Mình chưa phát hiện tên thành phố. Hãy nhập như “Thời tiết Hà Nội” hoặc “Hanoi today”.");
                    return;
                }

                // Hiển thị tạm loading
                if (askTomorrow) {
                    chatResult.setText("Bot: Đang lấy dự báo thời tiết cho ngày mai ở " + cityName + " …");
                } else {
                    chatResult.setText("Bot: Đang lấy dữ liệu thời tiết hiện tại cho " + cityName + " …");
                }

                // 3) Gọi helper để fetch coord + dữ liệu
                fetchCityCoordsAndThen(cityName, askTomorrow);
            }
        });
    }

    private void fetchCityCoordsAndThen(String cityName, boolean askTomorrow) {
        // Mã hóa thành phố
        String encodedCity;
        try {
            encodedCity = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            encodedCity = cityName.replace(" ", "%20");
        }

        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + encodedCity
                + "&appid=" + WeatherFetcher.API_KEY
                + "&units=metric&lang=vi";

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);

                            // Lấy coord (lat, lon)
                            JSONObject coord = json.getJSONObject("coord");
                            double lat = coord.getDouble("lat");
                            double lon = coord.getDouble("lon");

                            if (!askTomorrow) {
                                // Chỉ cần current weather → gọi fetchCurrentWeather để parse
                                WeatherFetcher.fetchCurrentWeather(ChatActivity.this, cityName, new WeatherFetcher.WeatherCallback() {
                                    @Override
                                    public void onSuccess(String reply) {
                                        runOnUiThread(() -> chatResult.setText("Bot: " + reply));
                                    }
                                    @Override
                                    public void onFailure(String errorMsg) {
                                        runOnUiThread(() -> chatResult.setText("Bot: " + errorMsg));
                                    }
                                });

                            } else {
                                // Forecast ngày mai: gọi fetchTomorrowForecast
                                WeatherFetcher.fetchTomorrowForecast(ChatActivity.this, lat, lon, new WeatherFetcher.WeatherCallback() {
                                    @Override
                                    public void onSuccess(String reply) {
                                        runOnUiThread(() -> chatResult.setText("Bot: " + reply));
                                    }
                                    @Override
                                    public void onFailure(String errorMsg) {
                                        runOnUiThread(() -> chatResult.setText("Bot: " + errorMsg));
                                    }
                                });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> chatResult.setText("Bot: Lỗi phân tích tọa độ thành phố."));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        runOnUiThread(() -> chatResult.setText("Bot: Không tìm thấy thành phố “" + cityName + "”."));
                    }
                }
        );
        queue.add(req);
    }
}
