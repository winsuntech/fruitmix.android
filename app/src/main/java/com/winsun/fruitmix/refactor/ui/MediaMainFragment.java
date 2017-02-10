package com.winsun.fruitmix.refactor.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.winsun.fruitmix.R;
import com.winsun.fruitmix.anim.BaseAnimationListener;
import com.winsun.fruitmix.eventbus.MediaShareCommentOperationEvent;
import com.winsun.fruitmix.eventbus.OperationEvent;
import com.winsun.fruitmix.interfaces.IPhotoListListener;
import com.winsun.fruitmix.interfaces.OnMainFragmentInteractionListener;
import com.winsun.fruitmix.mediaModule.fragment.AlbumList;
import com.winsun.fruitmix.mediaModule.fragment.MediaShareList;
import com.winsun.fruitmix.mediaModule.fragment.NewPhotoList;
import com.winsun.fruitmix.mediaModule.interfaces.OnMediaFragmentInteractionListener;
import com.winsun.fruitmix.mediaModule.interfaces.Page;
import com.winsun.fruitmix.mediaModule.model.Comment;
import com.winsun.fruitmix.mediaModule.model.MediaShare;
import com.winsun.fruitmix.model.OperationResultType;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.refactor.contract.MainPageContract;
import com.winsun.fruitmix.refactor.contract.MediaMainFragmentContract;
import com.winsun.fruitmix.refactor.presenter.MediaMainFragmentPresenterImpl;
import com.winsun.fruitmix.util.FNAS;
import com.winsun.fruitmix.util.LocalCache;
import com.winsun.fruitmix.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;

public class MediaMainFragment extends Fragment implements OnMediaFragmentInteractionListener,IPhotoListListener, MediaMainFragmentContract.MediaMainFragmentView {

