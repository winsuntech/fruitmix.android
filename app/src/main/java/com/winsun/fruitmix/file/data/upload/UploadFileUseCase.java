package com.winsun.fruitmix.file.data.upload;

import android.support.annotation.NonNull;
import android.util.Log;

import com.winsun.fruitmix.callback.BaseLoadDataCallback;
import com.winsun.fruitmix.callback.BaseLoadDataCallbackImpl;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.callback.BaseOperateDataCallbackImpl;
import com.winsun.fruitmix.file.data.model.AbstractRemoteFile;
import com.winsun.fruitmix.file.data.model.LocalFile;
import com.winsun.fruitmix.file.data.station.StationFileRepository;
import com.winsun.fruitmix.http.HttpResponse;
import com.winsun.fruitmix.model.OperationResultType;
import com.winsun.fruitmix.model.operationResult.OperationNetworkException;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.model.operationResult.OperationSuccess;
import com.winsun.fruitmix.model.operationResult.OperationSuccessWithFile;
import com.winsun.fruitmix.network.NetworkState;
import com.winsun.fruitmix.network.NetworkStateManager;
import com.winsun.fruitmix.parser.HttpErrorBodyParser;
import com.winsun.fruitmix.parser.RemoteDataParser;
import com.winsun.fruitmix.parser.RemoteMkDirParser;
import com.winsun.fruitmix.system.setting.SystemSettingDataSource;
import com.winsun.fruitmix.user.User;
import com.winsun.fruitmix.user.datasource.UserDataRepository;
import com.winsun.fruitmix.util.FileTool;
import com.winsun.fruitmix.util.FileUtil;

import org.json.JSONException;

import java.io.File;
import java.util.List;

/**
 * Created by Administrator on 2017/11/15.
 */

public class UploadFileUseCase {

    public static final String TAG = UploadFileUseCase.class.getSimpleName();

    private static final int UPLOAD_FILE_EEXIST = 0x1001;

    private static final int UPLOAD_FILE_SUCCEED = 0x1002;

    private static final int UPLOAD_FILE_FAIL = 0x1003;

    static final String UPLOAD_PARENT_FOLDER_NAME = "上传的文件";

    private static final String UPLOAD_FOLDER_NAME_PREFIX = "来自";

    private UserDataRepository userDataRepository;

    private StationFileRepository stationFileRepository;

    private SystemSettingDataSource systemSettingDataSource;

    private NetworkStateManager networkStateManager;

    private FileTool mFileTool;

    private String uploadFolderName;

    private String fileTemporaryFolderParentFolderPath;

    private String currentUserHome;

    private String currentUserUUID;

    private String uploadParentFolderUUID;

    private String uploadFolderUUID;

    boolean needRetryForCreateFolderEEXIST = false;

    private String uploadFilePath;

    private String fileOriginalName;

    UploadFileUseCase(UserDataRepository userDataRepository, StationFileRepository stationFileRepository,
                      SystemSettingDataSource systemSettingDataSource, NetworkStateManager networkStateManager,
                      FileTool fileTool, String uploadFolderName, String fileTemporaryFolderParentFolderPath) {
        this.userDataRepository = userDataRepository;
        this.stationFileRepository = stationFileRepository;
        this.systemSettingDataSource = systemSettingDataSource;
        this.uploadFolderName = uploadFolderName;
        this.networkStateManager = networkStateManager;
        mFileTool = fileTool;
        this.fileTemporaryFolderParentFolderPath = fileTemporaryFolderParentFolderPath;
    }

    public UploadFileUseCase copySelf(){

        return new UploadFileUseCase(userDataRepository,stationFileRepository,systemSettingDataSource,
                networkStateManager,mFileTool,uploadFolderName,fileTemporaryFolderParentFolderPath);

    }

