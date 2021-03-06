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

package com.kinvey.java.cache

import com.google.api.client.json.GenericJson

/**
 * Created by Prots on 1/26/16.
 */
interface ICacheManager {
    /**
     * Get cache object for given collection with given collectionItemClass
     * @param collection Name of the collection
     * @param collectionItemClass Class of single item object
     * @param ttl time to live
     * @param <T> Collection item class that extends GSON object
     * @return Cache object instance that could be queried
     */
    fun <T : GenericJson> getCache(collection: String?, collectionItemClass: Class<T>?, ttl: Long?): ICache<T>?

    /**
     * Delete all collections
     */
    fun clear()

    /**
     * Clear all cached data from the collection
     */
    fun <T : GenericJson> clearCollection(collection: String?, collectionItemClass: Class<T>?, ttl: Long?)
}