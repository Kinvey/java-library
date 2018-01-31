package com.kinvey.androidTest.callback;

import com.google.api.client.json.GenericJson;
import com.kinvey.android.sync.KinveyPullResponse;
import com.kinvey.android.sync.KinveyPushResponse;
import com.kinvey.android.sync.KinveySyncCallback;

import java.util.concurrent.CountDownLatch;

/**
 * Created by yuliya on 12/27/17.
 */

public class CustomKinveySyncCallback<T extends GenericJson> implements KinveySyncCallback<T> {

    private CountDownLatch latch;
    private KinveyPullResponse<T> result;
    private KinveyPushResponse kinveyPushResponse;
    private Throwable error;
    public CustomKinveySyncCallback(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void onSuccess(KinveyPushResponse kinveyPushResponse, KinveyPullResponse<T> kinveyPullResponse) {
        this.result = kinveyPullResponse;
        this.kinveyPushResponse = kinveyPushResponse;
        finish();
    }

    @Override
    public void onPullStarted() {

    }

    @Override
    public void onPushStarted() {

    }

    @Override
    public void onPullSuccess(KinveyPullResponse<T> kinveyPullResponse) {

    }

    @Override
    public void onPushSuccess(KinveyPushResponse kinveyPushResponse) {

    }

    @Override
    public void onFailure(Throwable t) {
        this.error = error;
        finish();
    }

    private void finish() {
        latch.countDown();
    }

    public KinveyPullResponse<T> getResult() {
        return result;
    }

    public void setResult(KinveyPullResponse<T> result) {
        this.result = result;
    }

    public KinveyPushResponse getKinveyPushResponse() {
        return kinveyPushResponse;
    }

    public void setKinveyPushResponse(KinveyPushResponse kinveyPushResponse) {
        this.kinveyPushResponse = kinveyPushResponse;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }
}