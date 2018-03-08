package com.winsun.fruitmix.group.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.winsun.fruitmix.PersonInfoActivity;
import com.winsun.fruitmix.R;
import com.winsun.fruitmix.databinding.ActivityGroupListBinding;
import com.winsun.fruitmix.eventbus.MqttMessageEvent;
import com.winsun.fruitmix.group.data.source.GroupRepository;
import com.winsun.fruitmix.group.data.source.InjectGroupDataSource;
import com.winsun.fruitmix.group.data.viewmodel.GroupListViewModel;
import com.winsun.fruitmix.group.interfaces.GoToBindWeChatListener;
import com.winsun.fruitmix.group.presenter.GroupListPresenter;
import com.winsun.fruitmix.interfaces.IShowHideFragmentListener;
import com.winsun.fruitmix.interfaces.Page;
import com.winsun.fruitmix.mqtt.InjectMqttUseCase;
import com.winsun.fruitmix.mqtt.MqttUseCase;
import com.winsun.fruitmix.system.setting.InjectSystemSettingDataSource;
import com.winsun.fruitmix.system.setting.SystemSettingDataSource;
import com.winsun.fruitmix.token.data.InjectTokenRemoteDataSource;
import com.winsun.fruitmix.token.data.TokenDataSource;
import com.winsun.fruitmix.user.User;
import com.winsun.fruitmix.user.datasource.InjectUser;
import com.winsun.fruitmix.user.datasource.UserDataRepository;
import com.winsun.fruitmix.util.Util;
import com.winsun.fruitmix.viewmodel.LoadingViewModel;
import com.winsun.fruitmix.viewmodel.NoContentViewModel;

import java.util.List;
import java.util.Map;

public class GroupListPage implements Page, IShowHideFragmentListener, GroupListPageView,GoToBindWeChatListener {

    public static final String TAG = GroupListPage.class.getSimpleName();

    private View view;

    private RecyclerView recyclerView;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private GroupListPresenter groupListPresenter;

    private Activity containerActivity;

    public static final int CREATE_GROUP_REQUEST_CODE = 1;

    public static final int GROUP_CONTENT_REQUEST_CODE = 2;

    private boolean startGroupContentActivity = false;

    public GroupListPage(Activity activity) {

        containerActivity = activity;

        ActivityGroupListBinding binding = ActivityGroupListBinding.inflate(LayoutInflater.from(activity), null, false);

        mSwipeRefreshLayout = binding.swipeRefreshLayout;

        initSwipeRefreshLayout();

        final LoadingViewModel loadingViewModel = new LoadingViewModel();

        binding.setLoadingViewModel(loadingViewModel);

        final NoContentViewModel noContentViewModel = new NoContentViewModel();
        noContentViewModel.setNoContentImgResId(R.drawable.no_file);
        noContentViewModel.setNoContentText("没有内容");

        binding.setNoContentViewModel(noContentViewModel);

        binding.setBindWeChatListener(this);

        final GroupListViewModel groupListViewModel = new GroupListViewModel();

        binding.setGroupListViewModel(groupListViewModel);

        view = binding.getRoot();

        final GroupRepository groupRepository = InjectGroupDataSource.provideGroupRepository(containerActivity);

        SystemSettingDataSource systemSettingDataSource = InjectSystemSettingDataSource.provideSystemSettingDataSource(containerActivity);

        String currentLoginUserUUID = systemSettingDataSource.getCurrentLoginUserUUID();

        UserDataRepository userDataRepository = InjectUser.provideRepository(containerActivity);

        User currentUser = userDataRepository.getUserByUUID(currentLoginUserUUID);

        groupRepository.setCurrentUser(currentUser);

        TokenDataSource tokenDataSource = InjectTokenRemoteDataSource.provideTokenDataSource(containerActivity);

        MqttUseCase mqttUseCase = InjectMqttUseCase.provideInstance(containerActivity);

        groupListPresenter = new GroupListPresenter(this, currentUser, tokenDataSource,
                groupRepository, loadingViewModel, noContentViewModel, groupListViewModel, userDataRepository,
                systemSettingDataSource, mqttUseCase);

        recyclerView = binding.groupRecyclerview;

        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(groupListPresenter.getGroupListAdapter());

    }

    private void initSwipeRefreshLayout() {

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                Log.d(TAG, "onRefresh: swipe refresh");

                refreshView();

            }
        });
    }

    @Override
    public View getView() {
        return view;
    }

    public void onResume() {

        if (startGroupContentActivity) {

            groupListPresenter.refreshGroupUsingMemoryCache();

            startGroupContentActivity = false;

        }

    }

    @Override
    public void refreshView() {

        groupListPresenter.refreshGroups();

    }

    @Override
    public void finishSwipeRefreshAnimation() {

        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public void refreshViewForce() {

    }

    public void handleMqttMessage(MqttMessageEvent mqttMessageEvent) {

        groupListPresenter.handleMqttMessageEvent(mqttMessageEvent);

    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {

    }

    @Override
    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {

    }

    @Override
    public void onDestroy() {

        groupListPresenter.onDestroyView();

        containerActivity = null;
    }

    @Override
    public void show() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void gotoGroupContentActivity(String groupUUID) {

        Intent intent = new Intent(containerActivity, GroupContentActivity.class);
        intent.putExtra(GroupContentActivity.GROUP_UUID, groupUUID);

        startGroupContentActivity = true;

        containerActivity.startActivityForResult(intent, GROUP_CONTENT_REQUEST_CODE);

    }

    @Override
    public Context getContext() {
        return containerActivity;
    }

    @Override
    public String getString(int resID, Object... formatArgs) {
        return containerActivity.getString(resID, formatArgs);
    }

    @Override
    public String getString(int resID) {
        return containerActivity.getString(resID);
    }

    @Override
    public boolean canEnterSelectMode() {
        return false;
    }

    public void showToast(String message){

        Toast.makeText(containerActivity,message,Toast.LENGTH_SHORT).show();

    }

    @Override
    public void toBindWeChat() {

        PersonInfoActivity.startPersonInfo(containerActivity);

    }
}
