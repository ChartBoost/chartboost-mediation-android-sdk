package com.chartboost.sdk.internal.logging

import android.annotation.SuppressLint
import android.util.Log
import com.chartboost.sdk.LoggingLevel

/**
 * A logging utility for the Chartboost Monetization SDK.
 */
object Logger {
    /**
     * The default logging level for the logger.
     */
    @JvmField
    internal var level = LoggingLevel.INTEGRATION

    /**
     * The default magic number used to determine the offset on the call stack of the calling class
     * and method so the names can be used in log messages.
     */
    private const val STACK_TRACE_LEVEL = 8

    /**
     * Prefix for all Chartboost Monetization SDK log messages.
     */
    private const val TAG = "[ChartboostMonetization]"

    /**
     * The log levels for the logger.
     */
    private enum class LogLevel {
        DEBUG,
        ERROR,
        WARNING,
        INFO,
        VERBOSE,
        WTF,
    }

    /**
     * Log an integration error-level message.
     */
    @JvmStatic
    fun integrationError(msg: String) {
        if (shouldLogIntegrationMessage()) log(LogLevel.ERROR, msg)
    }

    /**
     * Log an integration warning-level message.
     */
    @JvmStatic
    fun integrationWarning(msg: String) {
        if (shouldLogIntegrationMessage()) log(LogLevel.WARNING, msg)
    }

    /**
     * Log a debug-level message.
     */
    @JvmStatic
    fun d(
        msg: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.DEBUG, msg, throwable)
    }

    /**
     * Log an error-level message.
     */
    @JvmStatic
    fun e(
        msg: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.ERROR, msg, throwable)
    }

    /**
     * Log a warning-level message.
     */
    @JvmStatic
    fun w(
        msg: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.WARNING, msg, throwable)
    }

    /**
     * Log an info-level message.
     */
    @JvmStatic
    fun i(
        msg: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.INFO, msg, throwable)
    }

    /**
     * Log a verbose-level message.
     */
    @JvmStatic
    fun v(
        msg: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.VERBOSE, msg, throwable)
    }

    /**
     * Log a "What a Terrible Failure" message.
     */
    @JvmStatic
    fun wtf(
        msg: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.WTF, msg, throwable)
    }

    /**
     * Check if the integration message should be logged.
     */
    private fun shouldLogIntegrationMessage() = level == LoggingLevel.ALL || level == LoggingLevel.INTEGRATION

    /**
     * Log a message with the given log level.
     */
    @SuppressLint("LongLogTag")
    private fun log(
        level: LogLevel,
        msg: String,
        throwable: Throwable? = null,
    ) {
        if (this.level == LoggingLevel.ALL || this.level == LoggingLevel.INTEGRATION) {
            val logMessage = "${getCallerInfo()} $msg"
            when (level) {
                LogLevel.DEBUG -> Log.d(TAG, logMessage, throwable)
                LogLevel.ERROR -> Log.e(TAG, logMessage, throwable)
                LogLevel.WARNING -> Log.w(TAG, logMessage, throwable)
                LogLevel.INFO -> Log.i(TAG, logMessage, throwable)
                LogLevel.VERBOSE -> Log.v(TAG, logMessage, throwable)
                LogLevel.WTF -> Log.wtf(TAG, logMessage, throwable)
            }
        }
    }

    /**
     * Get the caller class and method name from the call stack.
     *
     * @param stackTraceLevel The level of the call stack to get the caller info from.
     *
     * @return The caller class and method name.
     */
    private fun getCallerInfo(stackTraceLevel: Int = STACK_TRACE_LEVEL): String {
        val element = getStackTraceElement(stackTraceLevel)
        return element?.let {
            "${it.className.substringAfterLast('.')}.${it.methodName}():"
        } ?: ""
    }

    /**
     * Get the stack trace element at the given level.
     *
     * @param level The level of the stack trace to get.
     *
     * @return The stack trace element at the given level.
     */
    private fun getStackTraceElement(level: Int): StackTraceElement? {
        val stackTrace = Thread.currentThread().stackTrace
        return if (stackTrace.size > level) stackTrace[level] else null
    }
}
