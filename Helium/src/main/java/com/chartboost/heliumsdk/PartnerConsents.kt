/*
 * Copyright 2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

/**
 * Get and set partner IDs to consent given for each partner.
 */
class PartnerConsents(private val coroutineScope: CoroutineScope = CoroutineScope(Main.immediate)) {
    internal interface PartnerConsentsObserver {
        /**
         * Called when per-partner consents are updated.
         */
        fun onPartnerConsentsUpdated()
    }

    private val partnerIdToConsentGivenMap: MutableMap<String, Boolean> = mutableMapOf()
    private val partnerConsentsObservers: MutableSet<PartnerConsentsObserver> = mutableSetOf()

    /**
     * Gets a copy of the partner ID to consent given map.
     */
    fun getPartnerIdToConsentGivenMapCopy(): Map<String, Boolean> =
        partnerIdToConsentGivenMap.toMap()

    /**
     * Adds a single partner's consent. This value is persisted between app launches.
     *
     * @param partnerId The partner ID.
     * @param consentGiven True if there is consent for this partner, false otherwise.
     */
    fun setPartnerConsent(partnerId: String, consentGiven: Boolean) {
        partnerIdToConsentGivenMap[partnerId] = consentGiven
        notifyPartnerConsentsUpdated()
    }

    /**
     * Adds a map of partner IDs to consent given. These values are persisted between app launches.
     * This is only additive. If you'd like to remove a consent for a particular partner, please
     * use removePartnerConsent().
     *
     * @param partnerIdToConsentGivenMap The map of partner IDs to consent given.
     */
    fun addPartnerConsents(partnerIdToConsentGivenMap: Map<String, Boolean>) {
        this.partnerIdToConsentGivenMap.putAll(partnerIdToConsentGivenMap)
        notifyPartnerConsentsUpdated()
    }

    /**
     * Remove a partner consent.
     *
     * @param partnerId The partner ID.
     * @return The previous consent state of this partner.
     */
    fun removePartnerConsent(partnerId: String): Boolean? {
        val valueToReturn = partnerIdToConsentGivenMap.remove(partnerId)
        if (valueToReturn != null) {
            notifyPartnerConsentsUpdated()
        }
        return valueToReturn
    }

    /**
     * Clears and adds a map of partner IDs to consent given. These values are persisted between app
     * launches.
     */
    fun replacePartnerConsents(partnerIdToConsentGivenMap: Map<String, Boolean>) {
        this.partnerIdToConsentGivenMap.clear()
        addPartnerConsents(partnerIdToConsentGivenMap)
    }

    /**
     * Clears all partner consents. This change will persist to disk.
     */
    fun clear() {
        val shouldNotify = partnerIdToConsentGivenMap.isNotEmpty()
        partnerIdToConsentGivenMap.clear()
        if (shouldNotify) {
            notifyPartnerConsentsUpdated()
        }
    }

    /**
     * Adds a [PartnerConsentsObserver]. This only happens on the main thread.
     *
     * @param observer The observer to add.
     */
    internal fun addPartnerConsentsObserver(observer: PartnerConsentsObserver) {
        coroutineScope.launch {
            partnerConsentsObservers.add(observer)
        }
    }

    /**
     * Merges partner consents from the publisher, if any, and from disk.
     * Prioritizes the new publisher consents.
     * This action does not notify anyone of the changes.
     */
    internal fun mergePartnerConsentsFromDisk(
        consentsFromDisk: Map<String, Boolean>
    ) {
        val consentsFromPublisher = partnerIdToConsentGivenMap.toMap()

        partnerIdToConsentGivenMap.putAll(consentsFromDisk)
        partnerIdToConsentGivenMap.putAll(consentsFromPublisher)
    }

    private fun notifyPartnerConsentsUpdated() {
        coroutineScope.launch {
            partnerConsentsObservers.forEach {
                it.onPartnerConsentsUpdated()
            }
        }
    }
}
