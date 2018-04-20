package com.addi.salim.ubiquouscomputing1.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.addi.salim.ubiquouscomputing1.R;

public class MagicWheelWidget extends FrameLayout {

    private View smallCogwheel;
    private View largeCogwheel;
    private Animation clockRotation;
    private Animation counterClockRotation;

    public MagicWheelWidget(Context context) {
        super(context);
        initLayout(context);
    }

    public MagicWheelWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public MagicWheelWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    private void initLayout(Context context) {
        LayoutInflater.from(context).inflate(R.layout.magic_wheel_widget, this);
        smallCogwheel = findViewById(R.id.cog_wheel_small);
        largeCogwheel = findViewById(R.id.cog_wheel_large);
        clockRotation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clockwise);
        counterClockRotation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_counter_clockwise);
    }

    public void start() {
        setVisibility(View.VISIBLE);
        stop();
        smallCogwheel.startAnimation(clockRotation);
        largeCogwheel.startAnimation(counterClockRotation);
    }

    public void stop() {
        clockRotation.reset();
        counterClockRotation.reset();
    }

    @Override
    public void setVisibility(final int visibility) {
        super.setVisibility(visibility);
        if (visibility == View.GONE) {
            smallCogwheel.clearAnimation();
            largeCogwheel.clearAnimation();
        }
    }
}
