package com.chartboost.sdk.internal.Libraries

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CBCryptoTest : FunSpec() {
    init {
        test("CBCrypto.getSha1Hex will return the SHA-1 in hexadecimal") {
            CBCrypto.getSha1Hex("Chartboost") shouldBe "6183198f8e485142d53ae0c9f73230ea15ade123"
            CBCrypto.getSha1Hex("test") shouldBe "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
        }
    }
}
