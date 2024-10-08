/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.testutils

import java.io.InputStreamReader

class MockResponseFileReader(
    path: String,
) {
    val content: String

    init {
        InputStreamReader(this.javaClass.classLoader!!.getResourceAsStream(path))
            .also {
                content = it.readText()
            }.close()
    }
}
