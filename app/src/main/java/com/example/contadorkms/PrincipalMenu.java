package com.example.contadorkms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


public class PrincipalMenu extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private String  newspeed;

    private Marker startMarker=null;

    private Marker currentLocationMarker;
    private Polyline routePolyline;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private Button location;
    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView calorias;
    private GoogleMap googleMap;
    private MediaPlayer startsound;
    private LocationManager locationManager;
    private boolean tracking = false;
    private Spinner weightSpinner;
    private double totalDistance = 0;
    private Location lastLocation;
    private boolean paused = false;
    private boolean isFirstLocation = true;

    private LocationHandlerThread locationHandlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_menu);
        startsound = MediaPlayer.create(this, R.raw.startsound);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        resetButton = findViewById(R.id.resetButton);
        location=findViewById(R.id.sendLocationButton);
        distanceTextView = findViewById(R.id.distanceTextView);
        speedTextView = findViewById(R.id.speedTextView);
        calorias = findViewById(R.id.caloriesTextView);
        weightSpinner = findViewById(R.id.weightSpinner);
        String[] weightValues = new String[201];
        for (int i = 0; i <= 200; i++) {
            weightValues[i] = String.valueOf(i);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weightValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightSpinner.setAdapter(adapter);

        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelerometer != null) {
          sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
          Toast.makeText(this,"Este dispositivo no tiene acelerometro",Toast.LENGTH_SHORT).show();
        }

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTracking(v);
            }
        });
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendLocation(v);
            }
        });

        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        requestLocationPermission();
        locationHandlerThread = new LocationHandlerThread("LocationHandlerThread", locationListener);
        locationHandlerThread.start();
        locationHandlerThread.prepareHandler();
        locationHandlerThread.setLocationListener(locationListener);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }

    public void goToMainActivity(View view) {
        startsound.start();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            startLocationUpdates();
        }
    }

    public void startTracking(View view) {
        startsound.start();
        if (weightSpinner.getSelectedItem().equals("0")) {
            Toast.makeText(this, "No ha ingresado su peso en kilos", Toast.LENGTH_SHORT).show();
        }
        else {
            tracking = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            paused = false;
            if (lastLocation == null) {
                lastLocation = getLastKnownLocation();
            }


            if (lastLocation != null && googleMap != null && startMarker == null) {
                LatLng startLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                startMarker = googleMap.addMarker(new MarkerOptions()
                        .position(startLatLng)
                        .title("Punto de inicio")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f));
            }
            locationHandlerThread.requestLocationUpdates(locationManager);
        }
    }

    public void resetTracking(View view) {
        startsound.start();

        if (weightSpinner.getSelectedItem().equals("0")) {
            Toast.makeText(this, "No ha ingresado su peso en kilos", Toast.LENGTH_SHORT).show();
        } else {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            totalDistance = 0;
            isFirstLocation = true;
            distanceTextView.setText("Distancia recorrida: 0 km");
            speedTextView.setText("Velocidad: 0 m/s");
            calorias.setText("Calorias quemadas: 0 cal.");
            paused = false;
            if (startMarker != null) {
                startMarker.remove();
                startMarker = null;
            }
            if (lastLocation != null && googleMap != null && startMarker == null) {
                LatLng startLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                startMarker = googleMap.addMarker(new MarkerOptions()
                        .position(startLatLng)
                        .title("Punto de inicio")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f));
            }
            Toast.makeText(this, "Recorrido reiniciado", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopTracking(View view) {
        startsound.start();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        speedTextView.setText("Velocidad: 0 m/s");
        if (tracking) {
            tracking = false;
            paused = true;

            stopLocationUpdates();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            showCurrentLocationOnMap(lastKnownLocation);
        }
    }

    private void showCurrentLocationOnMap(Location location) {
        if (googleMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentLocationMarker == null) {
                        currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                                .position(currentLatLng)
                                .title("Ubicación Actual")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    } else {
                        currentLocationMarker.setPosition(currentLatLng);
                    }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                    if (startMarker != null) {
                        if (routePolyline != null) {
                            routePolyline.remove();
                        }
                        LatLng startPoint = startMarker.getPosition();
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .add(startPoint, currentLatLng)
                                .width(5)
                                .color(Color.BLUE);

                        routePolyline = googleMap.addPolyline(polylineOptions);
                    }
                }
            });
        }
    }

    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return null;
    }

    public void startLocationUpdates() {
        if (locationHandlerThread != null) {
            locationHandlerThread.requestLocationUpdates(locationManager);
        } else {
            Log.e("PrincipalMenu", "locationHandlerThread is null");
        }
    }

    private void stopLocationUpdates() {
        if (!paused && locationManager != null) {
            locationHandlerThread.stopLocationUpdates(locationManager);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speedTextView.setText("Velocidad: 0 m/s");
                }
            });
        }
    }

    private void updateDistanceAndSpeed(Location newLocation) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(newLocation);
            totalDistance += distance;
            long timeElapsed = (newLocation.getTime() - lastLocation.getTime()) / 1000;
            if (timeElapsed > 0) {
                double speed = distance / timeElapsed;
                final String speedText = String.format("Velocidad: %.2f m/s", speed);
                final String distanceText = String.format("Distancia recorrida: %.2f km", totalDistance / 1000);

                if(!paused){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        distanceTextView.setText(distanceText);
                        if (accelerometer == null) {
                            speedTextView.setText(speedText);
                        }
                        calorias.setText("Calorias quemadas: " + ((int) (1.03 * Integer.parseInt(weightSpinner.getSelectedItem().toString()) * (totalDistance / 1000))) + " cal.");
                        showCurrentLocationOnMap(newLocation);
                    }
                });
            }
            }
        }
        lastLocation = newLocation;
        if (isFirstLocation) {
            lastLocation = newLocation;
            isFirstLocation = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Se requiere acceso a la ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

        if (locationHandlerThread != null) {
            locationHandlerThread.quit();
            locationHandlerThread = null;
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location newLocation) {
            updateDistanceAndSpeed(newLocation);
        }
    };
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(accelerometer!=null){
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float velocidadX = event.values[0];
            float velocidadY = event.values[1];
            float velocidadZ = event.values[2];


            float velocidadTotal = (float) Math.sqrt(velocidadX * velocidadX + velocidadY * velocidadY + velocidadZ * velocidadZ);

            float umbralMovimiento = 1.0f;

            if (velocidadTotal > umbralMovimiento) {
               newspeed = "Velocidad: " + velocidadTotal + " m/s";
               if(!paused){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speedTextView.setText(newspeed);
                        calorias.setText("Calorias quemadas: " + ((int)(1.03 * Integer.parseInt(weightSpinner.getSelectedItem().toString()) * (totalDistance / 1000))) + " cal.");
                  }
                });
               }
            }
        }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    public class LocationHandlerThread extends HandlerThread {
        private Handler handler;
        private LocationListener locationListener;
        public void setLocationListener(LocationListener listener) {
            locationListener = listener;
        }
        public LocationHandlerThread(String name, LocationListener listener) {
            super(name);
            locationListener = listener;
        }

        public void postTask(Runnable task) {
            handler.post(task);
        }

        public void prepareHandler() {
            handler = new Handler(getLooper());
        }

        public void requestLocationUpdates(LocationManager locationManager) {
            if (handler == null) {
                throw new IllegalStateException("Handler not prepared. Call prepareHandler() first.");
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (locationManager != null && locationListener != null) {
                        if (ActivityCompat.checkSelfPermission(PrincipalMenu.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(PrincipalMenu.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

                    }
                }
            });
        }

        public void stopLocationUpdates(LocationManager locationManager) {
            if (handler == null) {
                throw new IllegalStateException("Handler not prepared. Call prepareHandler() first.");
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (locationManager != null && locationListener != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                }
            });
        }
    }
    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return null;
    }
    private String generateGoogleMapsLink(double latitude, double longitude) {
        return "https://www.google.com/maps?q=" + latitude + "," + longitude;
    }
    public void sendLocation(View view) {
        Location currentLocation = getCurrentLocation();

        if (currentLocation != null) {
            double latitude = currentLocation.getLatitude();
            double longitude = currentLocation.getLongitude();

            String googleMapsLink = generateGoogleMapsLink(latitude, longitude);

            Intent sendIntent = new Intent("android.intent.action.SEND");
            Location myposition=getCurrentLocation();
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Mi ubicación: " + generateGoogleMapsLink(myposition.getLatitude(),myposition.getLongitude()));
            sendIntent.setType("text/plain");
            sendIntent.setPackage("com.whatsapp");

            try {
                startActivity(sendIntent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "No se pudo enviar la ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }
}