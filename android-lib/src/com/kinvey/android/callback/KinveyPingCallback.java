/** 
 * Copyright (c) 2014, Kinvey, Inc. All rights reserved.
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
package com.kinvey.android.callback;

import com.kinvey.java.core.KinveyClientCallback;

import java.util.List;

/**
 * This class provides callbacks for an asynchronous Ping
 *
 *
 * @author edwardf
 * @since 2.0
 */
public interface KinveyPingCallback extends KinveyClientCallback<Boolean>{

    @Override
    public void onSuccess(Boolean result);

    @Override
    public void onFailure(Throwable error);
}
