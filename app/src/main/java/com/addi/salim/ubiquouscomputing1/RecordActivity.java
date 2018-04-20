package com.addi.salim.ubiquouscomputing1;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class RecordActivity extends AppCompatActivity {

    private Button startStopRecordingButton;
    private TextView recordingTextView;
    private boolean isRecording;
    private List<Event> recordedEvents;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;


    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (isRecording) {
                    if (recordedEvents.size() < Constants.MAX_RECORDED_EVENTS) {
                        recordedEvents.add(new Event(event.timestamp, event.values[0], event.values[1], event.values[2]));
                        recordingTextView.setText(recordedEvents.size() + " events recorded!");
                    } else {
                        Toast.makeText(RecordActivity.this, "Max activity recording reached...Stopping now...", Toast.LENGTH_LONG).show();
                        stopRecording();
                        showStartRecordButton();
                        finish();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        startStopRecordingButton = findViewById(R.id.recording_saving);
        recordingTextView = findViewById(R.id.events_recorded_text);
        startStopRecordingButton.setActivated(true);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        startStopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startStopRecordingButton.isActivated()) {
                    startRecording();
                    showStopRecordButton();
                } else {
                    stopRecording();
                    Utils.saveDataToFile(getApplication(), Constants.RECORDED_ACTIVITY_FILENAME, recordedEvents);
                    recordedEvents = null;
                    Toast.makeText(RecordActivity.this, "Activity recording completed and saved!", Toast.LENGTH_SHORT).show();
                    showStartRecordButton();
                    finish();
                }
            }
        });
    }

    private void showStopRecordButton() {
        startStopRecordingButton.setText(R.string.stop_recording);
        startStopRecordingButton.setActivated(false);
    }

    private void startRecording() {
        isRecording = true;
        recordedEvents = new ArrayList<>();
        mSensorManager.registerListener(sensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        Toast.makeText(this, "Recording activity...", Toast.LENGTH_SHORT).show();

    }

    private void stopRecording() {
        isRecording = false;
        mSensorManager.unregisterListener(sensorEventListener);
    }


    private void showStartRecordButton() {
        startStopRecordingButton.setText(R.string.start_recording);
        startStopRecordingButton.setActivated(true);
    }
}
