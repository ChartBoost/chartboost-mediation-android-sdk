package com.chartboost.sdk.internal.initialization

import android.content.Context
import com.chartboost.sdk.test.FakeUiPoster
import io.mockk.mockk
import org.junit.Test

class ProviderInstallerHelperTest {
    private val contextMock = mockk<Context>()
    private val uiPosterMock = FakeUiPoster()
    private val providerInstallerHelper = ProviderInstallerHelper(contextMock, uiPosterMock)

    @Test
    fun `install provider`() {
        providerInstallerHelper.installProviderIfPossible()
    }

    @Test
    fun `on request success`() {
        providerInstallerHelper.onProviderInstalled()
    }

    @Test
    fun `on request failure`() {
        providerInstallerHelper.onProviderInstallFailed(0, null)
    }
}
