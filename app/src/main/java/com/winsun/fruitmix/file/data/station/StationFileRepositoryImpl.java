package com.winsun.fruitmix.file.data.station;

import com.winsun.fruitmix.BaseDataRepository;
import com.winsun.fruitmix.callback.BaseLoadDataCallback;
import com.winsun.fruitmix.callback.BaseLoadDataCallbackImpl;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.file.data.download.DownloadedFileWrapper;
import com.winsun.fruitmix.file.data.download.FinishedTaskItem;
import com.winsun.fruitmix.file.data.download.FileDownloadItem;
import com.winsun.fruitmix.file.data.download.FileDownloadState;
import com.winsun.fruitmix.file.data.model.AbstractRemoteFile;
import com.winsun.fruitmix.file.data.model.LocalFile;
import com.winsun.fruitmix.file.data.upload.FileUploadState;
import com.winsun.fruitmix.http.HttpResponse;
import com.winsun.fruitmix.model.OperationResultType;
import com.winsun.fruitmix.model.operationResult.OperationIOException;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.model.operationResult.OperationSuccess;
import com.winsun.fruitmix.thread.manage.ThreadManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Administrator on 2017/7/19.
 */

public class StationFileRepositoryImpl extends BaseDataRepository implements StationFileRepository {

    private static StationFileRepositoryImpl instance;

    private StationFileDataSource stationFileDataSource;
    private DownloadedFileDataSource downloadedFileDataSource;

    List<AbstractRemoteFile> stationFiles;

    private String currentFolderUUID;

    boolean cacheDirty = true;

    private StationFileRepositoryImpl(StationFileDataSource stationFileDataSource, DownloadedFileDataSource downloadedFileDataSource, ThreadManager threadManager) {
        super(threadManager);
        this.stationFileDataSource = stationFileDataSource;
        this.downloadedFileDataSource = downloadedFileDataSource;

        stationFiles = new ArrayList<>();

    }

    public static StationFileRepositoryImpl getInstance(StationFileDataSource stationFileDataSource, DownloadedFileDataSource downloadedFileDataSource, ThreadManager threadManager) {
        if (instance == null)
            instance = new StationFileRepositoryImpl(stationFileDataSource, downloadedFileDataSource, threadManager);
        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }

    @Override
    public void getRootDrive(final BaseLoadDataCallback<AbstractRemoteFile> callback) {

        mThreadManager.runOnCacheThread(new Runnable() {
            @Override
            public void run() {
                stationFileDataSource.getRootDrive(createLoadCallbackRunOnMainThread(callback));
            }
        });

    }

    public void getFile(final String rootUUID, final String folderUUID, final BaseLoadDataCallback<AbstractRemoteFile> callback) {

/*        if (currentFolderUUID != null && !currentFolderUUID.equals(folderUUID))
            cacheDirty = true;

        if (!cacheDirty) {
            callback.onSucceed(new ArrayList<>(stationFiles), new OperationSuccess());
            return;
        }*/

        final BaseLoadDataCallback<AbstractRemoteFile> runOnMainThreadCallback = createLoadCallbackRunOnMainThread(callback);

        mThreadManager.runOnCacheThread(new Runnable() {
            @Override
            public void run() {

                handleGeneralFolder(rootUUID, folderUUID, runOnMainThreadCallback);

            }

        });

    }

    @Override
    public void getFileWithoutCreateNewThread(String rootUUID, String folderUUID, BaseLoadDataCallback<AbstractRemoteFile> callback) {

        handleGeneralFolder(rootUUID, folderUUID, callback);

    }

    private void handleGeneralFolder(final String rootUUID, final String folderUUID, final BaseLoadDataCallback<AbstractRemoteFile> runOnMainThreadCallback) {
        stationFileDataSource.getFile(rootUUID, folderUUID, new BaseLoadDataCallbackImpl<AbstractRemoteFile>() {

            @Override
            public void onSucceed(List<AbstractRemoteFile> data, OperationResult operationResult) {
                super.onSucceed(data, operationResult);

                currentFolderUUID = folderUUID;

                for (AbstractRemoteFile file : data) {
                    file.setParentFolderUUID(folderUUID);
                }

                stationFiles.clear();

                stationFiles.addAll(data);

                runOnMainThreadCallback.onSucceed(data, operationResult);

                cacheDirty = false;

            }

            @Override
            public void onFail(OperationResult operationResult) {
                super.onFail(operationResult);

                currentFolderUUID = folderUUID;

                runOnMainThreadCallback.onFail(operationResult);

                cacheDirty = false;
            }
        });
    }

