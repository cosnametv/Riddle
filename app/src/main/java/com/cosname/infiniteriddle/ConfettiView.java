// ConfettiView.java
package com.cosname.infiniteriddle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfettiView extends View {
    private List<Confetti> confettis = new ArrayList<>();
    private Paint paint = new Paint();
    private Random random = new Random();
    private boolean isAnimating = false;
    private long startTime;

    // Color palette for confetti
    private final int[] colors = {
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#9C27B0")
    };

    public ConfettiView(Context context) {
        super(context);
        init();
    }

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    public void startConfetti() {
        if (isAnimating) return;

        isAnimating = true;
        startTime = System.currentTimeMillis();
        confettis.clear();

        // Create 150 confetti particles
        for (int i = 0; i < 150; i++) {
            confettis.add(new Confetti(random));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isAnimating) return;

        boolean hasActive = false;
        for (Confetti confetti : confettis) {
            if (confetti.isActive()) {
                paint.setColor(confetti.color);
                canvas.drawCircle(confetti.x, confetti.y, confetti.radius, paint);
                confetti.update();
                hasActive = true;
            }
        }

        // Stop after 3 seconds
        if (System.currentTimeMillis() - startTime > 3000) {
            isAnimating = false;
        }

        if (hasActive) {
            invalidate();
        }
    }

    private class Confetti {
        float x, y;
        float radius;
        int color;
        float speed;
        float angle;
        float rotation;
        float rotationSpeed;

        Confetti(Random random) {
            reset(random);
        }

        void reset(Random random) {
            x = random.nextFloat() * getWidth();
            y = -random.nextFloat() * 100;
            radius = 4 + random.nextFloat() * 8;
            color = colors[random.nextInt(colors.length)];
            speed = 5 + random.nextFloat() * 15;
            angle = -90 + random.nextFloat() * 180;
            rotation = random.nextFloat() * 360;
            rotationSpeed = random.nextFloat() * 10 - 5;
        }

        void update() {
            speed += 0.1f;

            x += (float) (Math.cos(Math.toRadians(angle)) * 2);
            y += speed;
            rotation += rotationSpeed;
            if (y > getHeight() + 50 || x < -50 || x > getWidth() + 50) {
                reset(random);
            }
        }

        boolean isActive() {
            return isAnimating;
        }
    }
}