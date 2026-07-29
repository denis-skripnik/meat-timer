package xyz.blinddev.cookbreathe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

public class BreathOrbView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean inhale = false;
    private float scale = 0.78f;

    public BreathOrbView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setPhase(String phase) {
        boolean nextInhale = "inhale".equals(phase);
        inhale = nextInhale;
        animate().scaleX(nextInhale ? 1.2f : 0.72f).scaleY(nextInhale ? 1.2f : 0.72f).setDuration(850L).start();
        scale = nextInhale ? 1.2f : 0.72f;
        invalidate();
    }

    public void resetOrb() {
        inhale = false;
        scale = 0.78f;
        animate().scaleX(0.78f).scaleY(0.78f).setDuration(200L).start();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) * 0.42f;
        float cx = width / 2f;
        float cy = height / 2f;
        int inner = inhale ? Color.rgb(185, 235, 255) : Color.rgb(146, 216, 255);
        int outer = inhale ? Color.rgb(38, 122, 160) : Color.rgb(36, 106, 140);
        paint.setShader(new RadialGradient(cx, cy, radius, inner, outer, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.argb(inhale ? 170 : 120, 146, 216, 255));
        canvas.drawCircle(cx, cy, radius * (scale > 1f ? 1.02f : 0.94f), paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
