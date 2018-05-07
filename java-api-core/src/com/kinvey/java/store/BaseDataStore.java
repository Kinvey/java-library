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

package com.kinvey.java.store;

import com.google.api.client.json.GenericJson;
import com.google.common.base.Preconditions;
import com.kinvey.java.AbstractClient;
import com.kinvey.java.Constants;
import com.kinvey.java.KinveyException;
import com.kinvey.java.Query;
import com.kinvey.java.cache.ICache;
import com.kinvey.java.cache.KinveyCachedClientCallback;
import com.kinvey.java.core.KinveyCachedAggregateCallback;
import com.kinvey.java.model.AggregateType;
import com.kinvey.java.model.Aggregation;
import com.kinvey.java.model.KinveyReadResponse;
import com.kinvey.java.model.KinveyPullResponse;
import com.kinvey.java.network.NetworkManager;
import com.kinvey.java.query.AbstractQuery;
import com.kinvey.java.store.requests.data.AggregationRequest;
import com.kinvey.java.store.requests.data.PushRequest;
import com.kinvey.java.store.requests.data.delete.DeleteIdsRequest;
import com.kinvey.java.store.requests.data.delete.DeleteQueryRequest;
import com.kinvey.java.store.requests.data.delete.DeleteSingleRequest;
import com.kinvey.java.store.requests.data.read.ReadAllRequest;
import com.kinvey.java.store.requests.data.read.ReadCountRequest;
import com.kinvey.java.store.requests.data.read.ReadIdsRequest;
import com.kinvey.java.store.requests.data.read.ReadQueryRequest;
import com.kinvey.java.store.requests.data.read.ReadSingleRequest;
import com.kinvey.java.store.requests.data.save.SaveListRequest;
import com.kinvey.java.store.requests.data.save.SaveRequest;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;


public class BaseDataStore<T extends GenericJson> {

    private static final int BATCH_SIZE = 5;

    protected static final String FIND = "find";
    protected static final String DELETE = "delete";
    protected static final String PURGE = "purge";
    protected static final String GROUP = "group";
    protected static final String COUNT = "count";

    private static final int DEFAULT_PAGE_SIZE = 10_000;  // default page size set to backend record retrieval limit

    protected final AbstractClient client;
    private final String collection;
    protected StoreType storeType;
    private Class<T> storeItemType;
    private ICache<T> cache;
    protected NetworkManager<T> networkManager;

    /**
     * It is a parameter to enable mechanism to optimize the amount of data retrieved from the backend.
     * When you use a Sync or Cache datastore, data requests to the backend
     * only fetch data that changed since the previous update.
     * Default value is false.
     */
    private boolean deltaSetCachingEnabled = false;

    KinveyDataStoreLiveServiceCallback<T> liveServiceCallback;

    /**
     * Constructor for creating BaseDataStore for given collection that will be mapped to itemType class
     * @param client Kinvey client instance to work with
     * @param collection collection name
     * @param itemType class that data should be mapped to
     * @param storeType type of storage that client want to use
     */
    protected BaseDataStore(AbstractClient client, String collection, Class<T> itemType, StoreType storeType){
        this(client, collection, itemType, storeType, new NetworkManager<T>(collection, itemType, client));
    }

    protected BaseDataStore(AbstractClient client, String collection, Class<T> itemType, StoreType storeType,
                            NetworkManager<T> networkManager){
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        this.storeType = storeType;
        this.client = client;
        this.collection = collection;
        this.storeItemType = itemType;
        if (storeType != StoreType.NETWORK) {
            cache = client.getCacheManager().getCache(collection, itemType, storeType.ttl);
        }
        this.networkManager = networkManager;
        this.deltaSetCachingEnabled = client.isUseDeltaCache();
    }

