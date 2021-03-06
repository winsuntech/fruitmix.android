package com.winsun.fruitmix;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.init.system.InitSystem;
import com.winsun.fruitmix.login.InjectLoginUseCase;
import com.winsun.fruitmix.login.LoginUseCase;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.thread.manage.ThreadManagerImpl;
import com.winsun.fruitmix.util.LocalCache;
import com.winsun.fruitmix.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by Administrator on 2016/5/9.
 */
public class SplashScreenActivity extends AppCompatActivity {

    public static final String TAG = "SplashScreenActivity";

    private String mUuid;
    private String mGateway;
    private String mToken;

    private Context mContext;

    public static final int WELCOME = 0x0010;

    public static final int DELAY_TIME_MILLISECOND = 3 * 1000;

    private CustomHandler mHandler;

    private boolean loginWithNoParamResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        mContext = this;

        InitSystem.initSystem(this);

 /*       String[] items = new String[]{"这是一串测试用的字符串，用于测试一行能放多少字符串", "这是一串测试用的字符串，用于测试一行能放多少字符串"};

        AlertDialog dialog;

        final SelectItem selectItem = new SelectItem();
        selectItem.setSelectItemPosition(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("选择一台wisnuc")
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        selectItem.setSelectItemPosition(which);

                    }
                }).setPositiveButton(getString(R.string.login), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Log.d(TAG, "onClick: selectItemPosition: " + selectItem.getSelectItemPosition());

                        dialog.dismiss();

                    }
                }).setCancelable(false);

        dialog = builder.create();

        dialog.show();*/

        final LoginUseCase loginUseCase = InjectLoginUseCase.provideLoginUseCase(this);

        loginWithNoParamInThread(loginUseCase);

//        FNAS.retrieveLocalMedia(mContext);

/*        DBUtils dbUtils = DBUtils.getInstance(this);
        LocalCache.LocalLoggedInUsers.addAll(dbUtils.getAllLoggedInUser());

        Log.i(TAG, "onCreate: LocalLoggedInUsers size: " + LocalCache.LocalLoggedInUsers.size());

        mGateway = LocalCache.getGateway(mContext);
        mUuid = LocalCache.getUserUUID(mContext);
        mToken = LocalCache.getToken(mContext);
        String mDeviceID = LocalCache.getGlobalData(mContext, Util.DEVICE_ID_MAP_NAME);

        if (!mUuid.isEmpty() && mGateway != null && mToken != null) {

            Util.loginType = LoginType.SPLASH_SCREEN;

            FNAS.Gateway = mGateway;
            FNAS.JWT = mToken;
            LocalCache.DeviceID = mDeviceID;
            FNAS.userUUID = mUuid;

            FNAS.retrieveUser(this);
        }*/

//        mHandler = new CustomHandler(this);
//        mHandler.sendEmptyMessageDelayed(WELCOME, DELAY_TIME_MILLISECOND);

    }

    private void loginWithNoParamInThread(LoginUseCase loginUseCase) {
        loginUseCase.loginWithNoParam(new BaseOperateDataCallback<Boolean>() {
            @Override
            public void onSucceed(Boolean data, OperationResult result) {

                loginWithNoParamResult = true;

                welcome();
            }

            @Override
            public void onFail(OperationResult result) {

                loginWithNoParamResult = false;

                welcome();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        MobclickAgent.onPageStart(TAG);
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        MobclickAgent.onPageEnd(TAG);
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mContext = null;
    }

    @Override
    public void onBackPressed() {
//        mHandler.removeMessages(WELCOME);
        finish();
    }

    private void welcome() {

        Intent intent = new Intent();

        if (loginWithNoParamResult) {

            Log.d(TAG, "welcome: start nav pager");

            intent.setClass(SplashScreenActivity.this, NavPagerActivity.class);
        } else {
            intent.setClass(SplashScreenActivity.this, EquipmentSearchActivity.class);
            intent.putExtra(Util.KEY_SHOULD_STOP_SERVICE, true);
        }

        startActivity(intent);
        finish();
    }

    private static class CustomHandler extends Handler {

        WeakReference<SplashScreenActivity> weakReference = null;

        CustomHandler(SplashScreenActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WELCOME:
                    weakReference.get().welcome();
                    break;
                default:
            }
        }
    }

    private void writeOneByteToFile() {

        File file = new File(getExternalFilesDir(null), "test");

        FileOutputStream fileOutputStream = null;

        try {

            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);

            byte[] bytes = {1};

            fileOutputStream.write(bytes);
            fileOutputStream.flush();

            Toast.makeText(mContext, " 创建成功", Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(mContext, " 创建失败", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, " 创建失败", Toast.LENGTH_SHORT).show();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}