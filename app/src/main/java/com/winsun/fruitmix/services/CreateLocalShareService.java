package com.winsun.fruitmix.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

import com.winsun.fruitmix.db.DBUtils;
import com.winsun.fruitmix.model.Share;
import com.winsun.fruitmix.util.Util;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class CreateLocalShareService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_CREATE_SHARE = "com.winsun.fruitmix.services.action.create.share";

    // TODO: Rename parameters
    private static final String EXTRA_SHARE = "com.winsun.fruitmix.services.extra.share";

    public CreateLocalShareService() {
        super("CreateShareService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionCreateLocalShare(Context context, Share share) {
        Intent intent = new Intent(context, CreateLocalShareService.class);
        intent.putExtra(EXTRA_SHARE, share);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CREATE_SHARE.equals(action)) {
                final Share share = intent.getParcelableExtra(EXTRA_SHARE);
                handleActionCreateShare(share);
            }
        }
    }

    /**
     * create local share and start upload share task if network connected
     */
    private void handleActionCreateShare(Share share) {

        DBUtils dbUtils = DBUtils.SINGLE_INSTANCE;
        dbUtils.insertLocalShare(share);

        if(Util.getNetworkState(Util.APPLICATION_CONTEXT))
            LocalShareUploadService.startActionLocalShareTask(Util.APPLICATION_CONTEXT);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(Util.APPLICATION_CONTEXT);
        Intent intent = new Intent(Util.CREATE_LOCAL_SHARE_FINISH);
        broadcastManager.sendBroadcast(intent);

    }

}
