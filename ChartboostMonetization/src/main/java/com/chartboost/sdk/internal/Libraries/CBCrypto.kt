package com.chartboost.sdk.internal.Libraries

import java.math.BigInteger
import java.security.MessageDigest

object CBCrypto {
    private const val SHA_1 = "SHA-1"

    private fun sha1(input: ByteArray) = input.let { MessageDigest.getInstance(SHA_1).digest(it) }

    private fun hexRepresentation(data: ByteArray) = "%0${data.size shl 1}x".format(BigInteger(1, data))

    @JvmStatic
    fun getSha1Hex(input: String): String = input.toByteArray(Charsets.UTF_8).let(CBCrypto::sha1).let(CBCrypto::hexRepresentation)
}
