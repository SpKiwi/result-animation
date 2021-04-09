package com.example.animation.fresco;

import android.content.Context;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.AnyRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.animation.R;
import com.facebook.cache.common.CacheKey;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.AspectRatioMeasure;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// We derive from LinearLayout, not from FrameLayout. That's because of the weird effect
// negative padding has on FrameLayout's subview positioning: subviews are never positioned
// beyond the top-right corner, which brakes image zoom effect in tango_chat_components drawer.
public class SmartImageView extends LinearLayout {

    public static final int DEFAULT_HIGH_RES_DELAY_MILLISEC = 1000;
    public static final int DEFAULT_FADE_DURATION_MILLISEC = 200;

    public static final int NO_CACHE_ONLY = 0;
    public static final int MEMORY_CACHE_ONLY = 1;
    public static final int MEMORY_OR_DISK_CACHE_ONLY = 2;

    public enum ScaleType {
        // Scales width and height independently, so that the child matches the parent exactly.
        // This may change the aspect ratio of the child.
        FIT_XY,

        // Scales the child so that it fits entirely inside the parent. At least one dimension (width or
        // height) will fit exactly. Aspect ratio is preserved.
        // Child is aligned to the top-left corner of the parent.
        FIT_START,

        // Scales the child so that it fits entirely inside the parent. At least one dimension (width or
        // height) will fit exactly. Aspect ratio is preserved.
        // Child is centered within the parent's bounds.
        FIT_CENTER,

        // Scales the child so that it fits entirely inside the parent. At least one dimension (width or
        // height) will fit exactly. Aspect ratio is preserved.
        // Child is aligned to the bottom-right corner of the parent.
        FIT_END,

        // Performs no scaling.
        // Child is centered within parent's bounds.
        CENTER,

        // Scales the child so that it fits entirely inside the parent. Unlike FIT_CENTER, if the child
        // is smaller, no up-scaling will be performed. Aspect ratio is preserved.
        // Child is centered within parent's bounds.
        CENTER_INSIDE,

        // Scales the child so that both dimensions will be greater than or equal to the corresponding
        // dimension of the parent. At least one dimension (width or height) will fit exactly.
        // Child is centered within parent's bounds.
        CENTER_CROP,

        // Scales the child so that both dimensions will be greater than or equal to the corresponding
        // dimension of the parent. At least one dimension (width or height) will fit exactly.
        // The child's focus point will be centered within the parent's bounds as much as possible
        // without leaving empty space.
        // It is guaranteed that the focus point will be visible and centered as much as possible.
        // If the focus point is set to (0.5f, 0.5f), result will be equivalent to CENTER_CROP.
        FOCUS_CROP,

        /* Only supported by SmartTangoImageViewChild. */
        MATRIX
    }

    public enum SetImageFlags {
        LowResFromCacheOnly,
        HighResFromCacheOnly,
        HighResDelay,
        ForceProgressiveImageLoading,
        SubmitImmediately,
        AutoPlayAnimations
    }

    public interface LoadResultHandler {
        public abstract void onIntermediateImageLoaded();
        public abstract void onFinalImageLoaded();
        public abstract void onImageLoadingFailed();
        public abstract void onImageLoadingCancelled();
    }

    public interface BitmapGenerator {
        public Object generateBitmap(@Nullable Object bitmapGeneratorParams,
                                     Context context,
                                     int reizeWidth,
                                     int resizeHeight,
                                     BitmapGeneratorCallback bitmapGeneratorCallback);
    }

    public interface BitmapGeneratorCallback {
        public void onBitmapGenerated(Bitmap bitmap);
        public void onBitmapGenerationFailed();
    }

    public static class ImageSourceDesc {
        public String imageUri = null;
        public BitmapGenerator bitmapGenerator = null;
        public Object bitmapGeneratorParams = null;
        public int resizeWidth = -1;
        public int resizeHeight = -1;
        public boolean resizeToFit = false;
        public int cacheOnlyLevel = NO_CACHE_ONLY;
        public boolean adaptToBandwidth = false;
        public boolean isPreferred = false;
        public boolean isFatal = false;
        public int delayMillisec = 0;
        public SmartImageViewPostprocessor postprocessor = null;

