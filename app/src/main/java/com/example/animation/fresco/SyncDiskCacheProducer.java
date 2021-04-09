package com.example.animation.fresco;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteStreams;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.DelegatingConsumer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.imagepipeline.producers.ProducerListener2;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class SyncDiskCacheProducer implements Producer<EncodedImage> {
    @VisibleForTesting static final String PRODUCER_NAME = "SyncDiskCacheProducer";
    @VisibleForTesting static final String VALUE_FOUND = "cached_value_found";

    private final SyncDiskCache m_cache;
    private final PooledByteBufferFactory m_pooledByteBufferFactory;
    private final PooledByteStreams m_pooledByteStreams;
    private final Producer<EncodedImage> m_nextProducer;

    public SyncDiskCacheProducer(final SyncDiskCache cache,
                                 final PooledByteBufferFactory pooledByteBufferFactory,
                                 final PooledByteStreams pooledByteStreams,
                                 final Producer<EncodedImage> nextProducer)
    {
        m_cache = cache;
        m_pooledByteBufferFactory = pooledByteBufferFactory;
        m_pooledByteStreams = pooledByteStreams;
        m_nextProducer = nextProducer;
    }

    private EncodedImage readFromCache(final CacheKey cacheKey) {
        try {
            final BinaryResource binaryResource = m_cache.getResource(cacheKey);
            if (binaryResource == null) {
                return null;
            }

            PooledByteBuffer byteBuffer;
            final InputStream is = binaryResource.openStream();
            try {
                byteBuffer = m_pooledByteBufferFactory.newByteBuffer(is, (int) binaryResource.size());
            } finally {
                is.close();
            }

            EncodedImage encodedImage;
            final CloseableReference<PooledByteBuffer> ref = CloseableReference.of(byteBuffer);
            try {
                encodedImage = new EncodedImage(ref);
            } finally {
                CloseableReference.closeSafely(ref);
            }

            return encodedImage;
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public void produceResults(final Consumer<EncodedImage> consumer,
                               final ProducerContext producerContext)
    {
        final ProducerListener2 listener = producerContext.getProducerListener();
        final String requestId = producerContext.getId();

        listener.onProducerStart(producerContext, getProducerName());

        final CacheKey cacheKey = new SimpleCacheKey(producerContext.getImageRequest().getSourceUri().toString());
        final EncodedImage encodedImage = readFromCache(cacheKey);

        if (encodedImage != null) {
            listener.onProducerFinishWithSuccess(producerContext,
                    getProducerName(),
                    listener.requiresExtraMap(producerContext, requestId) ? ImmutableMap.of(VALUE_FOUND, "true") : null);
            consumer.onProgressUpdate(1f);
            try {
                consumer.onNewResult(encodedImage, Consumer.IS_LAST);
            } finally {
                encodedImage.close();
            }
            return;
        }

        listener.onProducerFinishWithSuccess(
                producerContext,
                getProducerName(),
                listener.requiresExtraMap(producerContext, requestId) ? ImmutableMap.of(VALUE_FOUND, "false") : null);

        if (producerContext.getLowestPermittedRequestLevel().getValue() >= ImageRequest.RequestLevel.DISK_CACHE.getValue()) {
            consumer.onNewResult(null, Consumer.IS_LAST);
            return;
        }

        final Consumer<EncodedImage> wrappedConsumer = wrapConsumer(consumer, cacheKey);
        m_nextProducer.produceResults(wrappedConsumer, producerContext);
    }

    private Consumer<EncodedImage> wrapConsumer(final Consumer<EncodedImage> consumer, final CacheKey cacheKey) {
        return new DelegatingConsumer<EncodedImage, EncodedImage>(consumer) {

            @Override
            public void onNewResultImpl(final EncodedImage newResult, int status) {
                if (newResult == null || !newResult.isValid()) {
                    if (isLast(status)) {
                        getConsumer().onNewResult(null, status);
                    }
                    return;
                }
                m_cache.insert(
                        cacheKey,
                        new WriterCallback() {
                            @Override
                            public void write(final OutputStream os) throws IOException {
                                m_pooledByteStreams.copy(newResult.getInputStream(), os);
                            }
                        });
                try {
                    if (isLast(status)) {
                        getConsumer().onProgressUpdate(1f);
                    }
                    getConsumer().onNewResult(newResult, status);
                } finally {
                    newResult.close();
                }
            }
        };
    }

    private String getProducerName() {
        return PRODUCER_NAME;
    }
}

