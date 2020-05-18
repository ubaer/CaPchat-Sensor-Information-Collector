package com.example.capchat_sensor_information_collector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView tvDecibel;
    TextView tvAverage;
    Button btnStartSensorGathering;
    MediaRecorder mRecorder;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private final int MY_PERMISSIONS_INTERNET = 1;
    ArrayList<Integer> averageVolumes;
    double currentAmplitudeAvg = 0;
    double currentDecibelAvg = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDecibel = findViewById(R.id.tvDecibel);
        tvAverage = findViewById(R.id.tvAverage);
        btnStartSensorGathering = findViewById(R.id.btnStartSensorGathering);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_INTERNET);
        }
        btnStartSensorGathering.setOnClickListener(this::onClickStartSensorGathering);

        mRecorder = new MediaRecorder();

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile("/dev/null");
        try {
            mRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.start();
    }

    private void onClickStartSensorGathering(View v) {
        Timer timer = new Timer();
        averageVolumes = new ArrayList<>();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    int maxAmplitude = mRecorder.getMaxAmplitude();
                    tvDecibel.setText(String.valueOf(maxAmplitude));
                    averageVolumes.add(maxAmplitude);
                });
            }
        }, 0, 100);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    currentAmplitudeAvg = calculateAverage(averageVolumes);
                    tvAverage.setText(String.valueOf(currentAmplitudeAvg));
                    currentDecibelAvg = getDecibelLevel(currentAmplitudeAvg);
                    averageVolumes = new ArrayList<>();
                });
                if (currentAmplitudeAvg > 0) {
                    sendVolumeInfoPostRequest(currentAmplitudeAvg, currentDecibelAvg);
                }
            }
        }, 5000, 5000);

        btnStartSensorGathering.setEnabled(false);
    }

    private void sendVolumeInfoPostRequest(double amplitudeAverage, double decibelAverage) {
        try {
            URL url = new URL("http://192.168.1.13:5000/avg_volume/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            String jsonInputString = String.format(Locale.getDefault(), "{\"amplitude\":%f,\"decibel\":%f}", amplitudeAverage, decibelAverage);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response.toString());
            }

        } catch (MalformedURLException | ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double calculateAverage(List<Integer> values) {
        Integer sum = 0;
        if (!values.isEmpty()) {
            for (Integer mark : values) {
                sum += mark;
            }
            return sum.doubleValue() / values.size();
        }
        return sum;
    }

    private double getDecibelLevel(double level) {
        double db = 20 * Math.log10(level / 2700.0);
        if (db < 0) {
            db = 0;
        }
        return db;
    }
}