        // Preserve original bitmap aspect ratio when resizing.
        // Fresco always preserves bitmap aspect ratio, but CacheableImageView image loader doesn't.
        // This is useful when we intend to change image view size (and aspect ratio) and still
        // display the same bitmap (e.g. for zoom-in animation in photo gallery with square tiles).
        public boolean resizeNoCrop = false;

        public ImageSourceDesc(final String imageUri) {
            // Treat "" and null the same for image URIs.
            this.imageUri = TextUtils.isEmpty(imageUri) ? null : imageUri;
        }

        public ImageSourceDesc(final BitmapGenerator bitmapGenerator,
                               final Object bitmapGeneratorParams)
        {
            this.bitmapGenerator = bitmapGenerator;
            this.bitmapGeneratorParams = bitmapGeneratorParams;
        }

        public boolean hasResizeToFit() {
            // resize to fit requested and size is not set explicitly
            return resizeToFit && (resizeWidth <= 0 || resizeHeight <= 0);
        }

        public boolean equals(final ImageSourceDesc imageSourceDesc) {
            if (imageSourceDesc == null) {
                return false;
            }

            if (   cacheOnlyLevel != imageSourceDesc.cacheOnlyLevel
                    || adaptToBandwidth != imageSourceDesc.adaptToBandwidth
                    || isPreferred != imageSourceDesc.isPreferred
                    || isFatal != imageSourceDesc.isFatal)
            {
                return false;
            }

            if (!TextUtils.equals(imageUri, imageSourceDesc.imageUri)) {
                return false;
            }

            if ((bitmapGenerator == null) != (imageSourceDesc.bitmapGenerator == null)) {
                return false;
            }
            if (bitmapGenerator != null && !bitmapGenerator.equals(imageSourceDesc.bitmapGenerator)) {
                return false;
            }

            if ((bitmapGeneratorParams == null) != (imageSourceDesc.bitmapGeneratorParams == null)) {
                return false;
            }
            if (bitmapGeneratorParams != null && !bitmapGeneratorParams.equals(imageSourceDesc.bitmapGeneratorParams)) {
                return false;
            }

            return true;
        }
    }

    public interface External {
        boolean getUseFresco();

        boolean isProgressiveImageLoadingEnabled();

        boolean isDownlinkBandwidthGoodEnough();

        int getHighResDelayMillisec();

        ImagePipelineConfig getFrescoImagePipelineConfig();

        MemoryCache<CacheKey, CloseableImage> getImageMemoryCache();

        SyncDiskCache getSyncDiskCache();

        SmartImageViewChild createChild(Context context,
                                        @DrawableRes int failureImageResourceId,
                                        @DrawableRes int placeholderImageResourceId,
                                        @DrawableRes int overlayImageResourceId,
                                        int fadeDuration,
                                        SmartImageView.ScaleType actualImageScaleType,
                                        SmartImageView.ScaleType failureImageScaleType,
                                        SmartImageView.ScaleType placeholderImageScaleType,
                                        boolean dimOnPressed,
                                        RoundedParams cornersDescriptor,
                                        float borderWidth,
                                        int borderColor);

        void prefetch(@NonNull String uri);
    }

