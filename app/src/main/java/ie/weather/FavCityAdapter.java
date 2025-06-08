package ie.weather;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Adapter để hiển thị danh sách các thành phố yêu thích
 * Nhận dữ liệu từ FavCityModel và chuyển thành view
 */
public class FavCityAdapter extends RecyclerView.Adapter<FavCityAdapter.ViewHolder> {

    private Context context;
    private ArrayList<FavCityModel> arr;

    public FavCityAdapter(Context context, ArrayList<FavCityModel> arr) {
        this.context = context;
        this.arr = arr != null ? arr : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.favcity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= arr.size()) return;

        FavCityModel obj = arr.get(position);
        if (obj == null) return;

        // Set background image
        holder.imgCardBG.setImageResource(R.drawable.favoritecard);

        // Set city name
        holder.textCity.setText(obj.getCity() != null ? obj.getCity() : "Unknown City");

        // Set temperature
        holder.textTemperature.setText(obj.getTemperature() != null ? obj.getTemperature() : "--°");

        // Load weather condition icon
        String iconCode = obj.getImgCondition();
        if (iconCode != null && !iconCode.isEmpty()) {
            String iconUrl = "https://openweathermap.org/img/w/" + iconCode + ".png";
            Picasso.get()
                    .load(iconUrl)
                    .into(holder.imgCondition);
        }

        // Set weather condition text
        holder.textCondition.setText(obj.getCondition() != null ? obj.getCondition() : "Unknown");

        // Set wind speed
        String windSpeed = obj.getWindSpeed();
        if (windSpeed != null && !windSpeed.isEmpty()) {
            holder.textWindSpeed.setText(windSpeed + " Km/h");
        } else {
            holder.textWindSpeed.setText("-- Km/h");
        }
    }

    @Override
    public int getItemCount() {
        return arr != null ? arr.size() : 0;
    }

    /**
     * Update data and refresh the adapter
     */
    public void updateData(ArrayList<FavCityModel> newData) {
        this.arr = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Add new item to the list
     */
    public void addItem(FavCityModel item) {
        if (item != null) {
            arr.add(item);
            notifyItemInserted(arr.size() - 1);
        }
    }

    /**
     * Remove item from the list
     */
    public void removeItem(int position) {
        if (position >= 0 && position < arr.size()) {
            arr.remove(position);
            notifyItemRemoved(position);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCardBG, imgCondition;
        TextView textCity, textTemperature, textCondition, textWindSpeed;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCardBG = itemView.findViewById(R.id.imgCardBG);
            imgCondition = itemView.findViewById(R.id.imgCondition);
            textCity = itemView.findViewById(R.id.textCity);
            textTemperature = itemView.findViewById(R.id.textTemperature);
            textCondition = itemView.findViewById(R.id.textCondition);
            textWindSpeed = itemView.findViewById(R.id.textWindSpeed);
        }
    }
}