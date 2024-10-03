package com.chartboost.sdk.internal.video.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class RepositoryUtilsTest : ShouldSpec({

    context("toDownloadState") {

        should("return VIDEO_STATE_EMPTY when 0") {
            0f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_EMPTY
        }

        should("return VIDEO_STATE_QUARTILE_1 when between 0.0 and 0.25") {
            0.01f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_1
        }

        should("return VIDEO_STATE_QUARTILE_1 when < 0.25") {
            0.24f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_1
        }

        should("return VIDEO_STATE_QUARTILE_2 when between 0.25 and 0.5") {
            0.26f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_2
        }

        should("return VIDEO_STATE_QUARTILE_2 when < 0.5") {
            0.49f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_2
        }

        should("return VIDEO_STATE_QUARTILE_3 when between 0.5 and 0.75") {
            0.51f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_3
        }

        should("return VIDEO_STATE_QUARTILE_3 when < 0.75") {
            0.74f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_3
        }

        should("return VIDEO_STATE_QUARTILE_4 when between 0.75 and 1") {
            0.76f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_4
        }

        should("return VIDEO_STATE_QUARTILE_4 when < 1") {
            0.99f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_QUARTILE_4
        }

        should("return VIDEO_STATE_FULL when >= 1") {
            1f.toDownloadState() shouldBe VideoRepository.VIDEO_STATE_FULL
        }
    }
})
