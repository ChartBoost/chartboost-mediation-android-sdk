package com.chartboost.sdk.internal.clickthrough

import java.io.Serializable

internal sealed class UrlOpenerFallbackReason : Throwable(), Serializable {
    object MissingAppToOpenSchema : UrlOpenerFallbackReason() {
        private fun readResolve(): Any = MissingAppToOpenSchema
    }

    object WrongPreference : UrlOpenerFallbackReason() {
        private fun readResolve(): Any = WrongPreference
    }

    object NotValidScheme : UrlOpenerFallbackReason() {
        private fun readResolve(): Any = NotValidScheme
    }
}
