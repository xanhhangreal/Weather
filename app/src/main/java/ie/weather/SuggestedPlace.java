package ie.weather;

public class SuggestedPlace {
    public String name;
    public double temperature;
    public String condition;
    public String iconUrl;

    public SuggestedPlace(String name, double temperature, String condition, String iconUrl) {
        this.name = name;
        this.temperature = temperature;
        this.condition = condition;
        this.iconUrl = iconUrl;
    }
}