    private static External s_defaultFactory = new External() {
        @Override
        public boolean getUseFresco() {
            return true;
        }

        @Override
        public boolean isProgressiveImageLoadingEnabled() {
            return true;
        }

        @Override
        public boolean isDownlinkBandwidthGoodEnough() {
            return true;
        }

        @Override
        public int getHighResDelayMillisec() {
            return DEFAULT_HIGH_RES_DELAY_MILLISEC;
        }

        @Override
        public ImagePipelineConfig getFrescoImagePipelineConfig() {
            return null;
        }

        @Override
        public MemoryCache<CacheKey, CloseableImage> getImageMemoryCache() {
            return null;
        }

        @Override
        public SyncDiskCache getSyncDiskCache() {
            return null;
        }

        @Override
        public SmartImageViewChild createChild(
                Context context,
                @DrawableRes int failureImageResourceId,
                @DrawableRes int placeholderImageResourceId,
                @DrawableRes int overlayImageResourceId,
                int fadeDuration,
                SmartImageView.ScaleType actualImageScaleType,
                SmartImageView.ScaleType failureImageScaleType,
                SmartImageView.ScaleType placeholderImageScaleType,
                boolean dimOnPressed,
                RoundedParams roundedParams,
                float borderWidth,
                int borderColor) {
            return new SmartFrescoImageView(context,
                    failureImageResourceId,
                    placeholderImageResourceId,
                    overlayImageResourceId,
                    fadeDuration,
                    actualImageScaleType,
                    failureImageScaleType,
                    placeholderImageScaleType,
                    dimOnPressed,
                    roundedParams,
                    borderWidth,
                    borderColor);
        }

        @Override
        public void prefetch(@NonNull String uri) {
            final ImageRequestBuilder requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(uri));
            Fresco.getImagePipeline().prefetchToBitmapCache(requestBuilder.build(), null);
        }
    };

    private static External s_factory = s_defaultFactory;

    private SmartImageViewChild m_child = null;

    private @Nullable List<ImageSourceDesc> mImageSourceDescs;
    private @Nullable Set<SetImageFlags> mFlags;

    private boolean m_attached = false;
    private boolean m_temporaryDetach = false;

    private float m_aspectRatio = 0.0f;
    private final AspectRatioMeasure.Spec m_measureSpec = new AspectRatioMeasure.Spec();

    private boolean m_resizeToFit = false;

    public static void setExternal(final External external) {
        s_factory = (external != null) ? external : s_defaultFactory;
        SmartImageViewPostprocessorsWrapper.updateWithExternal(s_factory);
    }

    public static boolean isInStandaloneMode() {
        return s_factory == s_defaultFactory;
    }

    public static ImagePipelineConfig getFrescoImagePipelineConfig() {
        return s_factory.getFrescoImagePipelineConfig();
    }

    public static MemoryCache<CacheKey, CloseableImage> getImageMemoryCache() {
        return s_factory.getImageMemoryCache();
    }

    public static SyncDiskCache getSyncDiskCache() {
        return s_factory.getSyncDiskCache();
    }

    public static int getHighResDelayMillisec() {
        return s_factory.getHighResDelayMillisec();
    }

    public SmartImageView(final Context context) {
        super(context);
        init(context, /*attrs=*/ null, /*defaultRoundAsCircle=*/ false);
    }

    public SmartImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, /*defaultRoundAsCircle=*/ false);
    }

    public SmartImageView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, /*defaultRoundAsCircle=*/ false);
    }

    public SmartImageView(final boolean defaultRoundAsCircle, final Context context) {
        super(context);
        init(context, /*attrs=*/ null, defaultRoundAsCircle);
    }

    public SmartImageView(final boolean defaultRoundAsCircle, final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, defaultRoundAsCircle);
    }

    public SmartImageView(final boolean defaultRoundAsCircle, final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defaultRoundAsCircle);
    }

    private void init(final Context context, final AttributeSet attrs, final boolean defaultRoundAsCircle) {
        int                    failureImageResourceId     = 0;
        int                    placeholderImageResourceId = 0;
        int                    overlayImageResourceId     = 0;
        int                    fadeDuration               = DEFAULT_FADE_DURATION_MILLISEC;
        SmartImageView.ScaleType actualImageScaleType      = SmartImageView.ScaleType.CENTER_CROP;
        SmartImageView.ScaleType failureImageScaleType     = SmartImageView.ScaleType.CENTER_INSIDE;
        SmartImageView.ScaleType placeholderImageScaleType = SmartImageView.ScaleType.CENTER_INSIDE;
        boolean                dimOnPressed               = false;
        float                  borderWidth                = 0.0f;
        int                    borderColor                = 0xffffffff;
        RoundedParams          roundedParams              = new RoundedParams();

        if (attrs != null) {
            final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SmartImageView);
            try {
                m_aspectRatio = ta.getFloat(R.styleable.SmartImageView_viewAspectRatio, m_aspectRatio);

                failureImageResourceId     = ta.getResourceId(R.styleable.SmartImageView_failureImage,     failureImageResourceId);
                placeholderImageResourceId = ta.getResourceId(R.styleable.SmartImageView_placeholderImage, placeholderImageResourceId);
                overlayImageResourceId     = ta.getResourceId(R.styleable.SmartImageView_overlayImage,     overlayImageResourceId);
                fadeDuration               = ta.getInt       (R.styleable.SmartImageView_fadeDuration,     fadeDuration);

                actualImageScaleType      = getScaleTypeFromXml(ta, R.styleable.SmartImageView_actualImageScaleType,      actualImageScaleType);
                failureImageScaleType     = getScaleTypeFromXml(ta, R.styleable.SmartImageView_failureImageScaleType,     failureImageScaleType);
                placeholderImageScaleType = getScaleTypeFromXml(ta, R.styleable.SmartImageView_placeholderImageScaleType, placeholderImageScaleType);

                dimOnPressed  = ta.getBoolean(R.styleable.SmartImageView_dimOnPressed,  dimOnPressed);
                m_resizeToFit = ta.getBoolean(R.styleable.SmartImageView_resizeToFit,   m_resizeToFit);
                roundedParams = new RoundedParams(ta, defaultRoundAsCircle);

                borderWidth = ta.getDimension(R.styleable.SmartImageView_smartBorderWidth, borderWidth);
                borderColor = ta.getColor(R.styleable.SmartImageView_smartBorderColor, borderColor);
            } finally {
                ta.recycle();
            }
        }

        External factory = s_factory.getUseFresco() ? s_defaultFactory : s_factory;

        m_child = factory.createChild(context,
                failureImageResourceId,
                placeholderImageResourceId,
                overlayImageResourceId,
                fadeDuration,
                actualImageScaleType,
                failureImageScaleType,
                placeholderImageScaleType,
                dimOnPressed,
                roundedParams,
                borderWidth,
                borderColor);

        addView(m_child.getAsView(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setRoundedParams(RoundedParams roundedParams) {
        m_child.setRoundedParams(roundedParams);
    }

    @Nullable
    public RoundedParams getRoundedParams() {
        return m_child.getRoundedParams();
    }

    public void setActualImageScaleType(final SmartImageView.ScaleType scaleType) {
        m_child.setActualImageScaleType(scaleType);
    }

    public void setDimOnPressedEnabled(final boolean dimOnPressed) {
        m_child.smartSetDimOnPressedEnabled(dimOnPressed);
    }

    public void setOverlayDrawable(final Drawable drawable) {
        m_child.smartSetOverlayDrawable(drawable);
    }

    public void setPlaceholderImage(Drawable drawable) {
        m_child.setPlaceholderImage(drawable);
    }

    public void setPlaceholderImage(Drawable drawable, SmartImageView.ScaleType scaleType) {
        m_child.setPlaceholderImage(drawable, scaleType);
    }

    public void setPlaceholderImageResource(@DrawableRes int placeholderImageResourceId) {
        m_child.setPlaceholderImageResource(placeholderImageResourceId);
    }

    public void setPlaceholderImageResource(@DrawableRes int placeholderImageResourceId, SmartImageView.ScaleType scaleType) {
        m_child.setPlaceholderImageResource(placeholderImageResourceId, scaleType);
    }

    public void setFadeDuration(final int durationMs) {
        m_child.setFadeDuration(durationMs);
    }

    public void setSmartBorder(@ColorRes final int borderColor, @DimenRes int borderWidth) {
        int color = getResources().getColor(borderColor);
        int width = (int) getResources().getDimension(borderWidth);
        m_child.setSmartBorder(color, width);
    }

    /**
     * @note new value does not have effect until uri is set
     */
    public void setResizeToFit(final boolean resizeToFit) {
        m_resizeToFit = resizeToFit;
    }

    public void setAspectRatio(final float aspectRatio) {
        if (aspectRatio == m_aspectRatio) {
            return;
        }
        m_aspectRatio = aspectRatio;
        requestLayout();
    }

    public float getAspectRatio() {
        return m_aspectRatio;
    }

    public void setPreserveLoadResultHandlers(final boolean preserveLoadResultHandlers) {
        m_child.setPreserveLoadResultHandlers(preserveLoadResultHandlers);
    }

    /**
     * Sets the alpha value that should be applied to the image.
     *
     * @param alpha the alpha value that should be applied to the image
     *
     * @deprecated use #setImageAlpha(int) instead
     */
    public void setAlpha(final int alpha) {
        ImageView imageView = (ImageView) m_child;
        imageView.setAlpha(alpha);
    }

    /**
     * Sets the alpha value that should be applied to the image.
     *
     * @param alpha the alpha value that should be applied to the image
     *
     * @see #getImageAlpha()
     */
    public void setImageAlpha(final int alpha) {
        ImageView imageView = (ImageView) m_child;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imageView.setImageAlpha(alpha);
        } else {
            imageView.setAlpha(alpha);
        }
    }

    /**
     * Returns the alpha that will be applied to the drawable of this ImageView.
     *
     * @return the alpha that will be applied to the drawable of this ImageView
     *
     * @see #setImageAlpha(int)
     */
    public int getImageAlpha() {
        ImageView imageView = (ImageView) m_child;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return imageView.getImageAlpha();
        } else {
            return (int) (imageView.getAlpha() * 255.0f);
        }
    }

    public void setActualImageFocusPoint(final PointF focusPoint) {
        // TODO SmartImageViewChild.setActualImageFocusPoint()
        if (m_child instanceof SmartFrescoImageView) {
            ((SmartFrescoImageView) m_child).getHierarchy().setActualImageFocusPoint(focusPoint);
        }
    }

    public boolean isFinalOrIntermediateImageLoaded() {
        return m_child.isFinalOrIntermediateImageLoaded();
    }

    public boolean isFinalImageLoaded() {
        return m_child.isFinalImageLoaded();
    }

    /**
     * Soft-refresh an image (e.g. try to reload image if image load has failed before)
     */
    public void smartRefreshImage() {
        if (mImageSourceDescs != null) {
            smartSetImageSourceDescs(mImageSourceDescs, mFlags, null);
        }
    }

    public void smartResetImage() {
        m_child.smartResetImage();
        mImageSourceDescs = null;
        mFlags = null;
    }

    public void getActualImageBounds(final RectF rect) {
        m_child.getActualImageBounds(rect);
    }

    public void smartSetImageDrawable(Drawable drawable) {
        m_child.smartSetImageDrawable(drawable);
    }

    public void smartSetImageResource(@DrawableRes int resourceId)
    {
        smartSetImageUri((resourceId > 0 ? resourceIdToUri(resourceId) : null),
                /*flags=*/ null,
                /*loadResultHandler=*/ null,
                /*postprocessor*/ null);
    }

    public void smartSetImageUri(final @Nullable String imageUri)
    {
        smartSetImageUri(imageUri, /*flags=*/ null, /*loadResultHandler=*/ null, /*postprocessor*/ null);
    }

    public void smartSetImageUri(final @Nullable String imageUri,
                                 final @Nullable SmartImageViewPostprocessor postprocessor) {
        smartSetImageUri(imageUri, null, null, postprocessor);
    }

    public void smartSetImageUri(final @Nullable String imageUri,
                                 final @Nullable Set<SetImageFlags> flags) {
        smartSetImageUri(imageUri, flags, /*loadResultHandler=*/ null, /*postprocessor*/ null);
    }

    public void smartSetImageUri(final @Nullable String imageUri,
                                 final @Nullable Set<SetImageFlags> flags,
                                 final @Nullable LoadResultHandler loadResultHandler)
    {
        smartSetImageUri(imageUri, flags, loadResultHandler, null);
    }

    public void smartSetImageUri(final @Nullable String imageUri,
                                 final @Nullable Set<SetImageFlags> flags,
                                 final @Nullable LoadResultHandler loadResultHandler,
                                 final @Nullable SmartImageViewPostprocessor postprocessor)
    {
        List<ImageSourceDesc> imageSourceDescs = new ArrayList<>();
        if (!TextUtils.isEmpty(imageUri)) {
            final ImageSourceDesc desc = new ImageSourceDesc(imageUri);
            desc.isPreferred = true;
            desc.isFatal = true;
            desc.postprocessor = SmartImageViewPostprocessorsWrapper.wrap(postprocessor);
            desc.resizeToFit = m_resizeToFit;
            imageSourceDescs.add(desc);
        }

        smartSetImageSourceDescs(imageSourceDescs, flags, loadResultHandler);
    }

    public void smartSetImageUriWithLowResFirst(final @Nullable String lowResImageUri,
                                                final @Nullable String highResImageUri)
    {
        smartSetImageUriWithLowResFirst(lowResImageUri, highResImageUri, /*flags=*/ null, /*loadResultHandler=*/ null);
    }

    public void smartSetImageUriWithLowResFirst(final @Nullable String lowResImageUri,
                                                final @Nullable String highResImageUri,
                                                final @Nullable Set<SetImageFlags> flags)
    {
        smartSetImageUriWithLowResFirst(lowResImageUri, highResImageUri, flags, /*loadResultHandler=*/ null);
    }

    public void smartSetImageUriWithLowResFirst(final @Nullable String lowResImageUri,
                                                final @Nullable String highResImageUri,
                                                final @Nullable Set<SmartImageView.SetImageFlags> flags,
                                                final @Nullable SmartImageView.LoadResultHandler loadResultHandler)
    {
        if (TextUtils.isEmpty(lowResImageUri)) {
            smartSetImageUri(highResImageUri, flags, loadResultHandler);
            return;
        }

        if (TextUtils.isEmpty(highResImageUri)) {
            smartSetImageUri(lowResImageUri, flags, loadResultHandler);
            return;
        }

        boolean lowResFromCacheOnly = false;
        boolean highResFromCacheOnly = false;
        boolean forceProgressiveImageLoading = false;
        boolean loadHighResWithDelay = false;
        boolean resizeToFit = m_resizeToFit;
        if (flags != null) {
            lowResFromCacheOnly = flags.contains(SmartImageView.SetImageFlags.LowResFromCacheOnly);
            highResFromCacheOnly = flags.contains(SmartImageView.SetImageFlags.HighResFromCacheOnly);
            loadHighResWithDelay = flags.contains(SmartImageView.SetImageFlags.HighResDelay);
            forceProgressiveImageLoading = flags.contains(SmartImageView.SetImageFlags.ForceProgressiveImageLoading);
        }

        boolean progressiveEnabled = forceProgressiveImageLoading || s_factory.isProgressiveImageLoadingEnabled();

        final List<SmartImageView.ImageSourceDesc> imageSourceDescs = new ArrayList<>(3);

        // preferred: high-res image from cache
        {
            final ImageSourceDesc desc = new ImageSourceDesc(highResImageUri);
            desc.cacheOnlyLevel = MEMORY_OR_DISK_CACHE_ONLY;
            desc.isPreferred = true;
            desc.resizeToFit = resizeToFit;
            imageSourceDescs.add(desc);
        }

        // first: low-res image
        if (lowResFromCacheOnly) {
            // from cache
            final ImageSourceDesc desc = new ImageSourceDesc(lowResImageUri);
            desc.cacheOnlyLevel = MEMORY_OR_DISK_CACHE_ONLY;
            desc.resizeToFit = resizeToFit;
            imageSourceDescs.add(desc);
        } else {
            // from network or from cache
            final ImageSourceDesc desc = new ImageSourceDesc(lowResImageUri);
            desc.isFatal = true;
            desc.resizeToFit = resizeToFit;
            imageSourceDescs.add(desc);
        }

        // then: high-res from network
        if (!highResFromCacheOnly && progressiveEnabled) {
            final ImageSourceDesc desc = new ImageSourceDesc(highResImageUri);
            desc.adaptToBandwidth = true;
            desc.isPreferred = true;
            desc.isFatal = true;
            desc.delayMillisec = loadHighResWithDelay ? s_factory.getHighResDelayMillisec() : 0;
            desc.resizeToFit = resizeToFit;
            imageSourceDescs.add(desc);
        }

        smartSetImageSourceDescs(imageSourceDescs, flags, loadResultHandler);
    }

    public void smartSetFirstAvailableImageUri(final @Nullable String[] multiImageUris)
    {
        smartSetFirstAvailableImageUri(multiImageUris, /*flags=*/ null, /*loadResultHandler=*/ null);
    }

    public void smartSetFirstAvailableImageUri(final @Nullable String[] multiImageUris,
                                               final @Nullable Set<SetImageFlags> flags)
    {
        smartSetFirstAvailableImageUri(multiImageUris, flags, /*loadResultHandler=*/ null);
    }

    public void smartSetFirstAvailableImageUri(@Nullable String[] multiImageUris,
                                               final @Nullable Set<SetImageFlags> flags,
                                               final @Nullable LoadResultHandler loadResultHandler)
    {
        multiImageUris = removeEmptyStrings(multiImageUris);

        List<ImageSourceDesc> imageSourceDescs = new ArrayList<>();
        if (multiImageUris != null) {
            for (final String uri : multiImageUris) {
                final ImageSourceDesc desc = new ImageSourceDesc(uri);
                desc.isPreferred = true;
                desc.resizeToFit = m_resizeToFit;
                imageSourceDescs.add(desc);
            }
        }

        smartSetImageSourceDescs(imageSourceDescs, flags, loadResultHandler);
    }

    public void smartSetBitmapGenerator(final @Nullable BitmapGenerator bitmapGenerator,
                                        final @Nullable Object bitmapGeneratorParams,
                                        final @Nullable Set<SetImageFlags> flags,
                                        final @Nullable LoadResultHandler loadResultHandler)
    {
        List<ImageSourceDesc> imageSourceDescs = new ArrayList<>();
        if (bitmapGenerator != null) {
            final ImageSourceDesc desc = new ImageSourceDesc(bitmapGenerator, bitmapGeneratorParams);
            desc.isPreferred = true;
            desc.isFatal = true;
            desc.resizeToFit = m_resizeToFit;
            imageSourceDescs.add(desc);
        }

        smartSetImageSourceDescs(imageSourceDescs, flags, loadResultHandler);
    }

    public void smartSetImageSourceDescs(@NonNull List<ImageSourceDesc> imageSourceDescs,
                                         final @Nullable Set<SetImageFlags> flags,
                                         final @Nullable LoadResultHandler loadResultHandler)
    {
        mImageSourceDescs = imageSourceDescs;
        mFlags = flags;
        m_child.smartSetImageSourceDescs(imageSourceDescs,
                (flags != null && flags.contains(SetImageFlags.SubmitImmediately)),
                (flags != null && flags.contains(SetImageFlags.AutoPlayAnimations)),
                maybeWrapLoadResultHandler(loadResultHandler));
    }

    @Nullable
    public List<ImageSourceDesc> getImageSourceDescs() {
        return mImageSourceDescs;
    }

    @Nullable
    public Set<SetImageFlags> getFlags() {
        return mFlags;
    }

    protected LoadResultHandler maybeWrapLoadResultHandler(final LoadResultHandler loadResultHandler) {
        return loadResultHandler;
    }

    protected void setTransformationMatrix(final Matrix matrix) {
        m_child.setTransformationMatrix(matrix);
    }

    protected void setViewSizeIsValid(final boolean valid, final int measuredWidth, final int measuredHeight) {
        m_child.setViewSizeIsValid(valid, measuredWidth, measuredHeight);
    }

    public boolean isAttached() { return m_attached && !m_temporaryDetach; }

    @Override protected void onDetachedFromWindow()    { m_attached = false; m_temporaryDetach = false; super.onDetachedFromWindow(); }
    @Override protected void onAttachedToWindow()      { m_attached = true;  m_temporaryDetach = false; super.onAttachedToWindow(); }
    @Override public    void onStartTemporaryDetach()  { m_temporaryDetach = true;  super.onStartTemporaryDetach(); }
    @Override public    void onFinishTemporaryDetach() { m_temporaryDetach = false; super.onFinishTemporaryDetach(); }

    @Override
    protected void onMeasure(final int widthSpec, final int heightSpec) {
        m_measureSpec.width = widthSpec;
        m_measureSpec.height = heightSpec;
        AspectRatioMeasure.updateMeasureSpec(m_measureSpec,
                m_aspectRatio,
                getLayoutParams(),
                getPaddingLeft() + getPaddingRight(),
                getPaddingTop() + getPaddingBottom());
        super.onMeasure(m_measureSpec.width, m_measureSpec.height);
    }

    public static boolean isDownlinkBandwidthGoodEnough() {
        return s_factory.isDownlinkBandwidthGoodEnough();
    }

    private static SmartImageView.ScaleType getScaleTypeFromXml(final TypedArray attrs,
                                                                final int attrId,
                                                                final SmartImageView.ScaleType defaultScaleType)
    {
        final int idx = attrs.getInt(attrId, -1);
        if (idx < 0) {
            return defaultScaleType;
        }
        return SmartImageView.ScaleType.values()[idx];
    }

    static ScalingUtils.ScaleType smartToFrescoScaleType (final SmartImageView.ScaleType scaleType) {
        switch (scaleType) {
            case FIT_XY:        return ScalingUtils.ScaleType.FIT_XY;
            case FIT_START:     return ScalingUtils.ScaleType.FIT_START;
            case FIT_CENTER:    return ScalingUtils.ScaleType.FIT_CENTER;
            case FIT_END:       return ScalingUtils.ScaleType.FIT_END;
            case CENTER:        return ScalingUtils.ScaleType.CENTER;
            case CENTER_INSIDE: return ScalingUtils.ScaleType.CENTER_INSIDE;
            case CENTER_CROP:   return ScalingUtils.ScaleType.CENTER_CROP;
            case MATRIX:        return ScalingUtils.ScaleType.FIT_START;
            case FOCUS_CROP:    return ScalingUtils.ScaleType.FOCUS_CROP;
        }
        return ScalingUtils.ScaleType.CENTER_CROP;
    }

    public static String resourceIdToUri(@AnyRes int resourceId) {
        return "res:///" + resourceId;
    }

    public static void prefetch(@NonNull String uri) {
        s_factory.prefetch(uri);
    }

    private static String[] removeEmptyStrings(@Nullable String[] multiImageUris) {
        if (multiImageUris == null) {
            return null;
        }

        final int numUris = multiImageUris.length;

        int emptyCnt = 0;
        for (int i = 0; i < numUris; ++i) {
            if (TextUtils.isEmpty(multiImageUris[i])) {
                ++emptyCnt;
            }
        }

        if (numUris == emptyCnt) {
            // covers multiImageUris.length == 0 as well
            return null;
        }

        if (emptyCnt == 0) {
            return multiImageUris;
        }

        final String[] newUris = new String [numUris - emptyCnt];
        int j = 0;
        for (int i = 0; i < numUris; ++i) {
            if (!TextUtils.isEmpty(multiImageUris[i])) {
                newUris[j] = multiImageUris[i];
                ++j;
            }
        }

        return newUris;
    }

    public @Nullable Animatable getAnimatable() {
        return m_child.getAnimatable();
    }
}

