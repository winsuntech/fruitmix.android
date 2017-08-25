package com.winsun.fruitmix.upload.media;

import android.support.annotation.NonNull;

import com.winsun.fruitmix.BuildConfig;
import com.winsun.fruitmix.callback.BaseLoadDataCallback;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.file.data.model.AbstractRemoteFile;
import com.winsun.fruitmix.file.data.model.LocalFile;
import com.winsun.fruitmix.file.data.model.RemoteFile;
import com.winsun.fruitmix.file.data.station.StationFileRepository;
import com.winsun.fruitmix.http.HttpResponse;
import com.winsun.fruitmix.logged.in.user.LoggedInUser;
import com.winsun.fruitmix.logged.in.user.LoggedInUserDataSource;
import com.winsun.fruitmix.media.MediaDataSourceRepository;
import com.winsun.fruitmix.mediaModule.model.Media;
import com.winsun.fruitmix.mock.MockApplication;
import com.winsun.fruitmix.mock.MockThreadManager;
import com.winsun.fruitmix.model.operationResult.OperationIOException;
import com.winsun.fruitmix.model.operationResult.OperationNetworkException;
import com.winsun.fruitmix.model.operationResult.OperationSuccess;
import com.winsun.fruitmix.system.setting.SystemSettingDataSource;
import com.winsun.fruitmix.thread.manage.ThreadManager;
import com.winsun.fruitmix.user.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by Administrator on 2017/8/23.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, application = MockApplication.class)
public class UploadMediaUseCaseTest {

    private UploadMediaUseCase uploadMediaUseCase;

    @Mock
    private MediaDataSourceRepository mediaDataSourceRepository;

    @Mock
    private StationFileRepository stationFileRepository;

    @Mock
    private LoggedInUserDataSource loggedInUserDataSource;

    @Mock
    private SystemSettingDataSource systemSettingDataSource;

    @Mock
    private CheckMediaIsUploadStrategy checkMediaIsUploadStrategy;

    private ThreadManager threadManager;

    private String testUserUUID;
    private String testUserHome;

    private String testUploadParentFolderUUID;
    private String testUploadFolderUUID;

    private String testUploadFolderName;

