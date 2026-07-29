package xyz.blinddev.cookbreathe;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class FlameView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;

    public FlameView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1100L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float baseY = height * 0.86f;
        float pulse = 0.92f + phase * 0.18f;

        drawFlame(canvas, centerX, baseY, height * 0.58f * pulse, Color.argb(225, 255, 88, 0), Color.argb(0, 255, 88, 0));
        drawFlame(canvas, centerX, baseY - height * 0.08f, height * 0.42f * (1.1f - phase * 0.14f), Color.argb(235, 255, 178, 38), Color.argb(0, 255, 178, 38));
        drawFlame(canvas, centerX, baseY - height * 0.16f, height * 0.25f * pulse, Color.argb(220, 255, 238, 142), Color.argb(0, 255, 238, 142));
    }

    private void drawFlame(Canvas canvas, float cx, float cy, float radius, int inner, int outer) {
        paint.setShader(new RadialGradient(cx, cy, radius, inner, outer, Shader.TileMode.CLAMP));
        canvas.drawOval(cx - radius * 0.58f, cy - radius * 1.38f, cx + radius * 0.58f, cy + radius * 0.28f, paint);
        paint.setShader(null);
    }
}
