package ie.weather;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UriEncoder {
    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return s.replace(" ", "%20");
        }
    }
}
