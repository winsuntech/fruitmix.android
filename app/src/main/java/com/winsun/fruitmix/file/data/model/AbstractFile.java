package com.winsun.fruitmix.file.data.model;

/**
 * Created by Administrator on 2017/8/9.
 */

public abstract class AbstractFile {

    private String name;

    private String time;

    private int fileTypeResID;

    public abstract boolean isFolder();

    public abstract String getTimeDateText();

    public String getName() {
        return name;
    }

    public int getFileTypeResID() {
        return fileTypeResID;
    }

    public void setFileTypeResID(int fileTypeResID) {
        this.fileTypeResID = fileTypeResID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
