package com.winsun.fruitmix.user.datasource;

import com.winsun.fruitmix.R;
import com.winsun.fruitmix.callback.BaseLoadDataCallback;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.http.BaseRemoteDataSourceImpl;
import com.winsun.fruitmix.http.HttpRequest;
import com.winsun.fruitmix.http.HttpRequestFactory;
import com.winsun.fruitmix.http.HttpResponse;
import com.winsun.fruitmix.http.IHttpUtil;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.parser.RemoteCurrentUserParser;
import com.winsun.fruitmix.parser.RemoteInsertUserParser;
import com.winsun.fruitmix.parser.RemoteUserHomeParser;
import com.winsun.fruitmix.system.setting.SystemSettingDataSource;
import com.winsun.fruitmix.user.User;
import com.winsun.fruitmix.model.operationResult.OperationSuccess;
import com.winsun.fruitmix.parser.RemoteLoginUsersParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/7/7.
 */

public class UserRemoteDataSourceImpl extends BaseRemoteDataSourceImpl implements UserRemoteDataSource {

    public static final String TAG = UserRemoteDataSourceImpl.class.getSimpleName();

    public static final String USER_PARAMETER = "/users";

    public static final String USER_HOME_PARAMETER = "/drives";

    private SystemSettingDataSource systemSettingDataSource;

    public UserRemoteDataSourceImpl(IHttpUtil iHttpUtil, HttpRequestFactory httpRequestFactory, SystemSettingDataSource systemSettingDataSource) {
        super(iHttpUtil, httpRequestFactory);

        this.systemSettingDataSource = systemSettingDataSource;
    }

    @Override
    public void insertUser(String userName, String userPwd, BaseOperateDataCallback<User> callback) {

        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("username", userName);
            jsonObject.put("password", userPwd);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String body = jsonObject.toString();

        HttpRequest httpRequest = httpRequestFactory.createHttpPostRequest(USER_PARAMETER, body);

        wrapper.operateCall(httpRequest, callback, new RemoteInsertUserParser(), R.string.create_user);

    }

    @Override
    public void getUsers(final BaseLoadDataCallback<User> callback) {

        final List<User> users = new ArrayList<>();

        HttpRequest httpRequest = httpRequestFactory.createHttpGetRequest(USER_PARAMETER + "/" + systemSettingDataSource.getCurrentLoginUserUUID());

        wrapper.loadCall(httpRequest, new BaseLoadDataCallback<User>() {

            @Override
            public void onSucceed(List<User> data, OperationResult operationResult) {

                users.addAll(data);

                getOtherUsers(users, callback);

            }

            @Override
            public void onFail(OperationResult operationResult) {

                getOtherUsers(users, callback);

            }
        }, new RemoteCurrentUserParser());

    }

    private void getOtherUsers(final List<User> users, final BaseLoadDataCallback<User> callback) {
        HttpRequest loginHttpRequest = httpRequestFactory.createGetRequestByPathWithoutToken(USER_PARAMETER);

        wrapper.loadCall(loginHttpRequest, new BaseLoadDataCallback<User>() {

            @Override
            public void onSucceed(List<User> data, OperationResult operationResult) {

                addDifferentUsers(users, data);

                callback.onSucceed(users, operationResult);
            }

            @Override
            public void onFail(OperationResult operationResult) {

                callback.onSucceed(users, new OperationSuccess());

            }
        }, new RemoteLoginUsersParser());
    }


    private void addDifferentUsers(List<User> users, List<User> otherUsers) {
        for (User otherUser : otherUsers) {
            int i;
            for (i = 0; i < users.size(); i++) {
                if (otherUser.getUuid().equals(users.get(i).getUuid())) {
                    break;
                }
            }
            if (i >= users.size()) {
                users.add(otherUser);
            }
        }
    }

    @Override
    public String getCurrentUserHome() {

        HttpRequest httpRequest = httpRequestFactory.createHttpGetRequest(USER_HOME_PARAMETER);

        try {
            HttpResponse response = iHttpUtil.remoteCall(httpRequest);

            RemoteUserHomeParser parser = new RemoteUserHomeParser();
            return parser.parse(response.getResponseData());

        } catch (IOException e) {
            e.printStackTrace();

        } catch (JSONException e) {
            e.printStackTrace();

        }

        return "";
    }
}