    private String testMediaUUID;
    private String testMediaOriginalPhotoPath;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);

        testUploadFolderName = "";

        threadManager = new MockThreadManager();

        uploadMediaUseCase = UploadMediaUseCase.getInstance(mediaDataSourceRepository, stationFileRepository,
                loggedInUserDataSource, threadManager, systemSettingDataSource, checkMediaIsUploadStrategy, testUploadFolderName);

    }

    @After
    public void clean() {

        UploadMediaUseCase.destroyInstance();

    }

    @Test
    public void testStartUploadTwiceImmediately() {

        prepareStartUpload();

        uploadMediaUseCase.startUploadMedia();

        assertTrue(uploadMediaUseCase.mAlreadyStartUpload);

        verify(systemSettingDataSource).getCurrentLoginUserUUID();

        verify(loggedInUserDataSource).getLoggedInUserByUserUUID(anyString());

        uploadMediaUseCase.startUploadMedia();

        assertTrue(uploadMediaUseCase.mAlreadyStartUpload);

        verify(systemSettingDataSource, times(1)).getCurrentLoginUserUUID();

        verify(loggedInUserDataSource, times(1)).getLoggedInUserByUserUUID(anyString());

    }

    private void prepareStartUpload() {

        testUploadParentFolderUUID = "testUploadParentFolderUUID";
        testUploadFolderUUID = "testUploadFolderUUID";

        testUserUUID = "testUserUUID";
        testUserHome = "testUserHome";

        User user = new User();
        user.setUuid(testUserUUID);
        user.setHome(testUserHome);

        when(systemSettingDataSource.getCurrentLoginUserUUID()).thenReturn(testUserUUID);

        when(loggedInUserDataSource.getLoggedInUserByUserUUID(anyString())).thenReturn(new LoggedInUser("", "", "", "", user));
    }

    @Test
    public void testUploadParentFolderNotExist() {

        prepareStartUpload();

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.<Media>emptyList());

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        uploadMediaUseCase.startUploadMedia();

        assertNull(uploadMediaUseCase.uploadParentFolderUUID);
        assertNull(uploadMediaUseCase.uploadFolderUUID);

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> captor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(eq(testUserHome), eq(testUserHome), captor.capture());

        captor.getValue().onSucceed(Collections.<AbstractRemoteFile>emptyList(), new OperationSuccess());

        assertNotNull(uploadMediaUseCase.uploadedMediaHashs);

        verify(checkMediaIsUploadStrategy).setUploadedMediaHashs(Collections.<String>emptyList());

        ArgumentCaptor<BaseOperateDataCallback<HttpResponse>> createUploadParentFolderCaptor = ArgumentCaptor.forClass(BaseOperateDataCallback.class);

        verify(stationFileRepository).createFolder(eq(uploadMediaUseCase.UPLOAD_PARENT_FOLDER_NAME), eq(testUserHome), eq(testUserHome), createUploadParentFolderCaptor.capture());

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseData("{\n" +
                "    \"path\": [\n" +
                "        {\n" +
                "            \"uuid\": \"4a857fb9-e84d-4d11-a074-c79139b9d230\",\n" +
                "            \"name\": \"4a857fb9-e84d-4d11-a074-c79139b9d230\",\n" +
                "            \"mtime\": 1503366632432\n" +
                "        }\n" +
                "    ],\n" +
                "    \"entries\": [\n" +
                "        {\n" +
                "            \"uuid\": \"" + testUploadParentFolderUUID + "\",\n" +
                "            \"type\": \"directory\",\n" +
                "            \"name\": \"" + uploadMediaUseCase.UPLOAD_PARENT_FOLDER_NAME + "\",\n" +
                "            \"mtime\": 1503366446508\n" +
                "        }\n" +
                "    ]\n" +
                "}");

        createUploadParentFolderCaptor.getValue().onSucceed(httpResponse, new OperationSuccess());

        ArgumentCaptor<BaseOperateDataCallback<HttpResponse>> createUploadFolderCaptor = ArgumentCaptor.forClass(BaseOperateDataCallback.class);

        verify(stationFileRepository).createFolder(eq(uploadMediaUseCase.UPLOAD_FOLDER_NAME_PREFIX + testUploadFolderName), eq(testUserHome), eq(testUploadParentFolderUUID), createUploadFolderCaptor.capture());

        HttpResponse uploadFolderResponse = new HttpResponse();
        uploadFolderResponse.setResponseData("{\n" +
                "    \"path\": [\n" +
                "        {\n" +
                "            \"uuid\": \"4a857fb9-e84d-4d11-a074-c79139b9d230\",\n" +
                "            \"name\": \"4a857fb9-e84d-4d11-a074-c79139b9d230\",\n" +
                "            \"mtime\": 1503366632432\n" +
                "        }\n" +
                "    ],\n" +
                "    \"entries\": [\n" +
                "        {\n" +
                "            \"uuid\": \"" + testUploadFolderUUID + "\",\n" +
                "            \"type\": \"directory\",\n" +
                "            \"name\": \"" + uploadMediaUseCase.UPLOAD_FOLDER_NAME_PREFIX + testUploadFolderName + "\",\n" +
                "            \"mtime\": 1503366446508\n" +
                "        }\n" +
                "    ]\n" +
                "}");

        createUploadFolderCaptor.getValue().onSucceed(uploadFolderResponse, new OperationSuccess());

        assertTrue(uploadMediaUseCase.mStopUpload);
        assertFalse(uploadMediaUseCase.mAlreadyStartUpload);
        assertEquals(0, uploadMediaUseCase.uploadMediaCount);

    }

    @Test
    public void testUploadParentFolderExistButUploadFolderNotExist() {

        prepareStartUpload();

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.<Media>emptyList());

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        uploadMediaUseCase.startUploadMedia();

        assertNull(uploadMediaUseCase.uploadParentFolderUUID);
        assertNull(uploadMediaUseCase.uploadFolderUUID);

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> captor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(eq(testUserHome), eq(testUserHome), captor.capture());

        List<AbstractRemoteFile> files = new ArrayList<>();

        AbstractRemoteFile file = new RemoteFile();
        file.setName(UploadMediaUseCase.UPLOAD_PARENT_FOLDER_NAME);
        file.setUuid(testUploadParentFolderUUID);

        files.add(file);

        captor.getValue().onSucceed(files, new OperationSuccess());

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> uploadFolderCallbackCaptor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(eq(testUserHome), eq(testUploadParentFolderUUID), uploadFolderCallbackCaptor.capture());

        uploadFolderCallbackCaptor.getValue().onSucceed(Collections.<AbstractRemoteFile>emptyList(), new OperationSuccess());

        assertNotNull(uploadMediaUseCase.uploadedMediaHashs);

        verify(checkMediaIsUploadStrategy).setUploadedMediaHashs(Collections.<String>emptyList());

        ArgumentCaptor<BaseOperateDataCallback<HttpResponse>> createUploadFolderCaptor = ArgumentCaptor.forClass(BaseOperateDataCallback.class);

        verify(stationFileRepository).createFolder(eq(uploadMediaUseCase.UPLOAD_FOLDER_NAME_PREFIX + testUploadFolderName), eq(testUserHome), eq(testUploadParentFolderUUID), createUploadFolderCaptor.capture());

        HttpResponse uploadFolderResponse = new HttpResponse();
        uploadFolderResponse.setResponseData("{\n" +
                "    \"path\": [\n" +
                "        {\n" +
                "            \"uuid\": \"4a857fb9-e84d-4d11-a074-c79139b9d230\",\n" +
                "            \"name\": \"4a857fb9-e84d-4d11-a074-c79139b9d230\",\n" +
                "            \"mtime\": 1503366632432\n" +
                "        }\n" +
                "    ],\n" +
                "    \"entries\": [\n" +
                "        {\n" +
                "            \"uuid\": \"" + testUploadFolderUUID + "\",\n" +
                "            \"type\": \"directory\",\n" +
                "            \"name\": \"" + uploadMediaUseCase.UPLOAD_FOLDER_NAME_PREFIX + testUploadFolderName + "\",\n" +
                "            \"mtime\": 1503366446508\n" +
                "        }\n" +
                "    ]\n" +
                "}");

        createUploadFolderCaptor.getValue().onSucceed(uploadFolderResponse, new OperationSuccess());

        assertTrue(uploadMediaUseCase.mStopUpload);
        assertFalse(uploadMediaUseCase.mAlreadyStartUpload);
        assertEquals(0, uploadMediaUseCase.uploadMediaCount);

    }

    @Test
    public void testUploadParentFolderExistAndUploadFolderExist() {

        prepareStartUpload();

        Media media = createMedia();

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.singletonList(media));

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        uploadWhenUploadParentFolderExistAndUploadFolderExist();

        ArgumentCaptor<BaseOperateDataCallback<Boolean>> captor = ArgumentCaptor.forClass(BaseOperateDataCallback.class);

        verify(stationFileRepository).uploadFile(any(LocalFile.class), anyString(), anyString(), captor.capture());

        captor.getValue().onSucceed(true, new OperationSuccess());

        assertTrue(uploadMediaUseCase.mStopUpload);
        assertFalse(uploadMediaUseCase.mAlreadyStartUpload);
        assertEquals(0, uploadMediaUseCase.uploadMediaCount);

        assertEquals(1, uploadMediaUseCase.alreadyUploadedMediaCount);

    }

    private void uploadWhenUploadParentFolderExistAndUploadFolderExist() {

        uploadMediaUseCase.startUploadMedia();

        assertNull(uploadMediaUseCase.uploadParentFolderUUID);
        assertNull(uploadMediaUseCase.uploadFolderUUID);

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> captor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(eq(testUserHome), eq(testUserHome), captor.capture());

        List<AbstractRemoteFile> files = new ArrayList<>();

        AbstractRemoteFile file = new RemoteFile();
        file.setName(UploadMediaUseCase.UPLOAD_PARENT_FOLDER_NAME);
        file.setUuid(testUploadParentFolderUUID);

        files.add(file);

        captor.getValue().onSucceed(files, new OperationSuccess());

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> uploadFolderCallbackCaptor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(eq(testUserHome), eq(testUploadParentFolderUUID), uploadFolderCallbackCaptor.capture());

        AbstractRemoteFile uploadFolderFile = new RemoteFile();
        uploadFolderFile.setName(UploadMediaUseCase.UPLOAD_FOLDER_NAME_PREFIX + testUploadFolderName);
        uploadFolderFile.setUuid(testUploadFolderUUID);

        files.clear();
        files.add(uploadFolderFile);

        uploadFolderCallbackCaptor.getValue().onSucceed(files, new OperationSuccess());

        assertNull(uploadMediaUseCase.uploadedMediaHashs);

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> getUploadMediaHashCallbackCaptor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(eq(testUserHome), eq(testUploadFolderUUID), getUploadMediaHashCallbackCaptor.capture());

        AbstractRemoteFile uploadFile = new RemoteFile();
        ((RemoteFile) uploadFile).setFileHash(testMediaUUID);

        getUploadMediaHashCallbackCaptor.getValue().onSucceed(Collections.singletonList(uploadFile), new OperationSuccess());

        verify(checkMediaIsUploadStrategy).setUploadedMediaHashs(ArgumentMatchers.<String>anyList());

        verify(systemSettingDataSource).getAutoUploadOrNot();

        verify(stationFileRepository, never()).createFolder(anyString(), anyString(), anyString(), any(BaseOperateDataCallback.class));

    }

    @Test
    public void testUploadFileOccur404() {

        prepareStartUpload();

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        Media media = createMedia();

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.singletonList(media));

        uploadWhenUploadParentFolderExistAndUploadFolderExist();

        ArgumentCaptor<BaseOperateDataCallback<Boolean>> captor = ArgumentCaptor.forClass(BaseOperateDataCallback.class);

        verify(stationFileRepository).uploadFile(any(LocalFile.class), eq(testUserHome), eq(testUploadFolderUUID), captor.capture());

        captor.getValue().onFail(new OperationNetworkException(404));

        assertNull(uploadMediaUseCase.uploadParentFolderUUID);
        assertNull(uploadMediaUseCase.uploadFolderUUID);

    }

    @NonNull
    private Media createMedia() {
        testMediaUUID = "testMediaUUID";
        testMediaOriginalPhotoPath = "testMediaOriginalPhotoPath";

        Media media = new Media();
        media.setUuid(testMediaUUID);
        media.setOriginalPhotoPath(testMediaOriginalPhotoPath);
        return media;
    }

    @Test
    public void testStartUploadWhenUploadParentFolderUUIDNotNullAndUploadFolderUUIDNotNull() {

        prepareStartUpload();

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.<Media>emptyList());

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        uploadMediaUseCase.uploadParentFolderUUID = "";
        uploadMediaUseCase.uploadFolderUUID = "";

        uploadMediaUseCase.startUploadMedia();

        verify(stationFileRepository, never()).getFile(anyString(), anyString(), any(BaseLoadDataCallback.class));

        verify(systemSettingDataSource).getAutoUploadOrNot();

        verify(stationFileRepository, never()).createFolder(anyString(), anyString(), anyString(), any(BaseOperateDataCallback.class));

        assertFalse(uploadMediaUseCase.mAlreadyStartUpload);
        assertTrue(uploadMediaUseCase.mStopUpload);

    }


    @Test
    public void testFirstStartUploadNoAutoUploadPermissionThenSecondStartUploadHasAutoUploadPermission() {

        prepareStartUpload();

        Media media = createMedia();

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.singletonList(media));

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(false);

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        uploadWhenUploadParentFolderExistAndUploadFolderExist();

        verify(stationFileRepository, never()).uploadFile(any(LocalFile.class), anyString(), anyString(), any(BaseOperateDataCallback.class));

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        uploadMediaUseCase.startUploadMedia();

        verify(stationFileRepository).uploadFile(any(LocalFile.class), anyString(), anyString(), any(BaseOperateDataCallback.class));

    }

    @Test
    public void testGetUploadParentFolderUUIDFail() {

        prepareStartUpload();

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.<Media>emptyList());

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        uploadMediaUseCase.startUploadMedia();

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> getUploadParentFolderUUIDCallbackCaptor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(anyString(), anyString(), getUploadParentFolderUUIDCallbackCaptor.capture());

        getUploadParentFolderUUIDCallbackCaptor.getValue().onFail(new OperationIOException());

        assertFalse(uploadMediaUseCase.mAlreadyStartUpload);
        assertTrue(uploadMediaUseCase.mStopUpload);

    }


    @Test
    public void testCreateFolderFail() {

        prepareStartUpload();

        when(mediaDataSourceRepository.getLocalMedia()).thenReturn(Collections.<Media>emptyList());

        when(systemSettingDataSource.getAutoUploadOrNot()).thenReturn(true);

        when(checkMediaIsUploadStrategy.isMediaUploaded(any(Media.class))).thenReturn(false);

        uploadMediaUseCase.startUploadMedia();

        ArgumentCaptor<BaseLoadDataCallback<AbstractRemoteFile>> getUploadParentFolderUUIDCallbackCaptor = ArgumentCaptor.forClass(BaseLoadDataCallback.class);

        verify(stationFileRepository).getFile(anyString(),anyString(), getUploadParentFolderUUIDCallbackCaptor.capture());

        getUploadParentFolderUUIDCallbackCaptor.getValue().onSucceed(Collections.<AbstractRemoteFile>emptyList(), new OperationSuccess());

        ArgumentCaptor<BaseOperateDataCallback<HttpResponse>> createFolderCaptor = ArgumentCaptor.forClass(BaseOperateDataCallback.class);

        verify(stationFileRepository).createFolder(anyString(), anyString(), anyString(), createFolderCaptor.capture());

        createFolderCaptor.getValue().onFail(new OperationIOException());

        assertFalse(uploadMediaUseCase.mAlreadyStartUpload);
        assertTrue(uploadMediaUseCase.mStopUpload);

    }


}
