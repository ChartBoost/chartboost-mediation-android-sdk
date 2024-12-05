/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import android.util.Log
import com.chartboost.heliumsdk.utils.LogController.STACK_TRACE_LEVEL

/**
 * @suppress
 *
 * Logging system for the Helium SDK that handles both client-side logging and metrics data reporting.
 */
object LogController {
    /**
     * A data class containing the class and method name of the caller on the call stack.
     */
    data class StackTraceElements(val className: String, val methodName: String)

    /**
     * Collection of all supported log levels.
     */
    enum class LogLevel(val value: Int) {
        NONE(0),
        ERROR(1),
        WARNING(2),
        INFO(3),
        DEBUG(4),
        VERBOSE(5),
        ;

        companion object {
            /**
             * Get the log level corresponding to the integer value or VERBOSE if nothing matches.
             *
             * @param logLevelInt Integer representation of the log level.
             */
            fun fromInt(logLevelInt: Int?): LogLevel {
                return values().find { it.value == logLevelInt } ?: VERBOSE
            }
        }
    }

    /**
     * Specify whether debug mode is enabled. This flag does nothing.
     */
    @Deprecated("Use logLevel instead")
    var debugMode = false

    /**
     * Only see logs of this log level and more severe. This will suppress all logs with a higher
     * ordinal [LogLevel]. If set to [LogLevel.NONE], all logs are suppressed.
     */
    var logLevel: LogLevel = LogLevel.INFO
        get() = serverLogLevelOverride ?: field

    /**
     * The server log override of the log level.
     */
    internal var serverLogLevelOverride: LogLevel? = null

    /**
     * Prefix for all Helium log messages.
     */
    internal const val TAG = "[Helium]"

    /**
     * The default magic number used to determine the offset on the call stack of the calling class
     * and method so the names can be used in log messages.
     */
    internal const val STACK_TRACE_LEVEL = 5

    /**
     * Log an error-level message.
     *
     * @param message The message to log.
     */
    fun e(message: String?) {
        if (logLevel.value < LogLevel.ERROR.value) {
            return
        }
        message?.let { Log.e(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log a warning-level message.
     *
     * @param message The message to log.
     */
    fun w(message: String?) {
        if (logLevel.value < LogLevel.WARNING.value) {
            return
        }
        message?.let { Log.w(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log an info-level message.
     *
     * @param message The message to log.
     */
    fun i(message: String?) {
        if (logLevel.value < LogLevel.INFO.value) {
            return
        }
        message?.let { Log.i(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log a debug-level message.
     *
     * @param message The message to log.
     */
    fun d(message: String?) {
        if (logLevel.value < LogLevel.DEBUG.value) {
            return
        }
        message?.let { Log.d(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log a verbose-level message.
     *
     * @param message The message to log.
     */
    fun v(message: String?) {
        if (logLevel.value < LogLevel.VERBOSE.value) {
            return
        }
        message?.let { Log.v(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Get the class and method name of the caller on the call stack.
     *
     * @param stackTraceLevel A magic number used to determine the offset on the call stack of the
     * calling class and method so the names can be used in log messages. Defaults to [STACK_TRACE_LEVEL].
     *
     * @return A [StackTraceElements] object containing the class and method name of the caller.
     */
    internal fun getClassAndMethod(stackTraceLevel: Int = STACK_TRACE_LEVEL): StackTraceElements? {
        Thread.currentThread().stackTrace.run {
            return if (size > stackTraceLevel) {
                StackTraceElements(
                    this[stackTraceLevel].className.substringAfterLast('.'),
                    this[stackTraceLevel].methodName,
                )
            } else {
                null
            }
        }
    }

    /**
     * Build a log message using a pre-defined template given the class and method name, and the
     * actual message.
     *
     * The template: `ClassName.method(): message`.
     *
     * @param stackTraceElements The class and method name of the caller.
     * @param message The actual message to log.
     *
     * @return The log message based on the template.
     */
    internal fun buildLogMsg(
        stackTraceElements: StackTraceElements?,
        message: String,
    ): String {
        return "${
            if (stackTraceElements != null) {
                "${stackTraceElements.className}.${stackTraceElements.methodName}():"
            } else {
                ""
            }
        } $message"
    }
}
