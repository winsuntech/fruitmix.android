package com.winsun.fruitmix.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.winsun.fruitmix.db.DBUtils;
import com.winsun.fruitmix.eventbus.OperationEvent;
import com.winsun.fruitmix.mediaModule.model.MediaShare;
import com.winsun.fruitmix.util.FNAS;
import com.winsun.fruitmix.util.LocalCache;
import com.winsun.fruitmix.util.OperationResultType;
import com.winsun.fruitmix.util.Util;

import org.greenrobot.eventbus.EventBus;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DeleteRemoteMediaShareService extends IntentService {

    private static final String TAG = DeleteRemoteMediaShareService.class.getSimpleName();

    private static final String ACTION_DELETE_REMOTE_SHARE = "com.winsun.fruitmix.services.action.delete.remote.share";

    // TODO: Rename parameters
    private static final String EXTRA_SHARE = "com.winsun.fruitmix.services.extra.share";

    public DeleteRemoteMediaShareService() {
        super("DeleteRemoteMediaShareService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionDeleteRemoteShare(Context context, MediaShare mediaShare) {
        Intent intent = new Intent(context, DeleteRemoteMediaShareService.class);
        intent.setAction(ACTION_DELETE_REMOTE_SHARE);
        intent.putExtra(EXTRA_SHARE, mediaShare);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DELETE_REMOTE_SHARE.equals(action)) {
                final MediaShare mediaShare = intent.getParcelableExtra(EXTRA_SHARE);
                handleActionDeleteRemoteShare(mediaShare);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDeleteRemoteShare(MediaShare mediaShare) {

        OperationEvent operationEvent;

        if (mediaShare.isLocal()) {

            operationEvent = new OperationEvent(Util.REMOTE_SHARE_DELETED, OperationResultType.LOCAL_MEDIA_SHARE_UPLOADING);

        } else {

//            data = "{\"commands\": \"[{\\\"op\\\":\\\"replace\\\", \\\"path\\\":\\\"" + mediaShare.getUuid() + "\\\", \\\"value\\\":{\\\"archived\\\":\\\"true\\\",\\\"album\\\":\\\"true\\\", \\\"maintainers\\\":[\\\"" + FNAS.userUUID + "\\\"], \\\"tags\\\":[{\\\"albumname\\\":\\\"" + mediaShare.getTitle() + "\\\", \\\"desc\\\":\\\"" + mediaShare.getDesc() + "\\\"}], \\\"viewers\\\":[]}}]\"}";

            try {
                FNAS.DeleteRemoteCall(Util.MEDIASHARE_PARAMETER + "/" + mediaShare.getUuid(), "");

                operationEvent = new OperationEvent(Util.REMOTE_SHARE_DELETED, OperationResultType.SUCCEED);

                Log.i(TAG, "delete remote mediashare which source is network succeed");

                DBUtils dbUtils = DBUtils.getInstance(this);
                long dbResult = dbUtils.deleteRemoteShareByUUid(mediaShare.getUuid());

                Log.i(TAG, "delete remote mediashare which source is db result:" + dbResult);

                MediaShare mapResult = LocalCache.RemoteMediaShareMapKeyIsUUID.remove(mediaShare.getUuid());

                Log.i(TAG, "delete remote mediashare in map result:" + (mapResult != null ? "true" : "false"));


            } catch (Exception e) {

                e.printStackTrace();

                operationEvent = new OperationEvent(Util.REMOTE_SHARE_DELETED, OperationResultType.FAIL);

                Log.i(TAG, "delete remote mediashare fail");
            }
        }

        EventBus.getDefault().post(operationEvent);
    }
}
