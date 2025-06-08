package ie.weather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class SuggestedPlaceAdapter extends RecyclerView.Adapter<SuggestedPlaceAdapter.ViewHolder> {

    private final List<SuggestedPlace> places;

    public SuggestedPlaceAdapter(List<SuggestedPlace> places) {
        this.places = places;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggested_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SuggestedPlace place = places.get(position);
        holder.placeName.setText(place.name);
        holder.weatherInfo.setText(String.format("%.2f°C – %s", place.temperature, place.condition));
        Picasso.get().load(place.iconUrl).into(holder.icon);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView placeName, weatherInfo;
        ImageView icon;

        ViewHolder(View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.textPlaceName);
            weatherInfo = itemView.findViewById(R.id.textWeatherInfo);
            icon = itemView.findViewById(R.id.imgWeatherIcon);
        }
    }
}