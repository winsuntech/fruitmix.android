package com.winsun.fruitmix.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.winsun.fruitmix.eventbus.OperationEvent;
import com.winsun.fruitmix.util.FNAS;
import com.winsun.fruitmix.util.LocalCache;
import com.winsun.fruitmix.util.OperationResultType;
import com.winsun.fruitmix.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class RetrieveTokenService extends IntentService {

    public static final String TAG = RetrieveTokenService.class.getSimpleName();

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_RETRIEVE_TOKEN = "com.winsun.fruitmix.services.action.retrieve.token";

    private static String EXTRA_GATEWAY = "com.winsun.fruitmix.services.gateway";

    private static String EXTRA_USER_UUID = "com.winsun.fruitmix.services.user.uuid";

    private static String EXTRA_USER_PASSWORD = "com.winsun.fruitmix.services.user.password";

    public RetrieveTokenService() {
        super("RetrieveTokenService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionRetrieveToken(Context context, String gateway, String userUUID, String userPassword) {
        Intent intent = new Intent(context, RetrieveTokenService.class);
        intent.setAction(ACTION_RETRIEVE_TOKEN);
        intent.putExtra(EXTRA_GATEWAY, gateway);
        intent.putExtra(EXTRA_USER_UUID, userUUID);
        intent.putExtra(EXTRA_USER_PASSWORD, userPassword);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_RETRIEVE_TOKEN.equals(action)) {
                String gateway = intent.getStringExtra(EXTRA_GATEWAY);
                String userUUID = intent.getStringExtra(EXTRA_USER_UUID);
                String userPassword = intent.getStringExtra(EXTRA_USER_PASSWORD);
                handleActionRetrieveToken(gateway, userUUID, userPassword);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionRetrieveToken(String gateway, String userUUID, String userPassword) {

        String str;

        OperationEvent operationEvent;

        try {

            str = FNAS.loadToken(this, gateway, userUUID, userPassword);

            if (str.length() > 0) {
                FNAS.JWT = new JSONObject(str).getString("token");
            }

            LocalCache.saveToken(FNAS.JWT);

            operationEvent = new OperationEvent(Util.REMOTE_TOKEN_RETRIEVED, OperationResultType.SUCCEED);

        } catch (Exception e) {
            e.printStackTrace();

            operationEvent = new OperationEvent(Util.REMOTE_TOKEN_RETRIEVED, OperationResultType.FAIL);
        }

        EventBus.getDefault().postSticky(operationEvent);

        Log.i(TAG, "handleActionRetrieveToken: post sticky finish");

    }

}