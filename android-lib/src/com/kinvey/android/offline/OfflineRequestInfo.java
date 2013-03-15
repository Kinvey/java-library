/*
 * Copyright (c) 2013 Kinvey Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kinvey.android.offline;

import java.io.Serializable;

/**
 * This public static class maintains information about the client request.
 * <p/>
 * This stores the relationship between an Http Verb and and an associated entity's ID.
 * <p/>
 * myRequest.getHttpVerb() represents the HTTP verb as a String ("GET", "PUT", "DELETE", "POST");
 * myRequest.getEntityID() represents the id of the entity, which might be stored in the local store.
 */
public class OfflineRequestInfo implements Serializable {

    private static final long serialVersionUID = -444939394072970523L;

    //The Http verb of the client request ("GET", "PUT", "DELETE", "POST");
    private String verb;

    //The id of the entity, assuming it is in the store.
    private String id;

    public OfflineRequestInfo(String httpVerb, String entityID) {
        this.verb = httpVerb;
        this.id = entityID;
    }

    public String getHttpVerb() {
        return this.verb;
    }

    public String getEntityID() {
        return this.id;
    }

}