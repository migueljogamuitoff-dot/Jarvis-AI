package com.seuprojeto.nexus;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class OrbView extends View {

    public enum State { IDLE, LISTENING, THINKING, SPEAKING }

    private State state = State.IDLE;
    private float angle = 0f;
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public OrbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 360f);
        anim.setDuration(6000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            angle = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public void setState(State s) {
        this.state = s;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxR = Math.min(cx, cy) - 10f;

        int ringColor;
        int coreColor;
        float speedMultiplier;
        switch (state) {
            case LISTENING:
                ringColor = Color.parseColor("#FF5CA8");
                coreColor = Color.parseColor("#FF5CA8");
                speedMultiplier = 1.8f;
                break;
            case THINKING:
                ringColor = Color.parseColor("#B388FF");
                coreColor = Color.parseColor("#B388FF");
                speedMultiplier = 2.5f;
                break;
            case SPEAKING:
                ringColor = Color.parseColor("#4FD8FF");
                coreColor = Color.parseColor("#FFFFFF");
                speedMultiplier = 2.0f;
                break;
            default:
                ringColor = Color.parseColor("#4FD8FF");
                coreColor = Color.parseColor("#B388FF");
                speedMultiplier = 1f;
        }

        float a = angle * speedMultiplier;
        ringPaint.setColor(ringColor);

        for (int i = 1; i <= 3; i++) {
            float r = maxR * i / 3f;
            canvas.save();
            canvas.rotate(a * (i % 2 == 0 ? 1 : -1), cx, cy);
            ringPaint.setAlpha(255 - i * 50);
            canvas.drawCircle(cx, cy, r, ringPaint);
            canvas.drawLine(cx - r, cy, cx + r, cy, ringPaint);
            canvas.restore();
        }
        corePaint.setColor(coreColor);
        corePaint.setAlpha(255);
        canvas.drawCircle(cx, cy, 10f, corePaint);
        corePaint.setAlpha(80);
        canvas.drawCircle(cx, cy, 22f, corePaint);
    }
}
