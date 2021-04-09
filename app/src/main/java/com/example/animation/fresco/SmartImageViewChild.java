package com.example.animation.fresco;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import java.util.List;

public interface SmartImageViewChild {
    void setActualImageScaleType(SmartImageView.ScaleType scaleType);

    void smartSetDimOnPressedEnabled(boolean dimOnPressed);

    void smartSetOverlayDrawable(Drawable drawable);

    void setPlaceholderImage(@Nullable Drawable drawable);

    void setPlaceholderImage(@Nullable Drawable drawable, SmartImageView.ScaleType scaleType);

    void setPlaceholderImageResource(@DrawableRes int placeholderImageResourceId);

    void setPlaceholderImageResource(@DrawableRes int placeholderImageResourceId, SmartImageView.ScaleType scaleType);

    void setPreserveLoadResultHandlers(boolean preserveLoadResultHandlers);

    void setTransformationMatrix(Matrix matrix);

    void setFadeDuration(int durationMs);

    void setViewSizeIsValid(boolean valid, int measuredWidth, int measuredHeight);

    void setSmartBorder(@ColorInt final int borderColor, int borderWidth);

    boolean isFinalOrIntermediateImageLoaded();

    boolean isFinalImageLoaded();

    void getActualImageBounds(RectF rect);

    void smartResetImage();

    void smartSetImageDrawable(Drawable drawable);

    void smartSetImageSourceDescs(@NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                                  boolean submitImmediately,
                                  boolean autoPlayAnimations,
                                  @Nullable SmartImageView.LoadResultHandler loadResultHandler);

    void setRoundedParams(RoundedParams roundedParams);

    @Nullable
    RoundedParams getRoundedParams();

    View getAsView();

    @Nullable
    Animatable getAnimatable();
}

