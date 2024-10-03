package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class ImpressionTrackerRequestTest {
    private val eventTrackerExtensionsMock: EventTrackerExtensions = mockk()
    private val cbError =
        CBError(
            type = CBError.Internal.INTERNET_UNAVAILABLE,
            errorDesc = "foo",
        )

    private val url: String = "url"

    private val sut =
        ImpressionTrackerRequest(
            url = url,
            eventTracker = eventTrackerExtensionsMock,
        )

    @Test
    fun `when deliverError() is called with both arguments set as null, it should track error event with empty message`() {
        val slot = slot<ErrorEvent>()
        with(eventTrackerExtensionsMock) {
            every { capture(slot).track() } answers { firstArg() }
        }

        sut.deliverError(null, null)

        slot.captured.message.shouldBeEmpty()

        verify(exactly = 1) {
            with(eventTrackerExtensionsMock) {
                any<ErrorEvent>().track()
            }
        }
    }

    @Test
    fun `when deliverError() is called with server response as null and CBError not null, it should track error event with CBError message`() {
        val slot = slot<ErrorEvent>()
        with(eventTrackerExtensionsMock) {
            every { capture(slot).track() } answers { firstArg() }
        }

        sut.deliverError(cbError, null)

        slot.captured.message shouldContain url
        slot.captured.message shouldContain cbError.message!!
        slot.captured.message shouldContain cbError.type.toString()

        verify(exactly = 1) {
            with(eventTrackerExtensionsMock) {
                any<ErrorEvent>().track()
            }
        }
    }

    @Test
    fun `when deliverError() is called with server response OK and CBError null, it should track error event with empty message`() {
        val slot = slot<ErrorEvent>()
        with(eventTrackerExtensionsMock) {
            every { capture(slot).track() } answers { firstArg() }
        }
        val serverResponse =
            CBNetworkServerResponse(
                statusCode = 200,
                data = ByteArray(0),
            )

        sut.deliverError(null, serverResponse)

        slot.captured.message.shouldBeEmpty()

        verify(exactly = 1) {
            with(eventTrackerExtensionsMock) {
                any<ErrorEvent>().track()
            }
        }
    }

    @Test
    fun `when deliverError() is called with server response OK and CBError not null, it should track error event CBError message`() {
        val slot = slot<ErrorEvent>()
        with(eventTrackerExtensionsMock) {
            every { capture(slot).track() } answers { firstArg() }
        }
        val serverResponse =
            CBNetworkServerResponse(
                statusCode = 200,
                data = ByteArray(0),
            )

        sut.deliverError(cbError, serverResponse)

        slot.captured.message shouldContain cbError.type.toString()
        slot.captured.message shouldContain cbError.message!!
        slot.captured.message shouldContain url

        verify(exactly = 1) {
            with(eventTrackerExtensionsMock) {
                any<ErrorEvent>().track()
            }
        }
    }

    @Test
    fun `when deliverError() is called with server response KO and CBError null, it should track error event with server error message`() {
        val slot = slot<ErrorEvent>()
        with(eventTrackerExtensionsMock) {
            every { capture(slot).track() } answers { firstArg() }
        }
        val serverResponse =
            CBNetworkServerResponse(
                statusCode = 400,
                data = ByteArray(0),
            )

        sut.deliverError(null, serverResponse)

        slot.captured.message shouldContain url
        slot.captured.message shouldContain "400"

        verify(exactly = 1) {
            with(eventTrackerExtensionsMock) {
                any<ErrorEvent>().track()
            }
        }
    }

    @Test
    fun `when deliverError() is called with server response KO and CBError not null, it should track error event with server error message`() {
        val slot = slot<ErrorEvent>()
        with(eventTrackerExtensionsMock) {
            every { capture(slot).track() } answers { firstArg() }
        }
        val serverResponse =
            CBNetworkServerResponse(
                statusCode = 400,
                data = ByteArray(0),
            )

        sut.deliverError(cbError, serverResponse)

        slot.captured.message shouldContain url
        slot.captured.message shouldContain "400"

        verify(exactly = 1) {
            with(eventTrackerExtensionsMock) {
                any<ErrorEvent>().track()
            }
        }
    }
}
