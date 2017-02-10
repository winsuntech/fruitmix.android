package com.winsun.fruitmix.refactor.common;

import android.content.Intent;

/**
 * Created by Administrator on 2017/1/24.
 */

public interface BasePresenter<T> {

    void attachView(T view);

    void detachView();

    void startMission();

    void handleBackEvent();

    void handleOnActivityResult(int requestCode, int resultCode,Intent data);

}