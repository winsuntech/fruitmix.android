package com.winsun.fruitmix.refactor.presenter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;

import com.winsun.fruitmix.R;
import com.winsun.fruitmix.mediaModule.model.Media;
import com.winsun.fruitmix.mediaModule.model.MediaShare;
import com.winsun.fruitmix.mediaModule.model.MediaShareContent;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.refactor.business.DataRepository;
import com.winsun.fruitmix.refactor.business.callback.MediaOperationCallback;
import com.winsun.fruitmix.refactor.business.callback.MediaShareOperationCallback;
import com.winsun.fruitmix.refactor.contract.MediaFragmentContract;
import com.winsun.fruitmix.refactor.contract.MediaMainFragmentContract;
import com.winsun.fruitmix.refactor.model.MediaFragmentDataLoader;
import com.winsun.fruitmix.util.FNAS;
import com.winsun.fruitmix.util.LocalCache;
import com.winsun.fruitmix.util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/2/10.
 */

public class MediaFragmentPresenterImpl implements MediaFragmentContract.MediaFragmentPresenter {

    private MediaFragmentContract.MediaFragmentView mView;

    private MediaMainFragmentContract.MediaMainFragmentPresenter mMediaMainFragmentPresenter;

    private DataRepository mRepository;

    private List<String> mPhotoDateGroups;

    private Map<String, List<Media>> mMapKeyIsDateValueIsPhotoList;

    private SparseArray<String> mMapKeyIsPhotoPositionValueIsPhotoDate;
    private SparseArray<Media> mMapKeyIsPhotoPositionValueIsPhoto;

    private List<Media> mMedias;

    private List<String> mAlreadySelectedImageKeyArrayList;

    private int mAdapterItemTotalCount = 0;

    private int mSelectCount;

    private boolean mSelectMode = false;

    private Bundle reenterState;
    private boolean mPhotoListRefresh = false;

    private boolean mFabExpand = false;

    public MediaFragmentPresenterImpl(MediaMainFragmentContract.MediaMainFragmentPresenter mediaMainFragmentPresenter, DataRepository repository,List<String> alreadySelectedImageKeyArrayList) {
        mMediaMainFragmentPresenter = mediaMainFragmentPresenter;
        mRepository = repository;
        mAlreadySelectedImageKeyArrayList = alreadySelectedImageKeyArrayList;
    }

    private void clearSelectPhoto() {
        if (mMapKeyIsPhotoPositionValueIsPhoto == null) return;

        for (List<Media> mediaList : mMapKeyIsDateValueIsPhotoList.values()) {
            for (Media media : mediaList) {
                media.setSelected(false);
            }
        }
    }

    @Override
    public int getSelectCount() {
        return mSelectCount;
    }

    @Override
    public void calcSelectedPhoto() {

        mSelectCount = 0;

        for (List<Media> mediaList : mMapKeyIsDateValueIsPhotoList.values()) {
            for (Media media : mediaList) {
                if (media.isSelected())
                    mSelectCount++;
            }
        }

    }