    public void updateFile(final FileUploadState fileUploadState) {

        copyToTemporaryFolder(fileUploadState);

        if (!checkUploadCondition(fileUploadState))
            return;

        do {

            currentUserUUID = systemSettingDataSource.getCurrentLoginUserUUID();

            User user = userDataRepository.getUserByUUID(currentUserUUID);

            if (user == null) {

                notifyError(fileUploadState);

                return;
            }

            currentUserHome = user.getHome();

            checkFolderExist(currentUserHome, currentUserHome, fileUploadState, new BaseLoadDataCallbackImpl<AbstractRemoteFile>() {
                @Override
                public void onSucceed(List<AbstractRemoteFile> data, OperationResult operationResult) {
                    super.onSucceed(data, operationResult);

                    handleGetRootFolderResult(data, fileUploadState);
                }
            });

        } while (needRetryForCreateFolderEEXIST);

    }

    private void copyToTemporaryFolder(FileUploadState fileUploadState) {

        String fileOriginalPath = fileUploadState.getFilePath();

        FileUploadItem fileUploadItem = fileUploadState.getFileUploadItem();

        String fileTemporaryUploadFolderPath = mFileTool.getTemporaryUploadFolderPath(fileTemporaryFolderParentFolderPath,
                systemSettingDataSource.getCurrentLoginUserUUID());

        fileOriginalName = fileUploadItem.getFileName();

        Log.d(TAG, "copyToTemporaryFolder: originalName: " + fileOriginalName + this);

        boolean copyResult;

        copyResult = mFileTool.copyFileToDir(fileOriginalPath, fileUploadItem.getFileName(), fileTemporaryUploadFolderPath);

/*        fileOriginalName = fileUploadItem.getFileName();

        Log.d(TAG, "copyToTemporaryFolder: originalName: " + fileOriginalName);

        String fileName = fileOriginalName;

        int renameCode = 0;

        while (true) {

            String filePath = fileTemporaryUploadFolderPath + File.separator + fileName;

            File file = new File(filePath);

            if (file.exists()) {

                fileName = renameFileName(++renameCode, fileOriginalName);

            } else {

                Log.d(TAG, "copyToTemporaryFolder: file final name: " + fileName);

                fileUploadItem.setFileName(fileName);

                copyResult = mFileTool.copyFileToDir(fileOriginalPath, fileUploadItem.getFileName(), fileTemporaryUploadFolderPath);

                break;
            }

        }*/

        if (copyResult)
            uploadFilePath = fileTemporaryUploadFolderPath;
        else
            uploadFilePath = fileOriginalPath;

        uploadFilePath += File.separator + fileUploadItem.getFileName();

        if (copyResult)
            fileUploadItem.setTemporaryUploadFilePath(uploadFilePath);

        Log.d(TAG, "copyToTemporaryFolder: upload file path: " + uploadFilePath);

    }

    private boolean checkUploadCondition(FileUploadState fileUploadState) {

        FileUploadItem fileUploadItem = fileUploadState.getFileUploadItem();

        if (!checkUploadCondition(networkStateManager, systemSettingDataSource)) {

            fileUploadItem.setFileUploadState(new FileUploadPendingState(fileUploadItem, this, networkStateManager));

            return false;

        } else
            return true;

    }

    public static boolean checkUploadCondition(NetworkStateManager networkStateManager, SystemSettingDataSource systemSettingDataSource) {

        NetworkState networkState = networkStateManager.getNetworkState();

        if (!networkState.isMobileConnected() && !networkState.isWifiConnected()) {

            Log.d(TAG, "checkUploadCondition: network is unreached,set file upload pending state");

            return false;

        }

        if (systemSettingDataSource.getOnlyAutoUploadWhenConnectedWithWifi() && !networkState.isWifiConnected()) {

            Log.d(TAG, "checkUploadCondition: only auto upload when connect with wifi is set,but wifi is not connected,set file upload pending state");

            return false;

        }

        return true;

    }


