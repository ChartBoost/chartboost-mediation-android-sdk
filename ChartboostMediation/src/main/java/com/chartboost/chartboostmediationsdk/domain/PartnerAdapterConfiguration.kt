/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

/**
 * Represents the configuration of a partner adapter.
 */
interface PartnerAdapterConfiguration {
    /**
     * The underlying partner's SDK version.
     */
    val partnerSdkVersion: String

    /**
     * The version of this adapter.
     *
     * Note that the version string will be in the format of `Chartboost Mediation.Partner.Partner.Partner[.Partner].Adapter`,
     * in which `Chartboost Mediation` is the major version of the Chartboost Mediation SDK, `Partner` is the major.minor.patch[.build]
     * version of the partner SDK, and `Adapter` is the version of the adapter. Partners may have 3 or 4 digits.
     */
    val adapterVersion: String

    /**
     * The Chartboost Mediation internal partner ID.
     */
    val partnerId: String

    /**
     * The pretty display name or brand name of the partner.
     */
    val partnerDisplayName: String
}
