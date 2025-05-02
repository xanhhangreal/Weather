package ie.weather;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

public class WeatherMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String openWeatherTileUrl =
            "https://tile.openweathermap.org/map/clouds_new/{z}/{x}/{y}.png?appid=485ec85551ded720ef8f68eccf7f96e0";
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setContentView(R.layout.activity_weather_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        Spinner spinner = findViewById(R.id.spinnerLayer);


        String[] layers = {"☁ Clouds", "\uD83C\uDF21 Temperature", "\uD83D\uDCA8 Wind", "\uD83C\uDF27 Rain"};


        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                layers
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                Log.d("SPINNER", "Selected: " + selected);

                // Gọi update lớp
                String layer = "";
                switch (selected) {
                    case "☁ Clouds": layer = "clouds_new"; break;
                    case "\uD83C\uDF21 Temperature": layer = "temp_new"; break;
                    case "\uD83D\uDCA8 Wind": layer = "wind_new"; break;
                    case "\uD83C\uDF27 Rain": layer = "precipitation_new"; break;
                }
                updateWeatherLayer(layer);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private TileOverlay currentOverlay;

    private void updateWeatherLayer(String layerName) {
        if (currentOverlay != null) {
            currentOverlay.remove(); // Xoá lớp cũ
        }

        String tileUrl = "https://tile.openweathermap.org/map/" + layerName + "/{z}/{x}/{y}.png?appid=485ec85551ded720ef8f68eccf7f96e0";

        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                String s = tileUrl
                        .replace("{z}", String.valueOf(zoom))
                        .replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y));
                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        };

        currentOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Lớp tile từ OpenWeatherMap
        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                try {
                    String s = openWeatherTileUrl
                            .replace("{x}", Integer.toString(x))
                            .replace("{y}", Integer.toString(y))
                            .replace("{z}", Integer.toString(zoom));
                    return new URL(s);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        TileOverlayOptions tileOverlayOptions = new TileOverlayOptions()
                .tileProvider(tileProvider);
        mMap.addTileOverlay(tileOverlayOptions);

        // Zoom tới vị trí mặc định (Việt Nam)
        LatLng vietnam = new LatLng(21.0285, 105.8542);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnam, 5));
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        mMap.setMyLocationEnabled(true); // Chấm xanh hiện vị trí
        moveToCurrentLocation();        // Tự động di chuyển camera
    }
    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && mMap != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 10));
                    }
                });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    moveToCurrentLocation();
                }
            }
        }
    }
}