    @Override
    public void enterChooseMode() {

        mSelectMode = true;

        mMediaMainFragmentPresenter.setToolbarNavigationIcon(R.drawable.ic_back);
        mMediaMainFragmentPresenter.setToolbarNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quitChooseMode();
            }
        });

        mMediaMainFragmentPresenter.setSelectModeBtnVisibility(View.GONE);
        mMediaMainFragmentPresenter.dismissBottomNavAnim();
        mMediaMainFragmentPresenter.setTitleText(R.string.choose_photo);
        mMediaMainFragmentPresenter.lockDrawer();

        mView.setFABVisibility(View.VISIBLE);
        mView.notifyDataSetChangedUseAnim();
    }

    @Override
    public void quitChooseMode() {

        mSelectMode = false;
        clearSelectPhoto();

        mMediaMainFragmentPresenter.setToolbarNavigationIcon(R.drawable.menu);
        mMediaMainFragmentPresenter.setToolbarNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaMainFragmentPresenter.switchDrawerOpenState();
            }
        });

        mMediaMainFragmentPresenter.setSelectModeBtnVisibility(View.VISIBLE);
        mMediaMainFragmentPresenter.showBottomNavAnim();
        mMediaMainFragmentPresenter.setTitleText(R.string.photo);
        mMediaMainFragmentPresenter.unlockDrawer();

        mFabExpand = false;
        mView.collapseFab();
        mView.setFABVisibility(View.GONE);

        mView.notifyDataSetChangedUseAnim();
    }

    @NonNull
    private List<String> getSelectedImageKeys() {

        List<String> selectedImageKeys = new ArrayList<>();

        for (List<Media> mediaList : mMapKeyIsDateValueIsPhotoList.values()) {
            for (Media media : mediaList) {
                if (media.isSelected()) {

                    String mediaUUID = media.getUuid();
                    if (mediaUUID.isEmpty()) {
                        mediaUUID = Util.CalcSHA256OfFile(media.getThumb());
                    }

                    selectedImageKeys.add(mediaUUID);
                }
            }
        }

        if (mAlreadySelectedImageKeyArrayList != null) {
            for (String mediaUUID : mAlreadySelectedImageKeyArrayList) {
                if (!selectedImageKeys.contains(mediaUUID)) {
                    selectedImageKeys.add(mediaUUID);
                }
            }
        }

        return selectedImageKeys;
    }

    @Override
    public void albumBtnOnClick() {

        if (!mView.isNetworkAlive())
            mView.showNoNetwork();

        List<String> selectMediaKeys = getSelectedImageKeys();
        if (selectMediaKeys.size() == 0) {
            mView.showSelectNothingToast();
        }
        quitChooseMode();

        createAlbum(selectMediaKeys);

    }

    @Override
    public void shareBtnOnClick() {

        if (!mView.isNetworkAlive())
            mView.showNoNetwork();

        List<String> selectMediaKeys = getSelectedImageKeys();
        if (selectMediaKeys.size() == 0) {
            mView.showSelectNothingToast();
        }
        quitChooseMode();

        mView.showDialog();
        createMediaShare(selectMediaKeys);

    }

    @Override
    public void fabOnClick() {

        if (mFabExpand) {
            mFabExpand = false;
            mView.collapseFab();
        } else {
            mFabExpand = true;
            mView.expandFab();
        }

    }

    private MediaShare createMediaShareInMemory(List<String> selectMediaKeys) {

        MediaShare mediaShare = new MediaShare();
        mediaShare.setUuid(Util.createLocalUUid());

        List<MediaShareContent> mediaShareContents = new ArrayList<>();

        for (String mediaKey : selectMediaKeys) {
            MediaShareContent mediaShareContent = new MediaShareContent();
            mediaShareContent.setKey(mediaKey);
            mediaShareContent.setAuthor(FNAS.userUUID);
            mediaShareContent.setTime(String.valueOf(System.currentTimeMillis()));
            mediaShareContents.add(mediaShareContent);

        }

        mediaShare.initMediaShareContents(mediaShareContents);

        mediaShare.setCoverImageKey(selectMediaKeys.get(0));
        mediaShare.setTitle("");
        mediaShare.setDesc("");
        for (String userUUID : LocalCache.RemoteUserMapKeyIsUUID.keySet()) {
            mediaShare.addViewer(userUUID);
        }
        mediaShare.addMaintainer(FNAS.userUUID);
        mediaShare.setCreatorUUID(FNAS.userUUID);
        mediaShare.setTime(String.valueOf(System.currentTimeMillis()));
        mediaShare.setDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(Long.parseLong(mediaShare.getTime()))));
        mediaShare.setArchived(false);
        mediaShare.setAlbum(false);
        mediaShare.setLocal(true);

        return mediaShare;

    }

    private void createMediaShare(List<String> selectMediaKeys) {
        mRepository.createMediaShare(createMediaShareInMemory(selectMediaKeys), new MediaShareOperationCallback.OperateMediaShareCallback() {
            @Override
            public void onOperateSucceed(OperationResult operationResult, MediaShare mediaShare) {

                mView.dismissDialog();

                mMediaMainFragmentPresenter.setViewPageCurrentItem(MediaMainFragmentPresenterImpl.PAGE_SHARE);
            }

            @Override
            public void onOperateFail(OperationResult operationResult) {

                mView.dismissDialog();
            }
        });
    }

    private void createAlbum(List<String> selectMediaKeys) {

        LocalCache.mediaKeysInCreateAlbum.addAll(selectMediaKeys);
        clearSelectPhoto();

        mView.startCreateAlbumActivity();

    }

    @Override
    public boolean isSelectState() {
        return mSelectMode;
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {

        reenterState = new Bundle(data.getExtras());
        int initialPhotoPosition = reenterState.getInt(Util.INITIAL_PHOTO_POSITION);
        int currentPhotoPosition = reenterState.getInt(Util.CURRENT_PHOTO_POSITION);
        String currentMediaKey = reenterState.getString(Util.CURRENT_MEDIA_KEY);

        if (initialPhotoPosition != currentPhotoPosition) {

            int scrollToPosition = 0;

            Media media;

            int size = mMapKeyIsPhotoPositionValueIsPhoto.size();

            for (int i = 0; i < size; i++) {
                media = mMapKeyIsPhotoPositionValueIsPhoto.valueAt(i);
                if (media.getKey().equals(currentMediaKey))
                    scrollToPosition = mMapKeyIsPhotoPositionValueIsPhoto.keyAt(i);
            }

            mView.smoothScrollToPosition(scrollToPosition);

            mView.startPostponedEnterTransition();

        }

    }

    @Override
    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
        if (reenterState != null) {

            int initialPhotoPosition = reenterState.getInt(Util.INITIAL_PHOTO_POSITION);
            int currentPhotoPosition = reenterState.getInt(Util.CURRENT_PHOTO_POSITION);
            String currentMediaKey = reenterState.getString(Util.CURRENT_MEDIA_KEY);

            if (initialPhotoPosition != currentPhotoPosition) {

                names.clear();
                sharedElements.clear();

                Media media;
                Media currentMedia = null;

                int size = mMapKeyIsPhotoPositionValueIsPhoto.size();

                for (int i = 0; i < size; i++) {
                    media = mMapKeyIsPhotoPositionValueIsPhoto.valueAt(i);
                    if (media.getKey().equals(currentMediaKey))
                        currentMedia = media;
                }

                if (currentMedia == null) return;

                View newSharedElement = mView.findViewByMedia(currentMedia);
                String sharedElementName = currentMedia.getKey();

                names.add(sharedElementName);
                sharedElements.put(sharedElementName, newSharedElement);
            }

        }
        reenterState = null;

    }

    @Override
    public void onResume() {
        if (mPhotoListRefresh) {
            mRepository.loadLoadMediaInCamera(new MediaOperationCallback.LoadMediasCallback() {
                @Override
                public void onLoadSucceed(OperationResult operationResult, List<Media> medias) {
                    showMedias(medias);
                }

                @Override
                public void onLoadFail(OperationResult operationResult) {

                }
            });
        }
    }

    @Override
    public void imageOnLongClick(Media media, Context context) {

        enterChooseMode();

        media.setSelected(true);

        mSelectCount = 1;
        mMediaMainFragmentPresenter.setTitleText(String.format(context.getString(R.string.select_count), mSelectCount));
    }

    @Override
    public void attachView(MediaFragmentContract.MediaFragmentView view) {
        mView = view;
    }

    @Override
    public void detachView() {
        mView = null;
    }

    @Override
    public void startMission() {

        mRepository.loadMedias(new MediaOperationCallback.LoadMediasCallback() {
            @Override
            public void onLoadSucceed(OperationResult operationResult, List<Media> medias) {

                mPhotoListRefresh = true;

                showMedias(medias);
            }

            @Override
            public void onLoadFail(OperationResult operationResult) {

            }
        });
    }

    private void showMedias(final List<Media> medias) {
        mRepository.handleMediasForMediaFragment(medias, new MediaOperationCallback.HandleMediaForMediaFragmentCallback() {
            @Override
            public void onOperateFinished(MediaFragmentDataLoader loader) {
                mPhotoDateGroups = loader.getmPhotoDateGroups();
                mMapKeyIsPhotoPositionValueIsPhoto = loader.getmMapKeyIsPhotoPositionValueIsPhoto();
                mMapKeyIsPhotoPositionValueIsPhotoDate = loader.getmMapKeyIsPhotoPositionValueIsPhotoDate();
                mMapKeyIsDateValueIsPhotoList = loader.getmMapKeyIsDateValueIsPhotoList();
                mAdapterItemTotalCount = loader.getmAdapterItemTotalCount();
                mMedias = loader.getMedias();

                clearSelectPhoto();

                mView.dismissLoadingUI();
                if(mPhotoDateGroups.size() == 0){
                    mView.showMedias(mMapKeyIsPhotoPositionValueIsPhotoDate, mMapKeyIsPhotoPositionValueIsPhoto,mMapKeyIsDateValueIsPhotoList,mMedias);
                }else {
                    mView.showNoContentUI();
                }

            }
        });
    }

    @Override
    public void handleBackEvent() {

    }

    @Override
    public void handleOnActivityResult(int requestCode, int resultCode, Intent data) {

    }
}