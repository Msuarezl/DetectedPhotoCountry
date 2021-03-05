package com.example.evaluacion;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.example.evaluacion.WebServices.Asynchtask;
import com.example.evaluacion.WebServices.WebService;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Asynchtask {

    private GoogleMap mMap;
    private TextView view;
    private ImageView imageView;
    private JSONObject jsonObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        view = findViewById(R.id.IdText);
        imageView = findViewById(R.id.imageViewFlag);
        cord = new ArrayList<>();
        String countryCode = getIntent().getExtras().getString("countryCode");
        Glide.with( this.getApplicationContext()).load("http://www.geognos.com/api/en/countries/flag/"+countryCode+".png").into(imageView);
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Map<String, String> datos = new HashMap<String, String>();
        WebService ws= new WebService("http://www.geognos.com/api/en/countries/info/"+countryCode+".json", datos, this, this);
        ws.execute("");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    ArrayList<Double> cord;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
     //   LatLng sydney = new LatLng(-34, 151);
       // mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
      ///  mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


    }


    @Override
    public void processFinish(String result) throws JSONException {
        jsonObject =  new JSONObject(result);
        jsonObject = jsonObject.getJSONObject("Results");
        try {
            view.setText(jsonObject.getString("Name"));
            view.setText("Capital: "+jsonObject.getJSONObject("Capital").getString("Name")+"\n"+
                    "Code ISO 2: "+jsonObject.getJSONObject("CountryCodes").getString("iso2")+"\n"+
                    "Tel Prefix: "+jsonObject.getString("TelPref"));
            mMap.getUiSettings().setZoomControlsEnabled(false);
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            Double centerLat = jsonObject.getJSONArray("GeoPt").getDouble(0);
            Double centerLng = jsonObject.getJSONArray("GeoPt").getDouble(1);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(centerLat, centerLng), 4);
            mMap.moveCamera(cameraUpdate);
            cord.add(jsonObject.getJSONObject("GeoRectangle").getDouble("North")) ;
            cord.add(jsonObject.getJSONObject("GeoRectangle").getDouble("South"));
            cord.add(jsonObject.getJSONObject("GeoRectangle").getDouble("East"));
            cord.add(jsonObject.getJSONObject("GeoRectangle").getDouble("West"));
            PolylineOptions lines = new PolylineOptions()
                    .add(new LatLng(cord.get(0), cord.get(3)))
                    .add(new LatLng(cord.get(0), cord.get(2)))
                    .add(new LatLng(cord.get(1), cord.get(2)))
                    .add(new LatLng(cord.get(1), cord.get(3)))
                    .add(new LatLng(cord.get(0), cord.get(3)));
            mMap.addPolyline(lines);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}