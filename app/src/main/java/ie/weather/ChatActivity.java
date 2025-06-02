package ie.weather;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private ImageButton sendButton;
    private ScrollView scrollContainer;
    private LinearLayout messageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Ánh xạ view
        inputMessage = findViewById(R.id.inputMessage);
        sendButton   = findViewById(R.id.sendButton);
        scrollContainer = findViewById(R.id.scroll_container);
        messageContainer = findViewById(R.id.message_container);

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
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    processSend();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Xử lý khi user nhấn gửi:
     * 1) Thêm bubble user
     * 2) Ẩn bàn phím, xóa ô nhập
     * 3) Thêm bubble loading tạm
     * 4) Gọi API, sau khi có kết quả sẽ thêm bubble bot
     */
    private void processSend() {
        String userText = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) {
            return;
        }

        // 1) Thêm bubble user
        addUserBubble(userText);

        // 2) Ẩn bàn phím rồi xóa nội dung ô nhập
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(inputMessage.getWindowToken(), 0);
        }
        inputMessage.setText("");

        // 3) Phân tích câu hỏi
        String lower = userText.toLowerCase();
        boolean askTomorrow = false;
        if (lower.contains("ngày mai") || lower.contains("tomorrow") || lower.contains("mai")) {
            askTomorrow = true;
        }
        String cityName = lower
                .replaceAll("thời tiết", "")
                .replaceAll("hôm nay", "")
                .replaceAll("today", "")
                .replaceAll("ngày mai", "")
                .replaceAll("tomorrow", "")
                .trim();
        if (cityName.isEmpty()) {
            addBotBubble("Mình chưa phát hiện tên thành phố. Hãy nhập như “Thời tiết Hà Nội” hoặc “Hanoi today”.");
            return;
        }

        // 4) Thêm bubble loading tạm
        if (askTomorrow) {
            addBotBubble("Đang lấy dự báo thời tiết cho ngày mai ở " + cityName + " …");
        } else {
            addBotBubble("Đang lấy dữ liệu thời tiết hiện tại cho " + cityName + " …");
        }

        // 5) Gọi API
        fetchCityCoordsAndThen(cityName, askTomorrow);
    }

    /**
     * Thêm một bubble của user vào messageContainer.
     */
    private void addUserBubble(String message) {
        View bubbleView = LayoutInflater.from(this)
                .inflate(R.layout.item_user_message, messageContainer, false);
        TextView tv = bubbleView.findViewById(R.id.textUserMessage);
        tv.setText(message);
        messageContainer.addView(bubbleView);
        scrollToBottom();
    }

    /**
     * Thêm một bubble của bot vào messageContainer.
     */
    private void addBotBubble(String message) {
        View bubbleView = LayoutInflater.from(this)
                .inflate(R.layout.item_bot_message, messageContainer, false);
        TextView tv = bubbleView.findViewById(R.id.textBotMessage);
        tv.setText(message);
        messageContainer.addView(bubbleView);
        scrollToBottom();
    }

    /**
     * Cuộn ScrollView xuống dưới để hiển thị bubble mới nhất.
     */
    private void scrollToBottom() {
        scrollContainer.post(new Runnable() {
            @Override
            public void run() {
                scrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * Gọi API /weather để lấy tọa độ, sau đó:
     * - Nếu askTomorrow == false: gọi fetchCurrentWeather
     * - Nếu askTomorrow == true: gọi fetchTomorrowForecast
     */
    private void fetchCityCoordsAndThen(String cityName, boolean askTomorrow) {
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
                            JSONObject coord = json.getJSONObject("coord");
                            double lat = coord.getDouble("lat");
                            double lon = coord.getDouble("lon");

                            if (!askTomorrow) {
                                WeatherFetcher.fetchCurrentWeather(
                                        ChatActivity.this,
                                        cityName,
                                        new WeatherFetcher.WeatherCallback() {
                                            @Override
                                            public void onSuccess(String reply) {
                                                runOnUiThread(() -> addBotBubble(reply));
                                            }
                                            @Override
                                            public void onFailure(String errorMsg) {
                                                runOnUiThread(() -> addBotBubble(errorMsg));
                                            }
                                        }
                                );
                            } else {
                                WeatherFetcher.fetchTomorrowForecast(
                                        ChatActivity.this,
                                        lat,
                                        lon,
                                        new WeatherFetcher.WeatherCallback() {
                                            @Override
                                            public void onSuccess(String reply) {
                                                runOnUiThread(() -> addBotBubble(reply));
                                            }
                                            @Override
                                            public void onFailure(String errorMsg) {
                                                runOnUiThread(() -> addBotBubble(errorMsg));
                                            }
                                        }
                                );
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> addBotBubble("Lỗi phân tích tọa độ thành phố."));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        runOnUiThread(() -> addBotBubble("Không tìm thấy thành phố “" + cityName + "”."));
                    }
                }
        );

        queue.add(req);
    }
}
