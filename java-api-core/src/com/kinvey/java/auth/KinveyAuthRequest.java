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
package com.kinvey.java.auth;

import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UriTemplate;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.Key;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.Map;

import com.kinvey.java.core.KinveyHeaders;
import com.kinvey.java.core.KinveyJsonResponseException;

/**
 * @author m0rganic
 * @since 2.0
 */
public class KinveyAuthRequest extends GenericJson {

    //TODO:

    public enum LoginType {
        IMPLICIT,
        KINVEY,
        THIRDPARTY
    }
    /**
     * used to construct the request body
     */
    private static class AuthRequestPayload extends GenericJson {

        @Key
        private final String username;

        @Key
        private final String password;

        public AuthRequestPayload(String username, String passwd) {
            this.username = username;
            this.password = passwd;
        }
    }

    private boolean create;
    private LoginType type;
    /**
     * base url used for making authentication requests *
     */
    //TODO (mbickle): remove this redundancy and add to the be in to Builder
    private static final String BASE_URL = "https://baas.kinvey.com";

    /**
     * http transport to utilize for the request *
     */
    private final HttpTransport transport;

    /**
     * json factory from with the object parser is built *
     */
    private final JsonFactory jsonFactory;

    /**
     * appkey and secret RequestInitializer for the initial POST to the user endpoint *
     */
    private final BasicAuthentication appKeyAuthentication;

    /**
     * used to formulate the uri in {@link #buildHttpRequestUrl()} *
     */
    @Key
    private final String appKey;

    /**
     * payload containing the json object Kinvey expects *
     */
    private final GenericJson requestPayload;

    /**
     * the response returned from the Kinvey server on {@link #executeUnparsed()} *
     */
    private HttpResponse response;

    /**
     * standard headers included in all requests
     */
    private final static KinveyHeaders kinveyHeaders = new KinveyHeaders();

    /**
     * Keep protected for testing support.
     *
     * @param transport            http transport layer
     * @param jsonFactory          json object parser factory
     * @param appKeyAuthentication app key and secret used to initialize the auth request
     * @param username             user provided username or {@code null} if none is known
     * @param password             password for the user or {@code null} if none is known
     */
    protected KinveyAuthRequest(HttpTransport transport, JsonFactory jsonFactory,
                                BasicAuthentication appKeyAuthentication, String username, String password,
                                GenericJson user, boolean create) {
        this.transport = transport;
        this.jsonFactory = jsonFactory;
        this.appKeyAuthentication = appKeyAuthentication;
        this.appKey = appKeyAuthentication.getUsername();
        this.requestPayload = (username == null || password == null) ? null : new AuthRequestPayload(username, password);
        if (user != null) {
            this.putAll(user.getUnknownKeys());
        }
        this.create = create;
        this.type = requestPayload == null ? LoginType.IMPLICIT : LoginType.KINVEY;
    }

    protected KinveyAuthRequest(HttpTransport transport, JsonFactory jsonFactory,
                                BasicAuthentication appKeyAuthentication, ThirdPartyIdentity thirdPartyIdentity,
                                GenericJson user, boolean create) {
            this.transport = transport;
        this.jsonFactory = jsonFactory;
        this.appKeyAuthentication = appKeyAuthentication;
        this.appKey = appKeyAuthentication.getUsername();
        this.requestPayload = thirdPartyIdentity;
        if (user != null) {
            this.putAll(user.getUnknownKeys());
        }
        this.create = create;
        this.type=LoginType.THIRDPARTY;
    }

    /**
     * @return properly formed url for submitting requests to Kinvey authentication module
     */
    private GenericUrl buildHttpRequestUrl() {
        // we hit different end points depending on whether 3rd party auth is utilize
        return new GenericUrl(UriTemplate.expand(BASE_URL
                , "/user/{appKey}/" + (this.create ? "" : "login")
                , this
                , true));
    }


    /**
     * @return low level http response
     */
    public HttpResponse executeUnparsed() throws IOException {

        HttpContent content = (this.requestPayload != null) ? new JsonHttpContent(jsonFactory, this.requestPayload) : null;
        HttpRequest request = transport.createRequestFactory(appKeyAuthentication)
                .buildPostRequest(buildHttpRequestUrl(), content)
                .setEnableGZipContent(false)
                .setThrowExceptionOnExecuteError(false)
                .setParser(jsonFactory.createJsonObjectParser())
                .setHeaders(kinveyHeaders)
                .setEnableGZipContent(false);
        response = request.execute();
        if (response.isSuccessStatusCode()) {
            return response;
        } else if (response.getStatusCode() == 404 && this.type == LoginType.THIRDPARTY && this.create == false) {
            this.create = true;
            return executeUnparsed();
        }
        throw KinveyJsonResponseException.from(jsonFactory, response);
    }

    public KinveyAuthResponse execute() throws IOException {
        return executeUnparsed().parseAs(KinveyAuthResponse.class);
    }

