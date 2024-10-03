package com.chartboost.sdk.internal.clickthrough

import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class UrlRedirectTest {
    private val sslSocketFactoryMock = mockk<SSLSocketFactory>()
    private val urlFactoryMock = mockk<((String) -> URL)>()
    private val urlMock = mockk<URL>()
    private val urlConnectionMock = mockk<HttpsURLConnection>()

    private val urlRedirect =
        UrlRedirect(
            urlFactoryMock,
            sslSocketFactoryMock,
        )

    @Before
    fun setup() {
        every { urlFactoryMock.invoke(any()) } returns urlMock
        every { urlMock.openConnection() } returns urlConnectionMock
        every { urlConnectionMock.sslSocketFactory = sslSocketFactoryMock } just Runs
        every { urlConnectionMock.instanceFollowRedirects = false } just Runs
        every { urlConnectionMock.connectTimeout = 10000 } just Runs
        every { urlConnectionMock.readTimeout = 10000 } just Runs
        every { urlConnectionMock.disconnect() } just Runs
    }

    @Test
    fun `redirect invalid empty url`() {
        val invalidUrl = ""
        urlRedirect.redirect(invalidUrl, 0)
            .shouldBeFailure<UrlRedirect.Failure.EmptyOrNullUrl>()
    }

    @Test
    fun `redirect invalid null url`() {
        val invalidUrl = null
        urlRedirect.redirect(invalidUrl, 0)
            .shouldBeFailure<UrlRedirect.Failure.EmptyOrNullUrl>()
    }

    @Test
    fun `redirect already final url`() {
        val finalUrl = "https://redirected.com"
        every { urlConnectionMock.responseCode } returns 200
        urlRedirect.redirect(finalUrl, 0)
            .getOrThrow() shouldBe finalUrl
    }

    @Test
    fun `when url is 404 it should return a failure`() {
        val url = "https://redirected.com"
        every { urlConnectionMock.responseCode } returns 404
        urlRedirect.redirect(url, 0)
            .shouldBeFailure<UrlRedirect.Failure.HttpErrorCode>()
    }

    @Test
    fun `when connection is null it should return a failure`() {
        every { urlMock.openConnection() } returns null
        urlRedirect.redirect("foo", 0)
            .shouldBeFailure<UrlRedirect.Failure.NullConnection>()
    }

    @Test
    fun `when connection throws an exception it should return failure`() {
        every { urlMock.openConnection() } throws IOException("Boom")
        urlRedirect.redirect("foo", 0)
            .shouldBeFailure<UrlRedirect.Failure.UncontrolledError>()
    }

    @Test
    fun `when too many redirections it should return too many redirections`() {
        val url = "https://bit.ly/3OGvBVM"
        val redirectUrl = "https://redirected.com"
        every { urlConnectionMock.responseCode } returns 301
        every { urlConnectionMock.getHeaderField("Location") } returns redirectUrl
        urlRedirect.redirect(url, 100)
            .shouldBeFailure<UrlRedirect.Failure.TooManyRedirects>()
    }

    @Test
    fun `when redirecting it should return to the redirected url`() {
        val url = "https://bit.ly/3OGvBVM"
        val redirectUrl = "https://redirected.com"
        var openConnectionCount = 0
        every { urlMock.openConnection() } answers {
            openConnectionCount++
            urlConnectionMock
        }
        every { urlConnectionMock.responseCode } answers {
            if (openConnectionCount > 1) 200 else 302
        }
        every { urlConnectionMock.getHeaderField("Location") } returns redirectUrl
        urlRedirect.redirect(url, 1)
            .getOrThrow() shouldBe redirectUrl
    }

    @Test
    fun `when redirection limit is 0 it should return too many redirections`() {
        val url = "https://bit.ly/3OGvBVM"
        val redirectUrl = "https://redirected.com"
        every { urlConnectionMock.responseCode } returns 301
        every { urlConnectionMock.getHeaderField("Location") } returns redirectUrl
        urlRedirect.redirect(url, 0)
            .shouldBeFailure<UrlRedirect.Failure.TooManyRedirects>()
    }

    @Test
    fun `redirect market scheme`() {
        val url = "market://details?id=com.murka.scatterslots&referrer=adjust_reftag%3Dc9nVEM2bl2yTK%26utm_source%3DAarki%26utm_campaign%3D%255Bg%257CScatter%255D%255Bp%257Candroid%255D%255Bid%257C16238%255DTier_1%26utm_content%3DAndroid%2BTest%2BApp%2B%2528DO%2BNOT%2BMODIFY%2529_com.chartboost.sdk.sample.cbtest%26utm_term%3DScatterSlots_UA_WednesdayDance_EN_720x1280_Vid-20s_CRM_IP_MNAG-CL"
        val urlRedirectLocal =
            UrlRedirect(
                ::URL,
                sslSocketFactoryMock,
            )
        val result =
            urlRedirectLocal.redirect(url, 10)
                .shouldBeFailure<UrlRedirect.Failure.UncontrolledError>()
        assertEquals(result.url, url)
    }

    @Test
    fun `reconstruct relative path from location header`() {
        val url = "https://www.amazon.com/gp/mas/dl/android?asin=B004FRX0MY"
        val redirectUrl = "/bg/B004FRX0MY"
        every { urlMock.protocol } returns "https"
        every { urlMock.host } returns "www.amazon.com"
        var openConnectionCount = 0
        every { urlMock.openConnection() } answers {
            openConnectionCount++
            urlConnectionMock
        }
        every { urlConnectionMock.responseCode } answers {
            if (openConnectionCount > 1) 200 else 302
        }
        every { urlConnectionMock.getHeaderField("Location") } returns redirectUrl
        urlRedirect.redirect(url, 10)
            .shouldBeSuccess("https://www.amazon.com/bg/B004FRX0MY")
    }

    @Test
    fun `reconstruct relative path from location header and return market scheme`() {
        val urlConnectionMockThrows = mockk<HttpsURLConnection>()
        every { urlConnectionMockThrows.connect() } answers {
            throw Throwable("error")
        }

        val finalUrl = "market://details?id=com.murka.scatterslots&referrer=adjust_reftag%3Dc9nVEM2bl2yTK%26utm_source%3DAarki%26utm_campaign%3D%255Bg%257CScatter%255D%255Bp%257Candroid%255D%255Bid%257C16238%255DTier_1%26utm_content%3DAndroid%2BTest%2BApp%2B%2528DO%2BNOT%2BMODIFY%2529_com.chartboost.sdk.sample.cbtest%26utm_term%3DScatterSlots_UA_WednesdayDance_EN_720x1280_Vid-20s_CRM_IP_MNAG-CL"
        val url = "https://www.amazon.com/gp/mas/dl/android?asin=B004FRX0MY"
        val redirectUrl = "/bg/B004FRX0MY"
        every { urlMock.protocol } returns "https"
        every { urlMock.host } returns "www.amazon.com"
        var openConnectionCount = 0
        every { urlMock.openConnection() } answers {
            openConnectionCount++
            if (openConnectionCount > 2) urlConnectionMockThrows else urlConnectionMock
        }

        every { urlConnectionMock.responseCode } answers {
            if (openConnectionCount > 2) 200 else 302
        }

        every { urlConnectionMock.getHeaderField("Location") } answers {
            if (openConnectionCount > 1) finalUrl else redirectUrl
        }

        val result =
            urlRedirect.redirect(url, 10)
                .shouldBeFailure<UrlRedirect.Failure.UncontrolledError>()
        assertEquals(result.url, finalUrl)
    }
}
