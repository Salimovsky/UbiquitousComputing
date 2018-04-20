package com.addi.salim.ubiquouscomputing1;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AccelerometerActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor stepDetectorSensor;
    private Sensor stepCounterSensor;
    private GraphView graphView1;
    private TextView accelerometerCounterView;
    private TextView stepDetectorSensorView;
    private TextView stepCounterSensorView;

    private LineGraphSeries<DataPoint> serieX;
    private LineGraphSeries<DataPoint> serieY;

    private Button launchRecorderButton;
    private Button replayRecordingButton;
    private boolean isLiveActivity = true;
    private LowPassFilter lowPassFilter;
    private WalkingSignalPeekDetector walkingXSignalPeekDetector;
    private WalkingSignalPeekDetector walkingYSignalPeekDetector;
    private WalkingSignalPeekDetector walkingZSignalPeekDetector;
    private int accelerometerStepsCounter = 0;
    private int stepDetectorSensorStepsCounter = 0;
    private int stepCounterSensorStepsCounter = 0;
    private Integer stepCounterSensorStepsCounterZero;
    private Animation animation;

    private long timestampNanoSecondsZero;
    private Double referenceX;

    private boolean isDebugMode;

    private final SensorEventListener accelerometerSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!isLiveActivity) {
                return;
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                processNewEvent(event.timestamp, event.values[0], event.values[1], event.values[2]);
            } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                stepDetectorSensorStepsCounter++;
                updateStepCountersUI();
            } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                if (stepCounterSensorStepsCounterZero == null) {
                    stepCounterSensorStepsCounterZero = (int) event.values[0];
                } else {
                    stepCounterSensorStepsCounter = (int) event.values[0] - stepCounterSensorStepsCounterZero;
                    updateStepCountersUI();
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

    };

    private final SensorEventListener stepDetectorSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            stepDetectorSensorStepsCounter++;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void processNewEvent(long timestamp, double... signal) {
        final double x = signal[0] + signal[1] + signal[2];
        final double y = signal[1];
        final double z = signal[2];

        if (lowPassFilter.isInitialized()) {
            final float time = (float) ((double) (timestamp - timestampNanoSecondsZero) / (double) (TimeUnit.SECONDS.toNanos(1)));
            final double[] filteredSignal = lowPassFilter.applyLowPassFilter(timestamp, x, y, z);
            serieX.appendData(new DataPoint(10 * time, 5 + filteredSignal[0] - referenceX), true, 1000);
            serieY.appendData(new DataPoint(10 * time, 5 + x - referenceX), true, 1000);

            if (walkingXSignalPeekDetector.addAndDetectPeak(timestamp, filteredSignal[0])) {
                accelerometerStepsCounter++;
                playBeep();
                animateStepsCounterText();
            }

            updateStepCountersUI();
        } else {
            referenceX = x;
            timestampNanoSecondsZero = timestamp;
            walkingXSignalPeekDetector.initialize(timestampNanoSecondsZero);
            walkingYSignalPeekDetector.initialize(timestampNanoSecondsZero);
            walkingZSignalPeekDetector.initialize(timestampNanoSecondsZero);
            walkingXSignalPeekDetector.addAndDetectPeak(timestamp, x);
            walkingYSignalPeekDetector.addAndDetectPeak(timestamp, y);
            walkingZSignalPeekDetector.addAndDetectPeak(timestamp, z);
            lowPassFilter.initialize(timestampNanoSecondsZero);
            lowPassFilter.applyLowPassFilter(timestamp, x, y, z);
            serieX.resetData(new DataPoint[]{});
            serieY.resetData(new DataPoint[]{});
        }
    }

    private void animateStepsCounterText() {
        animation.reset();
        accelerometerCounterView.clearAnimation();
        accelerometerCounterView.startAnimation(animation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        animation = AnimationUtils.loadAnimation(this, R.anim.animation);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        stepCounterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        lowPassFilter = new LowPassFilter(Constants.RC);
        walkingXSignalPeekDetector = new WalkingSignalPeekDetector();
        walkingYSignalPeekDetector = new WalkingSignalPeekDetector();
        walkingZSignalPeekDetector = new WalkingSignalPeekDetector();

        graphView1 = findViewById(R.id.graph1);
        accelerometerCounterView = findViewById(R.id.steps_counter);
        stepDetectorSensorView = findViewById(R.id.step_detector_sensor_steps_counter);
        stepCounterSensorView = findViewById(R.id.step_counter_sensor_steps_counter);
        launchRecorderButton = findViewById(R.id.launch_recorder);
        replayRecordingButton = findViewById(R.id.replay_recording);
        launchRecorderButton.setActivated(false);

        hideDebugMode();
        final View labelView = findViewById(R.id.steps_counter_label);
        labelView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isDebugMode) {
                    isDebugMode = false;
                    hideDebugMode();
                } else {
                    isDebugMode = true;
                    showDebugMode();
                }

                return true;
            }
        });

        replayRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (replayRecordingButton.isActivated()) {
                    playLiveSignal();
                    showReplayOfflineButton();
                } else {
                    replayOfflineActivity();
                    showLiveActivityButton();
                }
            }
        });

        launchRecorderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AccelerometerActivity.this, RecordActivity.class));
            }
        });


        serieX = new LineGraphSeries<>();
        // custom paint to make a dotted line
        final Paint paintBlue = new Paint();
        paintBlue.setStyle(Paint.Style.STROKE);
        paintBlue.setStrokeWidth(8);
        paintBlue.setColor(Color.BLUE);
        serieX.setCustomPaint(paintBlue);

        serieY = new LineGraphSeries<>();
        // custom paint to make a dotted line
        final Paint paintRed = new Paint();
        paintRed.setStyle(Paint.Style.STROKE);
        paintRed.setStrokeWidth(2);
        paintRed.setColor(Color.RED);
        serieY.setCustomPaint(paintRed);

        graphView1.addSeries(serieX);
        graphView1.addSeries(serieY);

        graphView1.getViewport().setXAxisBoundsManual(true);
        graphView1.getViewport().setMinX(0);
        graphView1.getViewport().setMaxX(100);
        graphView1.getViewport().setMaxY(10);
        graphView1.getViewport().setYAxisBoundsManual(true);
    }

    private void showDebugMode() {
        graphView1.setVisibility(View.VISIBLE);
        stepDetectorSensorView.setVisibility(View.VISIBLE);
        stepCounterSensorView.setVisibility(View.VISIBLE);
        launchRecorderButton.setVisibility(View.VISIBLE);
        replayRecordingButton.setVisibility(View.VISIBLE);
    }

    private void hideDebugMode() {
        graphView1.setVisibility(View.INVISIBLE);
        stepDetectorSensorView.setVisibility(View.INVISIBLE);
        stepCounterSensorView.setVisibility(View.INVISIBLE);
        launchRecorderButton.setVisibility(View.INVISIBLE);
        replayRecordingButton.setVisibility(View.INVISIBLE);
    }

    private void updateStepCountersUI() {
        accelerometerCounterView.setText(accelerometerStepsCounter + "");
        stepDetectorSensorView.setText(getString(R.string.step_detector_text, stepDetectorSensorStepsCounter + ""));
        stepCounterSensorView.setText(getString(R.string.step_counter_sensor_text, stepCounterSensorStepsCounter + ""));
    }

    private void playBeep() {
        if (isLiveActivity) {
            final ToneGenerator beep = new ToneGenerator(AudioManager.STREAM_MUSIC, 500);
            beep.startTone(ToneGenerator.TONE_CDMA_PIP, 200);
        }
    }

    private void playLiveSignal() {
        isLiveActivity = true;
        accelerometerStepsCounter = 0;
        stepDetectorSensorStepsCounter = 0;
        stepCounterSensorStepsCounter = 0;
        stepCounterSensorStepsCounterZero = null;
        accelerometerCounterView.setText(accelerometerStepsCounter + "");
        stepDetectorSensorView.setText(getString(R.string.step_detector_text, stepDetectorSensorStepsCounter + ""));
        stepCounterSensorView.setText(getString(R.string.step_counter_sensor_text, stepCounterSensorStepsCounter + ""));
        serieX.resetData(new DataPoint[]{});
        serieY.resetData(new DataPoint[]{});
        lowPassFilter.reset();
        walkingXSignalPeekDetector.reset();
    }

    private void replayOfflineActivity() {
        isLiveActivity = false;
        accelerometerStepsCounter = 0;
        stepDetectorSensorStepsCounter = 0;
        stepCounterSensorStepsCounter = 0;
        stepCounterSensorStepsCounterZero = null;
        accelerometerCounterView.setText(accelerometerStepsCounter + "");
        stepDetectorSensorView.setText(getString(R.string.step_detector_text, stepDetectorSensorStepsCounter + ""));
        stepCounterSensorView.setText(getString(R.string.step_counter_sensor_text, stepCounterSensorStepsCounter + ""));
        serieX.resetData(new DataPoint[]{});
        serieY.resetData(new DataPoint[]{});
        lowPassFilter.reset();
        walkingXSignalPeekDetector.reset();
        final List<Event> events = Utils.readDataFromFile(getApplication(), Constants.RECORDED_ACTIVITY_FILENAME);
        for (Event event : events) {
            processNewEvent(event.time, event.x, event.y, event.z);
        }
    }

    private void showReplayOfflineButton() {
        replayRecordingButton.setText(R.string.replay_offline_activity);
        replayRecordingButton.setActivated(false);
    }

    private void showLiveActivityButton() {
        replayRecordingButton.setText(R.string.show_live_activity);
        replayRecordingButton.setActivated(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSensorManager.registerListener(accelerometerSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(accelerometerSensorEventListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(accelerometerSensorEventListener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(accelerometerSensorEventListener);
    }
}
