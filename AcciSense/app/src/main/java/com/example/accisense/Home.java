package com.example.accisense;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.*;
import android.location.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Window;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.*;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

public class Home extends AppCompatActivity implements SensorEventListener {

    ImageView qrImage;
    TextView name, email, phone, vehicle, blood, emergency;
    Button btnStartDriving;

    DatabaseReference ref;
    LocationManager locationManager;

    SensorManager sensorManager;
    Sensor accelerometer, gyroscope;

    float accX, accY, accZ;
    float gyroX, gyroY, gyroZ;

    boolean isDriving = false;
    boolean alertSent = false;
    boolean userCancelled = false;

    MediaPlayer mediaPlayer;
    CountDownTimer timer;

    long lastSentTime = 0;

    RequestQueue queue;

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        qrImage = findViewById(R.id.qrImage);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        vehicle = findViewById(R.id.vehicle);
        blood = findViewById(R.id.blood);
        emergency = findViewById(R.id.emergency);
        btnStartDriving = findViewById(R.id.startDriveBtn);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.CALL_PHONE
                }, 101);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        generateQR(uid);

        ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child("Driver")
                .child(uid);

        loadDriverData();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        queue = Volley.newRequestQueue(this);

        btnStartDriving.setOnClickListener(v -> {

            isDriving = true;
            alertSent = false;

            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);

            Toast.makeText(this, "🚗 Drive Mode Started", Toast.LENGTH_SHORT).show();
        });

        checkLocationPermission();
    }

    private void loadDriverData() {

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                name.setText("Name: " + snapshot.child("name").getValue());
                email.setText("Email: " + snapshot.child("email").getValue());
                phone.setText("Phone: " + snapshot.child("phone").getValue());
                vehicle.setText("Vehicle: " + snapshot.child("vehicle").getValue());
                blood.setText("Blood Group: " + snapshot.child("blood").getValue());
                emergency.setText("Emergency: " + snapshot.child("emergencyPhone").getValue());
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void generateQR(String uid) {

        try {
            String qrData = "https://anuragsdeshmukh28.github.io/accisense-profile/driver.html?id=" + uid;

            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 400, 400);

            qrImage.setImageBitmap(bitmap);

        } catch (Exception e) {
            Log.e("QR_ERROR", e.getMessage());
        }
    }

    private void checkLocationPermission() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );

        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                5,
                location -> {

                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    ref.child("lat").setValue(lat);
                    ref.child("lon").setValue(lon);
                });
    }

    // 🔥 SENSOR
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!isDriving) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];
        }

        double gForce = Math.sqrt(accX * accX + accY * accY + accZ * accZ) / 9.8;

        Log.d("GFORCE", "Value: " + gForce);

        // 🔥 HYBRID TRIGGER
        if (gForce > 2.5 && !alertSent) {

            long currentTime = System.currentTimeMillis();

            if (currentTime - lastSentTime > 3000) {

                lastSentTime = currentTime;

                Log.d("TRIGGER", "Sending 6 features to ML");

                sendDataToServer();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // 🌐 SEND 6 FEATURES
    private void sendDataToServer() {

        String url = "http://10.106.187.110:5000/predict";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {

                    Log.d("SERVER_RESPONSE", response);

                    if (response.toLowerCase().contains("accident") && !alertSent) {

                        alertSent = true;

                        sensorManager.unregisterListener(this);

                        startEmergencyWarning();
                    }
                },
                error -> Log.d("SERVER ERROR", error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("accX", String.valueOf(accX));
                params.put("accY", String.valueOf(accY));
                params.put("accZ", String.valueOf(accZ));

                params.put("gyroX", String.valueOf(gyroX));
                params.put("gyroY", String.valueOf(gyroY));
                params.put("gyroZ", String.valueOf(gyroZ));

                return params;
            }
        };

        queue.add(request);
    }

    // 🚨 ALERT SYSTEM (same as before)
    private void startEmergencyWarning() {

        userCancelled = false;

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_emergency_popup);
        dialog.setCancelable(false);

        TextView txtTimer = dialog.findViewById(R.id.txtTimer);
        Button btnSafe = dialog.findViewById(R.id.btnSafe);

        dialog.show();

        timer = new CountDownTimer(10000, 1000) {

            int seconds = 10;

            public void onTick(long millisUntilFinished) {
                txtTimer.setText("Sending alert in " + seconds + " sec...");
                seconds--;
            }

            public void onFinish() {

                stopAlarm();
                dialog.dismiss();

                if (!userCancelled) {
                    sendAlertToFamily();
                }
            }
        }.start();

        btnSafe.setOnClickListener(v -> {

            userCancelled = true;
            stopAlarm();
            dialog.dismiss();

            Toast.makeText(this, "Alert Cancelled", Toast.LENGTH_SHORT).show();
        });
    }

    private void stopAlarm() {

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (timer != null) {
            timer.cancel();
        }
    }

    private void sendAlertToFamily() {

        ref.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                String phone = snapshot.child("emergencyPhone").getValue(String.class);
                Double lat = snapshot.child("lat").getValue(Double.class);
                Double lon = snapshot.child("lon").getValue(Double.class);

                String message = "🚨 Accident Detected!\nLocation:\nhttps://maps.google.com/?q=" + lat + "," + lon;

                SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + phone));
                startActivity(callIntent);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}