/*
 *  Copyright (c) 2016, Kinvey, Inc. All rights reserved.
 *
 * This software is licensed to you under the Kinvey terms of service located at
 * http://www.kinvey.com/terms-of-use. By downloading, accessing and/or using this
 * software, you hereby accept such terms of service  (and any agreement referenced
 * therein) and agree that you have read, understand and agree to be bound by such
 * terms of service and are of legal age to agree to such terms with Kinvey.
 *
 * This software contains valuable confidential and proprietary information of
 * KINVEY, INC and is subject to applicable licensing agreements.
 * Unauthorized reproduction, transmission or distribution of this file and its
 * contents is a violation of applicable laws.
 *
 */

package com.kinvey.java.core

import com.google.api.client.http.HttpHeaders
import com.google.api.client.util.Key
import com.kinvey.BuildConfig
import com.kinvey.java.AbstractClient

/**
 * @author m0rganic
 * @since 2.0
 */
open class KinveyHeaders : HttpHeaders() {

    @Key("X-Kinvey-api-Version")
    private val kinveyApiVersion = if (AbstractClient.kinveyApiVersion.isNotEmpty()) AbstractClient.kinveyApiVersion
                                   else BuildConfig.KINVEY_API_VERSION

    private val userAgentVal = "android-kinvey-http/$VERSION"

    init {
        userAgent = userAgentVal
    }

    companion object {
        const val VERSION = BuildConfig.VERSION
    }
}
