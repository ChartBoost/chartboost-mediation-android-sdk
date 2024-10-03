package com.chartboost.sdk.internal.api

import com.chartboost.sdk.ads.Ad
import com.chartboost.sdk.callbacks.AdCallback
import com.chartboost.sdk.callbacks.DismissibleAdCallback
import com.chartboost.sdk.callbacks.RewardedCallback
import com.chartboost.sdk.events.*
import com.chartboost.sdk.test.FakeUiPoster
import io.mockk.CapturingSlot
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AdApiCallbackSenderTest {
    private val dismissEventCaptor = CapturingSlot<DismissEvent>()
    private val impressionEventCaptor = CapturingSlot<ImpressionEvent>()
    private val clickEventCaptor = CapturingSlot<ClickEvent>()
    private val clickErrorCaptor = CapturingSlot<ClickError>()
    private val rewardEventCaptor = CapturingSlot<RewardEvent>()
    private val cacheEventCaptor = CapturingSlot<CacheEvent>()
    private val cacheErrorCaptor = CapturingSlot<CacheError>()
    private val showEventCaptor = CapturingSlot<ShowEvent>()
    private val showErrorCaptor = CapturingSlot<ShowError>()
    private val lambdaCaptor = slot<() -> Unit>()
    private val uiPoster = mockk<FakeUiPoster>(relaxed = true)
    private val adApiCallbackSender = AdApiCallbackSender(uiPoster)
    private val impressionId = "impressionId"

    @Test
    fun `Send show callback in main thread`() {
        val showError = mockk<ShowError>()
        val adMock = mockk<Ad>()
        val adCallbackMock = mockk<AdCallback>(relaxed = true)
        adApiCallbackSender.sendShowCallbackInMainThread(
            impressionId,
            showError,
            adMock,
            adCallbackMock,
        )
        verify(exactly = 1) {
            uiPoster(capture(lambdaCaptor))
        }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            adCallbackMock.onAdShown(capture(showEventCaptor), capture(showErrorCaptor))
        }
        assertEquals(impressionId, showEventCaptor.captured.adID)
        assertNotNull(showErrorCaptor.captured)
    }

    @Test
    fun `Send reward callback on main thread`() {
        val adMock = mockk<Ad>()
        val adCallbackMock = mockk<RewardedCallback>(relaxed = true)
        adApiCallbackSender.sendRewardCallbackOnMainThread(
            impressionId,
            adMock,
            adCallbackMock,
            20,
        )
        verify(exactly = 1) {
            uiPoster(capture(lambdaCaptor))
        }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            adCallbackMock.onRewardEarned(capture(rewardEventCaptor))
        }
        assertEquals(impressionId, rewardEventCaptor.captured.adID)
        assertEquals(20, rewardEventCaptor.captured.reward)
    }

    @Test
    fun `Send cache callback in main thread`() {
        val cacheError = mockk<CacheError>()
        val adMock = mockk<Ad>()
        val adCallbackMock = mockk<AdCallback>(relaxed = true)
        adApiCallbackSender.sendCacheCallbackInMainThread(
            impressionId,
            cacheError,
            adMock,
            adCallbackMock,
        )
        verify(exactly = 1) {
            uiPoster(capture(lambdaCaptor))
        }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            adCallbackMock.onAdLoaded(
                capture(cacheEventCaptor),
                capture(cacheErrorCaptor),
            )
        }
        assertEquals(impressionId, cacheEventCaptor.captured.adID)
        assertNotNull(cacheErrorCaptor.captured)
    }

    @Test
    fun `Send click callback in main thread`() {
        val clickError = mockk<ClickError>()
        val adMock = mockk<Ad>()
        val adCallbackMock = mockk<AdCallback>(relaxed = true)
        adApiCallbackSender.sendClickCallbackInMainThread(
            impressionId,
            clickError,
            adMock,
            adCallbackMock,
        )
        verify(exactly = 1) {
            uiPoster(capture(lambdaCaptor))
        }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            adCallbackMock.onAdClicked(
                capture(clickEventCaptor),
                capture(clickErrorCaptor),
            )
        }
        assertEquals(impressionId, clickEventCaptor.captured.adID)
        assertNotNull(clickErrorCaptor.captured)
    }

    @Test
    fun `Send dismiss callback in main thread`() {
        val adMock = mockk<Ad>()
        val adCallbackMock = mockk<DismissibleAdCallback>(relaxed = true)
        adApiCallbackSender.sendDismissCallbackOnMainThread(
            impressionId,
            adMock,
            adCallbackMock,
        )
        verify(exactly = 1) {
            uiPoster(capture(lambdaCaptor))
        }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            adCallbackMock.onAdDismiss(capture(dismissEventCaptor))
        }
        assertEquals(
            impressionId,
            dismissEventCaptor.captured.adID,
        )
    }

    @Test
    fun `Send on impression recorded callback in main thread`() {
        val adMock = mockk<Ad>()
        val adCallbackMock = mockk<AdCallback>(relaxed = true)
        adApiCallbackSender.sendImpressionRecordedCallbackOnMainThread(
            impressionId,
            adMock,
            adCallbackMock,
        )
        verify(exactly = 1) {
            uiPoster(capture(lambdaCaptor))
        }
        lambdaCaptor.captured.invoke()
        verify(exactly = 1) {
            adCallbackMock.onImpressionRecorded(
                capture(impressionEventCaptor),
            )
        }
        assertEquals(
            impressionId,
            impressionEventCaptor.captured.adID,
        )
    }
}
