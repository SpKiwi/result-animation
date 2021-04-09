package com.example.animation.fresco;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SmartImageViewHelper {
    private final static String TAG = "SmartImageViewHelper";
    private final static boolean enableLog = false;

    private static void debugLog(final String str, Object... args) {
        if (enableLog) {
            if (args == null || args.length == 0) {
                Log.d(TAG, str);
            } else {
                Log.d(TAG, String.format(str, args));
            }
        }
    }

    private boolean m_preserveLoadResultHandlers = false;

    private ImageDesc m_currentImageDesc = new ImageDesc();
    private boolean m_currentImageDescIsValid = true;

    private boolean m_imageLoadingInProgress = false;
    private boolean m_intermediateImageLoaded = false;
    private boolean m_finalImageLoaded = false;

    private boolean m_viewSizeIsValid = true;

    private List<SmartImageView.ImageSourceDesc> m_pendingImageSourceDescs = null;
    private boolean m_pendingSubmitImmediately = false;
    private boolean m_pendingAutoPlayAnimations = false;

    private int m_guardCounter = 0;
    private List<SmartImageView.LoadResultHandler> m_loadResultHandlers = new ArrayList<SmartImageView.LoadResultHandler>();

    public void setPreserveLoadResultHandlers(final boolean preserveLoadResultHandlers) {
        m_preserveLoadResultHandlers = preserveLoadResultHandlers;
    }

    public void setViewSizeIsValid(final SetControllerCallback setControllerCallback,
                                   final boolean valid,
                                   final int currentViewWidth,
                                   final int currentViewHeight)
    {
        m_viewSizeIsValid = valid;

        if (valid && currentViewWidth > 0 && currentViewHeight > 0) {
            onSizeChanged(setControllerCallback, currentViewWidth, currentViewHeight);
        }
    }

    public boolean isFinalImageLoaded() {
        return m_finalImageLoaded;
    }

    public boolean isFinalOrIntermediateImageLoaded() {
        return m_finalImageLoaded || m_intermediateImageLoaded;
    }

    public void reset() {
        if (enableLog) {
            debugLog(System.identityHashCode(this) + " reset");
        }

        m_pendingImageSourceDescs = null;
        m_pendingSubmitImmediately = false;
        m_pendingAutoPlayAnimations = false;

        m_imageLoadingInProgress = false;
        m_intermediateImageLoaded = false;
        m_finalImageLoaded = false;

        getCurrentImageDesc().reset();
        m_currentImageDescIsValid = true;

        // NOTE: Callbacks may call smartSetImage*() and other methods of this view object.
        fireImageLoadingCancelledAndMaybeClearHandlers(/*forceClear=*/ false);
    }

    private void fireIntermediateImageLoaded() {
        if (m_loadResultHandlers.isEmpty()) {
            return;
        }

        final List<SmartImageView.LoadResultHandler> tmpHandlers = new ArrayList<SmartImageView.LoadResultHandler> (m_loadResultHandlers);
        for (final SmartImageView.LoadResultHandler handler : tmpHandlers) {
            // NOTE: Callbacks may call smartSetImage*() and other methods of this view object.
            handler.onIntermediateImageLoaded();
        }
    }

    private void fireFinalImageLoadedAndMaybeClearHandlers() {
        if (m_loadResultHandlers.isEmpty()) {
            return;
        }

        final List<SmartImageView.LoadResultHandler> tmpHandlers;
        if (m_preserveLoadResultHandlers) {
            tmpHandlers = new ArrayList<SmartImageView.LoadResultHandler> (m_loadResultHandlers);
        } else {
            tmpHandlers = m_loadResultHandlers;
            m_loadResultHandlers = new ArrayList<SmartImageView.LoadResultHandler>();
        }

        for (final SmartImageView.LoadResultHandler handler : tmpHandlers) {
            // NOTE: Callbacks may call smartSetImage*() and other methods of this view object.
            handler.onFinalImageLoaded();
        }
    }

    private void fireImageLoadingFailedAndMaybeClearHandlers() {
        if (enableLog) {
            debugLog("fireImageLoadingFailedAndMaybeClearHandlers " + System.identityHashCode(this));
        }
        if (m_loadResultHandlers.isEmpty()) {
            return;
        }

        final List<SmartImageView.LoadResultHandler> tmpHandlers;
        if (m_preserveLoadResultHandlers) {
            tmpHandlers = new ArrayList<SmartImageView.LoadResultHandler> (m_loadResultHandlers);
        } else {
            tmpHandlers = m_loadResultHandlers;
            m_loadResultHandlers = new ArrayList<SmartImageView.LoadResultHandler>();
        }

        for (final SmartImageView.LoadResultHandler handler : tmpHandlers) {
            if (enableLog) {
                debugLog("fireImageLoadingFailedAndMaybeClearHandlers " + System.identityHashCode(this) + ": calling onImageLoadingFailed()");
            }
            // NOTE: Callbacks may call smartSetImage*() and other methods of this view object.
            handler.onImageLoadingFailed();
        }
    }

    private void fireImageLoadingCancelledAndMaybeClearHandlers(final boolean forceClear) {
        if (m_loadResultHandlers.isEmpty()) {
            return;
        }

        final List<SmartImageView.LoadResultHandler> tmpHandlers;
        if (m_preserveLoadResultHandlers && !forceClear) {
            tmpHandlers = new ArrayList<SmartImageView.LoadResultHandler> (m_loadResultHandlers);
        } else {
            tmpHandlers = m_loadResultHandlers;
            m_loadResultHandlers = new ArrayList<SmartImageView.LoadResultHandler>();
        }

        for (final SmartImageView.LoadResultHandler handler : tmpHandlers) {
            // NOTE: Callbacks may call smartSetImage*() and other methods of this view object.
            handler.onImageLoadingCancelled();
        }
    }

    public void onIntermediateImageSet() {
        if (enableLog) {
            debugLog("onIntermediateImageSet: " + System.identityHashCode(this));
        }

        m_intermediateImageLoaded = true;

        fireIntermediateImageLoaded();
    }

    public void onFinalImageSet() {
        if (enableLog) {
            debugLog("onFinalImageSet: " + System.identityHashCode(this));
        }

        m_imageLoadingInProgress = false;
        m_intermediateImageLoaded = true;
        m_finalImageLoaded = true;

        // NOTE: Callbacks may call smartSetImage*() and other methods of the object.
        fireFinalImageLoadedAndMaybeClearHandlers();
    }

    public void onFailure() {
        if (enableLog) {
            debugLog("onFailure: " + System.identityHashCode(this));
        }

        m_imageLoadingInProgress = false;
        m_intermediateImageLoaded = false;
        m_finalImageLoaded = false;

        getCurrentImageDesc().reset();
        m_currentImageDescIsValid = false; // we don't know if a placeholder or low-res image is displayed

        // NOTE: Callbacks may call smartSetImage*() and other methods of the object.
        fireImageLoadingFailedAndMaybeClearHandlers();
    }

    public boolean waitingForSize() {
        return m_pendingImageSourceDescs != null;
    }

    public void onSizeChanged(final SetControllerCallback setControllerCallback,
                              final int currentViewWidth,
                              final int currentViewHeight)
    {
        if (enableLog) {
            debugLog("onSizeChanged " + System.identityHashCode(this) + ": " +
                    "currentViewWidth " + currentViewWidth + ", currentViewHeight " + currentViewHeight);
        }

        if (waitingForSize()) {
            smartSetImageSourceDescs(setControllerCallback,
                    m_pendingImageSourceDescs,
                    m_pendingSubmitImmediately,
                    m_pendingAutoPlayAnimations,
                    /*loadResultHandler=*/ null,
                    currentViewWidth,
                    currentViewHeight,
                    /*forceCurrent=*/ true /* avoiding currentImageUri check in smartSetImageUri() to force fetch */);
        }
    }

    public interface SetControllerCallback {
        public Object cancelRequestController();

        public void resetRequestController();

        public void setRequestController(Object oldController,
                                         @NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                                         int currentViewWidth,
                                         int currentViewHeight,
                                         boolean submitImmediately,
                                         boolean autoPlayAnimations);
    }

    private static class ImageDesc {
        private @NonNull List<SmartImageView.ImageSourceDesc> m_imageSourceDescs = new ArrayList<>();
        private int m_viewWidth = -1;
        private int m_viewHeight = -1;

        public void set(@NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                        final int viewWidth,
                        final int viewHeight)
        {
            m_imageSourceDescs = imageSourceDescs;
            m_viewWidth = viewWidth;
            m_viewHeight = viewHeight;
        }

        public void reset() {
            set(/*imageSourceDescs=*/ new ArrayList<SmartImageView.ImageSourceDesc>(),
                    /*viewWidth=*/ -1,
                    /*viewHeight=*/ -1);
        }

        public int getDescSize() {
            return m_imageSourceDescs.size();
        }

        private boolean sourceDescHasResizeToFit(@NonNull final List<SmartImageView.ImageSourceDesc> imageSourceDescs) {
            for (final SmartImageView.ImageSourceDesc desc : imageSourceDescs) {
                if (desc.hasResizeToFit()) {
                    return true;
                }
            }

            return false;
        }

        public boolean equals(@NonNull final List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                              final int viewWidth,
                              final int viewHeight)
        {
            if (m_imageSourceDescs.size() != imageSourceDescs.size()) {
                return false;
            }

            boolean gotResizeToFit = sourceDescHasResizeToFit(imageSourceDescs);
            if (sourceDescHasResizeToFit(m_imageSourceDescs) != gotResizeToFit)
            {
                return false;
            }

            // viewWidth and viewHeight only matter if one of imageSourceDescs has a "ResizeToFit" flag.
            if (gotResizeToFit) {
                // "unknown" view size is compatible with any size, i.e. we can keep the current request.
                if (m_viewWidth >= 0 || m_viewHeight >= 0) {
                    if (m_viewWidth != viewWidth || m_viewHeight != viewHeight)
                        return false;
                }
            }

            final Iterator<SmartImageView.ImageSourceDesc> iter = imageSourceDescs.iterator();
            for (final SmartImageView.ImageSourceDesc left : m_imageSourceDescs) {
                final SmartImageView.ImageSourceDesc right = iter.next();
                if (!left.equals(right)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return "ImageDesc{" +
                    ", m_imageSourceDescs=" + m_imageSourceDescs +
                    ", m_viewWidth=" + m_viewWidth +
                    ", m_viewHeight=" + m_viewHeight +
                    '}';
        }
    }

    private @NonNull ImageDesc getCurrentImageDesc() {
        return m_currentImageDesc;
    }

    public @NonNull List<SmartImageView.ImageSourceDesc> getCurrentImageSourceDesc() {
        return m_currentImageDesc.m_imageSourceDescs;
    }

    public boolean isNewSourceDescTheSame(final @NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                                          final int currentViewWidth,
                                          final int currentViewHeight) {
        return m_currentImageDescIsValid
                && getCurrentImageDesc().equals(imageSourceDescs, currentViewWidth, currentViewHeight)
                && !m_imageLoadingInProgress;
    }

    public void smartSetImageSourceDescs(final SetControllerCallback setControllerCallback,
                                         final @NonNull List<SmartImageView.ImageSourceDesc> imageSourceDescs,
                                         final boolean submitImmediately,
                                         final boolean autoPlayAnimations,
                                         final @Nullable SmartImageView.LoadResultHandler loadResultHandler,
                                         final int currentViewWidth,
                                         final int currentViewHeight,
                                         final boolean forceCurrent)
    {
        boolean gotResizeToFit = false;
        for (final SmartImageView.ImageSourceDesc desc : imageSourceDescs) {
            gotResizeToFit |= desc.hasResizeToFit();
        }

        debugLog("smartSetImageSourceDescs: %s, handler %s, curW %d, curH %d, valid %s, fcurrent %b",
                System.identityHashCode(this), System.identityHashCode(loadResultHandler),
                currentViewWidth, currentViewHeight, m_currentImageDescIsValid,
                forceCurrent);

        // check if image descriptor is not changed
        if (!forceCurrent
                && m_currentImageDescIsValid
                && getCurrentImageDesc().equals(imageSourceDescs, currentViewWidth, currentViewHeight))
        {
            debugLog("smartSetImageUri: %s: same image", System.identityHashCode(this));

            {
                final ImageDesc imageDesc = getCurrentImageDesc();
                imageDesc.m_viewWidth = currentViewWidth;
                imageDesc.m_viewHeight = currentViewHeight;
            }

            if (loadResultHandler != null) {
                if (m_imageLoadingInProgress || waitingForSize()) {
                    // The same image is currently being fetched.
                    // Don't create a new controller and wait
                    // for the current one to finish instead.
                    m_loadResultHandlers.add(loadResultHandler);
                } else {
                    // The same image has already been fetched
                    // and is currently displayed in this view.
                    // In this case, we do nothing.
                    if (getCurrentImageDesc().getDescSize() > 0) {
                        loadResultHandler.onFinalImageLoaded();
                    } else {
                        loadResultHandler.onImageLoadingFailed();
                    }
                }
            }

            return;
        }

        // The new image request is (probably) different from the previous one. Resetting fetch state.

        getCurrentImageDesc().set(imageSourceDescs,
                currentViewWidth,
                currentViewHeight);
        m_currentImageDescIsValid = true;

        final Object oldController = setControllerCallback.cancelRequestController();

        m_pendingImageSourceDescs = null;
        m_pendingSubmitImmediately = false;
        m_pendingAutoPlayAnimations = false;

        m_imageLoadingInProgress = true;
        m_intermediateImageLoaded = false;
        m_finalImageLoaded = false;

        ++m_guardCounter;
        final int savedGuardCounter = m_guardCounter;

        if (!forceCurrent) {
            // NOTE: Callbacks may call smartSetImage*() and other methods of this view object.
            fireImageLoadingCancelledAndMaybeClearHandlers(/*forceClear=*/ true);
        }

        if (m_guardCounter != savedGuardCounter) {
            debugLog("smartSetImageUri: %s: savedGuardCounter mismatch", System.identityHashCode(this));

            // Another image request has been made in a callback.
            if (loadResultHandler != null) {
                loadResultHandler.onImageLoadingCancelled();
            }
            return;
        }

        if (imageSourceDescs.isEmpty()) {
            debugLog("smartSetImageUri: %s: null request", System.identityHashCode(this));

            setControllerCallback.resetRequestController();
            m_imageLoadingInProgress = false;

            // NOTE: Callbacks may call smartSetImage*() and other methods of this view object.
            fireImageLoadingFailedAndMaybeClearHandlers();
            if (loadResultHandler != null) {
                loadResultHandler.onImageLoadingFailed();
            }

            return;
        }

        // At this point we know that we're not going to have immediate result.
        if (loadResultHandler != null) {
            m_loadResultHandlers.add(loadResultHandler);
        }

        boolean uriIsResource = imageSourceDescs.size() == 1
                && imageSourceDescs.get(0).imageUri != null
                && imageSourceDescs.get(0).imageUri.startsWith("res://");

        if (gotResizeToFit && !uriIsResource) {
            /* never resize resources => no need to wait
             * (common case optimization) */
            if (   !m_viewSizeIsValid
                    || currentViewWidth <= 0
                    || currentViewHeight <= 0)
            {
                m_pendingImageSourceDescs = imageSourceDescs;
                m_pendingSubmitImmediately = submitImmediately;
                m_pendingAutoPlayAnimations = autoPlayAnimations;

                // Wait for onSizeChanged() with non-zero dimensions.
                debugLog("smartSetImageUri: %s: waiting for onSizeChanged", System.identityHashCode(this));
                return;
            }
        }

        debugLog("smartSetImageUri: %s: calling setRequestController()", System.identityHashCode(this));
        setControllerCallback.setRequestController(oldController,
                imageSourceDescs,
                currentViewWidth,
                currentViewHeight,
                submitImmediately,
                autoPlayAnimations);
    }
}

