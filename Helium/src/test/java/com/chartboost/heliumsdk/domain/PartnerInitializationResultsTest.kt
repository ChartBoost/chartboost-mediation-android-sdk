/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

import com.chartboost.heliumsdk.PartnerInitializationResults
import junit.framework.TestCase.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class PartnerInitializationResultsTest {
    /**
     * A list of public fields that are required to be present in the public (i.e. transformed) payload.
     */
    private val requiredPublicFields = listOf(
        "session_id",
        "success",
        "failure",
        "in_progress",
        "skipped"
    )

    /**
     * A sample payload that is sent to the server. It has been prepared to contain all possible values
     * for the public fields.
     */
    private val internalPayloadRaw =
        "{ \"result\": \"success_with_cached_config\", \"metrics\": [ { \"partner\": \"adcolony\", \"start\": 1680291225659, \"end\": 1680291225659, \"is_success\": false, \"helium_error\": \"CM_INITIALIZATION_SKIPPED (CM_109). Cause: You explicitly skipped initializing the partner. Resolution: N\\/A.\", \"helium_error_code\": \"CM_109\", \"helium_error_message\": \"Partner initialization was skipped.\" }, { \"partner\": \"admob\", \"start\": 1680291225659, \"end\": 1680291225659, \"is_success\": false, \"helium_error\": \"CM_INITIALIZATION_SKIPPED (CM_109). Cause: You explicitly skipped initializing the partner. Resolution: N\\/A.\", \"helium_error_code\": \"CM_109\", \"helium_error_message\": \"Partner initialization was skipped.\" }, { \"partner\": \"applovin\", \"start\": 1680291225660, \"end\": 1680291227373, \"duration\": 1713, \"is_success\": true, \"partner_sdk_version\": \"11.8.1\", \"partner_adapter_version\": \"4.11.8.1.1\" }, { \"partner\": \"amazon_aps\", \"start\": 1680291225692, \"end\": 1680291225706, \"duration\": 14, \"is_success\": true, \"partner_sdk_version\": \"aps-android-9.7.0-OTHER\", \"partner_adapter_version\": \"4.9.7.0.1\" }, { \"partner\": \"chartboost\", \"start\": 1680291225706, \"end\": 1680291226079, \"duration\": 373, \"is_success\": true, \"partner_sdk_version\": \"9.2.1\", \"partner_adapter_version\": \"4.9.2.1.1\" }, { \"partner\": \"fyber\", \"start\": 1680291225715, \"end\": 1680291226188, \"duration\": 473, \"is_success\": true, \"partner_sdk_version\": \"8.2.2\", \"partner_adapter_version\": \"4.8.2.2.1\" }, { \"partner\": \"facebook\", \"start\": 1680291225732, \"is_success\": false, \"helium_error\": \"CM_INITIALIZATION_FAILURE_TIMEOUT (CM_108). Cause: The initialization operation has taken too long to complete. Resolution: This should not be a critical error. Typically the partner can continue to finish initialization in the background. If this error persists, contact Chartboost Mediation Support and provide a copy of your console logs.\", \"helium_error_code\": \"CM_108\", \"helium_error_message\": \"Partner initialization has failed.\" }, { \"partner\": \"google_googlebidding\", \"start\": 1680291225732, \"end\": 1680291225809, \"duration\": 77, \"is_success\": true, \"partner_sdk_version\": \"21.5.0\", \"partner_adapter_version\": \"4.21.5.0.1\" }, { \"partner\": \"inmobi\", \"start\": 1680291225810, \"end\": 1680291226136, \"duration\": 326, \"is_success\": true, \"partner_sdk_version\": \"10.1.3\", \"partner_adapter_version\": \"4.10.1.3.1\" }, { \"partner\": \"ironsource\", \"start\": 1680291225836, \"end\": 1680291225960, \"duration\": 124, \"is_success\": true, \"partner_sdk_version\": \"7.2.7\", \"partner_adapter_version\": \"4.7.2.7.0.1\" }, { \"partner\": \"mintegral\", \"start\": 1680291225970, \"end\": 1680291225994, \"duration\": 24, \"is_success\": true, \"partner_sdk_version\": \"MAL_16.3.91\", \"partner_adapter_version\": \"4.16.3.91.2\" }, { \"partner\": \"pangle\", \"start\": 1680291225994, \"end\": 1680291226078, \"duration\": 84, \"is_success\": true, \"partner_sdk_version\": \"4.9.1.3\", \"partner_adapter_version\": \"4.4.9.1.3.1\" }, { \"partner\": \"reference\", \"start\": 1680291226005, \"end\": 1680291226508, \"duration\": 503, \"is_success\": true, \"partner_sdk_version\": \"1.0.0\", \"partner_adapter_version\": \"4.1.0.0.1\" }, { \"partner\": \"tapjoy\", \"start\": 1680291226006, \"end\": 1680291226006, \"is_success\": false, \"helium_error\": \"CM_INITIALIZATION_FAILURE_UNKNOWN (CM_100). Cause: There was an error that was not accounted for. Resolution: Try again. If the problem persists, contact Chartboost Mediation Support and provide your console logs.\", \"helium_error_code\": \"CM_100\", \"helium_error_message\": \"Chartboost Mediation initialization has failed.\" }, { \"partner\": \"unity\", \"start\": 1680291226006, \"end\": 1680291227126, \"duration\": 1120, \"is_success\": true, \"partner_sdk_version\": \"4.6.0\", \"partner_adapter_version\": \"4.4.6.0.1\" }, { \"partner\": \"vungle\", \"start\": 1680291226017, \"end\": 1680291226467, \"duration\": 450, \"is_success\": true, \"partner_sdk_version\": \"6.12.1\", \"partner_adapter_version\": \"4.6.12.1.1\" }, { \"partner\": \"yahoo\", \"start\": 1680291226051, \"end\": 1680291226068, \"duration\": 17, \"is_success\": true, \"partner_sdk_version\": \"1.4.0\", \"partner_adapter_version\": \"4.1.4.0.1\" } ] }"

    private lateinit var getPublicPayloadMethod: Method

    /**
     * The JSON version of the raw payload.
     */
    private lateinit var internalPayload: JSONObject

    /**
     * The public version of the JSON version of the raw payload.
     */
    private lateinit var publicPayload: JSONObject

    @Before
    fun setUp() {
        getPublicPayloadMethod =
            PartnerInitializationResults::class.java.getDeclaredMethod(
                "getPublicPayload",
                JSONObject::class.java
            ).apply { isAccessible = true }

        internalPayload = JSONObject(internalPayloadRaw)
        publicPayload = getPublicPayloadMethod.invoke(
            PartnerInitializationResults(), internalPayload
        ) as JSONObject
    }

    @After
    fun tearDown() {
        getPublicPayloadMethod.isAccessible = false
    }

    @Test
    fun `public init metrics payload has all required fields`() {
        requiredPublicFields.forEach { field ->
            assertTrue(publicPayload.has(field))
        }
    }

    @Test
    fun `public init metrics payload has no extra fields`() {
        val children = publicPayload.keys().asSequence().toList()
        assertEquals(requiredPublicFields.size, children.size)
    }

    /**
     * This test is dependent upon the raw JSON payload, which has been pre-processed to have at least
     * 1 partner per success, failure, in_progress, and skipped.
     */
    @Test
    fun `public init metrics payload has at least 1 partner per success, failure, in_progress, skipped`() {
        val success = publicPayload.getJSONArray("success")
        val failure = publicPayload.getJSONArray("failure")
        val inProgress = publicPayload.getJSONArray("in_progress")
        val skipped = publicPayload.getJSONArray("skipped")

        assertTrue(success.length() > 0)
        assertTrue(failure.length() > 0)
        assertTrue(inProgress.length() > 0)
        assertTrue(skipped.length() > 0)
    }

    @Test
    fun `skipped array contains only strings`() {
        val skipped = publicPayload.getJSONArray("skipped")

        for (i in 0 until skipped.length()) {
            assertTrue(skipped[i] is String)
        }
    }

    @Test
    fun `success array contains only JSONObjects`() {
        val success = publicPayload.getJSONArray("success")

        for (i in 0 until success.length()) {
            assertTrue(success[i] is JSONObject)
        }
    }

    @Test
    fun `failure array contains only JSONObjects`() {
        val failure = publicPayload.getJSONArray("failure")

        for (i in 0 until failure.length()) {
            assertTrue(failure[i] is JSONObject)
        }
    }

    @Test
    fun `in_progress array contains only JSONObjects`() {
        val inProgress = publicPayload.getJSONArray("in_progress")

        for (i in 0 until inProgress.length()) {
            assertTrue(inProgress[i] is JSONObject)
        }
    }

    @Test
    fun `session id is a string`() {
        assertTrue(publicPayload["session_id"] is String)
    }

    @Test
    fun `all result arrays must contain unique partner names`() {
        val success = publicPayload.getJSONArray("success")
        val failure = publicPayload.getJSONArray("failure")
        val inProgress = publicPayload.getJSONArray("in_progress")
        val skipped = publicPayload.getJSONArray("skipped")

        val allPartners = mutableSetOf<String>()
        for (i in 0 until success.length()) {
            allPartners.add((success[i] as JSONObject).getString("partner"))
        }
        for (i in 0 until failure.length()) {
            allPartners.add((failure[i] as JSONObject).getString("partner"))
        }
        for (i in 0 until inProgress.length()) {
            allPartners.add((inProgress[i] as JSONObject).getString("partner"))
        }
        for (i in 0 until skipped.length()) {
            allPartners.add(skipped[i] as String)
        }

        assertEquals(
            allPartners.size,
            success.length() + failure.length() + inProgress.length() + skipped.length()
        )
    }

    @Test
    fun `in progress results must include a timeout in seconds for each partner`() {
        val inProgress = publicPayload.getJSONArray("in_progress")

        for (i in 0 until inProgress.length()) {
            val partner = inProgress[i] as JSONObject

            assertTrue(partner.has("timeout_seconds"))
            assertTrue(partner["timeout_seconds"] is Int)
        }
    }

    @Test
    fun `in progress results must include start but not end time`() {
        val inProgress = publicPayload.getJSONArray("in_progress")

        for (i in 0 until inProgress.length()) {
            val partner = inProgress[i] as JSONObject

            assertTrue(partner.has("start"))
            assertTrue(partner["start"] is Long)

            assertFalse(partner.has("end"))
        }
    }

    @Test
    fun `failure results must include helium_error, helium_error_code, and helium_error_message`() {
        val failure = publicPayload.getJSONArray("failure")

        for (i in 0 until failure.length()) {
            val partner = failure[i] as JSONObject

            assertTrue(partner.has("helium_error"))
            assertTrue(partner["helium_error"] is String)

            assertTrue(partner.has("helium_error_code"))
            assertTrue(partner["helium_error_code"] is String)

            assertTrue(partner.has("helium_error_message"))
            assertTrue(partner["helium_error_message"] is String)
        }
    }

    @Test
    fun `success results must include partner, start, end, duration, partner_sdk_version, and partner_adapter_version`() {
        val success = publicPayload.getJSONArray("success")

        for (i in 0 until success.length()) {
            val partner = success[i] as JSONObject

            assertTrue(partner.has("partner"))
            assertTrue(partner["partner"] is String)

            assertTrue(partner.has("start"))
            assertTrue(partner["start"] is Long)

            assertTrue(partner.has("end"))
            assertTrue(partner["end"] is Long)

            assertTrue(partner.has("duration"))
            assertTrue(partner["duration"] is Int)

            assertTrue(partner.has("partner_sdk_version"))
            assertTrue(partner["partner_sdk_version"] is String)

            assertTrue(partner.has("partner_adapter_version"))
            assertTrue(partner["partner_adapter_version"] is String)
        }
    }
}
