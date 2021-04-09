package com.example.animation.fresco;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.WriterCallback;

public interface SyncDiskCache {
    BinaryResource getResource(CacheKey cacheKey);
    void insert(CacheKey cacheKey, WriterCallback writerCallback);
}

