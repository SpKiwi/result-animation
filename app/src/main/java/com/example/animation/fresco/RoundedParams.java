package com.example.animation.fresco;

import android.content.res.TypedArray;

import com.example.animation.R;
import com.facebook.drawee.generic.RoundingParams;


public class RoundedParams extends RoundingParams {
    public RoundedParams() {
        setRoundAsCircle(false);
    }

    RoundedParams(TypedArray ta, boolean defaultRoundAsCircle) {
        setRoundAsCircle(ta.getBoolean(R.styleable.SmartImageView_roundAsCircle, defaultRoundAsCircle));
        float roundedCornerRadius = ta.getDimension(R.styleable.SmartImageView_roundedCornerRadius, 0.f);
        if (roundedCornerRadius > 0.f) {
            float topLeft = ta.getBoolean(R.styleable.SmartImageView_roundTopLeft, false) ? roundedCornerRadius : 0;
            float topRight = ta.getBoolean(R.styleable.SmartImageView_roundTopRight, false) ? roundedCornerRadius : 0;
            float bottomLeft = ta.getBoolean(R.styleable.SmartImageView_roundBottomLeft, false) ? roundedCornerRadius : 0;
            float bottomRight = ta.getBoolean(R.styleable.SmartImageView_roundBottomRight, false) ? roundedCornerRadius : 0;
            setCornersRadii(topLeft, topRight, bottomRight, bottomLeft);
        }
    }

    public float getTopLeftCornerRadius() {
        return getCornerRadius(0);
    }

    public float getTopRightCornerRadius() {
        return getCornerRadius(2);
    }

    public float getBottomRightCornerRadius() {
        return getCornerRadius(4);
    }

    public float getBottomLeftCornerRadius() {
        return getCornerRadius(6);
    }

    private float getCornerRadius(int position) {
        float[] radii = getCornersRadii();
        if (radii != null && position < radii.length) {
            return radii[position];
        }
        return 0;
    }
}
