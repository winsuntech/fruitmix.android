package com.winsun.fruitmix.system.setting;

import android.content.Context;
import android.content.SharedPreferences;

import com.winsun.fruitmix.util.Util;

/**
 * Created by Administrator on 2017/7/18.
 */

public class SystemSettingDataSource {

    private SharedPreferences sharedPreferences;

    private String showAutoUploadDialogKey = "showAutoUploadDialog";

    public static final String CURRENT_UPLOAD_USER_UUID = "current_upload_user_uuid";

    public static final String CURRENT_LOGIN_USER_UUID = "current_login_user_uuid";

    public static final String CURRENT_LOGIN_TOKEN = "current_login_token";

    public static final String CURRENT_LOGIN_STATION_ID = "current_login_station_id";

    public static final String CURRENT_EQUIPMENT_IP = "current_equipment_ip";

    private static SystemSettingDataSource instance;

    static SystemSettingDataSource getInstance(Context context) {

        if (instance == null)
            instance = new SystemSettingDataSource(context);

        return instance;
    }

    public void destroyInstance() {
        instance = null;
    }

    private SystemSettingDataSource(Context context) {

        sharedPreferences = context.getSharedPreferences(Util.FRUITMIX_SHAREDPREFERENCE_NAME, Context.MODE_PRIVATE);

    }

    public String getCurrentUploadUserUUID() {

        return sharedPreferences.getString(CURRENT_UPLOAD_USER_UUID, "");
    }

    public void setCurrentUploadUserUUID(String currentUploadUserUUID) {

        SharedPreferences.Editor editor;

        editor = sharedPreferences.edit();
        editor.putString(CURRENT_UPLOAD_USER_UUID, currentUploadUserUUID);
        editor.apply();
    }

    public boolean getAutoUploadOrNot() {

        return sharedPreferences.getBoolean(Util.AUTO_UPLOAD_OR_NOT, true);
    }

    public void setAutoUploadOrNot(boolean autoUploadOrNot) {

        SharedPreferences.Editor editor;

        editor = sharedPreferences.edit();
        editor.putBoolean(Util.AUTO_UPLOAD_OR_NOT, autoUploadOrNot);
        editor.apply();
    }

    public void setShowAutoUploadDialog(boolean showAutoUploadDialog) {

        SharedPreferences.Editor editor;

        editor = sharedPreferences.edit();
        editor.putBoolean(showAutoUploadDialogKey, showAutoUploadDialog);
        editor.apply();

    }

    public boolean getShowAutoUploadDialog() {

        return sharedPreferences.getBoolean(showAutoUploadDialogKey, false);

    }

    public String getCurrentLoginUserUUID() {

        return sharedPreferences.getString(CURRENT_LOGIN_USER_UUID, "");

    }

    public void setCurrentLoginUserUUID(String currentLoginUserUUID) {

        SharedPreferences.Editor editor;

        editor = sharedPreferences.edit();
        editor.putString(CURRENT_LOGIN_USER_UUID, currentLoginUserUUID);
        editor.apply();

    }

    public String getCurrentLoginToken() {

        return sharedPreferences.getString(CURRENT_LOGIN_TOKEN, "");

    }

    public void setCurrentLoginToken(String token) {

        SharedPreferences.Editor editor;

        editor = sharedPreferences.edit();
        editor.putString(CURRENT_LOGIN_TOKEN, token);
        editor.apply();

    }

    public String getCurrentLoginStationID() {

        return sharedPreferences.getString(CURRENT_LOGIN_STATION_ID, "");

    }

    public void setCurrentLoginStationID(String currentLoginUserUUID) {

        SharedPreferences.Editor editor;

        editor = sharedPreferences.edit();
        editor.putString(CURRENT_LOGIN_STATION_ID, currentLoginUserUUID);
        editor.apply();

    }

    public String getCurrentEquipmentIp() {

        return sharedPreferences.getString(CURRENT_EQUIPMENT_IP, "");

    }

    public void setCurrentEquipmentIp(String currentEquipmentIp) {

        SharedPreferences.Editor editor;

        editor = sharedPreferences.edit();
        editor.putString(CURRENT_EQUIPMENT_IP, currentEquipmentIp);
        editor.apply();

    }


}