    private void checkFolderExist(String rootUUID, String dirUUID, final FileUploadState fileUploadState, final BaseLoadDataCallback<AbstractRemoteFile> callback) {

        Log.i(TAG, "start check folder exist");

        OperationResult result = stationFileRepository.getFileWithoutCreateNewThread(rootUUID, dirUUID);

        if (result instanceof OperationSuccessWithFile)
            callback.onSucceed(((OperationSuccessWithFile) result).getList(), new OperationSuccess());
        else
            callback.onFail(result);
    }

    private void notifyError(FileUploadState fileUploadState) {
        FileUploadItem fileUploadItem = fileUploadState.getFileUploadItem();

        fileUploadItem.setFileUploadState(new FileUploadPendingState(fileUploadItem, this, networkStateManager));

        needRetryForCreateFolderEEXIST = false;
    }

    private void handleGetRootFolderResult(List<AbstractRemoteFile> data, final FileUploadState fileUploadState) {

        uploadParentFolderUUID = getFolderUUIDByName(data, UPLOAD_PARENT_FOLDER_NAME);

        if (uploadParentFolderUUID.length() != 0) {

            Log.i(TAG, "upload parent folder exist");

            checkFolderExist(currentUserHome, uploadParentFolderUUID, fileUploadState, new BaseLoadDataCallbackImpl<AbstractRemoteFile>() {
                @Override
                public void onSucceed(List<AbstractRemoteFile> data, OperationResult operationResult) {
                    super.onSucceed(data, operationResult);

                    handleGetUploadFolderResult(data, fileUploadState);

                }
            });

        } else {

            startCreateFolderAndUpload(fileUploadState);

        }

    }

    private String getFolderUUIDByName(List<AbstractRemoteFile> files, String folderName) {
        String folderUUID;
        for (AbstractRemoteFile file : files) {
            if (file.getName().equals(folderName)) {

                folderUUID = file.getUuid();

                return folderUUID;
            }

        }
        return "";
    }

    private void handleGetUploadFolderResult(List<AbstractRemoteFile> data, FileUploadState fileUploadState) {

        uploadFolderUUID = getFolderUUIDByName(data, getUploadFolderName());

        if (!uploadFolderUUID.isEmpty()) {

            Log.i(TAG, "uploaded folder exist");

//            getFileInUploadFolder(uploadFolderUUID, fileUploadState);

            startPrepareAutoUpload(fileUploadState);

        } else {
            startCreateFolderAndUpload(fileUploadState);
        }

    }

    String getUploadFolderName() {
        return UPLOAD_FOLDER_NAME_PREFIX + uploadFolderName;
    }

    private void startCreateFolderAndUpload(FileUploadState fileUploadState) {

        Log.d(TAG, "startCreateFolderAndUpload");

        startPrepareAutoUpload(fileUploadState);
    }

    private void startPrepareAutoUpload(FileUploadState fileUploadState) {

        createFolderIfNeed(fileUploadState);

    }

    private void createFolderIfNeed(FileUploadState fileUploadState) {

        if (uploadParentFolderUUID.isEmpty()) {

            Log.i(TAG, "start create upload parent folder");

            createUploadParentFolder(fileUploadState);
        } else if (uploadFolderUUID.isEmpty()) {

            Log.i(TAG, "start create upload folder");

            createUploadFolder(fileUploadState);
        } else {
            Log.i(TAG, "start upload file");

            startUploadFile(fileUploadState);

        }

    }

