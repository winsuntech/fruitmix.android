package com.winsun.fruitmix.contract;

import com.winsun.fruitmix.common.BasePresenter;
import com.winsun.fruitmix.common.BaseView;

/**
 * Created by Administrator on 2017/2/6.
 */

public interface SplashContract {

    interface SplashView extends BaseView {

        void emptyCacheToken();

        void welcome();


    }

    interface SplashPresenter extends BasePresenter<SplashView> {

        void createDownloadFileStoreFolder();

        void loadToken();

    }

}