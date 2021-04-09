package com.example.animation.fresco;

import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.request.BasePostprocessor;

/**
 * Created by EBratanova on 3/9/16.
 */
public class SmartImageViewPostprocessorFrescoWrapper extends BasePostprocessor implements SmartImageViewPostprocessor {
    private static final ResourceReleaser<Bitmap> resourceReleaser = new ResourceReleaser<Bitmap>() {
        @Override
        public void release(final Bitmap bitmap) {
            bitmap.recycle();
        }
    };

    private SmartImageViewPostprocessor mImpl;

    public SmartImageViewPostprocessorFrescoWrapper(SmartImageViewPostprocessor postprocessor) {
        mImpl = postprocessor;
    }

    @Override
    public CloseableReference<Bitmap> process(Bitmap sourceBitmap, PlatformBitmapFactory bitmapFactory) {
        if (mImpl != null) {
            Bitmap bitmap = mImpl.postprocessImage(sourceBitmap);
            if (bitmap != null) {
                return CloseableReference.of(bitmap, resourceReleaser);
            }
        }
        return super.process(sourceBitmap, bitmapFactory);
    }

    @Override
    public Bitmap postprocessImage(Bitmap source) {
        if (mImpl != null) {
            return mImpl.postprocessImage(source);
        }
        return null;
    }
}
