/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import com.chartboost.heliumsdk.utils.HeliumJson
import com.chartboost.heliumsdk.utils.getMaxJsonPayload
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HeliumJsonTest {

    private val SAMPLE_FULL_JSON_PAYLOAD = """{"nameform":"work_data","login_admin":"noteadm","login":"user","passw":"password","mode":"flat","mode_data":[{"flat_data":{"unique_start":"1516966548.648839","flat_on_stage_id":33,"flat_status_id":4,"retry_count":3,"time_start":"2017-01-26 15:51:18","duration":70,"gps_start":"GPS","gps_end":"GPS","audio_info":"35","photo_flat":"ph_1516966548_fl_hRd8dpz"},"quizer_data":{"argessive":0,"registration":1,"take_apm":0,"request":1,"live_in_flat":3,"first_vote":1,"will_vote":1,"home_vote":0,"rf":1}}]}"""

    private val PARTIAL_PAYLOAD_MAX_SIZE = 300

    private val BASE64_ENCODED_FULL_PAYLOAD = "eyJuYW1lZm9ybSI6IndvcmtfZGF0YSIsImxvZ2luX2FkbWluIjoibm90ZWFkbSIsImxvZ2luIjoidXNlciIsInBhc3N3IjoicGFzc3dvcmQiLCJtb2RlIjoiZmxhdCIsIm1vZGVfZGF0YSI6W3siZmxhdF9kYXRhIjp7InVuaXF1ZV9zdGFydCI6IjE1MTY5NjY1NDguNjQ4ODM5IiwiZmxhdF9vbl9zdGFnZV9pZCI6MzMsImZsYXRfc3RhdHVzX2lkIjo0LCJyZXRyeV9jb3VudCI6MywidGltZV9zdGFydCI6IjIwMTctMDEtMjYgMTU6NTE6MTgiLCJkdXJhdGlvbiI6NzAsImdwc19zdGFydCI6IkdQUyIsImdwc19lbmQiOiJHUFMiLCJhdWRpb19pbmZvIjoiMzUiLCJwaG90b19mbGF0IjoicGhfMTUxNjk2NjU0OF9mbF9oUmQ4ZHB6In0sInF1aXplcl9kYXRhIjp7ImFyZ2Vzc2l2ZSI6MCwicmVnaXN0cmF0aW9uIjoxLCJ0YWtlX2FwbSI6MCwicmVxdWVzdCI6MSwibGl2ZV9pbl9mbGF0IjozLCJmaXJzdF92b3RlIjoxLCJ3aWxsX3ZvdGUiOjEsImhvbWVfdm90ZSI6MCwicmYiOjF9fV19"
    private val BASE64_ENCODED_PARTIAL_PAYLOAD_WITH_MESSAGE = "VGhlIG1hbGZvcm1lZCBKU09OIGlzIHRvbyBsYXJnZSB0byBpbmNsdWRlLiBQYXJ0aWFsIEpTT046IHsibmFtZWZvcm0iOiJ3b3JrX2RhdGEiLCJsb2dpbl9hZG1pbiI6Im5vdGVhZG0iLCJsb2dpbiI6InVzZXIiLCJwYXNzdyI6InBhc3N3b3JkIiwibW9kZSI6ImZsYXQiLCJtb2RlX2RhdGEiOlt7ImZsYXRfZGF0YSI6eyJ1bmlxdWVfc3RhcnQiOiIxNTE2OTY2NTQ4LjY0ODgzOSIsImZsYXRfb25f"

    @Test
    fun `make sure HeliumJson has the correct flags set`() {
        assertTrue(HeliumJson.configuration.isLenient)
        assertTrue(HeliumJson.configuration.ignoreUnknownKeys)
        assertTrue(HeliumJson.configuration.encodeDefaults)

        assertFalse(HeliumJson.configuration.explicitNulls)
    }

    @Test
    fun `getMaxJsonPayload with payload smaller than max size returns base64 encoded payload`() {
        val actualResult = getMaxJsonPayload(SAMPLE_FULL_JSON_PAYLOAD, Int.MAX_VALUE)
        assertEquals(BASE64_ENCODED_FULL_PAYLOAD, actualResult)
    }

    @Test
    fun `getMaxJsonPayload with payload larger than max size returns base64 message and partial payload`() {
        val actualResult = getMaxJsonPayload(SAMPLE_FULL_JSON_PAYLOAD, PARTIAL_PAYLOAD_MAX_SIZE)
        assertEquals(BASE64_ENCODED_PARTIAL_PAYLOAD_WITH_MESSAGE, actualResult)
    }
}