    public static final String TAG = MediaMainFragment.class.getSimpleName();

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.right)
    TextView lbRight;
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.album_balloon)
    ImageView mAlbumBalloon;
    @BindView(R.id.bottom_navigation_view)
    BottomNavigationView bottomNavigationView;

    private List<Page> pageList;
    private AlbumList albumList;
    private MediaFragment mediaFragment;
    private MediaShareList shareList;

    private Context mContext;

    private ProgressDialog mDialog;

    private static final int PAGE_SHARE = 0;
    private static final int PAGE_PHOTO = 1;
    private static final int PAGE_ALBUM = 2;

    private boolean sInChooseMode = false;

    private boolean onResume = false;

    private boolean mPhotoListRefresh = false;
    private boolean mShareAlbumListRefresh = false;

    private MediaMainFragmentContract.MediaMainFragmentPresenter mPresenter;
    private MainPageContract.MainPagePresenter mMainPagePresenter;

    public MediaMainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MediaMainFragment.
     */
    public static MediaMainFragment newInstance(MainPageContract.MainPagePresenter mainPagePresenter) {
        MediaMainFragment fragment = new MediaMainFragment();
        fragment.mMainPagePresenter = mainPagePresenter;
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();

        initPageList();

        mPresenter = new MediaMainFragmentPresenterImpl(mMainPagePresenter, null, null, null);
        mPresenter.attachView(this);
        mMainPagePresenter = null;

        Log.d(TAG, "onCreate: ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.media_main_fragment, container, false);

        ButterKnife.bind(this, view);

        toolbar.setTitle("");
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.switchDrawerOpenState();
            }
        });

        initNavigationView();

        initViewPager();

        viewPager.setCurrentItem(PAGE_PHOTO);

        lbRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.selectModeBtnClick();
            }
        });


        Log.d(TAG, "onCreateView: ");

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        Log.d(TAG, "onStart: ");
    }

    @Override
    public void onResume() {
        super.onResume();

        mPresenter.startMission();

    }

    @Override
    public void onStop() {

        EventBus.getDefault().unregister(this);

        super.onStop();

        Log.d(TAG, "onStop: ");

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mPresenter.detachView();

        mediaFragment.onDestroyView();

        Log.d(TAG, "onDestroyView: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mContext = null;

        Log.d(TAG, "onDestroy: ");
    }


    private void initNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                mPresenter.onNavigationItemSelected(item.getItemId());

                return true;
            }
        });

        resetBottomNavigationItemCheckState();

    }

    public void resetBottomNavigationItemCheckState() {

        int size = bottomNavigationView.getMenu().size();

        for (int i = 0; i < size; i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }

    }

    @Override
    public void setBottomNavigationItemChecked(int position) {
        bottomNavigationView.getMenu().getItem(position).setChecked(true);
    }

    @Override
    public void setViewPageCurrentItem(int position) {
        viewPager.setCurrentItem(position);
    }

    @Override
    public void setTitleText(int resID) {
        title.setText(getString(resID));
    }

    @Override
    public void setTitleText(String titleText) {
        title.setText(titleText);
    }

    @Override
    public void setToolbarNavigationIcon(int resID) {
        toolbar.setNavigationIcon(resID);
    }

    @Override
    public void setSelectModeBtnVisibility(int visibility) {
        lbRight.setVisibility(visibility);
    }

    private void initPageList() {
        shareList = new MediaShareList(getActivity(), this);

        albumList = new AlbumList(getActivity(), this);
        pageList = new ArrayList<Page>();
        pageList.add(shareList);

        pageList.add(albumList);
    }

    private void initViewPager() {
        MyAdapter myAdapter = new MyAdapter();
        viewPager.setAdapter(myAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

                mPresenter.onPageSelected(position);

            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleOperationEvent(OperationEvent operationEvent) {

        String action = operationEvent.getAction();

        Log.i(TAG, "handleOperationEvent: action:" + action);

        switch (action) {
            case Util.REMOTE_SHARE_CREATED:
                handleRemoteShareCreated(operationEvent);
                break;
            case Util.REMOTE_SHARE_MODIFIED:
            case Util.REMOTE_SHARE_DELETED:
                handleRemoteShareModifiedDeleted(operationEvent);
                break;
            case Util.REMOTE_COMMENT_CREATED:
                handleRemoteCommentCreated(operationEvent);
                break;
            case Util.LOCAL_COMMENT_DELETED:
                handleLocalCommentDeleted(operationEvent);
                break;
            case Util.LOCAL_MEDIA_COMMENT_RETRIEVED:
                Log.i(TAG, "local media comment loaded");
                ((MediaShareList) pageList.get(PAGE_SHARE)).refreshLocalComment();

                doCreateRemoteMediaCommentInLocalMediaCommentMapFunction();
                break;
            case Util.REMOTE_MEDIA_COMMENT_RETRIEVED:

                Log.i(TAG, "remote media comment loaded ");

                ((MediaShareList) pageList.get(PAGE_SHARE)).refreshRemoteComment();

                break;

            case Util.NEW_LOCAL_MEDIA_IN_CAMERA_RETRIEVED:

                Log.i(TAG, "handleOperationEvent: new local media in camera retrieved succeed");

                setPhotoListRefresh();



                break;
            case Util.LOCAL_MEDIA_RETRIEVED:

                Log.i(TAG, "handleOperationEvent: local media in db retrieved succeed");

                setPhotoListRefresh();



                break;

            case Util.REMOTE_MEDIA_RETRIEVED:

                Log.i(TAG, "remote media loaded");

                setPhotoListRefresh();



                break;

            case Util.REMOTE_MEDIA_SHARE_RETRIEVED:

                Log.i(TAG, "remote share loaded");

                setShareAlbumListRefresh();

                albumList.refreshView();
                shareList.refreshView();

                break;

        }

    }

    public void setPhotoListRefresh() {
        if (Util.isLocalMediaInCameraLoaded() && Util.isLocalMediaInDBLoaded() && Util.isRemoteMediaLoaded())
            mPhotoListRefresh = true;
    }

    public void setShareAlbumListRefresh() {
        if (Util.isRemoteMediaShareLoaded())
            mShareAlbumListRefresh = true;
    }

    private void doCreateRemoteMediaCommentInLocalMediaCommentMapFunction() {

        for (Map.Entry<String, List<Comment>> entry : LocalCache.LocalMediaCommentMapKeyIsImageUUID.entrySet()) {

            for (Comment comment : entry.getValue()) {
                FNAS.createRemoteMediaComment(mContext, entry.getKey(), comment);
            }

        }
    }

    private void handleLocalCommentDeleted(OperationEvent operationEvent) {
        Log.i(TAG, "local comment changed");

        OperationResultType result = operationEvent.getOperationResult().getOperationResultType();

        if (result == OperationResultType.SUCCEED) {
            shareList.refreshView();
        }
    }

    private void handleRemoteCommentCreated(OperationEvent operationEvent) {

        OperationResultType result = operationEvent.getOperationResult().getOperationResultType();

        if (result == OperationResultType.SUCCEED) {
            Log.i(TAG, "remote comment created");

            Comment comment = ((MediaShareCommentOperationEvent) operationEvent).getComment();
            String imageUUID = ((MediaShareCommentOperationEvent) operationEvent).getImageUUID();
            FNAS.deleteLocalMediaComment(mContext, imageUUID, comment);
        }
    }

    private void handleRemoteShareModifiedDeleted(OperationEvent operationEvent) {
        dismissDialog();

        OperationResult operationResult = operationEvent.getOperationResult();

        OperationResultType operationResultType = operationResult.getOperationResultType();

        switch (operationResultType) {
            case SUCCEED:
                Toast.makeText(mContext, operationResult.getResultMessage(mContext), Toast.LENGTH_SHORT).show();
                albumList.refreshView();
                shareList.refreshView();
                break;
            default:
                Toast.makeText(mContext, operationResult.getResultMessage(mContext), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRemoteShareCreated(OperationEvent operationEvent) {
        Log.i(TAG, "remote share created");

        OperationResult operationResult = operationEvent.getOperationResult();

        dismissDialog();

        if (operationResult.getOperationResultType() == OperationResultType.SUCCEED) {
            viewPager.setCurrentItem(PAGE_SHARE);

            pageList.get(PAGE_SHARE).onDidAppear();
        }

        Toast.makeText(mContext, operationResult.getResultMessage(mContext), Toast.LENGTH_SHORT).show();

    }

    public void retrieveRemoteMediaComment(String mediaUUID) {

        FNAS.retrieveRemoteMediaCommentMap(mContext, mediaUUID);
    }

    public void setSelectCountText(String text) {
        title.setText(text);
    }

    public void showTips() {
        if (getShowAlbumTipsValue()) {
            setShowAlbumTipsValue(false);
            if (mAlbumBalloon != null) {
                mAlbumBalloon.setVisibility(View.VISIBLE);
                mAlbumBalloon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAlbumBalloon.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private boolean getShowAlbumTipsValue() {
        SharedPreferences sp;
        sp = getActivity().getSharedPreferences(Util.FRUITMIX_SHAREDPREFERENCE_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(Util.SHOW_ALBUM_TIPS, true);
    }

    private void setShowAlbumTipsValue(boolean value) {
        SharedPreferences sp;
        SharedPreferences.Editor editor;
        sp = getActivity().getSharedPreferences(Util.FRUITMIX_SHAREDPREFERENCE_NAME, Context.MODE_PRIVATE);
        editor = sp.edit();
        editor.putBoolean(Util.SHOW_ALBUM_TIPS, value);
        editor.apply();
    }


    public void showBottomNavAnim() {

        bottomNavigationView.setVisibility(View.VISIBLE);

        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.show_bottom_item_anim);
        animation.setAnimationListener(new BaseAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);

                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) viewPager.getLayoutParams();
                lp.bottomMargin = Util.dip2px(getActivity(), 56.0f);
                //if(LocalCache.ScreenWidth==540) lp.bottomMargin=76;
                //else if(LocalCache.ScreenWidth==1080) lp.bottomMargin=140;
                viewPager.setLayoutParams(lp);
            }
        });


        bottomNavigationView.startAnimation(animation);
    }

    private void showBottomNav() {
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    public void dismissBottomNavAnim() {

        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.dismiss_bottom_item_anim);
        animation.setAnimationListener(new BaseAnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                super.onAnimationStart(animation);

                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) viewPager.getLayoutParams();
                lp.bottomMargin = 0;
                viewPager.setLayoutParams(lp);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);

                bottomNavigationView.setVisibility(View.GONE);
            }
        });

        bottomNavigationView.startAnimation(animation);
    }

    @Override
    public void setToolbarNavigationOnClickListener(View.OnClickListener listener) {
        toolbar.setNavigationOnClickListener(listener);
    }

    @Override
    public int getCurrentViewPageItem() {
        return viewPager.getCurrentItem();
    }


    @Override
    public void onPhotoItemClick(int selectedItemCount) {

        setSelectCountText(String.format(getString(R.string.select_count), selectedItemCount));
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }

    @Override
    public void onPhotoItemLongClick() {

        dismissBottomNavAnim();

        setSelectCountText(String.format(getString(R.string.select_count), 1));
    }

    @Override
    public void onNoPhotoItem(boolean noPhotoItem) {

        Log.d(TAG, "onNoPhotoItem:" + noPhotoItem);

        int currentItem = viewPager.getCurrentItem();

        if (noPhotoItem && currentItem == PAGE_PHOTO) {
            lbRight.setVisibility(View.GONE);
        } else if (!noPhotoItem && currentItem == PAGE_PHOTO) {
            lbRight.setVisibility(View.VISIBLE);
        }
    }

    private boolean showNothingSelectToast(List<String> selectUUIDs) {
        if (selectUUIDs.size() == 0) {
            Toast.makeText(mContext, getString(R.string.select_nothing), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void modifyMediaShare(MediaShare mediaShare) {

        if (Util.getNetworkState(mContext)) {

            if (mediaShare.checkPermissionToOperate()) {
                mDialog = ProgressDialog.show(mContext, null, getString(R.string.operating_title), true, false);

                String requestData = mediaShare.createToggleShareStateRequestData();

                FNAS.modifyRemoteMediaShare(mContext, mediaShare, requestData);
            } else {
                Toast.makeText(mContext, getString(R.string.no_operate_media_share_permission), Toast.LENGTH_SHORT).show();

            }

        } else {
            Toast.makeText(mContext, mContext.getString(R.string.no_network), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void deleteMediaShare(MediaShare mediaShare) {

        if (Util.getNetworkState(mContext)) {

            if (mediaShare.checkPermissionToOperate()) {
                mDialog = ProgressDialog.show(mContext, null, getString(R.string.operating_title), true, false);

                FNAS.deleteRemoteMediaShare(mContext, mediaShare);
            } else {
                Toast.makeText(mContext, getString(R.string.no_operate_media_share_permission), Toast.LENGTH_SHORT).show();

            }

        } else {
            Toast.makeText(mContext, mContext.getString(R.string.no_network), Toast.LENGTH_SHORT).show();
        }

    }


    private class MyAdapter extends PagerAdapter {


        @Override
        public CharSequence getPageTitle(int position) {
            return "选X项" + position;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View view = pageList.get(position).getView();

            container.addView(view);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);

        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

    }


}