    private void createUploadParentFolder(final FileUploadState fileUploadState) {

        createFolder(UPLOAD_PARENT_FOLDER_NAME, currentUserHome, currentUserHome, fileUploadState, new BaseOperateDataCallbackImpl<HttpResponse>() {
            @Override
            public void onSucceed(HttpResponse data, OperationResult result) {
                super.onSucceed(data, result);

                RemoteDataParser<AbstractRemoteFile> parser = new RemoteMkDirParser();

                try {

                    AbstractRemoteFile file = parser.parse(data.getResponseData());

                    uploadParentFolderUUID = file.getUuid();

                    if (uploadParentFolderUUID.length() != 0) {

                        Log.i(TAG, "create upload folder succeed folder uuid:" + uploadParentFolderUUID);

                        createUploadFolder(fileUploadState);

                    } else {

                        Log.i(TAG, "create upload folder succeed but can not find folder uuid,stop upload");

                        notifyError(fileUploadState);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();

                    Log.d(TAG, "parse upload parent folder result fail,stop upload");

                    notifyError(fileUploadState);
                }

            }
        });

    }


    private void createUploadFolder(final FileUploadState fileUploadState) {
        createFolder(getUploadFolderName(), currentUserHome, uploadParentFolderUUID, fileUploadState, new BaseOperateDataCallbackImpl<HttpResponse>() {
            @Override
            public void onSucceed(HttpResponse data, OperationResult result) {
                super.onSucceed(data, result);

                RemoteDataParser<AbstractRemoteFile> parser = new RemoteMkDirParser();

                try {

                    AbstractRemoteFile file = parser.parse(data.getResponseData());

                    uploadFolderUUID = file.getUuid();

                    if (!uploadFolderUUID.isEmpty()) {

                        Log.i(TAG, "create upload folder succeed folder uuid:" + uploadFolderUUID);

                        startUploadFile(fileUploadState);

                    } else {

                        Log.i(TAG, "create upload folder succeed but can not find folder uuid,stop upload");

                        notifyError(fileUploadState);

                    }


                } catch (JSONException e) {
                    e.printStackTrace();

                    Log.d(TAG, "parse create upload folder result fail,stop upload");

                    notifyError(fileUploadState);

                }

            }

        });
    }

    private void createFolder(String folderName, String rootUUID, String dirUUID, final FileUploadState fileUploadState, final BaseOperateDataCallback<HttpResponse> callback) {
        stationFileRepository.createFolderWithoutCreateNewThread(folderName, rootUUID, dirUUID, new BaseOperateDataCallback<HttpResponse>() {
            @Override
            public void onSucceed(HttpResponse data, OperationResult result) {
                callback.onSucceed(data, result);
            }

            @Override
            public void onFail(OperationResult result) {

                Log.d(TAG, "onFail: create folder fail,stop upload");

                notifyError(fileUploadState);

                if (result instanceof OperationNetworkException) {

                    int code = ((OperationNetworkException) result).getHttpResponseCode();

                    Log.d(TAG, "create folder onFail,error code: " + code);

                    HttpErrorBodyParser parser = new HttpErrorBodyParser();

                    try {
                        String messageInBody = parser.parse(((OperationNetworkException) result).getHttpResponseData());

                        if (messageInBody.contains(HttpErrorBodyParser.UPLOAD_FILE_EXIST_CODE)) {

                            needRetryForCreateFolderEEXIST = true;

                        } else {
                            notifyError(fileUploadState);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();

                        notifyError(fileUploadState);
                    }


                } else
                    notifyError(fileUploadState);

            }
        });
    }

    private void startUploadFile(final FileUploadState fileUploadState) {

        while (true) {

            Log.i(TAG, "start getFileInUploadFolder");

            final FileUploadItem fileUploadItem = fileUploadState.getFileUploadItem();

            OperationResult result = stationFileRepository.getFileWithoutCreateNewThread(currentUserHome, uploadFolderUUID);

            if (!(result instanceof OperationSuccessWithFile))
                return;

            List<AbstractRemoteFile> files = ((OperationSuccessWithFile) result).getList();

            int preRenameCode;
            int renameCode = 0;

            Log.d(TAG, "startUploadFile: fileOriginalName: " + fileOriginalName + this);

            String newFileName = fileOriginalName;

            do {

                preRenameCode = renameCode;

                for (AbstractRemoteFile file : files) {

                    if (file.getName().equals(newFileName)) {

                        newFileName = renameFileName(++renameCode, fileOriginalName);

                        break;
                    }

                }

            } while (preRenameCode != renameCode);

            Log.d(TAG, "after getFileInUploadFolder newFileName: " + newFileName);

            fileUploadItem.setFileName(newFileName);

            int uploadResult = startUploadFileAfterCheck(fileUploadState);

            if (uploadResult != UPLOAD_FILE_EEXIST)
                break;

        }

    }

    @NonNull
    public String renameFileName(int renameCode, String fileName) {
        int dotIndex = fileName.indexOf(".");

        if (dotIndex == -1)
            dotIndex = fileName.length();

        String fileNameWithEnd = fileName.substring(0, dotIndex);

        String end = fileName.substring(dotIndex, fileName.length());

        fileName = fileNameWithEnd + "(" + renameCode + ")" + end;

        return fileName;

    }

    private int startUploadFileAfterCheck(FileUploadState fileUploadState) {

        FileUploadItem fileUploadItem = fileUploadState.getFileUploadItem();
        String fileName = fileUploadItem.getFileName();

        final LocalFile localFile = new LocalFile();

        localFile.setFileHash(fileUploadItem.getFileUUID());
        localFile.setSize(fileUploadItem.getFileSize() + "");

        localFile.setPath(uploadFilePath);

        localFile.setName(fileName);

        Log.d(TAG, "startUploadFile: file name: " + localFile.getName() + " file path: " + localFile.getPath()
                + " file hash: " + localFile.getFileHash());

        OperationResult result = stationFileRepository.uploadFileWithProgress(localFile, fileUploadState, currentUserHome, uploadFolderUUID, currentUserUUID);

        if (result.getOperationResultType() == OperationResultType.SUCCEED) {

            handleUploadSucceed(localFile.getPath(), localFile.getName(), fileUploadItem);

            mFileTool.deleteFile(localFile.getPath());

            return UPLOAD_FILE_SUCCEED;

        } else if (result instanceof OperationNetworkException) {

            int code = ((OperationNetworkException) result).getHttpResponseCode();

            Log.d(TAG, "upload onFail,error code: " + code);

            HttpErrorBodyParser parser = new HttpErrorBodyParser();

            try {
                String messageInBody = parser.parse(((OperationNetworkException) result).getHttpResponseData());

                if (messageInBody.contains(HttpErrorBodyParser.UPLOAD_FILE_EXIST_CODE)) {

                    return UPLOAD_FILE_EEXIST;

                } else {
                    notifyError(fileUploadState);

                    return UPLOAD_FILE_FAIL;

                }


            } catch (JSONException e) {
                e.printStackTrace();

                notifyError(fileUploadState);

                return UPLOAD_FILE_FAIL;
            }

        } else {

            Log.d(TAG, "startUploadFile: fail and notify error");

            notifyError(fileUploadState);

            return UPLOAD_FILE_FAIL;

        }

    }

    private void handleUploadSucceed(String filePath, String newFileName, FileUploadItem fileUploadItem) {

        boolean copyToDownloadFolderResult = mFileTool.copyFileToDir(filePath, newFileName, FileUtil.getDownloadFileStoreFolderPath());

        Log.d(TAG, "handleUploadSucceed: copy to download folder result: " + copyToDownloadFolderResult);

        fileUploadItem.setFilePath(FileUtil.getDownloadFileStoreFolderPath() + newFileName);

        Log.d(TAG, "handleUploadSucceed: insert record,file path: " + fileUploadItem.getFilePath());

        fileUploadItem.setFileTime(System.currentTimeMillis());

        fileUploadItem.setFileUploadState(new FileUploadFinishedState(fileUploadItem));

        boolean insertResult = stationFileRepository.insertFileUploadTask(fileUploadItem, currentUserUUID);

        Log.d(TAG, "handleUploadSucceed: insert record result: " + insertResult);

    }

}
