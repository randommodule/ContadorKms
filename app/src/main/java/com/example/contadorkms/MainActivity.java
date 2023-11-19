package com.example.contadorkms;


import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Button startButton;
    private Button stopButton;
    private TextView distanceTextView;
    private TextView speedTextView;

    private boolean tracking = false;
    private double totalDistance = 0;
    private Location lastLocation;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        distanceTextView = findViewById(R.id.distanceTextView);
        speedTextView = findViewById(R.id.speedTextView);

        handler = new Handler(Looper.getMainLooper());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public void startTracking(View view) {
        tracking = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        totalDistance = 0;
        lastLocation = null;

        // Inicia el hilo para la obtención de la ubicación
        startLocationUpdates();
    }

    public void stopTracking(View view) {
        tracking = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        // Implementa tu lógica para obtener la ubicación aquí
        // Puedes usar FusedLocationProviderClient, LocationManager, etc.

        // Este es solo un ejemplo simulado
        Runnable locationRunnable = new Runnable() {
            @Override
            public void run() {
                // Simulación de una nueva ubicación cada segundo
                double latitude = Math.random() * 0.01 + 37.7749;
                double longitude = Math.random() * 0.01 - 122.4194;

                Location newLocation = new Location("SimulatedProvider");
                newLocation.setLatitude(latitude);
                newLocation.setLongitude(longitude);

                updateDistanceAndSpeed(newLocation);

                if (tracking) {
                    handler.postDelayed(this, 1000); // 1000 milisegundos = 1 segundo
                }
            }
        };

        handler.post(locationRunnable);
    }

    private void stopLocationUpdates() {
        handler.removeCallbacksAndMessages(null);
    }

    private void updateDistanceAndSpeed(Location newLocation) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(newLocation);
            totalDistance += distance;

            double speed = distance / 1.0; // Cambia el divisor según tus necesidades para obtener la velocidad en m/s, km/h, etc.

            final String distanceText = String.format("Distancia recorrida: %.2f km", totalDistance / 1000);
            final String speedText = String.format("Velocidad: %.2f m/s", speed);

            // Actualiza la interfaz de usuario en el hilo principal
            handler.post(new Runnable() {
                @Override
                public void run() {
                    distanceTextView.setText(distanceText);
                    speedTextView.setText(speedText);
                }
            });
        }

        lastLocation = newLocation;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, realiza las operaciones necesarias
            } else {
                // Permiso denegado, puedes mostrar un mensaje o realizar acciones adicionales
            }
        }
    }
}


