package com.kinvey.androidTest.store.datastore

import android.content.Context
import android.os.Message
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.kinvey.android.Client
import com.kinvey.android.callback.AsyncUploaderProgressListener
import com.kinvey.android.model.User
import com.kinvey.android.store.FileStore
import com.kinvey.android.store.UserStore
import com.kinvey.androidTest.LooperThread
import com.kinvey.androidTest.TestManager
import com.kinvey.java.AbstractClient
import com.kinvey.java.core.KinveyClientCallback
import com.kinvey.java.core.MediaHttpUploader
import com.kinvey.java.model.FileMetaData
import com.kinvey.java.network.NetworkFileManager
import com.kinvey.java.store.StoreType
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@SmallTest
class FileStoreMockTest {

    private val kinveyApiVersion = "5"
    private var client: Client<User>? = null
    private var networkFileManager: NetworkFileManager? = null
    private var testFileName: String = "test_file.txt"

    @Before
    @Throws(InterruptedException::class, IOException::class)
    fun setUp() {
        val mMockContext: Context? = InstrumentationRegistry.getInstrumentation().targetContext
        AbstractClient.kinveyApiVersion = kinveyApiVersion
        client = Client.Builder<User>(mMockContext).build()
        client?.enableDebugLogging()
        AbstractClient.kinveyApiVersion = kinveyApiVersion
        val latch = CountDownLatch(1)
        var looperThread: LooperThread? = null
        if (client?.isUserLoggedIn == false) {
            looperThread = LooperThread(Runnable {
                try {
                    UserStore.login<User>(TestManager.USERNAME, TestManager.PASSWORD, client as AbstractClient<User>, object : KinveyClientCallback<User> {
                        override fun onSuccess(result: User?) {
                            Assert.assertNotNull(result)
                            latch.countDown()
                        }

                        override fun onFailure(error: Throwable?) {
                            Assert.assertNull(error)
                            latch.countDown()
                        }
                    })
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            })
            looperThread.start()
        } else {
            latch.countDown()
        }
        latch.await()
        looperThread?.mHandler?.sendMessage(Message())
        client?.let { networkFileManager = NetworkFileManager(it) }
    }

    @After
    fun tearDown() {
        client?.performLockDown()
        if (Client.kinveyHandlerThread != null) {
            try {
                client?.stopKinveyHandlerThread()
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
    }

    private fun getFileStore(storeType: StoreType): FileStore {
        val fileStore = spyk(FileStore(networkFileManager as NetworkFileManager,
                client?.cacheManager, 60 * 1000 * 1000L,
                storeType, client?.fileCacheFolder), recordPrivateCalls = true)
        return fileStore
    }

    private val asyncUploaderProgressListener =
    object: AsyncUploaderProgressListener<FileMetaData> {
        override fun progressChanged(uploader: MediaHttpUploader?) {}
        override fun onCancelled() {}
        override fun onSuccess(result: FileMetaData?) {}
        override fun onFailure(error: Throwable?) {}
        override var isCancelled: Boolean = false
    }

    @Test
    fun uploadTestFile() {

        assertNotNull(networkFileManager)

        val fileStore = getFileStore(StoreType.NETWORK)
        val file = File(testFileName)
        val testPrivateMethodName = "runAsyncUploadRequestUploadFile"

        val latch = CountDownLatch(1)

        val looperThread = LooperThread(Runnable {

            every { fileStore[testPrivateMethodName](file, asyncUploaderProgressListener)
            } returns true

            fileStore?.upload(file, asyncUploaderProgressListener)

            verify {
                fileStore[testPrivateMethodName](file, asyncUploaderProgressListener)
            }

            latch.countDown()
        })
        looperThread.start()
        latch.await()
        looperThread.mHandler?.sendMessage(Message())
    }

    @Test
    fun uploadTestFileMetadata() {

        assertNotNull(networkFileManager)

        val fileStore = getFileStore(StoreType.NETWORK)
        val fileMetaData = FileMetaData()
        val file = File(testFileName)
        val testPrivateMethodName = "runAsyncUploadRequestFileMetadata"

        val latch = CountDownLatch(1)

        val looperThread = LooperThread(Runnable {

            every { fileStore[testPrivateMethodName](file, fileMetaData, asyncUploaderProgressListener)
            } returns true

            fileStore?.upload(file, fileMetaData, asyncUploaderProgressListener)

            verify {
                fileStore[testPrivateMethodName](file, fileMetaData, asyncUploaderProgressListener)
            }

            latch.countDown()
        })
        looperThread.start()
        latch.await()
        looperThread.mHandler?.sendMessage(Message())
    }

    @Test
    fun uploadTestStreamMetadata() {

        assertNotNull(networkFileManager)

        val fileStore = getFileStore(StoreType.NETWORK)
        val fileMetaData = FileMetaData()
        val stream = ByteArrayInputStream(byteArrayOf())
        val testPrivateMethodName = "runAsyncUploadRequestStreamMetadata"

        val latch = CountDownLatch(1)

        val looperThread = LooperThread(Runnable {

            every { fileStore[testPrivateMethodName](stream, fileMetaData, asyncUploaderProgressListener)
            } returns true

            fileStore?.upload(stream, fileMetaData, asyncUploaderProgressListener)

            verify {
                fileStore[testPrivateMethodName](stream, fileMetaData, asyncUploaderProgressListener)
            }

            latch.countDown()
        })
        looperThread.start()
        latch.await()
        looperThread.mHandler?.sendMessage(Message())
    }

    @Test
    fun uploadTestStreamFilename() {

        assertNotNull(networkFileManager)

        val fileStore = getFileStore(StoreType.NETWORK)
        val fileName = "test_file.txt"
        val stream = ByteArrayInputStream(byteArrayOf())
        val testPrivateMethodName = "runAsyncUploadRequestStreamFilename"

        val latch = CountDownLatch(1)

        val looperThread = LooperThread(Runnable {

            every { fileStore[testPrivateMethodName](fileName, stream, asyncUploaderProgressListener)
            } returns true

            fileStore?.upload(fileName, stream, asyncUploaderProgressListener)

            verify {
                fileStore[testPrivateMethodName](fileName, stream, asyncUploaderProgressListener)
            }

            latch.countDown()
        })
        looperThread.start()
        latch.await()
        looperThread.mHandler?.sendMessage(Message())
    }


    @Test
    fun removeTest() {
    }

    @Test
    fun downloadTest() {
    }

    @Test
    fun refreshTest() {
    }

    @Test
    fun findTest() {
    }

    @Test
    fun cachedFileTest() {
    }

    @Test
    fun clearCacheTest() {
    }
}