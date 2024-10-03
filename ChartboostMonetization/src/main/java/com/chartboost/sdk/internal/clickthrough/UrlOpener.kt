package com.chartboost.sdk.internal.clickthrough

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.chartboost.sdk.internal.di.getApplicationContext
import com.chartboost.sdk.internal.di.getIntentResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun openInNativeBrowser(
    args: UrlArgs,
    context: Context = getApplicationContext(),
    uriParser: (String) -> Uri = Uri::parse,
    intentFactory: (Uri) -> Intent = { Intent(Intent.ACTION_VIEW, it) },
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
): Result<UrlActionResult> =
    runCatching {
        if (!args.isNativeBrowser()) throw UrlOpenerFallbackReason.WrongPreference
        args.convertMarketToHttps().let { args ->
            context.startActivityInMain(
                uriParser(args.url).let(intentFactory),
                mainDispatcher,
            )
        }
        UrlActionResult("openInNativeBrowser")
    }

internal suspend fun openInEmbeddedBrowser(
    args: UrlArgs,
    context: Context = getApplicationContext(),
    uriParser: (String) -> Uri = Uri::parse,
    intentFactory: (String) -> Intent = { url -> EmbeddedBrowserActivity.intent(context, url) },
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
): Result<UrlActionResult> =
    runCatching {
        if (!args.isEmbeddedBrowser()) throw UrlOpenerFallbackReason.WrongPreference
        uriParser(args.url) // Making sure the URL is valid before proceeding
        args.convertMarketToHttps().let { args ->
            context.startActivityInMain(
                intentFactory(args.url),
                mainDispatcher,
            )
        }
        UrlActionResult("openInEmbeddedBrowser")
    }

internal suspend fun openDeepLink(
    args: UrlArgs,
    context: Context = getApplicationContext(),
    intentResolver: IntentResolver = getIntentResolver(),
    uriParser: (String) -> Uri = Uri::parse,
    intentFactory: (Uri) -> Intent = { Intent(Intent.ACTION_VIEW, it) },
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
): Result<UrlActionResult> =
    runCatching {
        if (!intentResolver.canOpenDeeplink(args.url)) {
            // Should we update error codes? Check with iOS
            throw UrlOpenerFallbackReason.MissingAppToOpenSchema
        }
        context.startActivityInMain(
            uriParser(args.url).let(intentFactory),
            mainDispatcher,
        )
        UrlActionResult("openDeepLink")
    }

internal suspend fun openUnsecureLink(
    args: UrlArgs,
    context: Context = getApplicationContext(),
    uriParser: (String) -> Uri = Uri::parse,
    intentFactory: (Uri) -> Intent = { Intent(Intent.ACTION_VIEW, it) },
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
): Result<UrlActionResult> =
    runCatching {
        if (!args.isUnsecureScheme(uriParser)) throw UrlOpenerFallbackReason.NotValidScheme
        context.startActivityInMain(
            uriParser(args.url).let(intentFactory),
            mainDispatcher,
        )
        UrlActionResult("openUnsecureLink")
    }

private suspend fun Context.startActivityInMain(
    intent: Intent,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    withContext(mainDispatcher) {
        startActivity(
            intent.flagNewTask(),
        )
    }
}

private fun UrlArgs.isEmbeddedBrowser(): Boolean = clickPreference == ClickPreference.CLICK_PREFERENCE_EMBEDDED

private fun UrlArgs.isNativeBrowser(): Boolean = clickPreference == ClickPreference.CLICK_PREFERENCE_NATIVE

private fun Intent.flagNewTask(): Intent = apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

private fun UrlArgs?.isUnsecureScheme(uriParser: (String) -> Uri = Uri::parse): Boolean =
    this?.run { uriParser(url).scheme == "http" } ?: false
