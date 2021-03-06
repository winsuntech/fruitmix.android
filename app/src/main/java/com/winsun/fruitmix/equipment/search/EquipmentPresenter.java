package com.winsun.fruitmix.equipment.search;

import android.databinding.ViewDataBinding;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.winsun.fruitmix.R;
import com.winsun.fruitmix.callback.ActiveView;
import com.winsun.fruitmix.callback.BaseLoadDataCallback;
import com.winsun.fruitmix.callback.BaseLoadDataCallbackWrapper;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.component.UserAvatar;
import com.winsun.fruitmix.databinding.EquipmentItemBinding;
import com.winsun.fruitmix.databinding.EquipmentUserItemBinding;
import com.winsun.fruitmix.equipment.search.data.EquipmentDataSource;
import com.winsun.fruitmix.equipment.search.data.EquipmentSearchManager;
import com.winsun.fruitmix.login.LoginUseCase;
import com.winsun.fruitmix.equipment.search.data.Equipment;
import com.winsun.fruitmix.equipment.search.data.EquipmentTypeInfo;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.user.User;
import com.winsun.fruitmix.viewholder.BindingViewHolder;
import com.winsun.fruitmix.viewmodel.LoadingViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Administrator on 2017/8/15.
 */

public class EquipmentPresenter implements ActiveView {

    public static final String TAG = EquipmentPresenter.class.getSimpleName();

    private EquipmentViewPagerAdapter equipmentViewPagerAdapter;

    private EquipmentUserRecyclerViewAdapter equipmentUserRecyclerViewAdapter;

    private EquipmentSearchView equipmentSearchView;

    private LoadingViewModel loadingViewModel;

    private EquipmentSearchViewModel equipmentSearchViewModel;

    private final List<Equipment> mUserLoadedEquipments = new ArrayList<>();

    private List<String> equipmentSerialNumbers = new ArrayList<>();

    private List<String> equipmentIps = new ArrayList<>();

    private Equipment currentEquipment;

    private Map<String, List<User>> mMapKeyIsIpValueIsUsers;

    private CustomHandler mHandler;

    private static final int DATA_CHANGE = 0x0001;

    private static final int RETRY_GET_DATA = 0x0002;

    private static final int DISCOVERY_TIMEOUT = 0x0003;

    private static final int RESUME_DISCOVERY = 0x0004;

    private static final int RETRY_DELAY_MILLISECOND = 20 * 1000;

    private static final int DISCOVERY_TIMEOUT_TIME = 6 * 1000;
    private static final int RESUME_DISCOVERY_INTERVAL = 3 * 1000;

    private EquipmentSearchManager mEquipmentSearchManager;

    private EquipmentDataSource mEquipmentDataSource;

    private LoginUseCase loginUseCase;

    private ImageLoader imageLoader;

    private Random random;

    private int preAvatarBgColor = 0;

    private boolean onPause = false;

    private boolean hasFindEquipment = false;

    private boolean hasRefreshFirstEquipmentUsers = false;

    private class CustomHandler extends Handler {

        WeakReference<EquipmentPresenter> weakReference = null;

        private boolean hasForceRefresh = false;

