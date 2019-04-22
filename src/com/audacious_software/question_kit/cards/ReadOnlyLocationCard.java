package com.audacious_software.question_kit.cards;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.audacious_software.question_kit.QuestionsActivity;
import com.audacious_software.question_kit.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import androidx.core.content.res.ResourcesCompat;

public class ReadOnlyLocationCard extends ReadOnlyTextCard {
    public ReadOnlyLocationCard(QuestionsActivity activity, JSONObject prompt) {
        super(activity, prompt);
    }

    protected void initializeView(JSONObject prompt, ViewGroup parent) throws JSONException {
        super.initializeView(prompt, parent);

        final ReadOnlyLocationCard me = this;

        final ArrayList<LatLng> locations = new ArrayList<>();

        JSONArray points = prompt.getJSONArray("points");

        for (int i = 0; i < points.length(); i++) {
            JSONObject point = points.getJSONObject(i);

            LatLng latLng = new LatLng(point.getDouble("latitude"), point.getDouble("longitude"));

            locations.add(latLng);
        }

        final MapView mapView = this.findViewById(R.id.map_view);
        mapView.onCreate(null);

        mapView.getMapAsync(new OnMapReadyCallback() {
            public void onMapReady(final GoogleMap googleMap) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                googleMap.getUiSettings().setZoomControlsEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.getUiSettings().setAllGesturesEnabled(false);

                Handler handler = new Handler(Looper.getMainLooper());

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final DisplayMetrics metrics = new DisplayMetrics();
                        me.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

                        if (locations.size() > 0) {
                            try {
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locations.get(0), 16));
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                                // View not ready to update yet...
                            }
                        }

                        for (LatLng latLng : locations) {
                            googleMap.addMarker(new MarkerOptions().position(latLng));
                        }
                    }
                }, 500);

                mapView.onResume();
            }
        });
    }

    protected int getCardLayoutResource() {
        return R.layout.card_question_location;
    }
}