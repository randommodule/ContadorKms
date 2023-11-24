package com.example.contadorkms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class PrincipalMenu extends AppCompatActivity implements OnMapReadyCallback{

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Button startButton;
    private Button stopButton;
    private Button resetButton;
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
        startsound =MediaPlayer.create(this,R.raw.startsound);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        resetButton = findViewById(R.id.resetButton);
        distanceTextView = findViewById(R.id.distanceTextView);
        speedTextView = findViewById(R.id.speedTextView);
        calorias = findViewById(R.id.caloriesTextView);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        weightSpinner=findViewById(R.id.weightSpinner);
        String[] weightValues = new String[201];
        for (int i = 0; i <= 200; i++) {
            weightValues[i] = String.valueOf(i);
        }

        // Crea un adaptador y establece los valores al Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weightValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightSpinner.setAdapter(adapter);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTracking(v);
            }
        });

        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

        // Solicitar permisos
        requestLocationPermission();

        // Inicializa el LocationHandlerThread
        locationHandlerThread = new LocationHandlerThread("LocationHandlerThread", locationListener);
        locationHandlerThread.start();
        locationHandlerThread.prepareHandler();
        locationHandlerThread.setLocationListener(locationListener);
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
        if(weightSpinner.getSelectedItem().equals("0")){
            Toast.makeText(this,"No ha seleccionado su peso",Toast.LENGTH_SHORT).show();
        }
        else{
            if (!tracking ) {
                tracking = true;
                startButton.setEnabled(false);
                stopButton.setEnabled(true);

                if (paused) {
                    paused = false; // Se reinicia el indicador de pausa
                    // No reiniciar totalDistance y lastLocation, para continuar desde donde se detuvo
                } else {
                    // Si no está pausado, se está iniciando desde cero, entonces reinicia los valores
                    if (lastLocation == null) {
                        // Solo reinicia si lastLocation es nulo, es decir, al inicio
                        totalDistance = 0;
                    }
                }

                locationHandlerThread.requestLocationUpdates(locationManager);
            }
        }


        }
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Puedes personalizar el mapa aquí, si es necesario

        // Mostrar la ubicación actual si está disponible
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            showCurrentLocationOnMap(lastKnownLocation);
        }
    }

    private void showCurrentLocationOnMap(Location location) {
        if (googleMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Mueve la cámara a la ubicación actual
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));

            // Puedes agregar un marcador si lo deseas
            googleMap.addMarker(new MarkerOptions().position(currentLatLng).title("Ubicación Actual"));

            // Puedes ajustar el nivel de zoom según tus necesidades
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
        }
    }


    private Location getLastKnownLocation() {
        return null;  // Implementa la lógica para obtener la última ubicación conocida
    }

    public void stopTracking(View view) {
        startsound.start();
        if (tracking) {
            tracking = false;
            paused = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            stopLocationUpdates();
        }
    }

    public void startLocationUpdates() {
        if (locationHandlerThread != null) {
            locationHandlerThread.requestLocationUpdates(locationManager);
        } else {
            // Manejar la situación donde locationHandlerThread es nulo
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

    public void resetTracking(View view) {
        startsound.start();
        if(weightSpinner.getSelectedItem().equals("0")){
            Toast.makeText(this,"No ha seleccionado su peso",Toast.LENGTH_SHORT).show();
        }else{
            totalDistance = 0;
            lastLocation = null;
            isFirstLocation = true;
            distanceTextView.setText("Distancia recorrida: 0 km");
            speedTextView.setText("Velocidad: 0 m/s");
            calorias.setText("Calorias quemadas: 0 cal.");
            paused=false;
            startTracking(view);
            Toast.makeText(this, "Recorrido reiniciado", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDistanceAndSpeed(Location newLocation) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(newLocation);
            totalDistance += distance;

            long timeElapsed = (newLocation.getTime() - lastLocation.getTime()) / 1000;
            if (timeElapsed > 0) {
                double speed = distance / timeElapsed;

                final String distanceText = String.format("Distancia recorrida: %.2f km", totalDistance / 1000);
                final String speedText = String.format("Velocidad: %.2f m/s", speed);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        distanceTextView.setText(distanceText);
                        speedTextView.setText(speedText);
                        calorias.setText("Calorias quemadas: " + ((int)(1.03 * Integer.parseInt(weightSpinner.getSelectedItem().toString()) * (totalDistance / 1000))) + " cal.");
                    }
                });
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
                // Permiso denegado, muestra un mensaje o realiza acciones adicionales
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
        // Otros métodos de LocationListener...
    };




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
            // Asegúrate de que el handler esté preparado antes de llamar a este método
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
            // Asegúrate de que el handler esté preparado antes de llamar a este método
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


}