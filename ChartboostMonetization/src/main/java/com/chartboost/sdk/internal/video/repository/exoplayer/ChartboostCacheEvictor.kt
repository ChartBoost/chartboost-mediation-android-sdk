/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.internal.logging.Logger
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheEvictor
import com.google.android.exoplayer2.upstream.cache.CacheSpan
import java.util.TreeSet

/**
 * Logic copied from LeastRecentlyUsedCacheEvictor from ExoPlayer.
 * Added logic to handle fake precache and precache queue files.
 */
internal class ChartboostCacheEvictor(
    private val maxBytes: Long,
    private val evictUrlCallback: EvictUrlCallback,
    private val treeSetFactory: () -> TreeSet<CacheSpan> = { TreeSet(::compare) },
) : CacheEvictor {
    private val leastRecentlyUsed: TreeSet<CacheSpan> by lazy { treeSetFactory() }
    private var currentSize: Long = 0

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() {
        // Do nothing.
    }

    override fun onStartFile(
        cache: Cache,
        key: String,
        position: Long,
        length: Long,
    ) {
        if (length != C.LENGTH_UNSET.toLong()) {
            evictCache(cache, length)
        }
    }

    override fun onSpanAdded(
        cache: Cache,
        span: CacheSpan,
    ) {
        leastRecentlyUsed.add(span)
        currentSize += span.length
        evictCache(cache, 0)
    }

    override fun onSpanRemoved(
        cache: Cache,
        span: CacheSpan,
    ) {
        leastRecentlyUsed.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(
        cache: Cache,
        oldSpan: CacheSpan,
        newSpan: CacheSpan,
    ) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(
        cache: Cache,
        requiredSpace: Long,
    ) {
        while (currentSize + requiredSpace > maxBytes && !leastRecentlyUsed.isEmpty()) {
            val cacheSpanToEvict = leastRecentlyUsed.first()
            Logger.d("evictCache() - ${cacheSpanToEvict.key}")
            cache.removeSpan(cacheSpanToEvict)
            evictUrlCallback.onEvictingUrl(cacheSpanToEvict.key)
        }
    }

    fun interface EvictUrlCallback {
        fun onEvictingUrl(url: String)
    }
}

private fun compare(
    lhs: CacheSpan,
    rhs: CacheSpan,
): Int {
    val lastTouchTimestampDelta = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp
    return if (lastTouchTimestampDelta == 0L) {
        // Use the standard compareTo method as a tie-break.
        lhs.compareTo(rhs)
    } else if (lhs.lastTouchTimestamp < rhs.lastTouchTimestamp) {
        -1
    } else {
        1
    }
}