    public static <T extends GenericJson> BaseDataStore<T> collection(String collectionName, Class<T> myClass, StoreType storeType, AbstractClient client) {
        Preconditions.checkNotNull(collectionName, "collectionName cannot be null.");
        Preconditions.checkNotNull(storeType, "storeType cannot be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        return new BaseDataStore<>(client, collectionName, myClass, storeType);
    }

    /**
     * Look up for data with given id
     * @param id the id of object we need to find
     * @param cachedCallback callback to be executed in case of {@link StoreType#CACHE} is used to get cached data before network
     * @return null or object that matched given id
     */
    public T find (String id, KinveyCachedClientCallback<T> cachedCallback) throws IOException{
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(id, "id must not be null.");
        Preconditions.checkArgument(cachedCallback == null || storeType == StoreType.CACHE, "KinveyCachedClientCallback can only be used with StoreType.CACHE");
        T ret;
        if (storeType == StoreType.CACHE && cachedCallback != null) {
            ret = new ReadSingleRequest<>(cache, id, ReadPolicy.FORCE_LOCAL, networkManager).execute();
            cachedCallback.onSuccess(ret);
        }
        ret = new ReadSingleRequest<>(cache, id, this.storeType.readPolicy, networkManager).execute();
        return ret;
    }

    /**
     * Look up for data with given id
     * @param id the id of object we need to find
     * @return null or object that matched given id
     */
    public T find (String id) throws IOException {
        return find(id, null);
    }

    /**
     * Look up for object that have id in given collection of ids
     * @param ids collection of strings that identify a set of ids we have to look for
     * @param cachedCallback callback to be executed in case of {@link StoreType#CACHE} is used to get cached data before network
     * @return List of object found for given ids
     */
    public KinveyReadResponse<T> find(Iterable<String> ids, KinveyCachedClientCallback<KinveyReadResponse<T>> cachedCallback) throws IOException{
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(ids, "ids must not be null.");
        Preconditions.checkArgument(cachedCallback == null || storeType == StoreType.CACHE, "KinveyCachedClientCallback can only be used with StoreType.CACHE");
        KinveyReadResponse<T> ret;
        if (storeType == StoreType.CACHE && cachedCallback != null) {
            ret = new ReadIdsRequest<>(cache, networkManager, ReadPolicy.FORCE_LOCAL, ids).execute();
            cachedCallback.onSuccess(ret);
        }
        ret = new ReadIdsRequest<>(cache, networkManager, this.storeType.readPolicy, ids).execute();
        return ret;
    }

    /**
     * Look up for object that have id in given collection of ids
     * @param ids collection of strings that identify a set of ids we have to look for
     * @return List of object found for given ids
     */
    public KinveyReadResponse<T> find(Iterable<String> ids) throws IOException {
        return find(ids, null);
    }


    /**
     * Lookup objects in given collection by given query
     * @param query prepared query we have to look with
     * @param cachedCallback callback to be executed in case of {@link StoreType#CACHE} is used to get cached data before network
     * @return list of objects that are found
     */
    public KinveyReadResponse<T> find (Query query, KinveyCachedClientCallback<KinveyReadResponse<T>> cachedCallback) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(query, "query must not be null.");
        Preconditions.checkArgument(cachedCallback == null || storeType == StoreType.CACHE, "KinveyCachedClientCallback can only be used with StoreType.CACHE");
        // perform request based on policy
        KinveyReadResponse<T> ret;
        if (storeType == StoreType.CACHE && cachedCallback != null) {
            ret = new ReadQueryRequest<>(cache, networkManager, ReadPolicy.FORCE_LOCAL, query).execute();
            cachedCallback.onSuccess(ret);
        }
        ret = new ReadQueryRequest<>(cache, networkManager, this.storeType.readPolicy, query).execute();
        return ret;
    }

    /**
     * Lookup objects in given collection by given query
     * @param query prepared query we have to look with
     * @return list of objects that are found
     */
    public KinveyReadResponse<T> find (Query query) throws IOException {
        return find(query, null);
    }

