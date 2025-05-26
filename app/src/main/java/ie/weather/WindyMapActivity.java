package ie.weather;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WindyMapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true); // QUAN TRỌNG
        webView.getSettings().setUserAgentString("my.app.weather.android"); //
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/windy_map.html");
    }
}
