package com.example.animation.fresco;

import android.os.SystemClock;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.ref.WeakReference;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.AbstractDataSource;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;

public class StickyIncreasingQualityDataSourceSupplier implements Supplier<DataSource<CloseableReference<CloseableImage>>> {
    private final static String TAG = "Sticky...Supplier";
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

    private final List<DataSourceDesc> mDataSourceDescs;

    public static class DataSourceDesc {
        public Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier;
        public boolean adaptToBandwidth;
        public boolean isPreferred;
        public boolean isFatal;
        public int delayMillisec;

        DataSourceDesc(final Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier,
                       final boolean adaptToBandwidth,
                       final boolean isPreferred,
                       final boolean isFatal,
                       final int delayMillisec)
        {
            this.dataSourceSupplier = dataSourceSupplier;
            this.adaptToBandwidth = adaptToBandwidth;
            this.isPreferred = isPreferred;
            this.isFatal = isFatal;
            this.delayMillisec = delayMillisec;
        }

        @Override
        public String toString() {
            if (enableLog) {
                return "DataSourceDesc{" +
                        "adaptToBandwidth=" + adaptToBandwidth +
                        ", dataSourceSupplier=" + dataSourceSupplier +
                        ", isPreferred=" + isPreferred +
                        ", isFatal=" + isFatal +
                        ", delayMillisec=" + delayMillisec +
                        '}';
            } else {
                return super.toString();
            }
        }
    }

    private StickyIncreasingQualityDataSourceSupplier(final List<DataSourceDesc> dataSourceDescs)
    {
        if (enableLog) {
            debugLog("ctor " + System.identityHashCode(this));
        }

        FirstAvailableDataSource.ensureSpawnProgressiveImageLoadingThread();

        Preconditions.checkArgument(!dataSourceDescs.isEmpty(), "List of suppliers is empty!");
        mDataSourceDescs = dataSourceDescs;
    }

    public static StickyIncreasingQualityDataSourceSupplier create(final List<DataSourceDesc> dataSourceDescs)
    {
        return new StickyIncreasingQualityDataSourceSupplier(dataSourceDescs);
    }

    @Override
    public DataSource<CloseableReference<CloseableImage>> get() {
        final FirstAvailableDataSource dataSource = new FirstAvailableDataSource(mDataSourceDescs);
        if (enableLog) {
            debugLog("get " + System.identityHashCode(this) + ": new dataSource " + System.identityHashCode(dataSource));
        }
        return dataSource;
    }

