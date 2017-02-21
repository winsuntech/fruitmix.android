package com.winsun.fruitmix.refactor.presenter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.view.View;

import com.winsun.fruitmix.R;
import com.winsun.fruitmix.command.AbstractCommand;
import com.winsun.fruitmix.command.ChangeToDownloadPageCommand;
import com.winsun.fruitmix.command.DownloadFileCommand;
import com.winsun.fruitmix.command.MacroCommand;
import com.winsun.fruitmix.command.NullCommand;
import com.winsun.fruitmix.command.OpenFileCommand;
import com.winsun.fruitmix.command.ShowSelectModeViewCommand;
import com.winsun.fruitmix.command.ShowUnSelectModeViewCommand;
import com.winsun.fruitmix.dialog.BottomMenuDialogFactory;
import com.winsun.fruitmix.fileModule.download.FileDownloadManager;
import com.winsun.fruitmix.fileModule.model.AbstractRemoteFile;
import com.winsun.fruitmix.fileModule.model.BottomMenuItem;
import com.winsun.fruitmix.interfaces.OnViewSelectListener;
import com.winsun.fruitmix.model.User;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.refactor.business.DataRepository;
import com.winsun.fruitmix.refactor.business.callback.FileOperationCallback;
import com.winsun.fruitmix.refactor.contract.FileFragmentContract;
import com.winsun.fruitmix.refactor.contract.FileMainFragmentContract;
import com.winsun.fruitmix.util.FileUtil;
import com.winsun.fruitmix.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Administrator on 2017/2/21.
 */

public class FileFragmentPresenterImpl implements FileFragmentContract.FileFragmentPresenter, OnViewSelectListener {

    private FileFragmentContract.FileFragmentView mView;

    private FileMainFragmentContract.FileMainFragmentPresenter fileMainFragmentPresenter;

    private List<AbstractRemoteFile> abstractRemoteFiles;

    private boolean remoteFileLoaded = false;

    private String currentFolderUUID;
    private String currentFolderName;

    private List<String> retrievedFolderUUIDList;
    private List<String> retrievedFolderNameList;

    private boolean selectMode = false;

    private List<AbstractRemoteFile> selectedFiles;

    private AbstractCommand showUnSelectModeViewCommand;

    private AbstractCommand showSelectModeViewCommand;

    private AbstractCommand macroCommand;

    private AbstractCommand nullCommand;

    private DataRepository mRepository;

    public FileFragmentPresenterImpl(FileMainFragmentContract.FileMainFragmentPresenter fileMainFragmentPresenter, DataRepository repository) {

        this.fileMainFragmentPresenter = fileMainFragmentPresenter;

        mRepository = repository;

        abstractRemoteFiles = new ArrayList<>();

        retrievedFolderUUIDList = new ArrayList<>();
        retrievedFolderNameList = new ArrayList<>();

        selectedFiles = new ArrayList<>();

        showUnSelectModeViewCommand = new ShowUnSelectModeViewCommand(this);

        showSelectModeViewCommand = new ShowSelectModeViewCommand(this);

        nullCommand = new NullCommand();

    }

    @Override
    public void onResume() {

        if (!remoteFileLoaded && !mView.isHidden()) {

            User user = mRepository.loadCurrentLoginUserFromMemory();

            currentFolderUUID = user.getHome();

            if (!retrievedFolderUUIDList.contains(currentFolderUUID)) {
                retrievedFolderUUIDList.add(currentFolderUUID);
                retrievedFolderNameList.add(mView.getString(R.string.file));
            }

            loadCurrentFolder(currentFolderUUID);
        }

    }

    @Override
    public void onDestroyView() {
        remoteFileLoaded = false;
    }

