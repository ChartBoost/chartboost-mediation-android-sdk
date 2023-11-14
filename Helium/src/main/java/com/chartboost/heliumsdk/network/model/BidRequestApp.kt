/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.utils.Environment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
class BidRequestApp private constructor(
    /**
     * The Chartboost App ID.
     */
    @SerialName("id")
    val id: String?,

    /**
     * The application's app bundle.
     */
    @SerialName("bundle")
    val bundle: String?,

    /**
     * The application's version name.
     */
    @SerialName("ver")
    val ver: String?,

    @SerialName("ext")
    val ext: BidRequestAppExt? = null
) {
    constructor() : this(
        id = HeliumSdk.getAppId(),
        bundle = Environment.appBundle,
        ver = Environment.appVersionName,
        // This should be null (and absent from the request body) if there is no game engine or game engine version
        ext = if (HeliumSdk.getGameEngineName() == null && HeliumSdk.getGameEngineVersion() == null) null else BidRequestAppExt()
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestAppExt private constructor(
    /**
     * The name of the game engine being used (if applicable)
     */
    @SerialName("game_engine_name")
    val gameEngineName: String? = null,

    /**
     * The version of the game engine being used (if applicable)
     */
    @SerialName("game_engine_version")
    val gameEngineVersion: String? = null
) {
    constructor() : this(
        gameEngineName = HeliumSdk.getGameEngineName(),
        gameEngineVersion = HeliumSdk.getGameEngineVersion()
    )
}