        CustomHandler(EquipmentPresenter presenter, Looper looper) {
            super(looper);
            weakReference = new WeakReference<>(presenter);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DATA_CHANGE:

                    EquipmentViewPagerAdapter adapter = weakReference.get().equipmentViewPagerAdapter;

                    if (!hasFindEquipment)
                        hasFindEquipment = true;

                    if (equipmentSearchView == null)
                        return;

                    handleFindEquipment(adapter);

                    break;

                case RETRY_GET_DATA:

                    Equipment equipment = (Equipment) msg.obj;

                    Log.d(TAG, "handleMessage: retry get user equipment host0: " + equipment.getHosts().get(0));

                    getEquipmentInfo(equipment);

                    break;

                case DISCOVERY_TIMEOUT:

                    if (!hasFindEquipment) {

                        loadingViewModel.showLoading.set(false);

                        equipmentSearchViewModel.showEquipmentViewPager.set(true);
                        equipmentSearchViewModel.showEquipmentViewPagerIndicator.set(false);
                        equipmentSearchViewModel.showEquipmentUsers.set(false);

                        equipmentViewPagerAdapter.setEquipments(Collections.singletonList(Equipment.NULL));
                        equipmentViewPagerAdapter.notifyDataSetChanged();
                    } else {

                        handleFindEquipment(weakReference.get().equipmentViewPagerAdapter);

                    }

                    break;

                case RESUME_DISCOVERY:

                    if (!hasFindEquipment) {

                        equipmentSearchViewModel.showEquipmentViewPager.set(false);
                        equipmentSearchViewModel.showEquipmentViewPagerIndicator.set(false);
                        equipmentSearchViewModel.showEquipmentUsers.set(false);

                        loadingViewModel.showLoading.set(true);

                    } else {

                        handleFindEquipment(weakReference.get().equipmentViewPagerAdapter);

                    }

                    mHandler.sendEmptyMessageDelayed(DISCOVERY_TIMEOUT, DISCOVERY_TIMEOUT_TIME);

                    break;

                default:
            }
        }