    @Override
    public void downloadFile(final String currentUserUUID, final FileDownloadState fileDownloadState, final BaseOperateDataCallback<FileDownloadItem> callback) throws MalformedURLException, IOException, SocketTimeoutException {

        stationFileDataSource.downloadFile(fileDownloadState, new BaseOperateDataCallback<FileDownloadItem>() {
            @Override
            public void onSucceed(FileDownloadItem data, OperationResult result) {

                FinishedTaskItem finishedTaskItem = new FinishedTaskItem(data);

                finishedTaskItem.setFileTime(System.currentTimeMillis());
                finishedTaskItem.setFileCreatorUUID(currentUserUUID);

                downloadedFileDataSource.insertDownloadedFileRecord(finishedTaskItem);

                callback.onSucceed(fileDownloadState.getFileDownloadItem(), new OperationSuccess());

            }

            @Override
            public void onFail(OperationResult result) {
                callback.onFail(result);
            }
        });

    }

    void setCacheDirty() {
        cacheDirty = true;
    }


    @Override
    public List<FinishedTaskItem> getCurrentLoginUserDownloadedFileRecord(String currentLoginUserUUID) {

        return downloadedFileDataSource.getCurrentLoginUserDownloadedFileRecord(currentLoginUserUUID);

    }

    @Override
    public void clearDownloadFileRecordInCache() {

        downloadedFileDataSource.clearDownloadFileRecordInCache();

    }

    @Override
    public void deleteDownloadedFile(final Collection<DownloadedFileWrapper> downloadedFileWrappers, final String currentLoginUserUUID, BaseOperateDataCallback<Void> callback) {

        final BaseOperateDataCallback<Void> runOnMainThreadCallback = createOperateCallbackRunOnMainThread(callback);

        mThreadManager.runOnCacheThread(new Runnable() {
            @Override
            public void run() {
                deleteDownloadedFileInThread(downloadedFileWrappers, currentLoginUserUUID, runOnMainThreadCallback);
            }
        });

    }

    private void deleteDownloadedFileInThread(Collection<DownloadedFileWrapper> downloadedFileWrappers, String currentLoginUserUUID, BaseOperateDataCallback<Void> callback) {
        boolean result = false;

        for (DownloadedFileWrapper downloadedFileWrapper : downloadedFileWrappers) {

            result = downloadedFileDataSource.deleteDownloadedFile(downloadedFileWrapper.getFileName());

            if (result)
                downloadedFileDataSource.deleteDownloadedFileRecord(downloadedFileWrapper.getFileUnionKey(), currentLoginUserUUID);
        }

        if (result)
            callback.onSucceed(null, new OperationSuccess());
        else
            callback.onFail(new OperationIOException());
    }

    @Override
    public void createFolder(final String folderName, final String driveUUID, final String dirUUID, BaseOperateDataCallback<HttpResponse> callback) {

        final BaseOperateDataCallback<HttpResponse> runOnMainThreadCallback = createOperateCallbackRunOnMainThread(callback);

        mThreadManager.runOnCacheThread(new Runnable() {
            @Override
            public void run() {
                stationFileDataSource.createFolder(folderName, driveUUID, dirUUID, runOnMainThreadCallback);
            }
        });

    }

    @Override
    public void createFolderWithoutCreateNewThread(String folderName, String driveUUID, String dirUUID, BaseOperateDataCallback<HttpResponse> callback) {
        stationFileDataSource.createFolder(folderName, driveUUID, dirUUID, callback);
    }

    @Override
    public OperationResult uploadFile(LocalFile file, String driveUUID, String dirUUID) {

        return stationFileDataSource.uploadFile(file, driveUUID, dirUUID);

    }

    //TODO:create upload file db and finish create,delete,get

    @Override
    public OperationResult uploadFileWithProgress(LocalFile file, FileUploadState fileUploadState, String driveUUID, String dirUUID, String currentLoginUserUUID) {

        OperationResult result = stationFileDataSource.uploadFileWithProgress(file, fileUploadState, driveUUID, dirUUID);

        if (result.getOperationResultType() != OperationResultType.SUCCEED) {

            //insert into db


        }

        return result;

    }


}
