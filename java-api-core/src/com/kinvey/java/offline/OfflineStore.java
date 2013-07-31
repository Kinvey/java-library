/** 
 * Copyright (c) 2013, Kinvey, Inc. All rights reserved.
 *
 * This software contains valuable confidential and proprietary information of
 * KINVEY, INC and is subject to applicable licensing agreements.
 * Unauthorized reproduction, transmission or distribution of this file and its
 * contents is a violation of applicable laws.
 * 
 */
package com.kinvey.java.offline;

import com.kinvey.java.AbstractClient;
import com.kinvey.java.AppData;
import com.kinvey.java.model.KinveyDeleteResponse;

/**
 * @author edwardf
 */
public interface OfflineStore<T>  {


    public T executeGet(AbstractClient client, AppData<T> appData,  AbstractKinveyOfflineClientRequest request);

    public KinveyDeleteResponse executeDelete(AbstractClient client, AppData<T> appData, AbstractKinveyOfflineClientRequest request);

    public T executeSave(AbstractClient client, AppData<T> appData, AbstractKinveyOfflineClientRequest request);



}