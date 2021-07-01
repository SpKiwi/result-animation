package com.example.animation.fresco;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.example.animation.R;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.AbstractDataSource;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ArrayDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.view.DraweeView;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.CloseableReferenceFactory;
import com.facebook.imagepipeline.core.ExecutorSupplier;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ProducerFactory;
import com.facebook.imagepipeline.datasource.CloseableProducerToDataSourceAdapter;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.listener.ForwardingRequestListener2;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.producers.NullProducer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueueImpl;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

class SmartFrescoImageView extends DraweeView<GenericDraweeHierarchy> implements SmartImageViewChild, SmartImageViewHelper.SetControllerCallback {
    private final static String TAG = "SmartFrescoImageView";
    private final static boolean enableLog = false;
    private static final boolean ENABLE_DECODE_CANCELLATION = true;
    private static final boolean ENABLE_WEBP_DIRECT_DECODING = true;

    private static void debugLog(final String str, Object... args) {
        if (enableLog) {
            if (args == null || args.length == 0) {
                Log.d(TAG, str);
            } else {
                Log.d(TAG, String.format(str, args));
            }
        }
    }

    private SmartImageView.ScaleType m_placeholderImageScaleType;
    private RoundedParams m_roundedParams;
    private Matrix m_matrix = null;

    private ArrayDrawable m_overlayArrayDrawable = null;
    private Drawable m_emptyOverlayDrawable = null;
    private Drawable m_emptyDimDrawable = null;
    private final int OVERLAY_LAYER_IDX = 0;
    private final int DIM_LAYER_IDX = 1;
    private final int NUM_LAYERS = 2;

    private final SmartImageViewHelper m_helper = new SmartImageViewHelper();
    /**
     * Index of first preferred image which is in cache
     */
    private int m_preferredCacheIndex = Integer.MAX_VALUE;
    private BaseControllerListener<ImageInfo> m_controllerHandler = null;

    private static final ResourceReleaser<Bitmap> s_bitmapReleaser = new ResourceReleaser<Bitmap>() {
        @Override
        public void release(final Bitmap bitmap) {
            bitmap.recycle();
        }
    };

    // unused constructor to silence lint error
    private SmartFrescoImageView(final Context context) {
        super(context);
        m_placeholderImageScaleType = SmartImageView.ScaleType.CENTER_INSIDE;
    }

    public SmartFrescoImageView(final Context context,
                                final int failureImageResourceId,
                                final int placeholderImageResourceId,
                                final int overlayImageResourceId,
                                final int fadeDuration,
                                final SmartImageView.ScaleType actualImageScaleType,
                                final SmartImageView.ScaleType failureImageScaleType,
                                final SmartImageView.ScaleType placeholderImageScaleType,
                                final boolean dimOnPressed,
                                final RoundedParams roundedParams,
                                final float borderWidth,
                                final int borderColor) {
        super(context);
        setHierarchy(buildDraweeHierarchy(context,
                failureImageResourceId,
                placeholderImageResourceId,
                overlayImageResourceId,
                fadeDuration,
                SmartImageView.smartToFrescoScaleType(actualImageScaleType),
                SmartImageView.smartToFrescoScaleType(failureImageScaleType),
                SmartImageView.smartToFrescoScaleType(placeholderImageScaleType),
                roundedParams,
                borderWidth,
                borderColor));

        m_placeholderImageScaleType = placeholderImageScaleType;
        m_roundedParams = roundedParams;
        smartSetDimOnPressedEnabled(dimOnPressed);
    }