    @Override
    public int hashCode() {
        return mDataSourceDescs.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof StickyIncreasingQualityDataSourceSupplier)) {
            return false;
        }
        StickyIncreasingQualityDataSourceSupplier that = (StickyIncreasingQualityDataSourceSupplier) other;
        return Objects.equal(this.mDataSourceDescs, that.mDataSourceDescs);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("list", mDataSourceDescs)
                .toString();
    }

    private static class FirstAvailableDataSource extends AbstractDataSource<CloseableReference<CloseableImage>> {
        private final List<DataSourceDesc> mDataSourceDescs;

        private int mIndex = 0;
        private DataSourceDesc mCurrentDataSourceDesc = null;
        private DataSource<CloseableReference<CloseableImage>> mCurrentDataSource = null;
        private DataSource<CloseableReference<CloseableImage>> mDataSourceWithResult = null;

        private static ProgressiveImageLoadingThread s_progressiveImageLoadingThread;

        public static void ensureSpawnProgressiveImageLoadingThread() {
            if (s_progressiveImageLoadingThread != null) {
                return;
            }

            s_progressiveImageLoadingThread = new ProgressiveImageLoadingThread();
            new Thread(s_progressiveImageLoadingThread).start();
        }

        public FirstAvailableDataSource(final List<DataSourceDesc> dataSourceDescs) {
            if (enableLog) {
                debugLog("DataSource() " + System.identityHashCode(this));
            }

            mDataSourceDescs = dataSourceDescs;

            if (!startNextDataSource(/*forceNoDelay=*/ false)) {
                setFailure(new RuntimeException("No data source supplier or supplier returned null."));
            }
        }

        @Override
        @Nullable
        public synchronized CloseableReference<CloseableImage> getResult() {
            if (enableLog) {
                debugLog("DataSource.getResult " + System.identityHashCode(this) + ": " +
                        "mDataSourceWithResult " + (mDataSourceWithResult != null ? "non-null " + System.identityHashCode(mDataSourceWithResult) + ", " +
                        "hasResult " + mDataSourceWithResult.hasResult()
                        : "null"));
            }

            if (mDataSourceWithResult == null) {
                return null;
            }
            return mDataSourceWithResult.getResult();
        }

        @Override
        public synchronized boolean hasResult() {
            if (enableLog) {
                debugLog("DataSource.hasResult " + System.identityHashCode(this) + ": " +
                        "mDataSourceWithResult " + (mDataSourceWithResult != null ? "non-null " + System.identityHashCode(mDataSourceWithResult) + ", " +
                        "hasResult " + mDataSourceWithResult.hasResult()
                        : "null"));
            }

            if (mDataSourceWithResult == null) {
                return false;
            }
            return mDataSourceWithResult.hasResult();
        }

        @Override
        public boolean close() {
            if (enableLog) {
                debugLog("DataSource.close " + System.identityHashCode(this));
            }

            boolean result = true;

            DataSource currentDataSource;
            DataSource dataSourceWithResult;
            synchronized (FirstAvailableDataSource.this) {
                // it's fine to call {@code super.close()} within a synchronized block because we don't
                // implement {@link #closeResult()}, but perform result closing ourselves.
                if (!super.close()) {
                    result = false;
                }

                currentDataSource = mCurrentDataSource;
                mCurrentDataSource = null;
                dataSourceWithResult = mDataSourceWithResult;
                mDataSourceWithResult = null;
            }

            // TODO There may be race conditions with doStartNextDataSource resulting in non-closure of some requests
            if (dataSourceWithResult != null) {
                dataSourceWithResult.close();
            }
            if (currentDataSource != null) {
                currentDataSource.close();
            }

            // TODO what does the return value mean?
            return result;
        }

        private static class ProgressiveImageLoadingThread implements Runnable {

            private static class DelayedEntry {
                public final WeakReference<FirstAvailableDataSource> weakDataSource;
                public final long targetUptimeMillis;
                public final long clockSkewLimitMillis;

                public DelayedEntry(final FirstAvailableDataSource dataSource,
                                    final long targetUptimeMillis,
                                    final long clockSkewLimitMillis)
                {
                    this.weakDataSource = new WeakReference<FirstAvailableDataSource>(dataSource);
                    this.targetUptimeMillis = targetUptimeMillis;
                    this.clockSkewLimitMillis = clockSkewLimitMillis;
                }
            }

            private final Lock m_mutex;
            private final Condition m_cond;
            private final LinkedList<DelayedEntry> m_queue;

            public ProgressiveImageLoadingThread() {
                m_mutex = new ReentrantLock();
                m_cond = m_mutex.newCondition();
                m_queue = new LinkedList<DelayedEntry>();
            }

            public void schedule(final FirstAvailableDataSource dataSource,
                                 final long targetUptimeMillis,
                                 final long clockSkewLimitMillis)
            {
                m_mutex.lock();
                try {
                    m_queue.add(new DelayedEntry(dataSource, targetUptimeMillis, clockSkewLimitMillis));
                    m_cond.signal();
                } finally {
                    m_mutex.unlock();
                }
            }

            @Override
            public void run() {
                m_mutex.lock();
                try {
                    boolean forceDequeue = false;
                    for (;;) {
                        final boolean tmpForceDequeue = forceDequeue;
                        forceDequeue = false;

                        try {
                            if (!m_queue.isEmpty()) {
                                final DelayedEntry entry = m_queue.getFirst();
                                final long curUptimeMillis = SystemClock.uptimeMillis();
                                if (   tmpForceDequeue
                                        || entry.targetUptimeMillis <= curUptimeMillis
                                        || entry.targetUptimeMillis - curUptimeMillis > entry.clockSkewLimitMillis)
                                {
                                    m_queue.removeFirst();

                                    m_mutex.unlock();
                                    try {
                                        final FirstAvailableDataSource dataSource = entry.weakDataSource.get();
                                        if (dataSource != null && !dataSource.isClosed() /* TODO isClosed() check should become excessive after doStartNextDataSource() is fixed */) {
                                            if (enableLog) {
                                                debugLog("ProgressiveImageLoadingThread.run: continue dataSource " + System.identityHashCode(dataSource));
                                            }
                                            dataSource.startNextDataSource(/*forceNoDelay=*/ true);
                                        }
                                    } finally {
                                        m_mutex.lock();
                                    }
                                } else {
                                    if (!m_cond.await(entry.targetUptimeMillis - curUptimeMillis, TimeUnit.MILLISECONDS)) {
                                        forceDequeue = true;
                                    }
                                }
                            } else {
                                m_cond.await();
                            }
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (final Exception e) {
                            // do nothing
                        }
                    } // for (;;)
                } finally {
                    m_mutex.unlock();
                }
            }
        }

        private boolean startNextDataSource(final boolean forceNoDelay) {
            if (enableLog) {
                debugLog("DataSource.startNextDataSource " + System.identityHashCode(this));
            }

            Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier = null;
            synchronized (FirstAvailableDataSource.this) {
                // FIXME might have been closed

                mCurrentDataSource = null;

                DataSourceDesc dataSourceDesc = null;
                for (;;) {
                    if (mIndex >= mDataSourceDescs.size()) {
                        if (enableLog) {
                            debugLog("DataSource.startNextDataSource " + System.identityHashCode(this) + ": mIndex limit");
                        }
                        return false;
                    }

                    dataSourceDesc = mDataSourceDescs.get(mIndex);

                    if (dataSourceDesc.adaptToBandwidth && !SmartImageView.isDownlinkBandwidthGoodEnough()) {
                        if (enableLog) {
                            debugLog("DataSource.startNextDataSource %s: bandwidth is not good enough, skipping", System.identityHashCode(this));
                        }
                        ++mIndex;
                        continue;
                    }

                    break;
                }

                if (!forceNoDelay && dataSourceDesc.delayMillisec > 0) {
                    if (enableLog) {
                        debugLog("DataSource.startNextDataSource %s: delay %d, desc %s",
                                System.identityHashCode(this), dataSourceDesc.delayMillisec, dataSourceDesc);
                    }
                    s_progressiveImageLoadingThread.schedule(this,
                            SystemClock.uptimeMillis() + dataSourceDesc.delayMillisec,
                            2 * dataSourceDesc.delayMillisec /* paranoid protection against clock skew */);
                    return true;
                }

                mCurrentDataSourceDesc = dataSourceDesc;
                dataSourceSupplier = dataSourceDesc.dataSourceSupplier;
                ++mIndex;
            }
            if (dataSourceSupplier == null) {
                if (enableLog) {
                    debugLog("DataSource.startNextDataSource " + System.identityHashCode(this) + ": null dataSourceSupplier");
                }
                return false;
            }

            // start of new request
            final DataSource<CloseableReference<CloseableImage>> dataSource = dataSourceSupplier.get();

            if (enableLog) {
                debugLog("DataSource.startNextDataSource: %s desc %s", System.identityHashCode(this), mCurrentDataSourceDesc);
            }
            synchronized (FirstAvailableDataSource.this) {
                // FIXME might have been closed

                mCurrentDataSource = dataSource;
            }

            if (dataSource == null) {
                if (enableLog) {
                    debugLog("DataSource.startNextDataSource " + System.identityHashCode(this) + ": null dataSource");
                }
                return false;
            }

            final InternalDataSubscriber dataSubscriber = new InternalDataSubscriber();
            if (enableLog) {
                debugLog("DataSource.startNextDataSource " + System.identityHashCode(this) + ": " +
                        "dataSource " + System.identityHashCode(dataSource) + ", dataSubscriber " + System.identityHashCode(dataSubscriber));
            }
            dataSource.subscribe(dataSubscriber, CallerThreadExecutor.getInstance());

            return true;
        }

        private void onDataSourceFailed(final DataSource<CloseableReference<CloseableImage>> dataSource) {
            if (enableLog) {
                debugLog("DataSource.onDataSourceFailed %s: dataSource: %s %s",
                        System.identityHashCode(this), System.identityHashCode(dataSource), dataSource
                );
            }

            boolean allSourcedFailed;
            boolean fatalFailure = false;
            boolean close = false;
            synchronized (FirstAvailableDataSource.this) {
                if (isClosed()) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceFailed " + System.identityHashCode(this) + ": isClosed");
                    }
                    return;
                }

                if (dataSource != mCurrentDataSource) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceFailed " + System.identityHashCode(this) + ": spurious");
                    }
                    return;
                }

                mCurrentDataSource = null;

                allSourcedFailed = (mDataSourceWithResult == null);
                if (allSourcedFailed) {
                    if (mCurrentDataSourceDesc == null || mCurrentDataSourceDesc.isFatal) {
                        fatalFailure = true;
                    }

                    if (enableLog) {
                        debugLog("DataSource.onDataSourceFailed: %s: all failed, fatal: %b", System.identityHashCode(this), fatalFailure);
                    }
                }

                if (dataSource != mDataSourceWithResult) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceFailed " + System.identityHashCode(this) + ": close");
                    }
                    close = true;
                }
            }

            if (fatalFailure) {
                if (enableLog) {
                    debugLog("DataSource.onDataSourceFailed %s", System.identityHashCode(this));
                }
                setFailure(dataSource.getFailureCause());
            } else {
                // not a fatal failure, try to start next source
                boolean startedNext = startNextDataSource(/*forceNoDelay=*/ false);

                if (enableLog) {
                    debugLog("DataSource.onDataSourceFailed %s, started next: %b", System.identityHashCode(this), startedNext);
                }

                if (startedNext) {
                    // wait for callback
                } else {
                    // next one is not started, report failure
                    if (allSourcedFailed) {
                        setFailure(dataSource.getFailureCause());
                    } else {
                        setResult(null, /*isLast=*/ true);
                    }
                }
            }

            if (close) {
                dataSource.close();
            }
        }

        private void onDataSourceNewResult(final DataSource<CloseableReference<CloseableImage>> dataSource) {
            if (enableLog) {
                debugLog("DataSource.onDataSourceNewResult " + System.identityHashCode(this) + ": dataSource " + System.identityHashCode(dataSource));
            }

            if (!dataSource.isFinished()) {
                return;
            }

            DataSource oldDataSource = null;
            boolean isLast = false;

            synchronized (FirstAvailableDataSource.this) {
                if (isClosed()) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceNewResult " + System.identityHashCode(this) + ": isClosed");
                    }
                    dataSource.close();
                    return;
                }

                if (dataSource != mCurrentDataSource) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceNewResult " + System.identityHashCode(this) + ": spurious");
                    }
                    if (dataSource != mDataSourceWithResult) {
                        dataSource.close();
                    }
                    return;
                }

                mCurrentDataSource = null;

                if (dataSource != mDataSourceWithResult) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceNewResult " + System.identityHashCode(this) + ": new mDataSourceWithResult");
                    }
                    oldDataSource = mDataSourceWithResult;
                    mDataSourceWithResult = dataSource;
                }

                if (mCurrentDataSourceDesc == null || mCurrentDataSourceDesc.isPreferred) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceNewResult " + System.identityHashCode(this) + ": isLast");
                    }
                    isLast = true;
                }
            }

            if (oldDataSource != null) {
                oldDataSource.close();
            }

            setResult(null, isLast);

            if (!isLast) {
                if (!startNextDataSource(/*forceNoDelay=*/ false)) {
                    if (enableLog) {
                        debugLog("DataSource.onDataSourceNewResult " + System.identityHashCode(this) + ": !startNextDataSource, SIGNALING FINAL RESULT");
                    }
                    setResult(null, /*isLast=*/ true);
                }
            }
        }

        // TODO private static? Weak data source ref?
        private class InternalDataSubscriber implements DataSubscriber<CloseableReference<CloseableImage>> {

            @Override
            public void onFailure(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (enableLog) {
                    debugLog("DataSubscriber.onFailure " + System.identityHashCode(this) + ": " +
                            "dataSource " + System.identityHashCode(dataSource));
                }
                FirstAvailableDataSource.this.onDataSourceFailed(dataSource);
            }

            @Override
            public void onCancellation(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (enableLog) {
                    debugLog("DataSubscriber.onCancellation " + System.identityHashCode(this) + ": " +
                            "dataSource " + System.identityHashCode(dataSource));
                }
            }

            @Override
            public void onNewResult(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (enableLog) {
                    debugLog("DataSubscriber.onNewResult " + System.identityHashCode(this) + ": " +
                            "dataSource " + System.identityHashCode(dataSource));
                }
                if (dataSource.hasResult()) {
                    if (enableLog) {
                        debugLog("DataSubscriber.onNewResult " + System.identityHashCode(this) + ": " +
                                "dataSource " + System.identityHashCode(dataSource) + " has result");
                    }
                    FirstAvailableDataSource.this.onDataSourceNewResult(dataSource);
                } else if (dataSource.isFinished()) {
                    if (enableLog) {
                        debugLog("DataSubscriber.onNewResult " + System.identityHashCode(this) + ": " +
                                "dataSource " + System.identityHashCode(dataSource) + " finished");
                    }
                    FirstAvailableDataSource.this.onDataSourceFailed(dataSource);
                }
            }

            @Override
            public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (enableLog) {
                    debugLog("DataSubscriber.onProgressUpdate " + System.identityHashCode(this) + ": " +
                            "dataSource " + System.identityHashCode(dataSource) + ", progress " + dataSource.getProgress());
                }
                float oldProgress = FirstAvailableDataSource.this.getProgress();
                FirstAvailableDataSource.this.setProgress(Math.max(oldProgress, dataSource.getProgress()));
            }
        }
    }
}
