package com.winsun.fruitmix.user.manage;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;
import android.widget.TextView;

import com.winsun.fruitmix.BaseActivity;
import com.winsun.fruitmix.CreateUserActivity;
import com.winsun.fruitmix.R;
import com.winsun.fruitmix.databinding.ActivityUserManageBinding;
import com.winsun.fruitmix.equipment.EquipmentItemViewModel;
import com.winsun.fruitmix.equipment.data.InjectEquipment;
import com.winsun.fruitmix.file.data.station.InjectStationFileRepository;
import com.winsun.fruitmix.http.InjectHttp;
import com.winsun.fruitmix.stations.InjectStation;
import com.winsun.fruitmix.system.setting.InjectSystemSettingDataSource;
import com.winsun.fruitmix.thread.manage.ThreadManager;
import com.winsun.fruitmix.thread.manage.ThreadManagerImpl;
import com.winsun.fruitmix.user.datasource.InjectUser;
import com.winsun.fruitmix.user.manage.UserManagePresenterImpl;
import com.winsun.fruitmix.user.manage.UserManageView;
import com.winsun.fruitmix.user.manage.UserMangePresenter;
import com.winsun.fruitmix.util.Util;
import com.winsun.fruitmix.viewmodel.ToolbarViewModel;

public class UserManageActivity extends BaseActivity implements UserManageView {

    public static final String TAG = "UserManageActivity";

    ListView mUserListView;

    TextView mUserListEmpty;

    private UserMangePresenter userMangePresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityUserManageBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_user_manage);

        Toolbar mToolbar = binding.toolbarLayout.toolbar;

        if (Util.checkRunningOnLollipopOrHigher()) {
            mToolbar.setElevation(0f);
        }

        binding.toolbarLayout.title.setTextColor(ContextCompat.getColor(this,R.color.white));

        mToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.login_ui_blue));

        Util.setStatusBarColor(this, R.color.login_ui_blue);

        UserManageViewModel userManageViewModel = new UserManageViewModel();

        mUserListView = binding.userList;

        mUserListEmpty = binding.userListEmpty;

        binding.setUserManageViewModel(userManageViewModel);

        EquipmentItemViewModel equipmentItemViewModel = new EquipmentItemViewModel();

        equipmentItemViewModel.showEquipment.set(true);

        binding.setEquipmentItemViewModel(equipmentItemViewModel);

        userMangePresenter = new UserManagePresenterImpl(this, equipmentItemViewModel, userManageViewModel,
                InjectUser.provideRepository(this), InjectEquipment.provideEquipmentDataSource(this),
                InjectSystemSettingDataSource.provideSystemSettingDataSource(this),InjectStation.provideStationDataSource(this),
                InjectHttp.provideImageGifLoaderInstance(this).getImageLoader(this));

        binding.setUserPresenter(userMangePresenter);

        ToolbarViewModel toolbarViewModel = new ToolbarViewModel();

        toolbarViewModel.titleText.set(getString(R.string.user_manage));

        toolbarViewModel.navigationIconResId.set(R.drawable.ic_back);

        toolbarViewModel.setBaseView(this);

        binding.setToolbarViewModel(toolbarViewModel);

        mUserListView.setAdapter(userMangePresenter.getAdapter());

        userMangePresenter.refreshView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        userMangePresenter.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Util.KEY_CREATE_USER_REQUEST_CODE && resultCode == RESULT_OK) {
            userMangePresenter.refreshView();
        }
    }

    @Override
    public void gotoCreateUserActivity() {
        Intent intent = new Intent(this, CreateUserActivity.class);
        startActivityForResult(intent, Util.KEY_CREATE_USER_REQUEST_CODE);
    }

    public class UserManageViewModel {

        public final ObservableBoolean showUserListEmpty = new ObservableBoolean(false);

        public final ObservableBoolean showUserListView = new ObservableBoolean(true);

    }

}
