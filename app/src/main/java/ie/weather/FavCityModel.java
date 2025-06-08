package ie.weather;

/**
 * Model class định nghĩa các thành phố yêu thích
 * Chứa thông tin về thành phố, nhiệt độ, điều kiện thời tiết, tốc độ gió
 */
public class FavCityModel {
    private int imgcardBG;
    private String city;
    private String temperature;
    private String condition;
    private String windSpeed;
    private String imgCondition;

    // Default constructor
    public FavCityModel() {
        this.imgcardBG = 0;
        this.city = "";
        this.temperature = "";
        this.condition = "";
        this.windSpeed = "";
        this.imgCondition = "";
    }

    // Constructor với tất cả parameters
    public FavCityModel(int imgcardBG, String city, String temperature, String condition, String windSpeed, String imgCondition) {
        this.imgcardBG = imgcardBG;
        this.city = city != null ? city : "";
        this.temperature = temperature != null ? temperature : "";
        this.condition = condition != null ? condition : "";
        this.windSpeed = windSpeed != null ? windSpeed : "";
        this.imgCondition = imgCondition != null ? imgCondition : "";
    }

    // Constructor đơn giản cho thông tin cơ bản
    public FavCityModel(String city, String temperature, String condition) {
        this(0, city, temperature, condition, "", "");
    }

    // Getters
    public int getImgcardBG() {
        return imgcardBG;
    }

    public String getCity() {
        return city;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getCondition() {
        return condition;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public String getImgCondition() {
        return imgCondition;
    }

    // Setters với validation
    public void setImgcardBG(int imgcardBG) {
        this.imgcardBG = imgcardBG;
    }

    public void setCity(String city) {
        this.city = city != null ? city.trim() : "";
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature != null ? temperature.trim() : "";
    }

    public void setCondition(String condition) {
        this.condition = condition != null ? condition.trim() : "";
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed != null ? windSpeed.trim() : "";
    }

    public void setImgCondition(String imgCondition) {
        this.imgCondition = imgCondition != null ? imgCondition.trim() : "";
    }

    // Utility methods

    /**
     * Check if the model has valid data
     */
    public boolean isValid() {
        return city != null && !city.isEmpty() &&
                temperature != null && !temperature.isEmpty();
    }

    /**
     * Get formatted temperature with degree symbol
     */
    public String getFormattedTemperature() {
        if (temperature != null && !temperature.isEmpty()) {
            return temperature.endsWith("°") ? temperature : temperature + "°";
        }
        return "--°";
    }

    /**
     * Get formatted wind speed with unit
     */
    public String getFormattedWindSpeed() {
        if (windSpeed != null && !windSpeed.isEmpty()) {
            return windSpeed.endsWith("Km/h") ? windSpeed : windSpeed + " Km/h";
        }
        return "-- Km/h";
    }

    @Override
    public String toString() {
        return "FavCityModel{" +
                "city='" + city + '\'' +
                ", temperature='" + temperature + '\'' +
                ", condition='" + condition + '\'' +
                ", windSpeed='" + windSpeed + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FavCityModel that = (FavCityModel) obj;
        return city != null ? city.equals(that.city) : that.city == null;
    }

    @Override
    public int hashCode() {
        return city != null ? city.hashCode() : 0;
    }
}