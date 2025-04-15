package ie.weather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;

public class PlaceSuggestionFragment extends Fragment {

    private static final String ARG_LIST = "places_list";
    private ArrayList<SuggestedPlace> suggestedPlaces;

    public static PlaceSuggestionFragment newInstance(ArrayList<SuggestedPlace> places) {
        PlaceSuggestionFragment fragment = new PlaceSuggestionFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LIST, places);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaceSuggestionFragment() {
        // Required empty constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            suggestedPlaces = (ArrayList<SuggestedPlace>) getArguments().getSerializable(ARG_LIST);
        } else {
            suggestedPlaces = new ArrayList<>();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_place_suggestion, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.rvSuggestions);
        TextView textEmpty = view.findViewById(R.id.textNoResults);
        Button btnClose = view.findViewById(R.id.btnCloseFragment);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (suggestedPlaces != null && !suggestedPlaces.isEmpty()) {
            recyclerView.setVisibility(View.VISIBLE);
            textEmpty.setVisibility(View.GONE);
            recyclerView.setAdapter(new SuggestedPlaceAdapter(suggestedPlaces));
        } else {
            recyclerView.setVisibility(View.GONE);
            textEmpty.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "Không có địa điểm phù hợp!", Toast.LENGTH_SHORT).show();
        }

        btnClose.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }
}
