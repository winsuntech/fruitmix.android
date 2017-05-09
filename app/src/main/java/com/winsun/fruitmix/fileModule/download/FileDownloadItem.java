package com.winsun.fruitmix.fileModule.download;

/**
 * Created by Administrator on 2016/11/7.
 */

public class FileDownloadItem {

    private String fileName;
    private String fileUUID;
    private long fileSize;
    private long fileTime;
    private long fileCurrentDownloadSize;
    private String fileCreatorUUID;
    private FileDownloadState fileDownloadState;

    private String parentFolderUUID;

    public FileDownloadItem(String fileName, long fileSize, String fileUUID) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileUUID = fileUUID;
    }

    public FileDownloadItem(String fileName, long fileSize, String fileUUID, String parentFolderUUID) {
        this.fileName = fileName;
        this.fileUUID = fileUUID;
        this.fileSize = fileSize;
        this.parentFolderUUID = parentFolderUUID;
    }

    public void setFileDownloadState(FileDownloadState fileDownloadState) {
        this.fileDownloadState = fileDownloadState;

        fileDownloadState.setFileUUID(fileUUID);
        fileDownloadState.setFileName(fileName);
        fileDownloadState.setFileSize(fileSize);

        fileDownloadState.startWork();

        fileDownloadState.notifyDownloadStateChanged();
    }

    void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    void setFileCurrentDownloadSize(long fileCurrentDownloadSize) {
        this.fileCurrentDownloadSize = fileCurrentDownloadSize;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getFileCurrentDownloadSize() {
        return fileCurrentDownloadSize;
    }

    public DownloadState getDownloadState() {
        return fileDownloadState.getDownloadState();
    }

    public String getFileUUID() {
        return fileUUID;
    }

    public void setFileTime(long fileTime) {
        this.fileTime = fileTime;
    }

    public long getFileTime() {
        return fileTime;
    }

    public String getFileCreatorUUID() {
        return fileCreatorUUID;
    }

    public void setFileCreatorUUID(String fileCreatorUUID) {
        this.fileCreatorUUID = fileCreatorUUID;
    }

    public String getParentFolderUUID() {
        return parentFolderUUID;
    }
}