    private static GenericDraweeHierarchy buildDraweeHierarchy(
            final Context context,
            final int failureImageResourceId,
            final int placeholderImageResourceId,
            final int overlayImageResourceId,
            final int fadeDuration,
            final ScalingUtils.ScaleType actualImageScaleType,
            final ScalingUtils.ScaleType failureImageScaleType,
            final ScalingUtils.ScaleType placeholderImageScaleType,
            final RoundedParams roundedParams,
            final float borderWidth,
            final int borderColor) {
        final Resources resources = context.getResources();
        final GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources);
        if (failureImageResourceId > 0) {
            builder.setFailureImage(resources.getDrawable(failureImageResourceId), failureImageScaleType);
        }
        if (placeholderImageResourceId > 0) {
            builder.setPlaceholderImage(resources.getDrawable(placeholderImageResourceId), placeholderImageScaleType);
        }
        if (overlayImageResourceId > 0) {
            builder.setOverlay(resources.getDrawable(overlayImageResourceId));
        }
        builder.setFadeDuration(fadeDuration);
        builder.setActualImageScaleType(actualImageScaleType);
        if (borderWidth > 0) {
            roundedParams.setBorder(borderColor, borderWidth);
        }
        builder.setRoundingParams(roundedParams);
        return builder.build();
    }

    @Override
    public void setFadeDuration(int durationMs) {
        getHierarchy().setFadeDuration(durationMs);
    }

    @Override
    public void setActualImageScaleType(final SmartImageView.ScaleType scaleType) {
        getHierarchy().setActualImageScaleType(SmartImageView.smartToFrescoScaleType(scaleType));
    }

    private void createOverlayLayers() {
        if (m_overlayArrayDrawable != null) {
            return;
        }

        if (m_emptyOverlayDrawable == null) {
            m_emptyOverlayDrawable = new ColorDrawable(Color.TRANSPARENT);
        }
        if (m_emptyDimDrawable == null) {
            m_emptyDimDrawable = new ColorDrawable(Color.TRANSPARENT);
        }

        final Drawable[] drawableLayers = new Drawable[NUM_LAYERS];
        drawableLayers[OVERLAY_LAYER_IDX] = m_emptyOverlayDrawable;
        drawableLayers[DIM_LAYER_IDX] = m_emptyDimDrawable;

        m_overlayArrayDrawable = new ArrayDrawable(drawableLayers);
        getHierarchy().setControllerOverlay(m_overlayArrayDrawable);
    }

    @Override
    public void smartSetDimOnPressedEnabled(final boolean dimOnPressed) {
        if (!dimOnPressed) {
            if (m_overlayArrayDrawable == null) {
                return;
            }
            createOverlayLayers();
            m_overlayArrayDrawable.setDrawable(DIM_LAYER_IDX, m_emptyDimDrawable);
            return;
        }

        final Drawable drawable;
        if (m_roundedParams.getRoundAsCircle()) {
            drawable = getResources().getDrawable(R.drawable.round_selection_dim);
        } else {
            drawable = new ColorDrawable(getResources().getColor(R.color.selection_dim));
        }
        final StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, drawable);

        createOverlayLayers();
        m_overlayArrayDrawable.setDrawable(DIM_LAYER_IDX, stateListDrawable);
    }

    @Override
    public void smartSetOverlayDrawable(final Drawable drawable) {
        if (drawable == null) {
            if (m_overlayArrayDrawable == null) {
                return;
            }
            createOverlayLayers();
            m_overlayArrayDrawable.setDrawable(OVERLAY_LAYER_IDX, m_emptyOverlayDrawable);
            return;
        }

        createOverlayLayers();
        m_overlayArrayDrawable.setDrawable(OVERLAY_LAYER_IDX, drawable);
    }

    @Override
    public void setPlaceholderImageResource(@DrawableRes final int placeholderImageResourceId) {
        if (placeholderImageResourceId != 0) {
            setPlaceholderImage(getResources().getDrawable(placeholderImageResourceId));
        } else {
            setPlaceholderImage(null);
        }
    }

    @Override
    public void setPlaceholderImage(@Nullable Drawable drawable) {
        final DraweeHierarchy hierarchy_ = getHierarchy();
        if (hierarchy_ instanceof GenericDraweeHierarchy) {
            final GenericDraweeHierarchy hierarchy = (GenericDraweeHierarchy) hierarchy_;
            if (drawable != null) {
                hierarchy.setPlaceholderImage(
                        new ScaleTypeDrawable(
                                drawable,
                                SmartImageView.smartToFrescoScaleType(m_placeholderImageScaleType)));

                setRoundedParams(m_roundedParams);
                // Fresco looses placeholder image scale type after a call to setPlaceholderImage().
                //
                // hierarchy.setPlaceholderImage(placeholderImageResourceId);
            } else {
                hierarchy.setPlaceholderImage(/*drawable=*/ null);
                setRoundedParams(m_roundedParams);
            }
        }
    }

    @Override
    public void setPlaceholderImage(@Nullable Drawable drawable, SmartImageView.ScaleType scaleType) {
        m_placeholderImageScaleType = scaleType;
        setPlaceholderImage(drawable);
    }

    @Override
    public void setPlaceholderImageResource(@DrawableRes int placeholderImageResourceId, SmartImageView.ScaleType scaleType) {
        m_placeholderImageScaleType = scaleType;
        setPlaceholderImageResource(placeholderImageResourceId);
    }

    @Override
    public void setPreserveLoadResultHandlers(final boolean preserveLoadResultHandlers) {
        m_helper.setPreserveLoadResultHandlers(preserveLoadResultHandlers);
    }

    @Override
    public void setTransformationMatrix(final Matrix matrix) {
        m_matrix = matrix;
        invalidate();
    }

    @Override
    public void setViewSizeIsValid(final boolean valid, final int measuredWidth, final int measuredHeight) {
        m_helper.setViewSizeIsValid(/*setControllerCallback=*/ this, valid, measuredWidth, measuredHeight);
    }

    @Override
    public void setSmartBorder(int borderColor, int borderWidth) {
        m_roundedParams.setBorder(borderColor, 16);
//        m_roundedParams.setOverlayColor(borderColor);
//        m_roundedParams.
//        m_roundedParams.
        setRoundedParams(m_roundedParams);
    }

    @Override
    public boolean isFinalOrIntermediateImageLoaded() {
        return m_helper.isFinalOrIntermediateImageLoaded();
    }

    @Override
    public boolean isFinalImageLoaded() {
        return m_helper.isFinalImageLoaded();
    }

    @Override
    public void getActualImageBounds(final RectF rect) {
        getHierarchy().getActualImageBounds(rect);
        if (enableLog) {
            debugLog("getActualImageBounds " + System.identityHashCode(this) + ": " +
                    "l " + rect.left + ", t " + rect.top + ", r " + rect.right + ", b " + rect.bottom);
        }
    }

    @Override
    public void smartResetImage() {
        if (enableLog) {
            debugLog("smartResetImage " + System.identityHashCode(this));
        }
        m_controllerHandler = null;
        if (getController() instanceof AbstractDraweeController) {
            ((AbstractDraweeController) getController()).release();
        }
        setController(null);
        getHierarchy().reset();
        m_helper.reset();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void smartSetImageDrawable(Drawable drawable) {
        setImageDrawable(drawable);
    }

    private static class ControllerHandler extends BaseControllerListener<ImageInfo> {
        private final WeakReference<SmartFrescoImageView> m_weakSelf;
        private final boolean m_autoPlayAnimations;

        public ControllerHandler(final SmartFrescoImageView self, final boolean autoPlayAnimations) {
            debugLog("ControllerHandler.create ", System.identityHashCode(self));
            m_weakSelf = new WeakReference<SmartFrescoImageView>(self);
            m_autoPlayAnimations = autoPlayAnimations;
        }

        @Override
        public void onSubmit(final String id, final Object callerContext) {
            if (enableLog) {
                final SmartFrescoImageView self = m_weakSelf.get();
                debugLog("ControllerHandler.onSubmit " + System.identityHashCode(self));
            }
        }

        @Override
        public void onIntermediateImageSet(final String id, final ImageInfo imageInfo) {
            final SmartFrescoImageView self = m_weakSelf.get();
            if (enableLog) {
                debugLog("ControllerHandler.onIntermediateImageSet " + System.identityHashCode(self));
            }

            if (self == null || self.m_controllerHandler != this) {
                return;
            }

            self.m_helper.onIntermediateImageSet();
        }

        @Override
        public void onIntermediateImageFailed(final String id, final Throwable throwable) {
            if (enableLog) {
                final SmartFrescoImageView self = m_weakSelf.get();
                debugLog("ControllerHandler.onIntermediateImageFailed " + System.identityHashCode(self));
            }
        }

        @Override
        public void onRelease(final String id) {
            if (enableLog) {
                final SmartFrescoImageView self = m_weakSelf.get();
                debugLog("ControllerHandler.onRelease " + System.identityHashCode(self));
            }
        }

        @Override
        public void onFinalImageSet(final String id,
                                    final ImageInfo imageInfo,
                                    final Animatable animatable) {
            final SmartFrescoImageView self = m_weakSelf.get();
            debugLog("ControllerHandler.onFinalImageSet %s id %s %s anim %s", System.identityHashCode(self), id, imageInfo, animatable, m_autoPlayAnimations);
            if (self == null || self.m_controllerHandler != this) {
                return;
            }

            if (m_autoPlayAnimations && animatable != null) {
                animatable.start();
            }

            // update index, as new image may got into cache
            self.updatePreferredCachedIndex();
            self.m_helper.onFinalImageSet();
        }

        @Override
        public void onFailure(final String id, final Throwable throwable) {
            final SmartFrescoImageView self = m_weakSelf.get();
            if (enableLog) {
                debugLog("ControllerHandler.onFailure " + System.identityHashCode(self));
            }
            if (self == null || self.m_controllerHandler != this) {
                return;
            }

            self.m_helper.onFailure();
        }
    }

    private static class UriDataSourceSupplier implements Supplier<DataSource<CloseableReference<CloseableImage>>> {
        private final String m_uri;
        private final ResizeOptions m_resizeOptions;
        private int m_cacheOnlyLevel;
        private final Postprocessor m_postprocessor;

        private static final AtomicLong s_requestFutureId = new AtomicLong();
        private static final RequestListener2 s_requestListener = new ForwardingRequestListener2(new HashSet<>());

        public UriDataSourceSupplier(final String uri,
                                     final ResizeOptions resizeOptions,
                                     final int cacheOnlyLevel,
                                     final Postprocessor postprocessor) {
            m_uri = uri;
            m_resizeOptions = resizeOptions;
            m_cacheOnlyLevel = cacheOnlyLevel;
            m_postprocessor = postprocessor;
        }

        @Override
        public DataSource<CloseableReference<CloseableImage>> get() {
            if (enableLog) {
                debugLog("UriDataSourceSupplier.get: m_uri " + m_uri);
            }

            final ImageRequestBuilder requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(m_uri));
            requestBuilder.setImageDecodeOptions(
                    ImageDecodeOptions.newBuilder()
                            .setFrom(requestBuilder.getImageDecodeOptions())
                            .setDecodePreviewFrame(true)
                            .build()
            );
            requestBuilder.setResizeOptions(m_resizeOptions);
            requestBuilder.setPostprocessor(m_postprocessor);
            if (m_cacheOnlyLevel == SmartImageView.MEMORY_CACHE_ONLY) {
                requestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE);
            } else if (m_cacheOnlyLevel == SmartImageView.MEMORY_OR_DISK_CACHE_ONLY) {
                requestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE);
            }

            final ImageRequest imageRequest = requestBuilder.build();

            if (SmartImageView.isInStandaloneMode()) {
                // Plain fresco mode
                return createStandaloneDataSource(imageRequest);
            }

            // Tango mode (custom pipelines, tango in-memory bitmap cache)
            return createCustomDataSource(imageRequest);
        }

        private static DataSource<CloseableReference<CloseableImage>> createStandaloneDataSource(final ImageRequest imageRequest) {
            return Fresco.getImagePipeline().fetchDecodedImage(imageRequest, /*callerContext=*/ null);
        }

        private static DataSource<CloseableReference<CloseableImage>> createCustomDataSource(final ImageRequest imageRequest) {
            final Producer<CloseableReference<CloseableImage>> producer = getProducerForImageRequest(imageRequest);

            final SettableProducerContext settableProducerContext = new SettableProducerContext(imageRequest,
                    String.valueOf(s_requestFutureId.getAndIncrement()),
                    s_requestListener,
                    /*callerContext=*/ null,
                    imageRequest.getLowestPermittedRequestLevel(),
                    /*isPrefetch=*/ false,
                    /*isIntermediateResultExpected=*/
                    imageRequest.getProgressiveRenderingEnabled()
                            || !UriUtil.isNetworkUri(imageRequest.getSourceUri()),
                    imageRequest.getPriority(),
                    SmartImageView.getFrescoImagePipelineConfig());

            return CloseableProducerToDataSourceAdapter.create(producer, settableProducerContext, s_requestListener);
        }
    }

    private static class GeneratedBitmapDataSourceSupplier implements Supplier<DataSource<CloseableReference<CloseableImage>>> {
        private final SmartImageView.BitmapGenerator m_bitmapGenerator;
        private final Object m_bitmapGeneratorParams;
        private final Context m_context;
        private final ResizeOptions m_resizeOptions;

        public GeneratedBitmapDataSourceSupplier(final SmartImageView.BitmapGenerator bitmapGenerator,
                                                 final Object bitmapGeneratorParams,
                                                 final Context context,
                                                 final ResizeOptions resizeOptions) {
            m_bitmapGenerator = bitmapGenerator;
            m_bitmapGeneratorParams = bitmapGeneratorParams;
            m_context = context;
            m_resizeOptions = resizeOptions;
        }

        @Override
        public DataSource<CloseableReference<CloseableImage>> get() {
            return new GeneratedBitmapDataSource(m_bitmapGenerator, m_bitmapGeneratorParams, m_context, m_resizeOptions);
        }
    }

    private static class GeneratedBitmapDataSource extends AbstractDataSource<CloseableReference<CloseableImage>> implements SmartImageView.BitmapGeneratorCallback {
        private boolean m_gotResult = false;
        private CloseableReference<CloseableImage> m_image = null;

        /**
         * object to hold strong reference to bitmapGenerator's internal data
         */
        private Object bitmapGeneratorData;

        public GeneratedBitmapDataSource(final SmartImageView.BitmapGenerator bitmapGenerator,
                                         final Object bitmapGeneratorParams,
                                         final Context context,
                                         final ResizeOptions resizeOptions) {
            int resizeWidth = -1;
            int resizeHeight = -1;
            if (resizeOptions != null) {
                resizeWidth = resizeOptions.width;
                resizeHeight = resizeOptions.height;
            }

            bitmapGeneratorData = bitmapGenerator.generateBitmap(bitmapGeneratorParams, context, resizeWidth, resizeHeight, /*bitmapGeneratorCallback=*/ this);
        }

        @Override
        @Nullable
        public synchronized CloseableReference<CloseableImage> getResult() {
            return CloseableReference.cloneOrNull(m_image);
        }

        @Override
        public synchronized boolean hasResult() {
            return m_gotResult;
        }

        @Override
        public synchronized boolean close() {
            CloseableReference.closeSafely(m_image);
            m_image = null;
            return true;
        }

        @Override
        public synchronized void onBitmapGenerated(final Bitmap bitmap) {
            bitmapGeneratorData = null;
            m_image = CloseableReference.<CloseableImage>of(new CloseableStaticBitmap(bitmap, s_bitmapReleaser, ImmutableQualityInfo.FULL_QUALITY, /*rotationAngle=*/ 0));
            m_gotResult = true;
            setResult(null, /*isLast=*/ true);
        }

        @Override
        public synchronized void onBitmapGenerationFailed() {
            bitmapGeneratorData = null;
            m_gotResult = true;
            setResult(null, /*isLast=*/ true);
        }
    }

    private Supplier<DataSource<CloseableReference<CloseableImage>>> createSupplierForImageSourceDesc(final SmartImageView.ImageSourceDesc imageSourceDesc,
                                                                                                      final ResizeOptions resizeOptions,
                                                                                                      final int cacheOnlyLevel,
                                                                                                      final Postprocessor postprocessor) {
        if (imageSourceDesc.imageUri != null) {
            return new UriDataSourceSupplier(imageSourceDesc.imageUri, resizeOptions, cacheOnlyLevel, postprocessor);
        }

        if (imageSourceDesc.bitmapGenerator != null) {
            return new GeneratedBitmapDataSourceSupplier(imageSourceDesc.bitmapGenerator, imageSourceDesc.bitmapGeneratorParams, getContext(), resizeOptions);
        }

        return null;
    }

    private int getPreferredCachedIndex(@NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs) {
        final SyncDiskCache syncDiskCache = SmartImageView.getSyncDiskCache();
        if (syncDiskCache == null) {
            // rely on Fresco
            return 0;
        }

        // beyond last is the default
        int index = imageSourceDescs.size() - 1;

        for (int i = 0; i < imageSourceDescs.size(); i++) {
            SmartImageView.ImageSourceDesc desc = imageSourceDescs.get(i);
            if (TextUtils.isEmpty(desc.imageUri)) {
                continue;
            }

            boolean isPreferred = desc.isPreferred || (i == imageSourceDescs.size() - 1);

            if (isPreferred) {
                if (syncDiskCache.getResource(new SimpleCacheKey(desc.imageUri)) != null) {
                    index = i;
                    break;
                }
            }
        }

        return index;
    }

    private void updatePreferredCachedIndex() {
        m_preferredCacheIndex = getPreferredCachedIndex(m_helper.getCurrentImageSourceDesc());
    }

    @Override
    public void smartSetImageSourceDescs(final @NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                                         final boolean submitImmediately,
                                         final boolean autoPlayAnimations,
                                         final @Nullable SmartImageView.LoadResultHandler loadResultHandler) {
        debugLog("smartSetImageSourceDescs %s: begin: w %d, h %d",
                System.identityHashCode(this), getWidth(), getHeight());

        boolean descriptorIsTheSame = m_helper.isNewSourceDescTheSame(imageSourceDescs, getWidth(), getHeight());

        // detect change in cached images
        int oldCachedIndex = m_preferredCacheIndex;
        int newCachedIndex = getPreferredCachedIndex(imageSourceDescs);
        m_preferredCacheIndex = newCachedIndex;

        boolean force = false;
        if (descriptorIsTheSame && oldCachedIndex > newCachedIndex) {
            debugLog("same descriptor, newer data");
            force = true;
        }

        m_helper.smartSetImageSourceDescs(/*setControllerCallback=*/ this,
                imageSourceDescs,
                submitImmediately,
                autoPlayAnimations,
                loadResultHandler,
                getWidth(),
                getHeight(),
                /*forceCurrent=*/ force);

        debugLog("smartSetImageSourceDescs %s: end", System.identityHashCode(this));
    }

    @Override
    public void setRoundedParams(RoundedParams roundedParams) {
        m_roundedParams = roundedParams;
        getHierarchy().setRoundingParams(m_roundedParams);
    }

    @Nullable
    @Override
    public RoundedParams getRoundedParams() {
        return m_roundedParams;
    }

    @Override
    public Object cancelRequestController() {
        final DraweeController oldController = getController();
        m_controllerHandler = null;
        setController(null);
        return oldController;
    }

    @Override
    public void resetRequestController() {
        getHierarchy().reset();
    }

    private ResizeOptions createResizeOptionsIfNeeded(final SmartImageView.ImageSourceDesc imageSourceDesc,
                                                      final int currentViewWidth,
                                                      final int currentViewHeight) {
        if (imageSourceDesc.imageUri != null && imageSourceDesc.imageUri.startsWith("res://") /* never resize resources */) {
            return null;
        }

        if (imageSourceDesc.resizeWidth >= 0 && imageSourceDesc.resizeHeight >= 0) {
            return new ResizeOptions(imageSourceDesc.resizeWidth, imageSourceDesc.resizeHeight);
        }

        if (imageSourceDesc.resizeToFit) {
            return new ResizeOptions(currentViewWidth, currentViewHeight);
        }

        return null;
    }

    @Override
    public void setRequestController(final Object oldController,
                                     @NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                                     final int currentViewWidth,
                                     final int currentViewHeight,
                                     final boolean submitImmediately,
                                     final boolean autoPlayAnimations) {
        getHierarchy().reset();

        final PipelineDraweeControllerBuilder controllerBuilder = Fresco.newDraweeControllerBuilder();

        boolean requestConfigured = false;
        final int numImageSourceDescs = imageSourceDescs.size();
        if (numImageSourceDescs == 1) {
            final SmartImageView.ImageSourceDesc imageSourceDesc = imageSourceDescs.get(0);
            if (imageSourceDesc.imageUri != null) {
                final ResizeOptions resizeOptions = createResizeOptionsIfNeeded(imageSourceDesc, currentViewWidth, currentViewHeight);

                Postprocessor postprocessor = null;
                if (imageSourceDesc.postprocessor instanceof BasePostprocessor) {
                    postprocessor = (BasePostprocessor) imageSourceDesc.postprocessor;
                }

                final UriDataSourceSupplier uriDataSourceSupplier = new UriDataSourceSupplier(imageSourceDesc.imageUri, resizeOptions, imageSourceDesc.cacheOnlyLevel, postprocessor);
                controllerBuilder.setDataSourceSupplier(uriDataSourceSupplier);
                requestConfigured = true;
            }
        }

        if (!requestConfigured) {
            final List<StickyIncreasingQualityDataSourceSupplier.DataSourceDesc> dataSourceDescs = new ArrayList<>(numImageSourceDescs);
            for (final SmartImageView.ImageSourceDesc imageSourceDesc : imageSourceDescs) {
                final ResizeOptions resizeOptions = createResizeOptionsIfNeeded(imageSourceDesc, currentViewWidth, currentViewHeight);

                Postprocessor postprocessor = null;
                if (imageSourceDesc.postprocessor instanceof BasePostprocessor) {
                    postprocessor = (BasePostprocessor) imageSourceDesc.postprocessor;
                }

                dataSourceDescs.add(new StickyIncreasingQualityDataSourceSupplier.DataSourceDesc(
                        createSupplierForImageSourceDesc(imageSourceDesc, resizeOptions, imageSourceDesc.cacheOnlyLevel, postprocessor),
                        imageSourceDesc.adaptToBandwidth,
                        imageSourceDesc.isPreferred,
                        imageSourceDesc.isFatal,
                        imageSourceDesc.delayMillisec));
            }

            controllerBuilder.setDataSourceSupplier(StickyIncreasingQualityDataSourceSupplier.create(dataSourceDescs));
        }

        BaseControllerListener<ImageInfo> controllerHandler = new ControllerHandler(this, autoPlayAnimations);
        m_controllerHandler = controllerHandler;

        controllerBuilder.setControllerListener(m_controllerHandler)
                .setOldController((DraweeController) oldController);
        {
            final DraweeController controller = controllerBuilder.build();
            if (enableLog) {
                debugLog("setRequestController " + System.identityHashCode(this) + ": calling setController(), " +
                        "submitImmediately " + submitImmediately);
            }
            setController(controller);
            if (submitImmediately) {
                // Dirty trick to silence error log message from fresco.
                onAttachedToWindow();
            }
        }

        // setController() resets controller overlay, hence we have to restore it.
        if (m_overlayArrayDrawable != null) {
            getHierarchy().setControllerOverlay(m_overlayArrayDrawable);
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        m_helper.onSizeChanged(/*setControllerCallback=*/ this, getWidth(), getHeight());
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        //debugLog("onDraw %s", System.identityHashCode(this));

        try {
            if (m_matrix != null) {
                final int saveCount = canvas.save();
                canvas.concat(m_matrix);
                super.onDraw(canvas);
                canvas.restoreToCount(saveCount);
            } else {
                super.onDraw(canvas);
            }
        } catch (Exception e) {
            int resId = -1;
            if (getParent() instanceof View) {
                resId = ((View) getParent()).getId();
            }

            String resName = "";
            if (resId > 0) {
                resName = getResources().getResourceName(resId);
            }

            // print out SmartImageView class and id to help investigate crashes like "trying to use a recycled bitmap"
            throw new RuntimeException(String.format("Crash drawing %s %s %x: %s", getParent().getClass(), resName, resId, e.getMessage()), e);
        }
    }

    @Override
    public View getAsView() {
        return this;
    }

    @Nullable
    @Override
    public Animatable getAnimatable() {
        if (getController() != null) {
            return getController().getAnimatable();
        } else {
            return null;
        }
    }

    private static final int MAX_SIMULTANEOUS_FILE_FETCH = 4;
    private static boolean s_frescoProducersInitialized = false;
    private static Producer<CloseableReference<CloseableImage>> s_frescoNetworkProducer = null;
    private static Producer<CloseableReference<CloseableImage>> s_frescoLocalAssetProducer = null;
    private static Producer<CloseableReference<CloseableImage>> s_frescoLocalResourceProducer = null;
    private static Producer<CloseableReference<CloseableImage>> s_frescoLocalVideoProducer = null;
    private static Producer<CloseableReference<CloseableImage>> s_frescoLocalImageProducer = null;
    private static Producer<CloseableReference<CloseableImage>> s_frescoContentUriProducer = null;
    // This extra no-exif-thumbs producer is a workaround for a bug in Fresco: exif thumbs returned
    // for image requests without ResizeOptions (instead of returning a full-size image), which breaks
    // photo gallery. Note that Exif thumbnails are crucial for fast image gallery grid loading.
    private static Producer<CloseableReference<CloseableImage>> s_frescoLocalImageProducerNoExifThumbs = null;

    private static final HashMap<Producer<CloseableReference<CloseableImage>>, Producer<CloseableReference<CloseableImage>>> s_postprocessorProducers = new HashMap<>();
    private static ProducerFactory s_producerFactory = null;

    private static Producer<CloseableReference<CloseableImage>> getProducerForImageRequest(@Nullable final ImageRequest imageRequest) {
        final Producer<CloseableReference<CloseableImage>> producer = getStaticProducerForImageRequestWithoutPostprocessor(imageRequest);
        if (imageRequest.getPostprocessor() != null) {
            return getStaticProducerWithPostprocessor(producer);
        }
        return producer;
    }

    private static Producer<CloseableReference<CloseableImage>> getStaticProducerWithPostprocessor(Producer<CloseableReference<CloseableImage>> producer) {
        Producer<CloseableReference<CloseableImage>> postprocessorProducer = s_postprocessorProducers.get(producer);
        if (postprocessorProducer == null) {
            postprocessorProducer = s_producerFactory.newPostprocessorProducer(producer);
            postprocessorProducer = s_producerFactory.newPostprocessorBitmapMemoryCacheProducer(postprocessorProducer);
            s_postprocessorProducers.put(producer, postprocessorProducer);
        }
        return postprocessorProducer;
    }

    private static Producer<CloseableReference<CloseableImage>> getStaticProducerForImageRequestWithoutPostprocessor(@Nullable final ImageRequest imageRequest) {
        initFrescoProducers();

        final Uri uri = imageRequest.getSourceUri();
        if (uri == null) {
            return new NullProducer();
        }

        if (UriUtil.isNetworkUri(uri)) {
            return s_frescoNetworkProducer;
        } else if (UriUtil.isLocalAssetUri(uri)) {
            return s_frescoLocalAssetProducer;
        } else if (UriUtil.isLocalResourceUri(uri)) {
            return s_frescoLocalResourceProducer;
        } else if (UriUtil.isLocalFileUri(uri)) {
            if (MediaUtils.isVideo(MediaUtils.extractMime(uri.getPath()))) {
                return s_frescoLocalVideoProducer;
            }
            if (imageRequest.getResizeOptions() != null) {
                return s_frescoLocalImageProducer;
            }
            return s_frescoLocalImageProducerNoExifThumbs;
        } else if (UriUtil.isLocalContentUri(uri)) {
            return s_frescoContentUriProducer;
        }

        return new NullProducer();
    }

    private static void initFrescoProducers() {
        if (s_frescoProducersInitialized) {
            return;
        }
        s_frescoProducersInitialized = true;

        final ImagePipelineConfig imagePipelineConfig = SmartImageView.getFrescoImagePipelineConfig();
        final CloseableReferenceFactory closeableReferenceFactory = new CloseableReferenceFactory(imagePipelineConfig.getCloseableReferenceLeakTracker());
        final Context context = imagePipelineConfig.getContext();
        final PoolFactory poolFactory = imagePipelineConfig.getPoolFactory();
        final PlatformBitmapFactory platformBitmapFactory = imagePipelineConfig.getPlatformBitmapFactory();
        final ExecutorSupplier executorSupplier = imagePipelineConfig.getExecutorSupplier();
        final ImageDecoder imageDecoder = imagePipelineConfig.getImageDecoder();
        final int bitmapPrepareToDrawMinSizeBytes = 0;
        final int bitmapPrepareToDrawMaxSizeBytes = 0;
        final int maxBitmapSize = 1_024 * 1_204 * 32;
        final boolean preparePrefetchBitmaps = true;
        final ProducerFactory producerFactory = new ProducerFactory(context,
                poolFactory.getSmallByteArrayPool(),
                imageDecoder,
                imagePipelineConfig.getProgressiveJpegConfig(),
                imagePipelineConfig.isDownsampleEnabled(),
                imagePipelineConfig.isResizeAndRotateEnabledForNetwork(),
                ENABLE_DECODE_CANCELLATION,
                executorSupplier,
                poolFactory.getPooledByteBufferFactory(),
                SmartImageView.getImageMemoryCache(),
                /*encodedMemoryCache=*/ null,
                /*defaultBufferedDiskCache=*/ null,
                /*smallImageBufferedDiskCache=*/ null,
                imagePipelineConfig.getCacheKeyFactory(),
                platformBitmapFactory,
                bitmapPrepareToDrawMinSizeBytes,
                bitmapPrepareToDrawMaxSizeBytes,
                preparePrefetchBitmaps,
                maxBitmapSize,
                closeableReferenceFactory,
                imagePipelineConfig.getExperiments().shouldKeepCancelledFetchAsLowPriority(),
                imagePipelineConfig.getExperiments().getTrackedKeysSize());
        s_producerFactory = producerFactory;

        final Executor executor = executorSupplier.forLightweightBackgroundTasks();
        s_frescoNetworkProducer = createFrescoNetworkProducer(producerFactory, executor);
        s_frescoLocalAssetProducer = createFrescoLocalAssetProducer(producerFactory, executor);
        s_frescoLocalResourceProducer = createFrescoLocalResourceProducer(producerFactory, executor);
        s_frescoContentUriProducer = createFrescoContentUriProducer(producerFactory, executor);
        s_frescoLocalVideoProducer = createFrescoLocalVideoProducer(producerFactory, executor);
        s_frescoLocalImageProducer = createFrescoLocalImageProducer(producerFactory,
                /*enableExifThumbs=*/ true,
                executor);
        s_frescoLocalImageProducerNoExifThumbs = createFrescoLocalImageProducer(producerFactory,
                /*enableExifThumbs=*/ false,
                executor);
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoNetworkProducer(@NonNull ProducerFactory producerFactory,
                                                                                            @NonNull Executor executor) {
        final ImagePipelineConfig imagePipelineConfig = SmartImageView.getFrescoImagePipelineConfig();

        final Producer<EncodedImage> networkFetchProducer = producerFactory.newNetworkFetchProducer(imagePipelineConfig.getNetworkFetcher());
        final Producer<CloseableReference<CloseableImage>> fetchDecodeProducer = createFrescoDecodeProducer(
                producerFactory,
                networkFetchProducer,
                /*enableDiskCache=*/ true,
                /*enableExifThumbs=*/ false,
                /*enableThrottling=*/ false,
                executor);
        return fetchDecodeProducer;
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoLocalAssetProducer(@NonNull ProducerFactory producerFactory,
                                                                                               @NonNull Executor executor) {
        final Producer<EncodedImage> localAssetFetchProducer = producerFactory.newLocalAssetFetchProducer();
        return createFrescoLocalProducer(producerFactory,
                localAssetFetchProducer,
                /*enableExifThumbs=*/ false,
                executor);
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoLocalResourceProducer(@NonNull ProducerFactory producerFactory,
                                                                                                  @NonNull Executor executor) {
        final Producer<EncodedImage> localResourceFetchProducer = producerFactory.newLocalResourceFetchProducer();
        return createFrescoLocalProducer(producerFactory,
                localResourceFetchProducer,
                /*enableExifThumbs=*/ false,
                executor);
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoContentUriProducer(@NonNull ProducerFactory producerFactory,
                                                                                               @NonNull Executor executor) {
        final Producer<EncodedImage> contentUriFetchProducer = producerFactory.newLocalContentUriFetchProducer();
        return createFrescoLocalProducer(producerFactory,
                contentUriFetchProducer,
                /*enableExifThumbs=*/ false,
                executor);
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoLocalVideoProducer(@NonNull ProducerFactory producerFactory,
                                                                                               @NonNull Executor executor) {
        Producer<CloseableReference<CloseableImage>> producer;
        producer = producerFactory.newLocalVideoThumbnailProducer();
        producer = producerFactory.newBitmapMemoryCacheProducer(producer);
        producer = producerFactory.newBitmapMemoryCacheKeyMultiplexProducer(producer);
        producer = producerFactory.newBackgroundThreadHandoffProducer(producer, new ThreadHandoffProducerQueueImpl(executor));
        producer = producerFactory.newBitmapMemoryCacheGetProducer(producer);
        return producer;
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoLocalImageProducer(@NonNull ProducerFactory producerFactory,
                                                                                               final boolean enableExifThumbs,
                                                                                               @NonNull Executor executor) {
        final Producer<EncodedImage> localFileFetchProducer = producerFactory.newLocalFileFetchProducer();
        return createFrescoLocalProducer(producerFactory, localFileFetchProducer, enableExifThumbs, executor);
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoLocalProducer(final ProducerFactory producerFactory,
                                                                                          final Producer<EncodedImage> fetchProducer,
                                                                                          final boolean enableExifThumbs,
                                                                                          @NonNull Executor executor) {
        final Producer<CloseableReference<CloseableImage>> frescoDecodeProducer = createFrescoDecodeProducer(
                producerFactory,
                fetchProducer,
                /*enableDiskCache=*/ false,
                enableExifThumbs,
                /*enableThrottling=*/ true,
                executor);

        // This throttling producer also acts as a workaround for a bug in Fresco:
        // local file fetches are not actually cancelled when asked, which leads to
        // long image loading delays when scrolling through many images fast enough.
        final Producer<CloseableReference<CloseableImage>> decodeThrottlingProducer =
                producerFactory.newThrottlingProducer(frescoDecodeProducer);
        return decodeThrottlingProducer;
    }

    private static Producer<CloseableReference<CloseableImage>> createFrescoDecodeProducer(final ProducerFactory producerFactory,
                                                                                           final Producer<EncodedImage> fetchProducer,
                                                                                           final boolean enableDiskCache,
                                                                                           final boolean enableExifThumbs,
                                                                                           final boolean enableThrottling,
                                                                                           @NonNull Executor executor) {
        Producer<EncodedImage> producer = fetchProducer;

        if (enableDiskCache) {
            final SyncDiskCache syncDiskCache = SmartImageView.getSyncDiskCache();
            if (syncDiskCache != null) {
                final PoolFactory poolFactory = SmartImageView.getFrescoImagePipelineConfig().getPoolFactory();
                producer = new SyncDiskCacheProducer(syncDiskCache, poolFactory.getPooledByteBufferFactory(), poolFactory.getPooledByteStreams(), producer);
            }
        }

        producer = producerFactory.newAddImageTransformMetaDataProducer(producer);

        if (enableThrottling) {
            producer = producerFactory.newThrottlingProducer(producer);
        }

        if (enableExifThumbs) {
            final Producer<EncodedImage> localExifThumbnailProducer = producerFactory.newLocalExifThumbnailProducer();
            producer = producerFactory.newBranchOnSeparateImagesProducer(localExifThumbnailProducer, producer);
        }

        producer = producerFactory.newEncodedCacheKeyMultiplexProducer(producer);

        Producer<CloseableReference<CloseableImage>> decodeProducer = producerFactory.newDecodeProducer(producer);
        decodeProducer = producerFactory.newBitmapMemoryCacheProducer(decodeProducer);
        decodeProducer = producerFactory.newBitmapMemoryCacheKeyMultiplexProducer(decodeProducer);
        decodeProducer = producerFactory.newBackgroundThreadHandoffProducer(decodeProducer, new ThreadHandoffProducerQueueImpl(executor));
        decodeProducer = producerFactory.newBitmapMemoryCacheGetProducer(decodeProducer);
        return decodeProducer;
    }
}

