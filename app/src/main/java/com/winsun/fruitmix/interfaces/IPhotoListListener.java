package com.winsun.fruitmix.interfaces;

import android.view.View;

/**
 * Created by Administrator on 2016/8/4.
 */
public interface IPhotoListListener {

    void onPhotoItemClick(int selectedItemCount);

    void onPhotoItemLongClick();

    void onNoPhotoItem(boolean noPhotoItem);

    void onPhotoListScrollUp();

    void onPhotoListScrollDown();

    void onPhotoListScrollFinished();

    View getToolbar();

}
