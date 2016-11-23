package com.winsun.fruitmix.operationResult;

import android.content.Context;

import com.winsun.fruitmix.R;
import com.winsun.fruitmix.util.OperationResultType;

/**
 * Created by Administrator on 2016/11/23.
 */

public class OperationNoNetwork extends OperationResult {

    @Override
    public String getResultMessage(Context context) {
        return context.getString(R.string.no_network);
    }

    @Override
    public OperationResultType getOperationResultType() {
        return OperationResultType.NO_NETWORK_EXCEPTION;
    }
}