    @Override
    public void handleTitle() {
        if (handleBackPressedOrNot()) {

            fileMainFragmentPresenter.setTitleText(currentFolderName);

            fileMainFragmentPresenter.setNavigationIcon(R.drawable.ic_back);
            fileMainFragmentPresenter.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });

        } else {
            fileMainFragmentPresenter.setTitleText(mView.getString(R.string.file));
            fileMainFragmentPresenter.setNavigationIcon(R.drawable.menu);
            fileMainFragmentPresenter.setDefaultNavigationOnClickListener();
        }
    }

    private void onBackPressed() {

        if (notRootFolder()) {

            if (mView.isShowingLoadingUI())
                return;

            mView.showLoadingUI();

            retrievedFolderUUIDList.remove(retrievedFolderUUIDList.size() - 1);

            currentFolderUUID = retrievedFolderUUIDList.get(retrievedFolderUUIDList.size() - 1);

            retrievedFolderNameList.remove(retrievedFolderNameList.size() - 1);
            currentFolderName = retrievedFolderNameList.get(retrievedFolderUUIDList.size() - 1);

            loadCurrentFolder(currentFolderUUID);

        } else {
            addSelectedFiles(Collections.<AbstractRemoteFile>emptyList());
            refreshSelectMode(false);
        }

        handleTitle();

    }

    private void loadCurrentFolder(String folderUUID) {
        mRepository.loadRemoteFolderContent(folderUUID, new FileOperationCallback.LoadFileOperationCallback() {
            @Override
            public void onLoadSucceed(OperationResult result, List<AbstractRemoteFile> files) {

                mView.dismissLoadingUI();

                if (files.size() == 0) {
                    mView.showNoContentUI();
                    mView.dismissContentUI();
                } else {
                    mView.showContentUI();
                    mView.dismissNoContentUI();

                    abstractRemoteFiles.clear();
                    abstractRemoteFiles.addAll(files);
                    mView.showContents(abstractRemoteFiles, selectMode);
                }
            }

            @Override
            public void onLoadFail(OperationResult result) {
                mView.showNoContentUI();
                mView.dismissContentUI();
            }
        });
    }

    private boolean notRootFolder() {

        User user = mRepository.loadCurrentLoginUserFromMemory();

        String homeFolderUUID = user.getHome();

        return !currentFolderUUID.equals(homeFolderUUID);
    }

    @Override
    public void addSelectedFiles(List<AbstractRemoteFile> files) {
        selectedFiles.clear();
        if (files != null)
            selectedFiles.addAll(files);
    }

    @Override
    public void refreshSelectMode(boolean selectMode) {

        this.selectMode = selectMode;

        mView.showContents(abstractRemoteFiles, selectMode);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Util.WRITE_EXTERNAL_STORAGE_REQUEST_CODE:

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadSelectedFiles();
                } else {
                    mView.showNoWriteExternalStoragePermission();
                }

        }
    }

    @Override
    public boolean handleBackPressedOrNot() {
        return selectMode || notRootFolder();
    }

    @Override
    public void attachView(FileFragmentContract.FileFragmentView view) {
        mView = view;
    }

    @Override
    public void detachView() {
        mView = null;
    }

    @Override
    public void handleBackEvent() {

    }

    @Override
    public void handleOnActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void selectMode() {
        addSelectedFiles(Collections.<AbstractRemoteFile>emptyList());
        refreshSelectMode(true);
    }

    @Override
    public void unSelectMode() {
        addSelectedFiles(Collections.<AbstractRemoteFile>emptyList());
        refreshSelectMode(false);
    }

    @Override
    public void openAbstractRemoteFolder(AbstractRemoteFile abstractRemoteFile) {
        currentFolderUUID = abstractRemoteFile.getUuid();

        retrievedFolderUUIDList.add(currentFolderUUID);

        currentFolderName = abstractRemoteFile.getName();

        retrievedFolderNameList.add(currentFolderName);

        mView.showLoadingUI();

        loadCurrentFolder(abstractRemoteFile.getUuid());

        handleTitle();
    }

    @Override
    public boolean checkIsInSelectedFiles(AbstractRemoteFile file) {
        return selectedFiles.contains(file);
    }

    @Override
    public void toggleFileInSelectedFile(AbstractRemoteFile abstractRemoteFile) {
        if (selectedFiles.contains(abstractRemoteFile)) {
            selectedFiles.remove(abstractRemoteFile);
        } else {
            selectedFiles.add(abstractRemoteFile);
        }
    }

    @Override
    public void itemMenuOnClick(Context context, AbstractRemoteFile file) {

        List<BottomMenuItem> bottomMenuItems = new ArrayList<>();

        BottomMenuItem menuItem;
        if (FileDownloadManager.INSTANCE.checkIsDownloaded(file.getUuid())) {
            menuItem = new BottomMenuItem(mView.getString(R.string.open_the_item), new OpenFileCommand(context, file.getName()));
        } else {
            AbstractCommand macroCommand = new MacroCommand();
            AbstractCommand downloadFileCommand = new DownloadFileCommand(file);
            macroCommand.addCommand(downloadFileCommand);
            macroCommand.addCommand(new AbstractCommand() {
                @Override
                public void execute() {
                    fileMainFragmentPresenter.setBottomNavigationItemChecked(FileMainFragmentPresenterImpl.PAGE_FILE_DOWNLOAD);
                }

                @Override
                public void unExecute() {
                }
            });
            menuItem = new BottomMenuItem(mView.getString(R.string.download_the_item), macroCommand);
        }
        bottomMenuItems.add(menuItem);

        BottomMenuItem cancelMenuItem = new BottomMenuItem(mView.getString(R.string.cancel), nullCommand);
        bottomMenuItems.add(cancelMenuItem);

        mView.getBottomSheetDialog(bottomMenuItems).show();

    }

    @Override
    public void fileMainMenuOnClick() {
        mView.getBottomSheetDialog(getMainMenuItem()).show();
    }

    @Override
    public void fileItemOnClick(Context context, AbstractRemoteFile abstractRemoteFile) {

        FileDownloadManager fileDownloadManager = FileDownloadManager.INSTANCE;
        if (fileDownloadManager.checkIsDownloaded(abstractRemoteFile.getUuid())) {

            if (!FileUtil.openAbstractRemoteFile(context, abstractRemoteFile.getName())) {
                mView.showOpenFileFailToast();
            }


        } else {
            selectedFiles.add(abstractRemoteFile);

            mView.checkWriteExternalStoragePermission();
        }


    }

    private List<BottomMenuItem> getMainMenuItem() {

        List<BottomMenuItem> bottomMenuItems = new ArrayList<>();

        if (selectMode) {

            BottomMenuItem clearSelectItem = new BottomMenuItem(mView.getString(R.string.clear_select_item), showUnSelectModeViewCommand);

            bottomMenuItems.add(clearSelectItem);

            macroCommand = new MacroCommand();

            addSelectFilesToMacroCommand();

            macroCommand.addCommand(showUnSelectModeViewCommand);

            macroCommand.addCommand(new AbstractCommand() {
                @Override
                public void execute() {
                    fileMainFragmentPresenter.setBottomNavigationItemChecked(FileMainFragmentPresenterImpl.PAGE_FILE_DOWNLOAD);
                }

                @Override
                public void unExecute() {
                }
            });

            BottomMenuItem downloadSelectItem = new BottomMenuItem(mView.getString(R.string.download_select_item), macroCommand);

            bottomMenuItems.add(downloadSelectItem);

        } else {

            BottomMenuItem selectItem = new BottomMenuItem(mView.getString(R.string.select_file), showSelectModeViewCommand);

            bottomMenuItems.add(selectItem);
        }

        BottomMenuItem cancelMenuItem = new BottomMenuItem(mView.getString(R.string.cancel), nullCommand);

        bottomMenuItems.add(cancelMenuItem);

        return bottomMenuItems;

    }

    private void addSelectFilesToMacroCommand() {
        for (AbstractRemoteFile abstractRemoteFile : selectedFiles) {

            AbstractCommand abstractCommand = new DownloadFileCommand(abstractRemoteFile);
            macroCommand.addCommand(abstractCommand);

        }
    }

    @Override
    public void downloadSelectedFiles() {
        for (AbstractRemoteFile abstractRemoteFile : selectedFiles) {

            abstractRemoteFile.downloadFile();
        }

        fileMainFragmentPresenter.setBottomNavigationItemChecked(FileMainFragmentPresenterImpl.PAGE_FILE_DOWNLOAD);
    }

}
