package com.example.offmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.offmap.services.SQLiteManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.shapes.GHPoint;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** @noinspection ALL*/
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final String MAP_FILE_NAME = "jordan.map";


    private static final float DISTANCE_THRESHOLD = 1000; // 1000 meters (adjust as needed)
    private Location lastKnownLocation;

    private LatLong jordanNorthEast = new LatLong(33.5, 39.0); // Define Jordan's northeast corner
    private LatLong jordanSouthWest = new LatLong(29.0, 34.0); // Define Jordan's southwest corner
    private MapView mapView;
    private Button openMapButton;
    private Button centerOnLocationButton;

    private FusedLocationProviderClient fusedLocationClient;
    private GraphHopper hopper;
    private Marker destinationMarker;
    private SQLiteManager sqLiteManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidGraphicFactory.createInstance(getApplication());

        setContentView(R.layout.activity_map);

        initViews();
        initHelpers();

        Log.d("MainActivity", "onCreate: Initializing");
        setupMapButton();
        checkLocationPermission();
        setupLocationButton();
        setupMapClickListener();
        setupMapLongClickListener();
    }

    private void initViews() {
        mapView = findViewById(R.id.map);
        openMapButton = findViewById(R.id.display_map_button);
        centerOnLocationButton = findViewById(R.id.center_on_location_button);

    }

    private void initHelpers() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sqLiteManager = new SQLiteManager(this);
        initGraphHopper();
    }

    // Init graphHopper
    private void initGraphHopper() {
        new AsyncTask<Void, Void, GraphHopper>() {
            @Override
            protected GraphHopper doInBackground(Void... voids) {
                GraphHopperOSM hopper = new GraphHopperOSM();
                hopper.setProfiles(new Profile("car").setVehicle("car").setWeighting("fastest"));

                try {
                    // Ensure maps exists
                    File mapsDir = new File(getExternalFilesDir(null), "maps");
                    if (!mapsDir.exists()) {
                        if (!mapsDir.exists()) {
                            Log.e("MainActivity", "Failed to create directory: " + mapsDir.getAbsolutePath());
                            return null;
                        }
                    }
                    Log.d("MainActivity", "Maps directory path: " + mapsDir.getAbsolutePath());

                    if (!copyFileFromAssets("config.properties", new File(mapsDir, "config.properties")) ||
                            !copyFileFromAssets("profiles.properties", new File(mapsDir, "profiles.properties"))) {
                        return null;
                    }

                    // Verify config files exist and log thier contents
                    verifyAndLogConfigFiles(mapsDir);
                    // Check if the PBF file exists, if not copy it
                    File pbfFile = new File(mapsDir, "jordan-latest.osm.pbf");
                    if (!pbfFile.exists() && !copyFileFromAssets("jordan-latest.osm.pbf", pbfFile)) {
                        return null;
                    }

                    hopper.setGraphHopperLocation(mapsDir.getAbsolutePath());
                    hopper.setDataReaderFile(pbfFile.getAbsolutePath());
                    hopper.setEncodingManager(EncodingManager.create("car"));
                    hopper.importOrLoad();
                    return hopper;
                } catch (Exception e) {
                    Log.e("MainActivity", "Error initializing GraphHopper: " + e.getMessage(), e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(GraphHopper graphHopper) {
                super.onPostExecute(graphHopper);
                if (graphHopper != null) {
                    hopper = graphHopper;
                    Log.d("MainActivity", "GraphHopper initialized");
                    Toast.makeText(MainActivity.this, "GraphHopper initialized", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(MainActivity.this, "Failed to initialize GraphHopper", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void setupMapButton() {
        openMapButton.setOnClickListener(v -> {
            File mapFile = new File(getExternalFilesDir(null), MAP_FILE_NAME);
            if (mapFile.exists()) {
                loadOfflineMap(mapFile);
            } else {
               copyFileFromAssetsAsync(
                       MAP_FILE_NAME,
                       mapFile,
                       this::loadOfflineMap,
                       () -> Toast.makeText(this, "Failed to copy map file.", Toast.LENGTH_SHORT).show()
               );
            }
        });
    }
    private void setupMapClickListener() {
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                LatLong center = mapView.getModel().mapViewPosition.getCenter();
                if (!isInJordanBounds(center)) {
                    mapView.getModel().mapViewPosition.setCenter(clampToJordanBounds(center));
                    return true;
                }
            }
            return false;
        });
    }

    private void setupMapLongClickListener() {
        mapView.setOnTouchListener(new View.OnTouchListener() {
            private long lastTouchDown;
            private boolean longClickPerformed;
            private float touchDownX;
            private float touchDownY;
            private static final int TOUCH_SLOP = 20; // Threshold for movement detection

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchDown = System.currentTimeMillis();
                        touchDownX = event.getX();
                        touchDownY = event.getY();
                        longClickPerformed = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!longClickPerformed && System.currentTimeMillis() - lastTouchDown > ViewConfiguration.getLongPressTimeout()) {
                            // Long press detected
                            LatLong clickLocation = mapView.getMapViewProjection().fromPixels((int) event.getX(), (int) event.getY());

                            showConfirmationDialog(clickLocation);
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // If the touch moves beyond a certain threshold, consider it not a long click
                        if (Math.abs(event.getX() - touchDownX) > TOUCH_SLOP || Math.abs(event.getY() - touchDownY) > TOUCH_SLOP) {
                            longClickPerformed = true;
                        }
                        break;
                }
                return false;
            }
        });
    }
    private void loadOfflineMap(File mapFile) {
        try {
            TileCache cache = AndroidUtil.createTileCache(
                    this, "mycache", mapView.getModel().displayModel.getTileSize(),
                    4096, mapView.getModel().frameBufferModel.getOverdrawFactor()
            );

            MapFile mapStore = new MapFile(mapFile);
            TileRendererLayer renderLayer = new TileRendererLayer(
                    cache, mapStore, mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE
            );

            renderLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
            mapView.getLayerManager().getLayers().add(renderLayer);
            mapView.setZoomLevelMin((byte) 10);
            mapView.setZoomLevelMax((byte) 20);
            mapView.setZoomLevel((byte) 12);
            mapView.setVisibility(View.VISIBLE);

            getCurrentLocation();
            openMapButton.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading map: " + e.getMessage());
        }
    }

    // Location Button
    private void setupLocationButton() {
        centerOnLocationButton.setOnClickListener(v -> {
            if (lastKnownLocation != null) {
                LatLong userLocation = new LatLong(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                mapView.setCenter(userLocation);
                mapView.setZoomLevel((byte) 15);
            }
        });
    }

    // Get Current Location
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location location = task.getResult();
                        LatLong userLocation = new LatLong(location.getLatitude(), location.getLongitude());
                        addMarker(userLocation);
                        mapView.setCenter(userLocation);
                        mapView.setZoomLevel((byte) 15);

                        lastKnownLocation = location; // Update last known location
                    } else {
                        Log.e("MainActivity", "Failed to get location.");
                    }
                }
            });
        }
    }


    // Helpers -----------------------------------------------------------
    // Calculate Routes
    private void calculateRoute(LatLong start, LatLong end) {
        if (hopper == null) {
            Toast.makeText(MainActivity.this, "GraphHopper is still initializing. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AsyncTask<Void, Void, GHResponse>() {
            @Override
            protected GHResponse doInBackground(Void... voids) {
                GHRequest request = new GHRequest(new GHPoint(start.latitude, start.longitude), new GHPoint(end.latitude, end.longitude))
                        .setProfile("car")
                        .setAlgorithm("dijkstra");

                GHResponse response = hopper.route(request);
                return response;
            }

            @Override
            protected void onPostExecute(GHResponse response) {
                if (response != null && !response.hasErrors()) {
                    // Handle the successful route calculation
                    Log.d("MainActivity", "Route calculated: " + response.getBest().getDistance() + " meters");
                    // Display route on map or provide details to the user
                    displayRoute(response, end);
                } else {
                    // Handle errors
                    Log.e("MainActivity", "Error calculating route: " + response.getErrors().toString());
                    Toast.makeText(MainActivity.this, "Error calculating route", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
    // Display Route
    private void displayRoute(GHResponse response, LatLong destination) {
        // Remove existing route layers if any
        for (int i = mapView.getLayerManager().getLayers().size() - 1; i >=0; i--) {
            if (mapView.getLayerManager().getLayers().get(i) instanceof Polyline) {
                mapView.getLayerManager().getLayers().remove(i);
            }
        }
        // Remove the existing destination marker if present
        if (destinationMarker != null) {
            mapView.getLayerManager().getLayers().remove(destinationMarker);
        }

        // Extract points from the route
        List<LatLong> routePoints = new ArrayList<>();
        response.getBest().getPoints().forEach(point -> routePoints.add(new LatLong(point.lat, point.lon)));

        // Create a polyline for the route
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);
        paint.setStyle(Style.STROKE);

        Polyline polyline = new Polyline(paint, AndroidGraphicFactory.INSTANCE);
        polyline.getLatLongs().addAll(routePoints);
        mapView.getLayerManager().getLayers().add(polyline);

        // Add marker for the destination
        Drawable destinationDrawable = ContextCompat.getDrawable(this, R.drawable.placeholder);
        if (destinationDrawable != null) {
            destinationMarker = new Marker(destination, AndroidGraphicFactory.convertToBitmap(destinationDrawable), 0, 0);
            mapView.getLayerManager().getLayers().add(destinationMarker);
        } else {
            Log.e("MainActivity", "Destination drawable is null or not found.");
        }

        // Display expected arrival time
        double distance = response.getBest().getDistance(); // in meters
        double time = response.getBest().getTime() / 1000; // in seconds
        double expectedArrivalTime = distance / 1000.0 / 60.0; // in min ( assuming an average speed of 60 km/h)
        Toast.makeText(this, "Distance: " + distance + " meters, ETA: " + expectedArrivalTime + " minutes", Toast.LENGTH_LONG).show();


    }

    // Dialogs
    private void showConfirmationDialog(LatLong destination) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Destination")
                .setMessage("Do you want to navigate to this location?")
                .setPositiveButton("Yes",(dialog, which) -> calculateRoute(new LatLong(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), destination))
                .setNegativeButton("No", null)
                .show();
    }

    private void showAddPlaceDialog(LatLong location) {
        new AlertDialog.Builder(this)
                .setTitle("Add Place")
                .setMessage("Save this place to favorite.")
                .setPositiveButton("Save", (dialog, which) -> {
                    boolean success = sqLiteManager.addFavorite("", "Favourite Place", location.latitude, location.longitude);
                    if (success)
                        Toast.makeText(this, "Place saved!", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "Place was not saved!", Toast.LENGTH_SHORT).show();

                })
                .setNegativeButton("Cancel", null)
                .show();

    }
    // Jordan bounderies
    private boolean isInJordanBounds(LatLong point) {
        // Check if a point is within Jordan's bounds
        return point.getLatitude() >= jordanSouthWest.getLatitude() &&
                point.getLatitude() <= jordanNorthEast.getLatitude() &&
                point.getLongitude() >= jordanSouthWest.getLongitude() &&
                point.getLongitude() <= jordanNorthEast.getLongitude();
    }
    private LatLong clampToJordanBounds(LatLong point) {
        // Clamp a point to stay within Jordan's bounds
        double lat = Math.min(Math.max(point.getLatitude(), jordanSouthWest.getLatitude()), jordanNorthEast.getLatitude());
        double lon = Math.min(Math.max(point.getLongitude(), jordanSouthWest.getLongitude()), jordanNorthEast.getLongitude());
        return new LatLong(lat, lon);
    }


    // Add marker for getCurrent Location
    private void addMarker(LatLong position) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_location_marker);
        if (drawable != null) {
            Marker marker = new Marker(position, AndroidGraphicFactory.convertToBitmap(drawable), 0, 0);
            mapView.getLayerManager().getLayers().add(marker);
        } else {
            Log.e("MainActivity", "Drawable is null or not found.");
        }
    }

    // Copy Files assets
    private void copyFileFromAssetsAsync(String assetFileName, File destinationFile, Consumer<File> onSuccess, Runnable onFailure) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                return copyFileFromAssets(assetFileName, destinationFile);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                if (success) {
                    onSuccess.accept(destinationFile);
                } else {
                    onFailure.run();
                }
            }
        };

        task.execute();
    }
    private boolean copyFileFromAssets(String assetFileName, File destinationFile) {
        try (InputStream inputStream = getAssets().open(assetFileName);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            Log.d("MainActivity", "File "+ assetFileName + "copied successfully to " + destinationFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e("MainActivity", "Failed to copy file" + assetFileName);
            return false;
        }
    }

    private void verifyAndLogConfigFiles(File mapsDir) {
        String[] configFiles = {"config.properties", "profiles.properites"};
        for (String fileName : configFiles) {
            File file = new File(mapsDir, fileName);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    StringBuilder content = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    Log.d("MainActivity", fileName+" contents: \n" + content.toString());

                } catch (IOException e) {
                    Log.e("MainActivity", "Error reading" + fileName + ": " + e.getMessage(), e);
                }
            }
        }
    }

    private void copyConfigFile(String assetFileName, File mapsDir) throws IOException {
        File configFile = new File(mapsDir, assetFileName);
        if (!configFile.exists() || configFile.length() == 0) {
            if (!copyFileFromAssets(assetFileName, configFile)) {
                throw new IOException("Failed to copy " + assetFileName);
            }
        } else {
            Log.d("MainActivity", "File " + assetFileName + " already exists.");
        }
    }
    private boolean copyPbFile(String assetFileName, File destinationFile) {
        return copyFileFromAssets(assetFileName, destinationFile);
    }

    // Check location permission
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }
}