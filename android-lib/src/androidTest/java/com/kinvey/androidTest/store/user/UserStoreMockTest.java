package com.kinvey.androidTest.store.user;

import android.content.Context;
import android.os.Message;

import androidx.test.runner.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.kinvey.android.Client;
import com.kinvey.android.model.User;
import com.kinvey.android.store.UserStore;
import com.kinvey.androidTest.LooperThread;
import com.kinvey.java.core.KinveyJsonResponseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static com.kinvey.androidTest.TestManager.PASSWORD;
import static com.kinvey.androidTest.TestManager.USERNAME;
import static com.kinvey.androidTest.store.user.MockHttpErrorTransport.DESCRIPTION_500;
import static com.kinvey.androidTest.store.user.MockHttpErrorTransport.ERROR_500;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UserStoreMockTest {

    private Client client = null;
    private Context mMockContext = null;

    @Before
    public void setup() throws InterruptedException {
        if (mMockContext == null) {
            mMockContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        }
        if (client == null) {
            client = new Client.Builder(mMockContext).build();
        }
        if (client.isUserLoggedIn()) {
            logout(client);
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        if (client.isUserLoggedIn()) {
            logout(client);
        }
        client.performLockDown();
        if (client.getKinveyHandlerThread() != null) {
            try {
                client.stopKinveyHandlerThread();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    @Test
    public void testLogin() throws InterruptedException {
        MockClient<User> mockedClient = new MockClient.Builder<>(mMockContext).build();
        DefaultKinveyClientCallback callback = login(mockedClient);
        assertNull(callback.error);
        assertNotNull(callback.result);
    }

    @Test
    public void testLoginWithUsernameAndPassword() throws InterruptedException {
        MockClient<User> mockedClient = new MockClient.Builder<>(mMockContext).build();
        DefaultKinveyClientCallback callback = login(USERNAME, PASSWORD, mockedClient);
        assertNull(callback.error);
        assertNotNull(callback.result);
    }

    @Test
    public void testLoginError() throws InterruptedException {
        MockClient<User> mockedClient = new MockClient.Builder<>(mMockContext).build(new MockHttpErrorTransport());
        DefaultKinveyClientCallback callback = login(USERNAME, PASSWORD, mockedClient);
        assertNotNull(callback.error);
        assertNull(callback.result);
        assertEquals(500, ((KinveyJsonResponseException) callback.error).getStatusCode());
    }

    @Test
    public void testLoginError500() throws InterruptedException {
        MockClient<User> mockedClient = new MockClient.Builder<>(mMockContext).build(new MockHttpErrorTransport());
        DefaultKinveyClientCallback callback = login(USERNAME, PASSWORD, mockedClient);
        assertNotNull(callback.error);
        assertNull(callback.result);
        assertEquals(500, ((KinveyJsonResponseException) callback.error).getStatusCode());
        assertEquals(ERROR_500+"\n"+DESCRIPTION_500, callback.error.getMessage());
    }

    @Test
    public void testLoginWithCredentialsError() throws InterruptedException {
        login(USERNAME, PASSWORD, client); //to save login and password to Credential
        Assert.assertTrue(client.isUserLoggedIn());
        MockClient<User> mockedClient = new MockClient.Builder<>(mMockContext).build(new MockHttpErrorTransport());
        Assert.assertFalse(mockedClient.isUserLoggedIn());
    }

    @Test
    public void testLoginWithCredentials() throws InterruptedException {
        if (!client.isUserLoggedIn()) {
            login(USERNAME, PASSWORD, client); //to save login and password to Credential
        }
        Assert.assertTrue(client.isUserLoggedIn());
        MockClient<User> mockedClient = new MockClient.Builder<>(mMockContext).build(new MockHttpTransport());
        Assert.assertTrue(mockedClient.isUserLoggedIn());
    }

    private DefaultKinveyClientCallback login(final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        LooperThread looperThread = new LooperThread(() -> {
            try {
                UserStore.login(client, callback);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        looperThread.start();
        latch.await();
        looperThread.mHandler.sendMessage(new Message());
        return callback;
    }

    private DefaultKinveyClientCallback login(final String username, final String password, final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyClientCallback callback = new DefaultKinveyClientCallback(latch);
        LooperThread looperThread = new LooperThread(() -> {
            try {
                UserStore.login(username, password, client, callback);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        looperThread.start();
        latch.await();
        looperThread.mHandler.sendMessage(new Message());
        return callback;
    }

    private DefaultKinveyVoidCallback logout(final Client client) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DefaultKinveyVoidCallback callback = new DefaultKinveyVoidCallback(latch);
        LooperThread looperThread = new LooperThread(() -> UserStore.logout(client, callback));
        looperThread.start();
        latch.await();
        looperThread.mHandler.sendMessage(new Message());
        return callback;
    }

}
