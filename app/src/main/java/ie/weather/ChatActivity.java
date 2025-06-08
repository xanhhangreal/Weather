package ie.weather;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatActivity extends AppCompatActivity {

    // UI Components
    private EditText inputMessage;
    private ImageButton sendButton;
    private ScrollView scrollContainer;
    private LinearLayout messageContainer;

    // Utilities
    private WeatherFetcher weatherFetcher;
    private Handler mainHandler;

    // Loading state management
    private View currentLoadingBubble;
    private boolean isProcessing = false;

    // City name patterns for better extraction
    private static final List<String> WEATHER_KEYWORDS = Arrays.asList(
            "thời tiết", "weather", "nhiệt độ", "temperature", "nóng", "lạnh",
            "mưa", "rain", "nắng", "sunny", "mây", "cloud"
    );

    private static final List<String> TOMORROW_KEYWORDS = Arrays.asList(
            "ngày mai", "tomorrow", "mai", "hôm sau", "next day"
    );

    private static final List<String> TODAY_KEYWORDS = Arrays.asList(
            "hôm nay", "today", "hiện tại", "current", "bây giờ", "now"
    );

    // Pattern for extracting city names
    private static final Pattern CITY_PATTERN = Pattern.compile(
            "(?:thời tiết|weather)\\s+(?:ở|tại|at|in)?\\s*([\\p{L}\\s]+?)(?:\\s+(?:hôm nay|today|ngày mai|tomorrow|mai))?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeViews();
        initializeComponents();
        setupEventListeners();
        showWelcomeMessage();
    }

    /**
     * Initialize UI components
     */
    private void initializeViews() {
        inputMessage = findViewById(R.id.inputMessage);
        sendButton = findViewById(R.id.sendButton);
        scrollContainer = findViewById(R.id.scroll_container);
        messageContainer = findViewById(R.id.message_container);
    }

    /**
     * Initialize other components
     */
    private void initializeComponents() {
        weatherFetcher = WeatherFetcher.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Setup event listeners
     */
    private void setupEventListeners() {
        sendButton.setOnClickListener(v -> processSend());

        inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                processSend();
                return true;
            }
            return false;
        });

        // Enable/disable send button based on input
        inputMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(!TextUtils.isEmpty(s.toString().trim()) && !isProcessing);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    /**
     * Show welcome message when activity starts
     */
    private void showWelcomeMessage() {
        String welcomeMsg = "Xin chào! Tôi có thể giúp bạn tra cứu thời tiết. " +
                "Hãy nhập như:\n• \"Thời tiết Hà Nội\"\n• \"Weather in Hanoi today\"\n• \"Thời tiết TP.HCM ngày mai\"";
        addBotBubble(welcomeMsg);
    }

    /**
     * Process send message with improved error handling and UI feedback
     */
    private void processSend() {
        if (isProcessing) {
            Toast.makeText(this, "Đang xử lý yêu cầu trước đó...", Toast.LENGTH_SHORT).show();
            return;
        }

        String userText = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) {
            return;
        }

        // Set processing state
        isProcessing = true;
        sendButton.setEnabled(false);

        // Add user message
        addUserBubble(userText);

        // Hide keyboard and clear input
        hideKeyboardAndClearInput();

        // Parse user intent
        WeatherRequest request = parseWeatherRequest(userText);

        if (request.cityName.isEmpty()) {
            handleInvalidRequest();
            return;
        }

        // Add loading message
        String loadingMsg = request.isTomorrow ?
                "Đang tìm dự báo thời tiết ngày mai cho " + request.cityName + "..." :
                "Đang tìm thông tin thời tiết hiện tại cho " + request.cityName + "...";

        currentLoadingBubble = addBotBubble(loadingMsg);

        // Fetch weather data
        fetchWeatherData(request);
    }

    /**
     * Parse weather request from user input
     */
    private WeatherRequest parseWeatherRequest(String userText) {
        String lower = userText.toLowerCase().trim();

        // Check if asking for tomorrow
        boolean isTomorrow = TOMORROW_KEYWORDS.stream().anyMatch(lower::contains);

        // Extract city name using pattern matching
        String cityName = extractCityName(userText);

        // If pattern matching fails, try fallback method
        if (cityName.isEmpty()) {
            cityName = extractCityNameFallback(lower);
        }

        return new WeatherRequest(cityName, isTomorrow);
    }

    /**
     * Extract city name using regex pattern
     */
    private String extractCityName(String text) {
        Matcher matcher = CITY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * Fallback method to extract city name
     */
    private String extractCityNameFallback(String lowerText) {
        String cityName = lowerText;

        // Remove weather keywords
        for (String keyword : WEATHER_KEYWORDS) {
            cityName = cityName.replace(keyword, "");
        }

        // Remove time keywords
        for (String keyword : TOMORROW_KEYWORDS) {
            cityName = cityName.replace(keyword, "");
        }
        for (String keyword : TODAY_KEYWORDS) {
            cityName = cityName.replace(keyword, "");
        }

        // Remove common prepositions
        cityName = cityName.replaceAll("\\b(ở|tại|at|in|của|cho)\\b", "");

        return cityName.trim();
    }

    /**
     * Fetch weather data using optimized WeatherFetcher
     */
    private void fetchWeatherData(WeatherRequest request) {
        WeatherFetcher.WeatherCallback callback = new WeatherFetcher.WeatherCallback() {
            @Override
            public void onSuccess(String result) {
                mainHandler.post(() -> {
                    removeLoadingBubble();
                    addBotBubble(result);
                    resetProcessingState();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                mainHandler.post(() -> {
                    removeLoadingBubble();
                    addBotBubble("❌ " + errorMessage + "\n\nVui lòng thử lại với tên thành phố khác.");
                    resetProcessingState();
                });
            }
        };

        if (request.isTomorrow) {
            weatherFetcher.fetchTomorrowForecast(request.cityName, callback);
        } else {
            weatherFetcher.fetchCurrentWeather(request.cityName, callback);
        }
    }

    /**
     * Handle invalid request (no city name found)
     */
    private void handleInvalidRequest() {
        String errorMsg = "❓ Không tìm thấy tên thành phố trong câu hỏi của bạn.\n\n" +
                "Vui lòng thử lại với định dạng:\n" +
                "• \"Thời tiết Hà Nội\"\n" +
                "• \"Weather Ho Chi Minh City\"\n" +
                "• \"Thời tiết Đà Nẵng ngày mai\"";
        addBotBubble(errorMsg);
        resetProcessingState();
    }

    /**
     * Reset processing state
     */
    private void resetProcessingState() {
        isProcessing = false;
        sendButton.setEnabled(!TextUtils.isEmpty(inputMessage.getText().toString().trim()));
    }

    /**
     * Hide keyboard and clear input field
     */
    private void hideKeyboardAndClearInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(inputMessage.getWindowToken(), 0);
        }
        inputMessage.setText("");
    }

    /**
     * Add user message bubble
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
     * Add bot message bubble
     */
    private View addBotBubble(String message) {
        View bubbleView = LayoutInflater.from(this)
                .inflate(R.layout.item_bot_message, messageContainer, false);
        TextView tv = bubbleView.findViewById(R.id.textBotMessage);
        tv.setText(message);
        messageContainer.addView(bubbleView);
        scrollToBottom();
        return bubbleView;
    }

    /**
     * Remove loading bubble when request completes
     */
    private void removeLoadingBubble() {
        if (currentLoadingBubble != null) {
            messageContainer.removeView(currentLoadingBubble);
            currentLoadingBubble = null;
        }
    }

    /**
     * Scroll to bottom of conversation
     */
    private void scrollToBottom() {
        scrollContainer.post(() -> scrollContainer.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (weatherFetcher != null) {
            weatherFetcher.cleanup();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hide keyboard when activity is paused
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Data class for weather request
     */
    private static class WeatherRequest {
        final String cityName;
        final boolean isTomorrow;

        WeatherRequest(String cityName, boolean isTomorrow) {
            this.cityName = cityName;
            this.isTomorrow = isTomorrow;
        }
    }
}