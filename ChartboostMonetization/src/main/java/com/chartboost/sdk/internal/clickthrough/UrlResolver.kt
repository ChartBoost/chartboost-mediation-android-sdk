package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.internal.Model.CBError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Coordinate url validity, redirection and openers
 */
internal class UrlResolver(
    private val urlRedirect: UrlRedirect,
    private val actions: List<UrlAction> =
        listOf(
            { openUnsecureLink(it) },
            { openDeepLink(it) },
            { openInEmbeddedBrowser(it) },
            { openInNativeBrowser(it) },
        ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun resolve(
        url: String?,
        clkp: ClickPreference,
        clickTracking: ClickTracking,
    ): CBError.Click? {
        if (url.isNullOrEmpty()) return CBError.Click.URI_INVALID
        CoroutineScope(ioDispatcher).launch {
            val finalUrl =
                urlRedirect.redirect(url)
                    .trackRedirectResult(url, clickTracking)
                    .fold(
                        onSuccess = { it },
                        onFailure = {
                            if (it is UrlRedirect.Failure.UncontrolledError) {
                                it.url
                            } else {
                                url
                            }
                        },
                    )
            val args = UrlArgs(finalUrl, clkp)
            executeUrlActions(args, clickTracking)
        }
        return null
    }

    private fun Result<String>.trackRedirectResult(
        originalUrl: String,
        clickTracking: ClickTracking,
    ): Result<String> =
        apply {
            fold(
                onSuccess = {
                    clickTracking.trackNavigationSuccess("Redirection successful from $originalUrl to $it")
                },
                onFailure = {
                    clickTracking.trackNavigationFailure("Redirection failed for $originalUrl: $it")
                },
            )
        }

    private suspend fun executeUrlActions(
        args: UrlArgs,
        clickTracking: ClickTracking,
    ) {
        actions.fold(Result.failure<UrlActionResult>(Exception())) { previousResult, action ->
            previousResult.fold(
                onSuccess = { previousResult },
                onFailure = { executeAndTrackAction(action, args, clickTracking) },
            )
        }.onFailure {
            clickTracking.trackNavigationFailure("None of the actions was able to process URL ${args.url}")
        }
    }

    private suspend fun executeAndTrackAction(
        action: UrlAction,
        args: UrlArgs,
        clickTracking: ClickTracking,
    ): Result<UrlActionResult> {
        return action(args)
            .onSuccess {
                clickTracking.trackNavigationSuccess("Url ${args.url} opened with action ${it.actionName}")
            }
            .onFailure {
                // Report only unexpected errors and not valid fallback reasons to avoid sending invalid data
                if (it !is UrlOpenerFallbackReason) {
                    clickTracking.trackNavigationFailure("Url ${args.url} opening failed with error $it")
                }
            }
    }
}

internal typealias UrlAction = suspend (UrlArgs) -> Result<UrlActionResult>

internal data class UrlArgs(
    val url: String,
    val clickPreference: ClickPreference,
)

internal data class UrlActionResult(val actionName: String)