        private void handleFindEquipment(EquipmentViewPagerAdapter adapter) {
            if (loadingViewModel.showLoading.get()) {
                loadingViewModel.showLoading.set(false);
            }

            if (!hasForceRefresh) {
                hasForceRefresh = true;

                adapter.setForceRefresh();
            }

            equipmentSearchViewModel.showEquipmentViewPager.set(true);
            equipmentSearchViewModel.showEquipmentViewPagerIndicator.set(true);


            adapter.setEquipments(mUserLoadedEquipments);

            adapter.notifyDataSetChanged();

            if (equipmentSearchView.getCurrentViewPagerItem() == 0 && !hasRefreshFirstEquipmentUsers) {

                refreshEquipment(0);

                hasRefreshFirstEquipmentUsers = true;

            }

        }
    }

    public EquipmentPresenter(LoadingViewModel loadingViewModel, EquipmentSearchViewModel equipmentSearchViewModel, EquipmentSearchView equipmentSearchView,
                              EquipmentDataSource equipmentDataSource, LoginUseCase loginUseCase, ImageLoader imageLoader) {
        this.loadingViewModel = loadingViewModel;
        this.equipmentSearchViewModel = equipmentSearchViewModel;
        this.equipmentSearchView = equipmentSearchView;
        this.mEquipmentDataSource = equipmentDataSource;
        this.loginUseCase = loginUseCase;

        mMapKeyIsIpValueIsUsers = new HashMap<>();

        mHandler = new CustomHandler(this, Looper.getMainLooper());

        random = new Random();

        equipmentUserRecyclerViewAdapter = new EquipmentUserRecyclerViewAdapter();
        equipmentViewPagerAdapter = new EquipmentViewPagerAdapter();

    }

    public EquipmentUserRecyclerViewAdapter getEquipmentUserRecyclerViewAdapter() {
        return equipmentUserRecyclerViewAdapter;
    }

    public EquipmentViewPagerAdapter getEquipmentViewPagerAdapter() {
        return equipmentViewPagerAdapter;
    }

    public void onCreate() {

        // handle for huawei mate9 search result has no ip

 /*       getEquipmentTypeInfo(new Equipment("", Collections.singletonList("192.168.0.81"), 0));

        hasFindEquipment = true;*/

        mHandler.sendEmptyMessage(RESUME_DISCOVERY);

    }

    public void onResume() {

        onPause = false;

        startDiscovery();

        Log.d(TAG, "onResume: start discovery");
    }

    public void onPause() {

        onPause = true;

        mHandler.removeMessages(RETRY_GET_DATA);
        mHandler.removeMessages(DATA_CHANGE);

        stopDiscovery();

        Log.d(TAG, "onPause: stop discovery");

    }

    public void onDestroy() {

        mHandler.removeMessages(DISCOVERY_TIMEOUT);
        mHandler.removeMessages(RESUME_DISCOVERY);

        mHandler.removeMessages(RETRY_GET_DATA);
        mHandler.removeMessages(DATA_CHANGE);

        mHandler = null;

        equipmentSearchView = null;

    }

    @Override
    public boolean isActive() {
        return equipmentSearchView != null;
    }

    public void handleInputIpbyByUser(String ip) {
        List<String> hosts = new ArrayList<>();
        hosts.add(ip);

        Equipment equipment = new Equipment("Winsuc Appliction " + ip, hosts, 6666);
        getEquipmentInfo(equipment);
    }

    public void refreshEquipment(int position) {

        if (mUserLoadedEquipments.size() <= position)
            return;

        currentEquipment = mUserLoadedEquipments.get(position);

        int state = currentEquipment.getState();

        if (state == EquipmentDataSource.EQUIPMENT_READY) {

            equipmentSearchViewModel.showEquipmentUsers.set(true);

            int size = equipmentUserRecyclerViewAdapter.mCurrentUsers.size();

            equipmentUserRecyclerViewAdapter.clearCurrentUsers();

            equipmentUserRecyclerViewAdapter.notifyItemRangeRemoved(0, size);

            List<User> users = mMapKeyIsIpValueIsUsers.get(currentEquipment.getHosts().get(0));

            equipmentUserRecyclerViewAdapter.setCurrentUsers(users);

            equipmentUserRecyclerViewAdapter.notifyItemRangeInserted(0, users.size());

        } else {

            equipmentSearchViewModel.showEquipmentUsers.set(false);

            equipmentSearchViewModel.setEquipmentStateCode(state);

            if (state == EquipmentDataSource.EQUIPMENT_SYSTEM_ERROR) {

                equipmentSearchViewModel.equipmentState.set(equipmentSearchView.getContext().getString(R.string.equipment_system_error));

                equipmentSearchViewModel.equipmentStateIcon.set(R.drawable.initial_equipment);

            } else if (state == EquipmentDataSource.EQUIPMENT_UNINITIALIZED) {

                equipmentSearchViewModel.equipmentState.set(equipmentSearchView.getContext().getString(R.string.initial_equipment));

                equipmentSearchViewModel.equipmentStateIcon.set(R.drawable.initial_equipment);

                equipmentSearchViewModel.setIp(currentEquipment.getHosts().get(0));
                equipmentSearchViewModel.setEquipmentName(currentEquipment.getEquipmentName());

            } else if (state == EquipmentDataSource.EQUIPMENT_MAINTENANCE) {

                equipmentSearchViewModel.equipmentState.set(equipmentSearchView.getContext().getString(R.string.maintenance));

                equipmentSearchViewModel.equipmentStateIcon.set(R.drawable.maintenance_enter_icon);

            }

        }

    }

    private void startDiscovery() {

        mEquipmentSearchManager = equipmentSearchView.getEquipmentSearchManager();

        mEquipmentSearchManager.startDiscovery(new EquipmentSearchManager.IEquipmentDiscoveryListener() {
            @Override
            public void call(Equipment equipment) {

                getEquipmentInfo(equipment);

            }
        });

        mHandler.sendEmptyMessageDelayed(DISCOVERY_TIMEOUT, DISCOVERY_TIMEOUT_TIME);

    }

    private void stopDiscovery() {

        mEquipmentSearchManager = equipmentSearchView.getEquipmentSearchManager();

        mEquipmentSearchManager.stopDiscovery();
    }

    private void getEquipmentInfo(final Equipment equipment) {

        if (equipment.getSerialNumber().length() != 0) {

            if (equipmentSerialNumbers.contains(equipment.getSerialNumber())) {

                Log.d(TAG, "getEquipmentTypeInfo: serial number has founded: " + equipment.getSerialNumber());

                return;
            } else {
                equipmentSerialNumbers.add(equipment.getSerialNumber());
            }

        }

        if (equipmentIps.contains(equipment.getHosts().get(0))) {

            Log.d(TAG, "getEquipmentTypeInfo: host has founded: " + equipment.getHosts().get(0));

            return;
        } else
            equipmentIps.add(equipment.getHosts().get(0));

        synchronized (mUserLoadedEquipments) {

            for (Equipment loadedEquipment : mUserLoadedEquipments) {
                if (loadedEquipment.getSerialNumber().equals(equipment.getSerialNumber())) {

                    Log.d(TAG, "getEquipmentTypeInfo: second check in user loaded equipments,serial number has founded: " + equipment.getSerialNumber());

                    return;
                } else if (loadedEquipment.getHosts().contains(equipment.getHosts().get(0))) {

                    Log.d(TAG, "getEquipmentTypeInfo: second check in user loaded equipments,host has founded: " + equipment.getHosts().get(0));

                    return;
                }

            }

            getEquipmentInfoInThread(equipment);

        }

    }

    private void getEquipmentInfoInThread(final Equipment equipment) {
        mEquipmentDataSource.getEquipmentTypeInfo(equipment.getHosts().get(0), new BaseLoadDataCallbackWrapper<>(new BaseLoadDataCallback<EquipmentTypeInfo>() {
            @Override
            public void onSucceed(List<EquipmentTypeInfo> data, OperationResult operationResult) {

                EquipmentTypeInfo equipmentTypeInfo = data.get(0);

                Log.d(TAG, "onSucceed: equipment info: " + equipmentTypeInfo);

                if (equipmentTypeInfo == null) {
                    equipmentTypeInfo = new EquipmentTypeInfo();
                }

                equipment.setEquipmentTypeInfo(equipmentTypeInfo);

                checkEquipmentState(equipment);

            }

            @Override
            public void onFail(OperationResult operationResult) {

                Log.d(TAG, "onFail: get equipment info");

                EquipmentTypeInfo equipmentTypeInfo = new EquipmentTypeInfo();

                equipment.setEquipmentTypeInfo(equipmentTypeInfo);

                checkEquipmentState(equipment);

            }
        }, this));
    }

    private void checkEquipmentState(final Equipment equipment) {

        mEquipmentDataSource.checkEquipmentState(equipment.getHosts().get(0), new BaseOperateDataCallback<Integer>() {
            @Override
            public void onSucceed(Integer data, OperationResult result) {

                switch (data) {
                    case EquipmentDataSource.EQUIPMENT_READY:

                        equipment.setState(EquipmentDataSource.EQUIPMENT_READY);

                        getUserInThread(equipment);
                        break;
                    case EquipmentDataSource.EQUIPMENT_UNINITIALIZED:

                        equipment.setState(EquipmentDataSource.EQUIPMENT_UNINITIALIZED);

                        mUserLoadedEquipments.add(equipment);

                        sendUpdateListMessage();

                        break;
                    case EquipmentDataSource.EQUIPMENT_MAINTENANCE:

                        removeAlreadyFoundEquipmentIp(equipment);

                        equipment.setState(EquipmentDataSource.EQUIPMENT_MAINTENANCE);

                        mUserLoadedEquipments.add(equipment);

                        sendUpdateListMessage();

                        break;
                    default:
                        Log.d(TAG, "onSucceed: equipment not ready or uninitialized,error,maintenance");
                }

            }

            @Override
            public void onFail(OperationResult operationResult) {

                handleRetrieveEquipmentInfoFail(equipment);

            }
        });

    }

    private void getUserInThread(final Equipment equipment) {
        mEquipmentDataSource.getUsersInEquipment(equipment, new BaseLoadDataCallbackWrapper<>(new BaseLoadDataCallback<User>() {
            @Override
            public void onSucceed(List<User> data, OperationResult operationResult) {

                handleRetrieveUsersSucceed(data, equipment);

            }

            @Override
            public void onFail(OperationResult operationResult) {

                handleRetrieveEquipmentInfoFail(equipment);
            }
        }, this));
    }

    private void handleRetrieveEquipmentInfoFail(Equipment equipment) {

        removeAlreadyFoundEquipmentIp(equipment);

        equipment.setState(EquipmentDataSource.EQUIPMENT_SYSTEM_ERROR);

        mUserLoadedEquipments.add(equipment);

        sendUpdateListMessage();

//        if (mHandler != null && !onPause) {
//            Message message = Message.obtain(mHandler, RETRY_GET_DATA, equipment);
//            mHandler.sendMessageDelayed(message, RETRY_DELAY_MILLISECOND);
//        }

    }


    private void handleRetrieveUsersSucceed(List<User> data, Equipment equipment) {
        if (data.isEmpty()) {

            removeAlreadyFoundEquipmentIp(equipment);

            equipment.setState(EquipmentDataSource.EQUIPMENT_MAINTENANCE);

            mUserLoadedEquipments.add(equipment);

            sendUpdateListMessage();

            return;

        }

        mUserLoadedEquipments.add(equipment);
        mMapKeyIsIpValueIsUsers.put(equipment.getHosts().get(0), data);

        Log.d(TAG, "EquipmentSearch: " + mMapKeyIsIpValueIsUsers.toString());

        sendUpdateListMessage();

    }

    private void removeAlreadyFoundEquipmentIp(Equipment equipment) {
        equipmentSerialNumbers.remove(equipment.getSerialNumber());
        equipmentIps.remove(equipment.getHosts().get(0));
    }

    private void sendUpdateListMessage() {
        //update list
        if (mHandler != null && !onPause)
            mHandler.sendEmptyMessage(DATA_CHANGE);
    }


    private class EquipmentViewPagerAdapter extends PagerAdapter {

        private List<Equipment> mEquipments;

        private boolean forceRefresh = false;

        EquipmentViewPagerAdapter() {
            mEquipments = new ArrayList<>();
        }

        void setEquipments(List<Equipment> equipments) {
            mEquipments.clear();
            mEquipments.addAll(equipments);
        }

        public void setForceRefresh() {
            forceRefresh = true;
        }

        @Override
        public int getCount() {
            return mEquipments.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            Log.d(TAG, "instantiateItem: position: " + position);

            EquipmentItemBinding binding = EquipmentItemBinding.inflate(LayoutInflater.from(container.getContext()), container, false);

            container.addView(binding.getRoot());

            EquipmentItemViewModel equipmentItemViewModel = new EquipmentItemViewModel();

            Equipment equipment = mEquipments.get(position);

            if (equipment.equals(Equipment.NULL)) {

                equipmentItemViewModel.showNoEquipment.set(true);
                equipmentItemViewModel.showEquipment.set(false);

//                binding.noEquipmentLayout.setVisibility(View.VISIBLE);
//                binding.equipmentLayout.setVisibility(View.GONE);

                initEquipmentViewModelDefaultBackgroundColor(container, equipmentItemViewModel);

                binding.research.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        stopDiscovery();
                        startDiscovery();

                    }
                });


            } else {

                equipmentItemViewModel.showNoEquipment.set(false);
                equipmentItemViewModel.showEquipment.set(true);

//                binding.noEquipmentLayout.setVisibility(View.GONE);
//                binding.equipmentLayout.setVisibility(View.VISIBLE);

                initEquipmentItemViewModel(container, equipmentItemViewModel, equipment);

            }

            binding.setEquipmentItemViewModel(equipmentItemViewModel);

            return binding.getRoot();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            Log.d(TAG, "destroyItem: position: " + position);

            container.removeView((View) object);

        }

        @Override
        public int getItemPosition(Object object) {

            if (forceRefresh) {

                forceRefresh = false;

                return POSITION_NONE;

            }

            return super.getItemPosition(object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private void initEquipmentViewModelDefaultBackgroundColor(ViewGroup container, EquipmentItemViewModel equipmentItemViewModel) {
        equipmentItemViewModel.cardBackgroundColorID.set(ContextCompat.getColor(container.getContext(), R.color.login_ui_blue));

        equipmentItemViewModel.backgroundColorID.set(R.color.equipment_ui_blue);

        equipmentSearchView.setBackgroundColor(R.color.equipment_ui_blue);
    }

    private void initEquipmentItemViewModel(ViewGroup container, EquipmentItemViewModel equipmentItemViewModel, Equipment equipment) {
        EquipmentTypeInfo equipmentTypeInfo = equipment.getEquipmentTypeInfo();

        if (equipmentTypeInfo != null) {

            String type = equipmentTypeInfo.getType(equipmentSearchView.getContext());

            equipmentItemViewModel.type.set(type);

            equipmentItemViewModel.label.set(equipmentTypeInfo.getFormatLabel(equipmentSearchView.getContext()));

            if (type.equals(EquipmentTypeInfo.WS215I)) {

                equipmentItemViewModel.equipmentIconID.set(R.drawable.equipment_215i);

                initEquipmentViewModelDefaultBackgroundColor(container, equipmentItemViewModel);

            } else {

                equipmentItemViewModel.equipmentIconID.set(R.drawable.virtual_machine);

                initEquipmentViewModelDefaultBackgroundColor(container, equipmentItemViewModel);

            }

        }

        List<String> hosts = equipment.getHosts();

        String and = container.getContext().getString(R.string.and);

        int size = hosts.size();

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {

            if (i != 0)
                builder.append(and);

            String host = hosts.get(i);

            builder.append(host);
        }

        equipmentItemViewModel.ip.set(builder.toString());
    }

    private class EquipmentUserRecyclerViewAdapter extends RecyclerView.Adapter<EquipmentUserViewHolder> {

        private List<User> mCurrentUsers;

        EquipmentUserRecyclerViewAdapter() {
            mCurrentUsers = new ArrayList<>();
        }

        public void clearCurrentUsers() {
            mCurrentUsers.clear();
        }

        public void setCurrentUsers(List<User> currentUsers) {

            for (User user : currentUsers) {

                if (!user.isDisabled())
                    mCurrentUsers.add(user);

            }

        }

        @Override
        public EquipmentUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            EquipmentUserItemBinding binding = EquipmentUserItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            return new EquipmentUserViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(EquipmentUserViewHolder holder, int position) {

            holder.refreshView(mCurrentUsers.get(position));

        }

        @Override
        public int getItemCount() {
            return mCurrentUsers.size();
        }

    }

    private class EquipmentUserViewHolder extends BindingViewHolder {

        private UserAvatar userAvatar;
        private TextView mChildName;

        private ViewGroup equipmentUserItemLayout;

        private EquipmentUserItemBinding binding;

        EquipmentUserViewHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);

            binding = (EquipmentUserItemBinding) viewDataBinding;

            userAvatar = binding.userAvatar;
            mChildName = binding.equipmentChildName;
            equipmentUserItemLayout = binding.equipmentUserItemLayout;
        }

        public void refreshView(final User user) {

            mChildName.setText(user.getFormatUserName(binding.getRoot().getContext()));

            if (user.getDefaultAvatarBgColor() == 0) {

                int avatarBgColor = random.nextInt(3) + 1;

                if (preAvatarBgColor != 0) {

                    if (avatarBgColor == preAvatarBgColor) {
                        if (avatarBgColor == 3) {
                            avatarBgColor--;
                        } else if (avatarBgColor == 1) {
                            avatarBgColor++;
                        } else {

                            if (random.nextBoolean()) {
                                avatarBgColor++;
                            } else {
                                avatarBgColor--;
                            }

                        }
                    }

                    preAvatarBgColor = avatarBgColor;

                } else {
                    preAvatarBgColor = avatarBgColor;
                }


                user.setDefaultAvatarBgColor(avatarBgColor);
            }

            userAvatar.setUser(user, imageLoader);

            equipmentUserItemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    loginWithUserInThread(user);

                }
            });

        }
    }


    private void loginWithUserInThread(final User user) {

        loginUseCase.loginWithUser(user, new BaseOperateDataCallback<Boolean>() {
            @Override
            public void onSucceed(final Boolean data, OperationResult result) {

                if (equipmentSearchView != null)
                    equipmentSearchView.handleLoginWithUserSucceed(data);

            }

            @Override
            public void onFail(OperationResult result) {

                if (equipmentSearchView != null)
                    equipmentSearchView.handleLoginWithUserFail(currentEquipment, user);

            }

        });
    }

}