    /**
     * Used to construct a {@link KinveyAuthRequest}. The result will be an auth request that adjusts for the
     * authentication scenario.
     * <p/>
     * <p>
     * There are three scenarios that this builder will support for Kinvey authentication.
     * </p>
     * <p/>
     * <p>
     * The first is where the user is known
     * and the both the username and password have been provided.
     * </p>
     * <p/>
     * <pre>
     *      KinveyAuthResponse response = new KinveyAuthRequest.Builder(transport,jsonfactory,appKey,appSecret)
     *          .setUserIdAndPassword(userid, password)
     *          .build()
     *          .execute();
     * </pre>
     * <p/>
     * <p>
     * The second is where the user has established their identity with one of the supported 3rd party authentication
     * systems like Facebook, LinkedInCredential, etc.
     * </p>
     * <pre>
     *      KinveyAuthResponse response = new KinveyAuthRequest.Builder(transport,jsonfactory,appKey,appSecret)
     *              .setThirdPartyAuthToken(thirdpartytoken)
     *              .build()
     *              .execute();
     * </pre>
     * <p/>
     * <p>
     * The third and final way authentication can occur is with just the appKey and appSecret, in this case, an
     * implicit user is created when the {@link com.kinvey.java.auth.KinveyAuthRequest#execute()} is called.
     * </p>
     * <p/>
     * <pre>
     *      KinveyAuthResponse response = new KinveyAuthRequest.Builder(transport,jsonfactory,appKey,appSecret)
     *              .build()
     *              .execute();
     * </pre>
     */
    public static class Builder {

        private final HttpTransport transport;

        private final JsonFactory jsonFactory;

        private final BasicAuthentication appKeyAuthentication;

        private Boolean create = false;

        private String username;

        private GenericJson user;

        private String password;
        private boolean isThirdPartyAuthUsed = false;
        private ThirdPartyIdentity thirdPartyIdentity;

        public Builder(HttpTransport transport, JsonFactory jsonFactory, String appKey, String appSecret,
                       GenericJson user) {
            this.transport = Preconditions.checkNotNull(transport);
            this.jsonFactory = Preconditions.checkNotNull(jsonFactory);
            this.appKeyAuthentication = new BasicAuthentication(Preconditions.checkNotNull(appKey), Preconditions.checkNotNull(appSecret));

        }

        public Builder(HttpTransport transport, JsonFactory jsonFactory, String appKey, String appSecret,
                       String username, String password, GenericJson user) {
            this(transport, jsonFactory, appKey, appSecret, user);
            Preconditions.checkArgument(!"".equals(username), "username cannot be empty, use null no username is known");
            Preconditions.checkArgument(!"".equals(password), "password cannot be empty, use null no password is known");
            this.username = username;
            this.password = password;
            this.user = user;
        }

        public Builder(HttpTransport transport, JsonFactory jsonFactory, String appKey, String appSecret,
                       ThirdPartyIdentity identity, GenericJson user) {
            this(transport, jsonFactory, appKey, appSecret, user);
            Preconditions.checkNotNull(identity, "identity must not be null");
            this.isThirdPartyAuthUsed = (identity != null);
            this.thirdPartyIdentity = identity;
            this.user = user;
        }

        public KinveyAuthRequest build() {
            if (!isThirdPartyAuthUsed) {
                return new KinveyAuthRequest(getTransport()
                        , getJsonFactory()
                        , getAppKeyAuthentication()
                        , getUsername()
                        , getPassword(),user, this.create);
            }
            return new KinveyAuthRequest(getTransport()
                    , getJsonFactory()
                    , getAppKeyAuthentication()
                    , getThirdPartyIdentity(),user, this.create);
        }

        public Builder setUsernameAndPassword(String username, String password) {
            this.username = username;
            this.password = password;
            isThirdPartyAuthUsed = false;
            return this;
        }

        public Builder setThirdPartyIdentity(ThirdPartyIdentity identity) {
            thirdPartyIdentity = identity;
            isThirdPartyAuthUsed = true;
            return this;
        }

        public Builder setCreate(boolean create) {
            this.create = create;
            return this;
        }

        public Builder setUser(GenericJson user) {
            this.user = user;
            return this;
        }

        protected GenericJson getUser() {
            return user;
        }

        public ThirdPartyIdentity getThirdPartyIdentity() {
            return thirdPartyIdentity;
        }

        /**
         * @return the http trasport
         */
        public HttpTransport getTransport() {
            return transport;
        }

        /**
         * @return json factory
         */
        public JsonFactory getJsonFactory() {
            return jsonFactory;
        }

        /**
         * @return appkey and appsecret RequestInitializer used to createBlocking the BasicAuthentication for the POST request
         */
        public BasicAuthentication getAppKeyAuthentication() {
            return appKeyAuthentication;
        }

        /**
         * @return username or {@code null} in none is set
         */
        public final String getUsername() {
            return username;
        }

        /**
         * @return password or {@code null} if none is set
         */
        public final String getPassword() {
            return password;
        }

        /**
         * @param username uniquely identifies the user
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * @param password user provided password for the authentication request
         */
        public void setPassword(String password) {
            this.password = password;
        }

        protected boolean getThirdPartyAuthStatus() {
            return isThirdPartyAuthUsed;
        }

        protected boolean getCreate() {
            return create;
        }

    }
}
