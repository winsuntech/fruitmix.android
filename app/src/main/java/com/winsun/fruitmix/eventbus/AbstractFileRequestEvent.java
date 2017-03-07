package com.winsun.fruitmix.eventbus;

import com.winsun.fruitmix.model.OperationTargetType;
import com.winsun.fruitmix.model.OperationType;

/**
 * Created by Administrator on 2016/11/9.
 */

public class AbstractFileRequestEvent extends RequestEvent {

    private String folderUUID;

    public AbstractFileRequestEvent(OperationType operationType, OperationTargetType operationTargetType, String folderUUID) {
        super(operationType, operationTargetType);
        this.folderUUID = folderUUID;
    }

    public String getFolderUUID() {
        return folderUUID;
    }
}