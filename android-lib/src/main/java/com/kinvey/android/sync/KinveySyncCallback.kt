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
package com.kinvey.android.sync

import com.kinvey.java.model.KinveyPullResponse

/**
 * This class provides callbacks from requests executed by the Sync API.
 *
 * @author edwardf
 */
interface KinveySyncCallback {
    fun onSuccess(kinveyPushResponse: KinveyPushResponse?, kinveyPullResponse: KinveyPullResponse?)
    /**
     * Used to indicate start of pull request by background service
     */
    fun onPullStarted()

    /**
     * Used to indicate start of push request by background service
     */
    fun onPushStarted()

    /**
     * Used to indicate successful execution of pull request by background service
     */
    fun onPullSuccess(kinveyPullResponse: KinveyPullResponse?)

    /**
     * Used to indicate successful execution of push request by background service
     */
    fun onPushSuccess(kinveyPushResponse: KinveyPushResponse?)

    /**
     * Used to indicate the failed execution of a request by the background service.
     */

    fun onFailure(t: Throwable?)
}