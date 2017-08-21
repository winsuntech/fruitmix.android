package com.winsun.fruitmix.file.data.station;

import android.util.Log;

import com.winsun.fruitmix.callback.BaseLoadDataCallback;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.file.data.download.FileDownloadItem;
import com.winsun.fruitmix.file.data.download.FileDownloadState;
import com.winsun.fruitmix.file.data.model.AbstractRemoteFile;
import com.winsun.fruitmix.file.data.model.LocalFile;
import com.winsun.fruitmix.file.data.model.RemoteFile;
import com.winsun.fruitmix.http.BaseRemoteDataSourceImpl;
import com.winsun.fruitmix.http.HttpRequest;
import com.winsun.fruitmix.http.HttpRequestFactory;
import com.winsun.fruitmix.http.HttpResponse;
import com.winsun.fruitmix.http.IHttpFileUtil;
import com.winsun.fruitmix.http.IHttpUtil;
import com.winsun.fruitmix.model.operationResult.OperationIOException;
import com.winsun.fruitmix.model.operationResult.OperationNetworkException;
import com.winsun.fruitmix.model.operationResult.OperationSuccess;
import com.winsun.fruitmix.parser.RemoteFileFolderParser;
import com.winsun.fruitmix.util.FileUtil;
import com.winsun.fruitmix.util.Util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

import okhttp3.ResponseBody;

/**
 * Created by Administrator on 2017/7/19.
 */

public class StationFileDataSourceImpl extends BaseRemoteDataSourceImpl implements StationFileDataSource {

    public static final String LIST_FILE_PARAMETER = "/drives";

    public static final String DOWNLOAD_FILE_PARAMETER = "/drives";

    public static final String TAG = StationFileDataSourceImpl.class.getSimpleName();

    private IHttpFileUtil iHttpFileUtil;

    private static StationFileDataSource instance;

    public static StationFileDataSource getInstance(IHttpUtil iHttpUtil, HttpRequestFactory httpRequestFactory, IHttpFileUtil iHttpFileUtil) {

        if (instance == null)
            instance = new StationFileDataSourceImpl(iHttpUtil, httpRequestFactory, iHttpFileUtil);

        return instance;
    }


    private StationFileDataSourceImpl(IHttpUtil iHttpUtil, HttpRequestFactory httpRequestFactory, IHttpFileUtil iHttpFileUtil) {
        super(iHttpUtil, httpRequestFactory);
        this.iHttpFileUtil = iHttpFileUtil;
    }

    public void getFile(String folderUUID, String rootUUID, BaseLoadDataCallback<AbstractRemoteFile> callback) {

        HttpRequest httpRequest = httpRequestFactory.createHttpGetRequest(LIST_FILE_PARAMETER + "/" + rootUUID + "/dirs/" + folderUUID);

        wrapper.loadCall(httpRequest, callback, new RemoteFileFolderParser());

    }

    @Override
    public void downloadFile(FileDownloadState fileDownloadState, BaseOperateDataCallback<FileDownloadItem> callback) throws MalformedURLException, IOException, SocketTimeoutException {

        //TODO:add file state(downloading,pending,finishing.etc) and scheduler,use state mode and do function:1.log child node 2.log parent node 3.find node and return

        HttpRequest httpRequest = httpRequestFactory.createHttpGetRequest(DOWNLOAD_FILE_PARAMETER + "/"
                + fileDownloadState.getDriveUUID() + "/dirs/" + fileDownloadState.getParentFolderUUID()
                + "/entries/" + fileDownloadState.getFileUUID() + "?name=" + fileDownloadState.getFileName());

        ResponseBody responseBody = iHttpFileUtil.downloadFile(httpRequest);

        Log.d(TAG, "call: downloadFile");

        boolean result = FileUtil.writeResponseBodyToFolder(responseBody, fileDownloadState);

        Log.d(TAG, "call: download result:" + result);

        if (result)
            callback.onSucceed(fileDownloadState.getFileDownloadItem(), new OperationSuccess());
        else
            callback.onFail(new OperationIOException());

    }

    @Override
    public void createFolder(String folderName, String driveUUID, String dirUUID, BaseOperateDataCallback<HttpResponse> callback) {

        String path = "/drives/" + driveUUID + "/dirs/" + dirUUID + "/entries";

        HttpRequest httpRequest = httpRequestFactory.createHttpGetRequest(path);

        Log.i(TAG, "createFolder: start create");

        HttpResponse httpResponse = iHttpFileUtil.createFolder(httpRequest, folderName);

        Log.i(TAG, "createFolder: result: " + (httpResponse != null ? "succeed" : "fail"));

        if (httpResponse != null)
            callback.onSucceed(httpResponse, new OperationSuccess());
        else
            callback.onFail(new OperationIOException());
    }

    @Override
    public void uploadFile(LocalFile file, String driveUUID, String dirUUID, BaseOperateDataCallback<Boolean> callback) {

        String path = "/drives/" + driveUUID + "/dirs/" + dirUUID + "/entries";

        HttpRequest httpRequest = httpRequestFactory.createHttpGetRequest(path);

        Log.i(TAG, "uploadFile: start upload: " + httpRequest.getUrl());

        boolean result = iHttpFileUtil.uploadFile(httpRequest, file);

        Log.i(TAG, "uploadFile: result: " + (result ? "succeed" : "fail"));

        if (result)
            callback.onSucceed(true, new OperationSuccess());
        else
            callback.onFail(new OperationIOException());


    }
}
