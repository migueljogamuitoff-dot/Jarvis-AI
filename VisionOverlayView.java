package com.seuprojeto.nexus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.List;

public class VisionOverlayView extends View {

    private static final int[][] HAND_CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4},
            {0, 5}, {5, 6}, {6, 7}, {7, 8},
            {5, 9}, {9, 10}, {10, 11}, {11, 12},
            {9, 13}, {13, 14}, {14, 15}, {15, 16},
            {13, 17}, {17, 18}, {18, 19}, {19, 20},
            {0, 17}
    };

    private HandLandmarkerResult result;
    private int imageWidth = 1, imageHeight = 1;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public VisionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dotPaint.setColor(Color.parseColor("#4FD8FF"));
        dotPaint.setStyle(Paint.Style.FILL);
        linePaint.setColor(Color.parseColor("#B388FF"));
        linePaint.setStrokeWidth(5f);
    }

    public void updateResult(HandLandmarkerResult result, int imgW, int imgH) {
        this.result = result;
        this.imageWidth = imgW;
        this.imageHeight = imgH;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (result == null || result.landmarks().isEmpty()) return;

        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;

        for (List<NormalizedLandmark> hand : result.landmarks()) {
            for (int[] conn : HAND_CONNECTIONS) {
                NormalizedLandmark a = hand.get(conn[0]);
                NormalizedLandmark b = hand.get(conn[1]);
                canvas.drawLine(a.x() * imageWidth * scaleX, a.y() * imageHeight * scaleY,
                        b.x() * imageWidth * scaleX, b.y() * imageHeight * scaleY, linePaint);
            }
            for (NormalizedLandmark lm : hand) {
                canvas.drawCircle(lm.x() * imageWidth * scaleX, lm.y() * imageHeight * scaleY, 12f, dotPaint);
            }
        }
    }
}
