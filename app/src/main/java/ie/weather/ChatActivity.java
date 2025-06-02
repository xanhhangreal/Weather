package ie.weather;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
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
    private ImageButton sendButton;   // Đổi từ Button thành ImageButton
    private TextView chatResult;
    private ScrollView scrollContainer; // Thêm ScrollView để cuộn cuối

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // Ánh xạ view từ layout activity_chat.xml
        inputMessage = findViewById(R.id.inputMessage);
        sendButton   = findViewById(R.id.sendButton);
        chatResult   = findViewById(R.id.chatResult);
        scrollContainer = findViewById(R.id.scroll_container);
        // Bắt sự kiện khi nhấn icon gửi
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processSend();
            }
        });
        // Bắt phím “Send” (actionSend) trên bàn phím
        inputMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Khi người dùng ấn nút “Gửi” trên bàn phím (IME_ACTION_SEND)
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    processSend();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Hàm xử lý khi người dùng nhấn gửi (icon mũi tên) hoặc nhấn Send trên bàn phím.
     */
    private void processSend() {
        String userText = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) {
            // Nếu ô nhập rỗng, hiển thị thông báo tạm thời
            chatResult.append("\n\nBot: Hãy nhập tên thành phố hoặc câu hỏi.");
            scrollToBottom();
            return;
        }

        // 1) Hiển thị nội dung do người dùng gửi lên
        chatResult.append("\n\nBạn: " + userText);
        scrollToBottom();

        // 2) Ẩn bàn phím sau khi nhấn gửi
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(inputMessage.getWindowToken(), 0);
        }

        // 3) Xóa ô nhập để người dùng có thể gõ tiếp
        inputMessage.setText("");

        // 4) Chuyển về lowercase để dễ check
        String lower = userText.toLowerCase();

        // 5) Kiểm tra ngày: nếu có "ngày mai" hoặc "tomorrow" hoặc "mai" → askTomorrow = true
        boolean askTomorrow = false;
        if (lower.contains("ngày mai") || lower.contains("tomorrow") || lower.contains("mai")) {
            askTomorrow = true;
        }

        // 6) Xóa các từ khoá để lấy đúng tên thành phố
        String cityName = lower
                .replaceAll("thời tiết", "")
                .replaceAll("hôm nay", "")
                .replaceAll("today", "")
                .replaceAll("ngày mai", "")
                .replaceAll("tomorrow", "")
                .trim();

        // Nếu không tách được cityName, báo lỗi
        if (cityName.isEmpty()) {
            chatResult.append("\nBot: Mình chưa phát hiện tên thành phố. Hãy nhập như “Thời tiết Hà Nội” hoặc “Hanoi today”.");
            scrollToBottom();
            return;
        }

        // 7) Hiển thị loading tạm thời
        if (askTomorrow) {
            chatResult.append("\nBot: Đang lấy dự báo thời tiết cho ngày mai ở " + cityName + " …");
        } else {
            chatResult.append("\nBot: Đang lấy dữ liệu thời tiết hiện tại cho " + cityName + " …");
        }
        scrollToBottom();

        // 8) Gọi helper để fetch tọa độ + dữ liệu
        fetchCityCoordsAndThen(cityName, askTomorrow);
    }

    /**
     * Gọi API /weather để lấy tọa độ (coord) của cityName. Sau đó:
     *  - Nếu askTomorrow == false → gọi fetchCurrentWeather để lấy dữ liệu hiện tại.
     *  - Nếu askTomorrow == true  → gọi fetchTomorrowForecast để lấy dự báo ngày mai.
     *
     * @param cityName    Tên thành phố (đã trim, lowercase)
     * @param askTomorrow True nếu user muốn forecast ngày mai, false nếu chỉ xem hiện tại
     */
    private void fetchCityCoordsAndThen(String cityName, boolean askTomorrow) {
        // Mã hóa tên thành phố (UTF-8) để đưa vào URL
        String encodedCity;
        try {
            encodedCity = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            encodedCity = cityName.replace(" ", "%20");
        }

        // Tạo URL gọi API current weather
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
                                // Nếu user chỉ hỏi hiện tại → gọi fetchCurrentWeather
                                WeatherFetcher.fetchCurrentWeather(
                                        ChatActivity.this,
                                        cityName,
                                        new WeatherFetcher.WeatherCallback() {
                                            @Override
                                            public void onSuccess(String reply) {
                                                runOnUiThread(() -> {
                                                    // Thêm dòng Bot: reply vào chatResult
                                                    chatResult.append("\nBot: " + reply);
                                                    scrollToBottom();
                                                });
                                            }
                                            @Override
                                            public void onFailure(String errorMsg) {
                                                runOnUiThread(() -> {
                                                    chatResult.append("\nBot: " + errorMsg);
                                                    scrollToBottom();
                                                });
                                            }
                                        }
                                );
                            } else {
                                // Nếu user hỏi ngày mai → gọi fetchTomorrowForecast
                                WeatherFetcher.fetchTomorrowForecast(
                                        ChatActivity.this,
                                        lat,
                                        lon,
                                        new WeatherFetcher.WeatherCallback() {
                                            @Override
                                            public void onSuccess(String reply) {
                                                runOnUiThread(() -> {
                                                    chatResult.append("\nBot: " + reply);
                                                    scrollToBottom();
                                                });
                                            }
                                            @Override
                                            public void onFailure(String errorMsg) {
                                                runOnUiThread(() -> {
                                                    chatResult.append("\nBot: " + errorMsg);
                                                    scrollToBottom();
                                                });
                                            }
                                        }
                                );
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                chatResult.append("\nBot: Lỗi phân tích tọa độ thành phố.");
                                scrollToBottom();
                            });
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        runOnUiThread(() -> {
                            chatResult.append("\nBot: Không tìm thấy thành phố “" + cityName + "”.");
                            scrollToBottom();
                        });
                    }
                }
        );

        queue.add(req);
    }

    /**
     * Cuộn ScrollView xuống dưới để hiển thị tin nhắn mới nhất.
     */
    private void scrollToBottom() {
        scrollContainer.post(new Runnable() {
            @Override
            public void run() {
                scrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
}