    /**
     * get all objects for given collections
     * @param cachedCallback callback to be executed in case of {@link StoreType#CACHE} is used to get cached data before network
     * @return all objects in given collection
     */
    public KinveyReadResponse<T> find(KinveyCachedClientCallback<KinveyReadResponse<T>> cachedCallback) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkArgument(cachedCallback == null || storeType == StoreType.CACHE, "KinveyCachedClientCallback can only be used with StoreType.CACHE");
        // perform request based on policy
        KinveyReadResponse<T> ret;
        if (storeType == StoreType.CACHE && cachedCallback != null) {
            ret = new ReadAllRequest<>(cache, ReadPolicy.FORCE_LOCAL, networkManager).execute();
            cachedCallback.onSuccess(ret);
        }
        ret = new ReadAllRequest<>(cache, this.storeType.readPolicy, networkManager).execute();
        return ret;
    }

    /**
     * get all objects for given collections
     * @return all objects in given collection
     */
    public KinveyReadResponse<T> find() throws IOException {
        return find((KinveyCachedClientCallback<KinveyReadResponse<T>>)null);
    }

    /**
     * Get items count in collection
     * @return items count in collection
     */
    public Integer count() throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        return count(null);
    }

    /**
     * Get items count in collection
     * @param cachedCallback is using with StoreType.CACHE to get items count in collection
     * @return items count in collection
     */
    public Integer count(KinveyCachedClientCallback<Integer> cachedCallback) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkArgument(cachedCallback == null || storeType == StoreType.CACHE, "KinveyCachedClientCallback can only be used with StoreType.CACHE");
        if (storeType == StoreType.CACHE && cachedCallback != null) {
            Integer ret = new ReadCountRequest<T>(cache, networkManager, ReadPolicy.FORCE_LOCAL, null, client.getSyncManager()).execute();
            cachedCallback.onSuccess(ret);
        }
        return new ReadCountRequest<T>(cache, networkManager, this.storeType.readPolicy, null, client.getSyncManager()).execute();
    }

    /**
     * Get items count in collection on the server
     * @return items count in collection on the server
     */
    public Integer countNetwork() throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        return new ReadCountRequest<T>(cache, networkManager, ReadPolicy.FORCE_NETWORK, null, client.getSyncManager()).execute();
    }

    /**
     * Save multiple objects for collections
     * @param objects list of objects to be saved
     * @return updated list of object that will contain ids if they was not present in moment of saving
     * @throws IOException
     */
    public List<T> save (Iterable<T> objects) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(objects, "objects must not be null.");
        return new SaveListRequest<T>(cache, networkManager, this.storeType.writePolicy, objects, client.getSyncManager()).execute();
    }


    /**
     * Save single object into collection
     * @param object Object to be saved in given collection
     * @return updated object with filled some required fields
     * @throws IOException
     */
    public T save (T object) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(object, "object must not be null.");
        return new SaveRequest<T>(cache, networkManager, this.storeType.writePolicy, object, client.getSyncManager()).execute();
    }

    /**
     * Clear the local cache storage
     */
    public void clear() {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        client.getCacheManager().clearCollection(getCollectionName(), storeItemType, Long.MAX_VALUE);
        purge();
    }

    /**
     * Clear the local cache storage
     */
    public void clear(Query query) {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        purge(query);
        client.getCacheManager().getCache(getCollectionName(), storeItemType, Long.MAX_VALUE).delete(query);
    }

    /**
     * Remove object from from given collection with given id
     * @param id id of object to be deleted
     * @return count of object that was deleted
     * @throws IOException
     */
    public Integer delete (String id) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(id, "id must not be null.");
        return new DeleteSingleRequest<T>(cache, networkManager, this.storeType.writePolicy, id, client.getSyncManager()).execute();
    }

    /**
     * Remove objects from given query that matches given query
     * @param query query to lookup objects for given collection
     * @return count of objects that was removed
     * @throws IOException
     */
    public Integer delete (Query query) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(query, "query must not be null.");
        return new DeleteQueryRequest<T>(cache, networkManager, this.storeType.writePolicy, query, client.getSyncManager()).execute();
    }

    /**
     * Remove objects from given collections with list of ids
     * @param ids identifiers of objects to be deleted
     * @return count of objects that was deleted bu given call
     * @throws IOException
     */
    public Integer delete (Iterable<String> ids) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkNotNull(ids, "ids must not be null.");
        return new DeleteIdsRequest<T>(cache, networkManager, this.storeType.writePolicy, ids, client.getSyncManager()).execute();
    }

    /**
     * Push local changes to network
     * should be used with {@link StoreType#SYNC}
     */
    public void pushBlocking() throws IOException {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        new PushRequest<T>(collection, cache, networkManager, client).execute();
    }

    /**
     * Pull network data with given query into local storage
     * should be used with {@link StoreType#SYNC}
     * @param query query to pull the objects
     */
    public KinveyPullResponse pullBlocking(Query query) throws IOException {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkArgument(client.getSyncManager().getCount(getCollectionName()) == 0, "InvalidOperation. You must push all pending sync items before new data is pulled. Call push() on the data store instance to push pending items, or purge() to remove them.");
        KinveyPullResponse response = new KinveyPullResponse();
        query = query == null ? client.query() : query;
        KinveyReadResponse<T> readResponse = networkManager.pullBlocking(query, cache, isDeltaSetCachingEnabled()).execute();
        cache.delete(query);
        response.setCount(cache.save(readResponse.getResult()).size());
        response.setListOfExceptions(readResponse.getListOfExceptions());
        return response;
    }

    /**
     * Pull network data with given query into local storage
     * should be used with {@link StoreType#SYNC}
     * @param isAutoPagination true if auto-pagination is used
     * @param query query to pull the objects
     */
    public KinveyPullResponse pullBlocking(Query query, boolean isAutoPagination) throws IOException {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkArgument(client.getSyncManager().getCount(getCollectionName()) == 0, "InvalidOperation. You must push all pending sync items before new data is pulled. Call push() on the data store instance to push pending items, or purge() to remove them.");
        if (isAutoPagination) {
            return pullBlocking(query, DEFAULT_PAGE_SIZE);
        } else {
            return pullBlocking(query);
        }
    }

    /**
     * Pull network data with given query into local storage page by page
     * getting pages works concurrently
     * should be used with {@link StoreType#SYNC}
     * @param query query to pull the objects
     * @param pageSize page size for auto-pagination
     */
    public KinveyPullResponse pullBlocking(Query query, int pageSize) throws IOException {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkArgument(client.getSyncManager().getCount(getCollectionName()) == 0, "InvalidOperation. You must push all pending sync items before new data is pulled. Call push() on the data store instance to push pending items, or purge() to remove them.");

        KinveyPullResponse response = new KinveyPullResponse();
        query = query == null ? client.query() : query;

        if (query.getSortString() == null || query.getSortString().isEmpty()) {
            query.addSort(Constants._ID, AbstractQuery.SortOrder.ASC);
        }
        List<Exception> exceptions = new ArrayList<>();
        int skipCount = 0;

        // First, get the count of all the items to pull
        int totalItemNumber = countNetwork();
        int pulledItemCount = 0;
        int totalPagesNumber = Math.abs(totalItemNumber/pageSize) + 1;
        int batchSize = BATCH_SIZE; // batch size for concurrent push requests
        ExecutorService executor;
        List<FutureTask<PullTaskResponse>> tasks;
        NetworkManager.Get pullRequest;
        FutureTask<PullTaskResponse> ft;
        for (int i = 0; i < totalPagesNumber; i += batchSize) {
            executor = Executors.newFixedThreadPool(batchSize);
            tasks = new ArrayList<>();
            do {
                query.setSkip(skipCount).setLimit(pageSize);
                pullRequest = networkManager.pullBlocking(query, cache, deltaSetCachingEnabled);
                skipCount += pageSize;
                try {
                    ft = new FutureTask<PullTaskResponse>(new CallableAsyncPullRequestHelper(pullRequest, query));
                    tasks.add(ft);
                    executor.execute(ft);
                } catch (AccessControlException | KinveyException e) {
                    e.printStackTrace();
                    exceptions.add(e);
                } catch (Exception e) {
                    throw e;
                }
            } while (skipCount < totalItemNumber);

            for (FutureTask<PullTaskResponse> task : tasks) {
                try {
                    PullTaskResponse tempResponse = task.get();
                    cache.delete(tempResponse.getQuery());
                    pulledItemCount += cache.save(tempResponse.getKinveyReadResponse().getResult()).size();
                    exceptions.addAll(tempResponse.getKinveyReadResponse().getListOfExceptions());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            executor.shutdown();
        }
        response.setListOfExceptions(exceptions);
        response.setCount(pulledItemCount);
        return response;
    }


    /**
     * Run sync operation to sync local and network storages
     * @param query query to pull the objects
     */
    public void syncBlocking(Query query) throws IOException {
        pushBlocking();
        pullBlocking(query);
    }

    /**
     * Run sync operation to sync local and network storages
     * @param query query to pull the objects
     * @param isAutoPagination true if auto-pagination is used
     */
    public void syncBlocking(Query query, boolean isAutoPagination) throws IOException {
        pushBlocking();
        pullBlocking(query, isAutoPagination);
    }

    /**
     * Run sync operation to sync local and network storages
     * @param query query to pull the objects
     * @param pageSize page size for auto-pagination
     */
    public void syncBlocking(Query query, int pageSize) throws IOException {
        pushBlocking();
        pullBlocking(query, pageSize);
    }

    public void purge() {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        client.getSyncManager().clear(collection);
    }

    public void purge(Query query) {
        Preconditions.checkArgument(storeType != StoreType.NETWORK, "InvalidDataStoreType");
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Object t;
        for (T item : cache.get(query)) {
            t = item.get("_id");
            if (t != null) {
                client.getSyncManager().deleteCachedItems(new Query().equals("meta.id", item.get("_id")));
            }
        }
    }

    /**
     * Collect all entities with the same value for fields,
     * and then apply a reduce function (such as count or average) on all those items.
     * @param aggregateType {@link AggregateType} (such as min, max, sum, count, average)
     * @param fields fields for group by
     * @param reduceField field for apply reduce function
     * @param query query to filter results
     * @return the array of groups containing the result of the reduce function
     */
    public Aggregation group(AggregateType aggregateType, ArrayList<String> fields, String reduceField, Query query,
                           KinveyCachedAggregateCallback cachedCallback) throws IOException {
        Preconditions.checkNotNull(client, "client must not be null.");
        Preconditions.checkArgument(client.isInitialize(), "client must be initialized.");
        Preconditions.checkArgument(cachedCallback == null || storeType == StoreType.CACHE, "KinveyCachedClientCallback can only be used with StoreType.CACHE");
        return aggregation(aggregateType, fields, reduceField, query, cachedCallback);
    }

    /**
     * Used for aggregate fields
     */
    private Aggregation aggregation(AggregateType type, ArrayList<String> fields,
                                    String field, Query query, KinveyCachedAggregateCallback cachedCallback) throws IOException {
        Aggregation ret = null;
        if (storeType == StoreType.CACHE && cachedCallback != null) {
            try {
                ret = new Aggregation(Arrays.asList(new AggregationRequest(type, cache, ReadPolicy.FORCE_LOCAL, networkManager, fields, field, query).execute()));
            } catch (IOException e) {
                cachedCallback.onFailure(e);
            }
            cachedCallback.onSuccess(ret);
        }
        ret = new Aggregation(Arrays.asList(new AggregationRequest(type, cache, this.storeType.readPolicy, networkManager, fields, field, query).execute()));
        return ret;
    }

    /**
     * Set store type for current BaseDataStore
     * @param storeType
     */
    public void setStoreType(StoreType storeType) {
        Preconditions.checkNotNull(storeType, "storeType must not be null.");
        this.storeType = storeType;
    }

    /**
     * Getter for client
     * @return Client instance for given BaseDataStore
     */
    public AbstractClient getClient() {
        return client;
    }

    public Class<T> getCurrentClass() {
        return storeItemType;
    }

    public String getCollectionName() {
        return collection;
    }

    /**
     * Getter to check if delta set cache is enabled
     * @return delta set get flag
     */
    public boolean isDeltaSetCachingEnabled() {
        return deltaSetCachingEnabled;
    }

    /**
     * Setter for delta set get cache flag
     * @param deltaSetCachingEnabled boolean representing if we should use delta set caching
     */
    public void setDeltaSetCachingEnabled(boolean deltaSetCachingEnabled) {
        this.deltaSetCachingEnabled = deltaSetCachingEnabled;
    }

    public boolean subscribe(KinveyDataStoreLiveServiceCallback<T> storeLiveServiceCallback) throws IOException {
        boolean success = false;
        if (storeLiveServiceCallback != null) {
            liveServiceCallback = storeLiveServiceCallback;
            networkManager.subscribe(client.getDeviceId()).execute();
            KinveyLiveServiceCallback<String> callback = new KinveyLiveServiceCallback<String>() {
                @Override
                public void onNext(String next) {
                    try {
                        liveServiceCallback.onNext(client.getJsonFactory().createJsonParser(next).parse(getCurrentClass()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        liveServiceCallback.onError(e);
                    }
                }

                @Override
                public void onError(Exception e) {
                    liveServiceCallback.onError(e);
                }

                @Override
                public void onStatus(KinveyLiveServiceStatus status) {
                    liveServiceCallback.onStatus(status);
                }
            };
            success = LiveServiceRouter.getInstance().subscribeCollection(collection, callback);
        }
        return success;
    }

    public void unsubscribe() throws IOException {
        liveServiceCallback = null;
        LiveServiceRouter.getInstance().unsubscribeCollection(collection);
    }
}
