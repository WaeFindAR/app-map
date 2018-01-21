package penn.waefinder;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLocation;
    private Double LONGITUDE, LATITUDE;
    private Double defaultValue = 0.0;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        LONGITUDE = getIntent().getDoubleExtra("LONGITUDE", defaultValue);
        LATITUDE = getIntent().getDoubleExtra("LATITUDE", defaultValue);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Failed to get Location", Toast.LENGTH_SHORT);
            return ;
        }

        // Set global & Style
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));

        // Current Location Tracker
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        // Get current location
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                // Set global
                mLocation = location;

                // Current Location Success
                if (location != null) {

                    // Setup HTTP Call
                    String template = "https://maps.googleapis.com/maps/api/directions/json?origin=%s,%s&destination=%s,%s&key=AIzaSyB7nET7ULmOJ74MWSwuytzUn37oWDhT36I";
                    String url = String.format(template, location.getLatitude(), location.getLongitude(), LATITUDE, LONGITUDE);
                    Log.println(Log.INFO, "JSON_URL", url);
                    OkHttpClient client = new OkHttpClient();

                    // Do HTTP Call
                    Request req = new Request.Builder().url(url).get().build();
                    client.newCall(req).enqueue(new Callback() {

                        // Success
                        @Override
                        public void onResponse(final Call call, final Response response) throws IOException {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Holders
                                    try {
                                        JSONObject res = new JSONObject(response.body().string());
                                        JSONObject routes = (JSONObject) res.getJSONArray("routes").get(0);
                                        JSONObject legs = (JSONObject) routes.getJSONArray("legs").get(0);

                                        LatLng local = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                                        JSONArray steps = legs.getJSONArray("steps");

                                        List<LatLng> points = new ArrayList<>();
                                        points.add(local);
                                        for (int i = 0; i < steps.length(); i++) {
                                            String poly = steps.getJSONObject(i).getJSONObject("polyline").getString("points");
                                            String move = "Hello";

                                            try {
                                                move = steps.getJSONObject(i).getString("maneuver");
                                            } catch (JSONException err) {
                                                err.printStackTrace();
                                            }

                                            Log.println(Log.INFO, "MOVEMENT", move);
                                            List<LatLng> path = decodePoly(poly);
                                            points.addAll(path);
                                            mMap.addMarker(new MarkerOptions().position(path.get(0)).title(move + " Booda").icon(BitmapDescriptorFactory.fromResource(R.drawable.sprite)));
                                        }

                                        // Setup line
                                        PolylineOptions line = new PolylineOptions().width(10).color(Color.BLUE);
                                        line.addAll(points);

                                        // Add Start, Line, End
                                        mMap.addMarker(new MarkerOptions().position(local).title("You're Here Booda"));
                                        mMap.addPolyline(line);
                                        mMap.addMarker(new MarkerOptions().position((points.get(points.size() - 1))).title("Da Wae").icon(BitmapDescriptorFactory.fromResource(R.drawable.sprite)));

                                        // Move Camera to Last Point
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 20));
                                    } catch (Exception err) {
                                        err.printStackTrace();
                                    }
                                }
                            });
                        }

                        // Failure
                        @Override
                        public void onFailure(final Call call, IOException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "HTTP Failure :c", Toast.LENGTH_SHORT);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }
}
