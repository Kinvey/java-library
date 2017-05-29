package com.kinvey.androidTest.store;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyMICCallback;
import com.kinvey.android.callback.KinveyUserCallback;
import com.kinvey.android.callback.KinveyUserDeleteCallback;
import com.kinvey.android.callback.KinveyUserManagementCallback;
import com.kinvey.android.store.DataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.androidTest.model.Person;
import com.kinvey.androidTest.model.TestUser;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.dto.User;
import com.kinvey.java.store.StoreType;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UserStoreTest {

    private Client client = null;
    private final String testUser = "test40";
    private final String testPassword = testUser;
    private final String username = "test";
    private final String password = "test";
    private final String appKey = "kid_H1lH5Dsw";
    private final String appSecret = "cc5c72d261d8473590a0fa01024fb313";

    private static class DefaultKinveyClientCallback implements KinveyClientCallback<User> {

        private CountDownLatch latch;
        User result;
        Throwable error;

        DefaultKinveyClientCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(User user) {
            this.result = user;
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }
    }

    private static class CustomKinveyClientCallback implements KinveyClientCallback<TestUser> {

        private CountDownLatch latch;
        TestUser result;
        Throwable error;

        CustomKinveyClientCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(TestUser user) {
            this.result = user;
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }
    }

    private static class DefaultKinveyUserDeleteCallback implements KinveyUserDeleteCallback {
        private CountDownLatch latch;
        Throwable error;

        DefaultKinveyUserDeleteCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(Void result) {
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }
    }

    private static class DefaultKinveyVoidCallback implements KinveyClientCallback<Void> {

        private CountDownLatch latch;
        Void result;
        Throwable error;

        DefaultKinveyVoidCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(Void result) {
            this.result = result;
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }
    }

    private static class DefaultKinveyMICCallback implements KinveyMICCallback<User> {

        private CountDownLatch latch;
        User result;
        Throwable error;
        String myURLToRender;

        DefaultKinveyMICCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(User result) {
            this.result = result;
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }

        @Override
        public void onReadyToRender(String myURLToRender) {
            this.myURLToRender = myURLToRender;
            finish();
        }
    }

    private static class DefaultKinveyUserCallback implements KinveyUserCallback<User> {

        private CountDownLatch latch;
        User result;
        Throwable error;

        DefaultKinveyUserCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(User result) {
            this.result = result;
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }
    }

    private static class DefaultPersonKinveyClientCallback implements KinveyClientCallback<Person> {

        private CountDownLatch latch;
        Person result;
        Throwable error;

        DefaultPersonKinveyClientCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(Person result) {
            this.result = result;
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }
    }

    private static class DefaultKinveyUserManagementCallback implements KinveyUserManagementCallback {

        private CountDownLatch latch;
        boolean result;
        Throwable error;

        DefaultKinveyUserManagementCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onSuccess(Void v) {
            result = true;
            finish();
        }

        @Override
        public void onFailure(Throwable error) {
            result = false;
            this.error = error;
            finish();
        }

        void finish() {
            latch.countDown();
        }
    }

    @Before
    public void setup() throws InterruptedException {
        Context mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        client = new Client.Builder(mMockContext).setUserClass(TestUser.class).build();
        final CountDownLatch latch = new CountDownLatch(1);
        if (client.isUserLoggedIn()) {
            new Thread(new Runnable() {
                public void run() {
                    Looper.prepare();
                    UserStore.logout(client, new KinveyClientCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            latch.countDown();
                        }
                    });
                    Looper.loop();
                }
            }).start();
        } else {
            latch.countDown();
        }
        latch.await();
    }

    @Test
    public void testLogin() throws InterruptedException {
        DefaultKinveyClientCallback callback = login("test", "test");
        assertNull(callback.error);
        assertNotNull(callback.result);
        assertNull(logout(client).error);
    }

    @Test
    public void testCustomSignUp() throws InterruptedException {
        TestUser user = new TestUser();
        user.setCompanyName("Test Company");
        CustomKinveyClientCallback callback = signUp(user);
        assertNull(callback.error);
        assertNotNull(callback.result);
        destroyUser();
        assertEquals(user.getCompanyName(), callback.result.getCompanyName());
    }

    @Test
    public void testDestroy() throws InterruptedException {
        DefaultKinveyClientCallback callback = signUp();
        assertNull(callback.error);
        assertNotNull(callback.result);
        DefaultKinveyUserDeleteCallback deleteCallback = destroyUser();
        assertNull(deleteCallback.error);
    }

    @Test
    public void testCustomDestroy() throws InterruptedException {
        TestUser user = new TestUser();
        user.setCompanyName("Test Company");
        CustomKinveyClientCallback callback = signUp(user);
        assertNull(callback.error);
        assertNotNull(callback.result);
        DefaultKinveyUserDeleteCallback deleteCallback = destroyUser();
        assertNull(deleteCallback.error);
    }

    private DefaultKinveyClientCallback login(final String userName, final String password) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final UserStoreTest.DefaultKinveyClientCallback callback = new UserStoreTest.DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.login(userName, password, client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    private DefaultKinveyClientCallback signUp() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final UserStoreTest.DefaultKinveyClientCallback callback = new UserStoreTest.DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.signUp(testUser, testPassword, client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    private CustomKinveyClientCallback signUp(final TestUser user) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final UserStoreTest.CustomKinveyClientCallback callback = new UserStoreTest.CustomKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.signUp(testUser, testPassword, user, client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    private DefaultKinveyUserDeleteCallback destroyUser() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyUserDeleteCallback callback = new DefaultKinveyUserDeleteCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.destroy(true, client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testLoginAsync() throws InterruptedException {
        DefaultKinveyClientCallback userCallback = login(client);
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
        assertNull(logout(client).error);
    }

    private DefaultKinveyClientCallback login(final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.login(client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    private DefaultKinveyVoidCallback logout(final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyVoidCallback callback = new DefaultKinveyVoidCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.logout(client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testSharedClientLoginAsync() throws InterruptedException {
        DefaultKinveyClientCallback userCallback = login(Client.sharedInstance());
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
        assertNull(logout(client).error);
    }

    @Test
    public void testLoginAsyncBad() throws InterruptedException {
        Context mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        Client fakeClient = new Client.Builder("app_key_fake", "app_secret_fake", mMockContext).build();
        DefaultKinveyClientCallback userCallback = login(fakeClient);
        assertNotNull(userCallback.error);
        userCallback = login(username, password, fakeClient);
        assertNotNull(userCallback.error);
    }

    @Test
    public void testLoginUserPassAsync() throws InterruptedException {
        DefaultKinveyClientCallback userCallback = login(username, password, client);
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
        assertNull(logout(client).error);
    }

    @Test
    public void testLoginUserPassAsyncBad() throws InterruptedException {
        DefaultKinveyClientCallback userCallback = login(username, "wrongPassword", client);
        assertNotNull(userCallback.error);
        assertFalse(client.isUserLoggedIn());
    }

    @Test
    @Ignore // need facebookAccessToken
    public void testLoginFacebookAsync() throws InterruptedException {
        String facebookAccessToken = "";
        DefaultKinveyClientCallback userCallback = loginFacebook(facebookAccessToken, client);
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
    }

    @Test
    public void testLoginFacebookAsyncBad() throws InterruptedException {
        String facebookAccessToken = "wrong_access_token";
        DefaultKinveyClientCallback userCallback = loginFacebook(facebookAccessToken, client);
        assertNotNull(userCallback.error);
        assertFalse(client.isUserLoggedIn());
    }

    private DefaultKinveyClientCallback loginFacebook(final String accessToken, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.loginFacebook(accessToken, client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    @Ignore // need googleAccessToken
    public void testLoginGoogleAsync() throws InterruptedException {
        String googleAccessToken = "";
        DefaultKinveyClientCallback userCallback = loginGoogle(googleAccessToken, client);
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
    }

    @Test
    public void testLoginGoogleAsyncBad() throws InterruptedException {
        String googleAccessToken = "wrong_access_token";
        DefaultKinveyClientCallback userCallback = loginGoogle(googleAccessToken, client);
        assertNotNull(userCallback.error);
        assertFalse(client.isUserLoggedIn());
    }

    private DefaultKinveyClientCallback loginGoogle(final String accessToken, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.loginGoogle(accessToken, client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    @Ignore // need to add accessToken,  accessSecret, consumerKey, consumerSecret
    public void testLoginTwitterAsync() throws InterruptedException {
        String accessToken = "";
        String accessSecret = "";
        String consumerKey = "";
        String consumerSecret = "";
        DefaultKinveyClientCallback userCallback = loginTwitter(accessToken, accessSecret, consumerKey, consumerSecret, client);
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
    }

    @Test
    public void testLoginTwitterAsyncBad() throws InterruptedException {
        String accessToken = "wrongAccessToken";
        String accessSecret = "wrongAccessSecret";
        String consumerKey = "wrongConsumerKey";
        String consumerSecret = "wrongConsumerSecret";
        DefaultKinveyClientCallback userCallback = loginTwitter(accessToken, accessSecret, consumerKey, consumerSecret, client);
        assertNotNull(userCallback.error);
        assertFalse(client.isUserLoggedIn());
    }

    private DefaultKinveyClientCallback loginTwitter(final String accessToken, final String accessSecret, final String consumerKey, final String consumerSecret, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.loginTwitter(accessToken, accessSecret, consumerKey, consumerSecret, client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    // TODO: 09.12.2016 should be checked
    @Test
    @Ignore // need to add accessToken,  accessSecret, consumerKey, consumerSecret
    public void testLoginLinkedInAsync() throws InterruptedException {
        String accessToken = "AQXu60okmBXrQkBm5BOpBCBBpCYc3y9uKWHtF559A1j4ttwjf5bXNeq0nVOHtgPomuw9Wn661BYbZal-3IReW0zc-Ed8NvP0FNdOTQVt9c8qz9EL5sezCYKd_I2VPEEMSC-YOyvhi-7WsttjaPnU_9H_kCnfVJuU7Fyt8Ph1XTw66xZeu2U";
        String accessSecret = "ExAZxYxvo42UfOCN";
        String consumerKey = "86z99b0orhyt7s";
        String consumerSecret = "ExAZxYxvo42UfOCN";
        DefaultKinveyClientCallback userCallback = loginLinkedIn(accessToken, consumerSecret, consumerKey, consumerSecret, client);
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
    }

    @Test
    public void testLoginLinkedInAsyncBad() throws InterruptedException {
        String accessToken = "wrongAccessToken";
        String accessSecret = "wrongAccessSecret";
        String consumerKey = "wrongConsumerKey";
        String consumerSecret = "wrongConsumerSecret";
        DefaultKinveyClientCallback userCallback = loginLinkedIn(accessToken, accessSecret, consumerKey, consumerSecret, client);
        assertNotNull(userCallback.error);
        assertFalse(client.isUserLoggedIn());
    }

    private DefaultKinveyClientCallback loginLinkedIn(final String accessToken, final String accessSecret, final String consumerKey, final String consumerSecret, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.loginLinkedIn(accessToken, accessSecret, consumerKey, consumerSecret, client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    @Ignore // need to change accessToken, refreshToken, clientID, ID
    public void testLoginSalesforceAsync() throws InterruptedException {
        String accessToken = "00D6F000000Dct5!AQkAQFAWrMjHboaD6Yn71NezV9yizZiM_MJLodm.ppn7TgypzET20QagfusU7UCAJw7jbnjWxjsWpYI2Xoa82ehmJum65Phd";
        String refreshToken = "5Aep861..zRMyCurAW3YNVSrR4jYtnt9rDCBsqQ.ytSywG1HaexWXOn07YXPwep1YmQVmuuc9YM8sWS8pyFbC2G";
        String clientID = "3MVG9YDQS5WtC11o5afZtRCMB4EGBMjwb0MfQOBSW2u2EZ5r6fHt_sXtYx9i2.nJIkhzicIPWpyhm1zc3HlWw";
        String ID = "https://login.salesforce.com/id/00D6F000000Dct5UAC/0056F000006Xw0jQAC";
        DefaultKinveyClientCallback userCallback = loginSalesforce(accessToken, refreshToken, clientID, ID, client);
        if (userCallback.error != null) {
            Log.d("test: ", userCallback.error.getMessage());
        }
        assertNotNull(userCallback.result);
        assertTrue(client.isUserLoggedIn());
    }

    @Test
    public void testLoginSalesforceAsyncBad() throws InterruptedException {
        String accessToken = "wrong";
        String refreshToken = "5Aep861..zRMyCurAW3YNVSrR4jYtnt9rDCBsqQ.ytSywG1HactASZdIt3r1Nxf4rdPDcKdjGhHQPU7TIva6NOR";
        String clientID = "3MVG9YDQS5WtC11o5afZtRCMB4EGBMjwb0MfQOBSW2u2EZ5r6fHt_sXtYx9i2.nJIkhzicIPWpyhm1zc3HlWw";
        String ID = "https://login.salesforce.com/id/00D6F000000Dct5UAC/0056F000006Xw0jQAC";
        DefaultKinveyClientCallback userCallback = loginSalesforce(accessToken, refreshToken, clientID, ID, client);
        assertNotNull(userCallback.error);
        assertFalse(client.isUserLoggedIn());
    }

    private DefaultKinveyClientCallback loginSalesforce(final String accessToken, final String refreshToken, final String clientID, final String ID, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.loginSalesForce(accessToken, refreshToken, clientID, ID, client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testMIC_LoginWithAuthorizationCodeLoginPage() throws InterruptedException {
        String redirectURI = "http://test.redirect";
        DefaultKinveyMICCallback userCallback = loginWithAuthorizationCodeLoginPage(redirectURI, client);
        assertNotNull(userCallback.myURLToRender);
        assertTrue(!userCallback.myURLToRender.isEmpty());
        assertTrue(userCallback.myURLToRender.startsWith(client.getMICHostName() + "oauth/auth?client_id"));
    }

    private DefaultKinveyMICCallback loginWithAuthorizationCodeLoginPage(final String redirectUrl, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyMICCallback callback = new DefaultKinveyMICCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.loginWithAuthorizationCodeLoginPage(client, redirectUrl, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testMIC_LoginWithAuthorizationCodeAPI() throws InterruptedException {
        String redirectURI = "kinveyAuthDemo://";
        String appKey = "kid_ZyrG_IFcGe";
        String appSecret = "f554f49974d14962bf72814268c2a7ba";
        Context mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        client = new Client.Builder(appKey, appSecret, mMockContext).build();
        if (client.isUserLoggedIn()) {
            logout(client);
        }
        DefaultKinveyUserCallback userCallback = loginWithAuthorizationCodeAPIAsync(username, password, redirectURI, client);
        assertNotNull(userCallback.result);
        logout(client);
    }

    private DefaultKinveyUserCallback loginWithAuthorizationCodeAPIAsync(final String username, final String password, final String redirectUrl, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyUserCallback callback = new DefaultKinveyUserCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.loginWithAuthorizationCodeAPI(client, username, password, redirectUrl, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testLogout() throws InterruptedException {
        login(username, password, client);
        DataStore<Person> personStore = DataStore.collection(Person.COLLECTION, Person.class, StoreType.SYNC, client);
        Person p = new Person();
        p.setUsername("TestUser");
        save(personStore, p);
        DataStore<Person> userStore = DataStore.collection("users", Person.class, StoreType.SYNC, client);
        Person p2 = new Person();
        p2.setUsername("TestUser2");
        save(userStore, p2);
        assertNull(logout(client).error);
        assertTrue(!client.isUserLoggedIn());
        assertTrue(client.getSycManager().getCount(Person.COLLECTION) == 0);
    }

    private DefaultPersonKinveyClientCallback save(final DataStore<Person> store, final Person person) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultPersonKinveyClientCallback callback = new DefaultPersonKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                store.save(person, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testLogoutWithNoDatabaseTables() throws InterruptedException {
        login(username, password, client);
        assertNull(logout(client).error);
        assertTrue(!client.isUserLoggedIn());
        assertTrue(client.getSycManager().getCount(Person.COLLECTION) == 0);
    }

    @Test
    public void testLogoutWithDatabaseTablesButNoAPICalls() throws InterruptedException {
        login(username, password, client);
        DataStore<Person> personStore = DataStore.collection(Person.COLLECTION, Person.class, StoreType.SYNC, client);
        assertNull(logout(client).error);
        assertTrue(!client.isUserLoggedIn());
        assertTrue(client.getSycManager().getCount(Person.COLLECTION) == 0);
    }

    @Test
    public void testCreateUserAsync() throws InterruptedException {
        DefaultKinveyClientCallback callback = signUp(createRandomUserName("CreateUser"), password, client);
        assertNotNull(callback.result);
        assertNotNull(callback.result.getUsername());
        assertTrue(client.isUserLoggedIn());
        assertNotNull(callback.result.getUsername().equals(username));
        assertNull(logout(client).error);
    }

    private DefaultKinveyClientCallback signUp(final String user, final String password, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.signUp(user, password, client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    private DefaultKinveyClientCallback login(final String user, final String password, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                try {
                    UserStore.login(user, password, client, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    @Ignore //reason: need master key auth
    public void testFindUserAsync() throws InterruptedException {
        DefaultKinveyClientCallback callback = login(username, password, client);
        User u = callback.result;
        assertTrue(client.isUserLoggedIn());
        assertNotNull(client.getActiveUser());
        assertTrue(client.getActiveUser().getId().equals(u.getId()));
        assertNull(logout(client).error);
    }

    @Test
    @Ignore //reason: need master key auth
    public void testFindUserAsyncBad() throws InterruptedException {

    }

    @Test
    @Ignore //reason: android UserStore don't have lookupAsync(criteria) method
    public void testLookupUsersAsync() throws InterruptedException {

    }

    @Test
    @Ignore //reason: android UserStore don't have lookupAsync(criteria) method
    public void testLookupUsersAsyncBad() throws InterruptedException {

    }

    @Test
    @Ignore//reason: need master key auth
    public void testDoesUsernameExist() throws InterruptedException {
        User user = login(username, password, client).result;
        boolean isNameExists = exists(user.getUsername(), client).result;
        assertTrue(isNameExists);
    }

    @Test //reason: need master key auth
    public void testDoesUsernameExistBad() throws InterruptedException {

    }

    private DefaultKinveyUserManagementCallback exists(final String username, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyUserManagementCallback callback = new DefaultKinveyUserManagementCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.exists(username,  client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test // this test always creates new user, to be careful
    @Ignore //reason: need master key auth,
    public void testForgotUsername() throws InterruptedException {
        User user = signUp(createRandomUserName("forgotUserName"), password, client).result;
        assertNotNull(user);
        assertNotNull(user.getUsername());
        assertTrue(client.isUserLoggedIn());
        boolean isForgotten = forgot(user.getUsername(), client).result;
        assertTrue(isForgotten);
    }

    private DefaultKinveyUserManagementCallback forgot(final String username, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyUserManagementCallback callback = new DefaultKinveyUserManagementCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.forgotUsername(client, username, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    @Ignore //this test always creates new user, to be careful
    public void testDeleteUserSoftAsync() throws InterruptedException {
        User user = signUp(createRandomUserName("deleteUserSoft"), password, client).result;
        assertNotNull(user);
        assertNotNull(user.getId());
        assertNull(deleteUser(false, client).error);
        assertNull(logout(client).error);
    }

    private DefaultKinveyUserDeleteCallback deleteUser(final boolean isHard, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyUserDeleteCallback callback = new DefaultKinveyUserDeleteCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.destroy(isHard, client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testDeleteUserHardAsync() throws InterruptedException {
        User user = signUp(createRandomUserName("deleteUserHard"), password, client).result;
        assertNotNull(user);
        assertNotNull(user.getId());
        assertNull(deleteUser(false, client).error);
        assertNull(logout(client).error);
    }

    @Test
    public void testUserEmailVerification() throws InterruptedException {
        User user = login(username, password, client).result;
        assertNotNull(user);
        assertNotNull(user.getId());
        boolean isEmailVerificationSent = sentEmailVerification(client).result;
        assertTrue(isEmailVerificationSent);
        assertNull(logout(client).error);
    }

    private DefaultKinveyUserManagementCallback sentEmailVerification(final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyUserManagementCallback callback = new DefaultKinveyUserManagementCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.sendEmailConfirmation(client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testUserPasswordReset() throws InterruptedException {
        User user = login(username, password, client).result;
        assertNotNull(user);
        assertNotNull(user.getId());
        assertNotNull(user.getUsername());
        boolean isPasswordReset = resetPassword(username, client).result;
        assertTrue(isPasswordReset);
        assertNull(logout(client).error);
    }

    private DefaultKinveyUserManagementCallback resetPassword(final String username, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyUserManagementCallback callback = new DefaultKinveyUserManagementCallback(latch);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                UserStore.resetPassword(username, client, callback);
                Looper.loop();
            }
        }).start();
        latch.await();
        return callback;
    }

    @Test
    public void testUserInitFromCredential() throws InterruptedException {
        Context mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
        Client.Builder localBuilder = new Client.Builder(appKey, appSecret, mMockContext);
        Client localClient = localBuilder.build();
        DefaultKinveyClientCallback callback = login(username, password, localClient);
        assertNotNull(callback.result);
        User activeUser = callback.result;
        Context mMockContext2 = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getContext(), "test_");
        Client.Builder localBuilder2 = new Client.Builder(appKey, appSecret, mMockContext2);
        Client localClient2 = localBuilder2.build();
        assertNotNull(localClient2.getActiveUser());
        assertTrue(activeUser.getId().equals(localClient2.getActiveUser().getId()));
        assertTrue(activeUser.getUsername().equals(localClient2.getActiveUser().getUsername()));
        assertNull(logout(localClient).error);
        assertNull(logout(localClient2).error);
    }

    private String createRandomUserName(String testName) {
        return testName + "_" +System.currentTimeMillis();
    }

}
