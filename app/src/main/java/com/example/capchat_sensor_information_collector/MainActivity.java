package com.example.capchat_sensor_information_collector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView tvDecibel;
    Button btnStartSensorGathering;
    MediaRecorder mRecorder;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvDecibel = findViewById(R.id.tvDecibel);
        btnStartSensorGathering = findViewById(R.id.btnStartSensorGathering);
        btnStartSensorGathering.setOnClickListener(this::onClickStartSensorGathering);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
        }

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
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    tvDecibel.setText(String.valueOf(getDecibelLevel()));
                });
            }
        }, 0, 200);
        btnStartSensorGathering.setEnabled(false);
    }

    private double getDecibelLevel() {
        if (mRecorder != null) {
            double db = 20 * Math.log(mRecorder.getMaxAmplitude() / 2700.0);
            if (db < 0) {
                db = 0;
            }
            return db;
        } else {
            return -1;
        }
    }
}
