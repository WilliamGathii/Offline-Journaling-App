package com.example.offlinedailyjournal;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class CustomTypefaceSpan extends MetricAffectingSpan {
    private final Typeface typeface;

    public CustomTypefaceSpan(Typeface typeface) {
        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        apply(tp);
    }

    @Override
    public void updateMeasureState(TextPaint tp) {
        apply(tp);
    }

    private void apply(Paint paint) {
        paint.setTypeface(typeface);
    }
}
