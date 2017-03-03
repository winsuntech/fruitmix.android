package com.winsun.fruitmix.fileModule.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2016/10/25.
 */

public class RemoteFolder extends AbstractRemoteFile {

    private List<AbstractRemoteFile> abstractRemoteFiles;

    public RemoteFolder() {
        super();
        abstractRemoteFiles = new ArrayList<>();
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public List<AbstractRemoteFile> listChildAbstractRemoteFileList() {
        return Collections.unmodifiableList(abstractRemoteFiles);
    }

    @Override
    public void initChildAbstractRemoteFileList(List<AbstractRemoteFile> abstractRemoteFiles) {
        this.abstractRemoteFiles.addAll(abstractRemoteFiles);
    }

    @Override
    public String getTimeDateText() {
        if (getTime().equals(""))
            return "";
        else {
            return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(Long.parseLong(getTime())));
        }
    }

}
