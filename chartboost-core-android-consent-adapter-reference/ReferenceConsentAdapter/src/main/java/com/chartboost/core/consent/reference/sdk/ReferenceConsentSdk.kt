/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.core.consent.reference.sdk

import android.app.AlertDialog
import android.content.Context
import com.chartboost.core.consent.ConsentValues

/**
 * An example consent SDK that doesn't actually do anything. This class only provides basic APIs to
 * showcase what types of methods are available in real consent management platforms.
 */
object ReferenceConsentSdk {
    /**
     * A consent management platform typically has a notification system to suggest that the publisher
     * should show a consent dialog or not.
     */
    var shouldShowConsentDialog = true

    /**
     * Usually, consent management platforms use IAB defined consents like TCF or GPP. This
     * is to just demonstrate a custom key.
     */
    var referenceConsentStatus = ConsentValues.DENIED

    /**
     * This shows a concise dialog that usually only has the grant/deny buttons. The reference
     * SDK doesn't have any real UI to show this, but use your imagination on what it could look like!
     */
    fun showConciseDialog(context: Context) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Concise Dialog Shown!")
        alertDialogBuilder.setNeutralButton("OK") { dialog, _ ->
            dialog?.dismiss()
            shouldShowConsentDialog = false
        }
        alertDialogBuilder.show()
    }

    /**
     * This shows a detailed dialog that usually has every vendor or purpose and detailed information
     * about each. The reference SDK doesn't have any real UI to show this, but feel free to use your
     * imagination on what it could look like!
     */
    fun showDetailedDialog(context: Context) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Detailed Dialog Shown!")
        alertDialogBuilder.setNeutralButton("OK") { dialog, _ ->
            dialog?.dismiss()
            shouldShowConsentDialog = false
        }
        alertDialogBuilder.show()
    }

    /**
     * Not all consent management platforms allows changing consent programmatically. This one
     * just does, though. Some CMPs also notifies some callback. This one does not.
     */
    fun grantConsent() {
        referenceConsentStatus = ConsentValues.GRANTED
    }

    /**
     * Not all consent management platforms allows changing consent programmatically. This one
     * just does, though. Some CMPs also notifies some callback. This one does not.
     */
    fun denyConsent() {
        referenceConsentStatus = ConsentValues.DENIED
    }

    /**
     * Resets the state of the consent management platform. Not all CMPs will have something like this.
     */
    fun reset() {
        shouldShowConsentDialog = true
        referenceConsentStatus = ConsentValues.DENIED
    }

    /**
     * Initializes the consent management platform.
     */
    fun initialize() {
        // Since this is a sample SDK, there's nothing to do here.
    }
}
