/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import android.util.Log

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
        ERROR(0),
        WARNING(1),
        INFO(2),
        DEBUG(3),
        VERBOSE(4);
    }

    /**
     * Specify whether debug mode is enabled.
     */
    var debugMode = false

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
        message?.let { Log.e(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log a warning-level message.
     *
     * @param message The message to log.
     */
    fun w(message: String?) {
        message?.let { Log.w(TAG, buildLogMsg(getClassAndMethod(), it)) }
    }

    /**
     * Log an info-level message.
     *
     * @param message The message to log.
     */
    fun i(message: String?) {
        if (debugMode) {
            message?.let { Log.i(TAG, buildLogMsg(getClassAndMethod(), it)) }
        }
    }

    /**
     * Log a debug-level message.
     *
     * @param message The message to log.
     */
    fun d(message: String?) {
        if (debugMode) {
            message?.let { Log.d(TAG, buildLogMsg(getClassAndMethod(), it)) }
        }
    }

    /**
     * Log a verbose-level message.
     *
     * @param message The message to log.
     */
    fun v(message: String?) {
        if (debugMode) {
            message?.let { Log.v(TAG, buildLogMsg(getClassAndMethod(), it)) }
        }
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
                    this[stackTraceLevel].methodName
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
        message: String
    ): String {
        return "${
            if (stackTraceElements != null) "${stackTraceElements.className}.${stackTraceElements.methodName}():"
            else ""
        } $message"
    }
}
