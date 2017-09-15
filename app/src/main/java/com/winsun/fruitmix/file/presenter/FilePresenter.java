package com.winsun.fruitmix.file.presenter;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.winsun.fruitmix.BR;
import com.winsun.fruitmix.R;
import com.winsun.fruitmix.callback.BaseLoadDataCallback;
import com.winsun.fruitmix.callback.BaseOperateDataCallback;
import com.winsun.fruitmix.command.AbstractCommand;
import com.winsun.fruitmix.command.ChangeToDownloadPageCommand;
import com.winsun.fruitmix.command.DownloadFileCommand;
import com.winsun.fruitmix.command.MacroCommand;
import com.winsun.fruitmix.command.NullCommand;
import com.winsun.fruitmix.command.OpenFileCommand;
import com.winsun.fruitmix.command.ShowSelectModeViewCommand;
import com.winsun.fruitmix.command.ShowUnSelectModeViewCommand;
import com.winsun.fruitmix.databinding.RemoteFileItemLayoutBinding;
import com.winsun.fruitmix.databinding.RemoteFolderItemLayoutBinding;
import com.winsun.fruitmix.dialog.BottomMenuDialogFactory;
import com.winsun.fruitmix.eventbus.DownloadStateChangedEvent;
import com.winsun.fruitmix.eventbus.OperationEvent;
import com.winsun.fruitmix.file.data.download.DownloadState;
import com.winsun.fruitmix.file.data.download.DownloadedFileWrapper;
import com.winsun.fruitmix.file.data.download.FileDownloadItem;
import com.winsun.fruitmix.file.data.download.FileDownloadManager;
import com.winsun.fruitmix.file.data.model.AbstractRemoteFile;
import com.winsun.fruitmix.file.data.model.RemoteFile;
import com.winsun.fruitmix.file.data.model.RemoteFolder;
import com.winsun.fruitmix.file.data.station.StationFileRepository;
import com.winsun.fruitmix.file.view.FileDownloadActivity;
import com.winsun.fruitmix.file.view.interfaces.FileListSelectModeListener;
import com.winsun.fruitmix.file.view.interfaces.FileView;
import com.winsun.fruitmix.file.view.interfaces.HandleFileListOperateCallback;
import com.winsun.fruitmix.file.view.viewmodel.FileItemViewModel;
import com.winsun.fruitmix.file.view.viewmodel.FileViewModel;
import com.winsun.fruitmix.interfaces.OnViewSelectListener;
import com.winsun.fruitmix.logged.in.user.LoggedInUserDataSource;
import com.winsun.fruitmix.model.BottomMenuItem;
import com.winsun.fruitmix.model.operationResult.OperationResult;
import com.winsun.fruitmix.system.setting.SystemSettingDataSource;
import com.winsun.fruitmix.user.datasource.UserDataRepository;
import com.winsun.fruitmix.util.FileUtil;
import com.winsun.fruitmix.util.Util;
import com.winsun.fruitmix.viewholder.BaseBindingViewHolder;
import com.winsun.fruitmix.viewmodel.LoadingViewModel;
import com.winsun.fruitmix.viewmodel.NoContentViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Administrator on 2017/7/27.
 */

public class FilePresenter implements OnViewSelectListener {

    private FileRecyclerViewAdapter fileRecyclerViewAdapter;

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

    private AbstractCommand nullCommand;

    private String rootUUID;

    private List<String> alreadyDownloadedFilePathForShare;

    private List<AbstractRemoteFile> needDownloadFilesForShare;

    private ProgressDialog currentDownloadFileForShareProgressDialog;

    private MacroCommand currentDownloadFileForShareCommand;

    private ProgressDialog currentDownloadFileProgressDialog;

    private int progressMax = 100;

    private DownloadFileCommand mCurrentDownloadFileCommand;

    private boolean cancelDownload = false;

    private NoContentViewModel noContentViewModel;
    private LoadingViewModel loadingViewModel;

    private FileViewModel fileViewModel;

    private Activity activity;

    private FileView fileView;

    private HandleFileListOperateCallback handleFileListOperateCallback;

    private ChangeToDownloadPageCommand.ChangeToDownloadPageCallback changeToDownloadPageCallback;

    private StationFileRepository stationFileRepository;

    private String currentUserUUID;

    private FileDownloadManager fileDownloadManager;

    private FileListSelectModeListener fileListSelectModeListener;

    public FilePresenter(Activity activity, FileView fileView, FileListSelectModeListener fileListSelectModeListener, StationFileRepository stationFileRepository, NoContentViewModel noContentViewModel, LoadingViewModel loadingViewModel, FileViewModel fileViewModel,
                         HandleFileListOperateCallback handleFileListOperateCallback, UserDataRepository userDataRepository, SystemSettingDataSource systemSettingDataSource, FileDownloadManager fileDownloadManager) {
        this.activity = activity;
        this.fileView = fileView;
        this.fileListSelectModeListener = fileListSelectModeListener;
        this.stationFileRepository = stationFileRepository;
        this.noContentViewModel = noContentViewModel;
        this.loadingViewModel = loadingViewModel;
        this.fileViewModel = fileViewModel;
        this.handleFileListOperateCallback = handleFileListOperateCallback;
        this.fileDownloadManager = fileDownloadManager;

        currentUserUUID = systemSettingDataSource.getCurrentLoginUserUUID();
        rootUUID = userDataRepository.getUserByUUID(currentUserUUID).getHome();

        init();
    }

    private void init() {
        abstractRemoteFiles = new ArrayList<>();

        retrievedFolderUUIDList = new ArrayList<>();
        retrievedFolderNameList = new ArrayList<>();

        selectedFiles = new ArrayList<>();

        showUnSelectModeViewCommand = new ShowUnSelectModeViewCommand(this);

        showSelectModeViewCommand = new ShowSelectModeViewCommand(this);

        nullCommand = new NullCommand();

        changeToDownloadPageCallback = new ChangeToDownloadPageCommand.ChangeToDownloadPageCallback() {
            @Override
            public void changeToDownloadPage() {
                activity.startActivity(new Intent(activity, FileDownloadActivity.class));
            }
        };

        fileRecyclerViewAdapter = new FileRecyclerViewAdapter();

        currentFolderUUID = rootUUID;
        currentFolderName = activity.getString(R.string.file);

    }


    private class ReDownloadCommand extends AbstractCommand {

        private AbstractRemoteFile abstractRemoteFile;

        ReDownloadCommand(AbstractRemoteFile abstractRemoteFile) {
            this.abstractRemoteFile = abstractRemoteFile;
        }

        @Override
        public void execute() {

            File file = new File(FileUtil.getDownloadFileStoreFolderPath(), abstractRemoteFile.getName());

            if (file.exists()) {

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                builder.setMessage("是否下载并覆盖原文件").setPositiveButton("覆盖", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        DownloadedFileWrapper downloadedFileWrapper = new DownloadedFileWrapper(abstractRemoteFile.getUuid(), abstractRemoteFile.getName());

                        stationFileRepository.deleteDownloadedFile(Collections.singleton(downloadedFileWrapper), currentUserUUID, new BaseOperateDataCallback<Void>() {
                            @Override
                            public void onSucceed(Void data, OperationResult result) {

                                MacroCommand macroCommand = createDownloadFileMarcoCommand(abstractRemoteFile);

                                macroCommand.execute();
                            }

                            @Override
                            public void onFail(OperationResult result) {

                                Toast.makeText(activity, "删除原文件失败", Toast.LENGTH_SHORT).show();

                            }
                        });


                    }
                }).setNegativeButton(activity.getString(R.string.cancel), null)
                        .setCancelable(false).create().show();


            } else {

                MacroCommand macroCommand = createDownloadFileMarcoCommand(abstractRemoteFile);

                macroCommand.execute();
            }

        }

        @Override
        public void unExecute() {

        }
    }

    private MacroCommand createDownloadFileMarcoCommand(AbstractRemoteFile abstractRemoteFile) {

        MacroCommand macroCommand = new MacroCommand();

        AbstractCommand downloadFileCommand = new DownloadFileCommand(fileDownloadManager, abstractRemoteFile, stationFileRepository, currentUserUUID, rootUUID);
        macroCommand.addCommand(downloadFileCommand);
        macroCommand.addCommand(new ChangeToDownloadPageCommand(changeToDownloadPageCallback));

        return macroCommand;
    }


    public FileRecyclerViewAdapter getFileRecyclerViewAdapter() {
        return fileRecyclerViewAdapter;
    }

    public void refreshView(boolean force) {

        if (force)
            remoteFileLoaded = false;

        if (!remoteFileLoaded) {

            if (!retrievedFolderUUIDList.contains(currentFolderUUID)) {
                retrievedFolderUUIDList.add(currentFolderUUID);
                retrievedFolderNameList.add(currentFolderName);
            }

            getFile();

        }

    }

    public void refreshCurrentFolder() {

        fileViewModel.swipeRefreshEnabled.set(true);

        getFileInThread();

    }

    private void getFile() {

        noContentViewModel.showNoContent.set(false);

        loadingViewModel.showLoading.set(true);

        fileViewModel.swipeRefreshEnabled.set(false);

        getFileInThread();

    }

    private void getFileInThread() {
        stationFileRepository.getFile(rootUUID, currentFolderUUID, new BaseLoadDataCallback<AbstractRemoteFile>() {
            @Override
            public void onSucceed(final List<AbstractRemoteFile> data, OperationResult operationResult) {

                handleGetFileSucceed(data);

            }

            @Override
            public void onFail(OperationResult operationResult) {

                handleGetFileFail();

            }
        });
    }

    private void handleGetFileFail() {

        loadingViewModel.showLoading.set(false);

        fileViewModel.swipeRefreshEnabled.set(true);
        fileView.setSwipeRefreshing(false);

        noContentViewModel.showNoContent.set(true);

        abstractRemoteFiles.clear();
    }

    private void handleGetFileSucceed(List<AbstractRemoteFile> files) {

        loadingViewModel.showLoading.set(false);

        fileViewModel.swipeRefreshEnabled.set(true);
        fileView.setSwipeRefreshing(false);

        remoteFileLoaded = true;

        if (files.size() == 0) {

            noContentViewModel.showNoContent.set(true);

            fileViewModel.showFileRecyclerView.set(false);

            abstractRemoteFiles.clear();

        } else {

            noContentViewModel.showNoContent.set(false);

            fileViewModel.showFileRecyclerView.set(true);

            abstractRemoteFiles.clear();
            abstractRemoteFiles.addAll(files);

            sortFile(abstractRemoteFiles);

            fileRecyclerViewAdapter.notifyDataSetChanged();
        }
    }


    private void sortFile(List<AbstractRemoteFile> abstractRemoteFiles) {

        List<AbstractRemoteFile> files = new ArrayList<>();
        List<AbstractRemoteFile> folders = new ArrayList<>();

        for (AbstractRemoteFile abstractRemoteFile : abstractRemoteFiles) {
            if (abstractRemoteFile.isFolder())
                folders.add(abstractRemoteFile);
            else
                files.add(abstractRemoteFile);
        }

        Comparator<AbstractRemoteFile> comparator = new Comparator<AbstractRemoteFile>() {
            @Override
            public int compare(AbstractRemoteFile lhs, AbstractRemoteFile rhs) {
                return (int) (Long.parseLong(lhs.getTime()) - Long.parseLong(rhs.getTime()));
            }
        };

        Collections.sort(folders, comparator);
        Collections.sort(files, comparator);

        abstractRemoteFiles.clear();
        abstractRemoteFiles.addAll(folders);
        abstractRemoteFiles.addAll(files);

    }

    public void handleEvent(DownloadStateChangedEvent downloadStateChangedEvent) {

        DownloadState downloadState = downloadStateChangedEvent.getDownloadState();

        switch (downloadState) {
            case START_DOWNLOAD:
            case PENDING:
                break;
            case DOWNLOADING:

                if (mCurrentDownloadFileCommand != null){

                    FileDownloadItem fileDownloadItem = mCurrentDownloadFileCommand.getFileDownloadItem();
                    currentDownloadFileProgressDialog.setProgress(fileDownloadItem.getCurrentProgress(progressMax));

                }

                break;
            case FINISHED:

                if (mCurrentDownloadFileCommand != null) {

                    FileDownloadItem fileDownloadItem = mCurrentDownloadFileCommand.getFileDownloadItem();

                    mCurrentDownloadFileCommand = null;

                    currentDownloadFileProgressDialog.dismiss();

                    OpenFileCommand openFileCommand = new OpenFileCommand(activity, fileDownloadItem.getFileName());
                    openFileCommand.execute();

                } else {

                    checkFileForShareDownloaded(needDownloadFilesForShare);

                    if (needDownloadFilesForShare.isEmpty()) {

                        currentDownloadFileForShareCommand = null;

                        currentDownloadFileForShareProgressDialog.dismiss();

                        FileUtil.sendShareToOtherApp(activity, alreadyDownloadedFilePathForShare);
                    }

                }

                break;
            case ERROR:

                if (mCurrentDownloadFileCommand != null) {

                    mCurrentDownloadFileCommand.unExecute();

                    mCurrentDownloadFileCommand = null;

                    currentDownloadFileProgressDialog.dismiss();

                    if (cancelDownload)
                        cancelDownload = false;
                    else
                        Toast.makeText(activity, activity.getText(R.string.download_failed), Toast.LENGTH_SHORT).show();

                } else {

                    currentDownloadFileForShareCommand.unExecute();

                    currentDownloadFileForShareCommand = null;

                    currentDownloadFileForShareProgressDialog.dismiss();

                    Toast.makeText(activity, activity.getString(R.string.download_failed), Toast.LENGTH_SHORT).show();


                }

                break;
            case NO_ENOUGH_SPACE:

                if (mCurrentDownloadFileCommand != null) {

                    mCurrentDownloadFileCommand.unExecute();

                    mCurrentDownloadFileCommand = null;

                    currentDownloadFileProgressDialog.dismiss();

                    Toast.makeText(activity, activity.getString(R.string.no_enough_space), Toast.LENGTH_SHORT).show();

                } else {

                    currentDownloadFileForShareCommand.unExecute();

                    currentDownloadFileForShareCommand = null;

                    currentDownloadFileForShareProgressDialog.dismiss();

                    Toast.makeText(activity, activity.getString(R.string.no_enough_space), Toast.LENGTH_SHORT).show();

                }

                break;
        }

    }

    public void handleOperationEvent(OperationEvent operationEvent) {

    }

    public String getCurrentFolderName() {
        return currentFolderName;
    }

    public boolean handleBackPressedOrNot() {
        return selectMode || notRootFolder();
    }

    private boolean notRootFolder() {

        return !currentFolderUUID.equals(rootUUID);
    }

    public void onBackPressed() {

        if (selectMode) {
            unSelectMode();
        } else if (notRootFolder()) {

            if (loadingViewModel.showLoading.get() && !noContentViewModel.showNoContent.get())
                return;

            retrievedFolderUUIDList.remove(retrievedFolderUUIDList.size() - 1);
            currentFolderUUID = retrievedFolderUUIDList.get(retrievedFolderUUIDList.size() - 1);

            retrievedFolderNameList.remove(retrievedFolderNameList.size() - 1);
            currentFolderName = retrievedFolderNameList.get(retrievedFolderNameList.size() - 1);

            getFile();

            handleFileListOperateCallback.handleFileListOperate(currentFolderName);

        }

    }

    public void onDestroy() {

        activity = null;

    }

    public boolean canEnterSelectMode() {

        for (AbstractRemoteFile file : abstractRemoteFiles) {

            if (file instanceof RemoteFile) {
                return true;
            }

        }
        return false;
    }

    public void quitSelectMode() {
        unSelectMode();
    }

    public void enterSelectMode() {
        selectMode();
    }

    @Override
    public void selectMode() {

        selectMode = true;
        refreshSelectMode(selectMode, null);
    }

    @Override
    public void unSelectMode() {

        selectMode = false;
        refreshSelectMode(selectMode, null);
    }

    private void refreshSelectMode(boolean selectMode, AbstractRemoteFile selectFile) {

        this.selectMode = selectMode;

        if (selectMode)
            fileViewModel.swipeRefreshEnabled.set(false);
        else
            fileViewModel.swipeRefreshEnabled.set(true);

        selectedFiles.clear();
        if (selectFile != null)
            selectedFiles.add(selectFile);

        fileRecyclerViewAdapter.notifyDataSetChanged();

    }


    public void shareSelectFilesToOtherApp(Context context) {

        alreadyDownloadedFilePathForShare = new ArrayList<>();

        needDownloadFilesForShare = new ArrayList<>();

        checkFileForShareDownloaded(selectedFiles);

        if (selectedFiles.isEmpty()) {
            FileUtil.sendShareToOtherApp(context, alreadyDownloadedFilePathForShare);
        } else {

            needDownloadFilesForShare.addAll(selectedFiles);

            currentDownloadFileForShareProgressDialog = ProgressDialog.show(context, null, String.format(context.getString(R.string.operating_title), context.getString(R.string.download_select_item)), true, true);
            currentDownloadFileForShareProgressDialog.setCanceledOnTouchOutside(false);

            currentDownloadFileForShareCommand = new MacroCommand();
            addSelectFilesToMacroCommand(currentDownloadFileForShareCommand, needDownloadFilesForShare);

            currentDownloadFileForShareCommand.execute();

        }

    }

    private void checkFileForShareDownloaded(List<AbstractRemoteFile> files) {

        Iterator<AbstractRemoteFile> iterator = files.iterator();

        while (iterator.hasNext()) {

            AbstractRemoteFile file = iterator.next();

            if (fileDownloadManager.checkIsDownloaded(file.getUuid())) {

                alreadyDownloadedFilePathForShare.add(FileUtil.getDownloadFileStoreFolderPath() + file.getName());

                iterator.remove();
            }

        }

    }


    public void requestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case Util.WRITE_EXTERNAL_STORAGE_REQUEST_CODE:

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    openFileWhenOnClick();

                } else {

                    Toast.makeText(activity, activity.getString(R.string.android_no_write_external_storage_permission), Toast.LENGTH_SHORT).show();

                }

        }

    }

    public void checkWriteExternalStoragePermission() {

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Util.WRITE_EXTERNAL_STORAGE_REQUEST_CODE);

        } else {
            openFileWhenOnClick();
        }

    }

    private void openFileWhenOnClick() {

        mCurrentDownloadFileCommand = new DownloadFileCommand(fileDownloadManager, selectedFiles.get(0), stationFileRepository, currentFolderUUID, rootUUID);

        currentDownloadFileProgressDialog = new ProgressDialog(activity);

        currentDownloadFileProgressDialog.setTitle(activity.getString(R.string.downloading));

        currentDownloadFileProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        currentDownloadFileProgressDialog.setIndeterminate(false);

        currentDownloadFileProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getText(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCurrentDownloadFileCommand.unExecute();

                mCurrentDownloadFileCommand = null;

                cancelDownload = true;

                currentDownloadFileProgressDialog.dismiss();
            }
        });

        currentDownloadFileProgressDialog.setMax(progressMax);

        currentDownloadFileProgressDialog.setCancelable(false);

        currentDownloadFileProgressDialog.show();

        mCurrentDownloadFileCommand.execute();
    }

    public Dialog getBottomSheetDialog(List<BottomMenuItem> bottomMenuItems) {

        Dialog dialog = new BottomMenuDialogFactory(bottomMenuItems).createDialog(activity);

        for (BottomMenuItem bottomMenuItem : bottomMenuItems) {
            bottomMenuItem.setDialog(dialog);
        }

        return dialog;
    }

    public List<BottomMenuItem> getMainMenuItem() {

        List<BottomMenuItem> bottomMenuItems = new ArrayList<>();

        if (selectMode) {

            BottomMenuItem clearSelectItem = new BottomMenuItem(R.drawable.cancel, activity.getString(R.string.clear_select_item), showUnSelectModeViewCommand);

            bottomMenuItems.add(clearSelectItem);

            MacroCommand macroCommand = createDownloadSelectFilesCommand();

            BottomMenuItem downloadSelectItem = new BottomMenuItem(R.drawable.download, activity.getString(R.string.download_select_item), macroCommand);

            bottomMenuItems.add(downloadSelectItem);

        } else {

            BottomMenuItem selectItem = new BottomMenuItem(R.drawable.check, activity.getString(R.string.select_file), showSelectModeViewCommand);

            if (abstractRemoteFiles.isEmpty())
                selectItem.setDisable(true);

            bottomMenuItems.add(selectItem);
        }

        BottomMenuItem cancelMenuItem = new BottomMenuItem(R.drawable.close, activity.getString(R.string.cancel), nullCommand);

        bottomMenuItems.add(cancelMenuItem);

        return bottomMenuItems;

    }

    public void downloadSelectItems() {

        MacroCommand macroCommand = createDownloadSelectFilesCommand();

        macroCommand.execute();

    }

    private MacroCommand createDownloadSelectFilesCommand() {
        MacroCommand macroCommand = new MacroCommand();

        addSelectFilesToMacroCommand(macroCommand, selectedFiles);

        macroCommand.addCommand(showUnSelectModeViewCommand);

        macroCommand.addCommand(new ChangeToDownloadPageCommand(changeToDownloadPageCallback));

        return macroCommand;
    }

    private void addSelectFilesToMacroCommand(AbstractCommand macroCommand, List<AbstractRemoteFile> files) {

        for (AbstractRemoteFile abstractRemoteFile : files) {

            AbstractCommand abstractCommand = new DownloadFileCommand(fileDownloadManager, abstractRemoteFile, stationFileRepository, currentUserUUID, rootUUID);
            macroCommand.addCommand(abstractCommand);

        }

    }


    class FileRecyclerViewAdapter extends RecyclerView.Adapter<BaseBindingViewHolder> {

        private static final int VIEW_FILE = 0;
        private static final int VIEW_FOLDER = 1;

        @Override
        public int getItemCount() {
            return abstractRemoteFiles.size();
        }

        @Override
        public BaseBindingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            ViewDataBinding binding;
            BaseBindingViewHolder viewHolder;

            switch (viewType) {
                case VIEW_FILE:

                    binding = RemoteFileItemLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

                    viewHolder = new FileViewHolder(binding);

                    break;
                case VIEW_FOLDER:

                    binding = RemoteFolderItemLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

                    viewHolder = new FolderViewHolder(binding);
                    break;
                default:
                    binding = RemoteFileItemLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

                    viewHolder = new FileViewHolder(binding);
            }


            return viewHolder;
        }

        @Override
        public void onBindViewHolder(BaseBindingViewHolder holder, int position) {

            holder.getViewDataBinding().setVariable(BR.file, abstractRemoteFiles.get(position));

            holder.refreshView(position);

            holder.getViewDataBinding().executePendingBindings();

        }

        @Override
        public int getItemViewType(int position) {

            return abstractRemoteFiles.get(position).isFolder() ? VIEW_FOLDER : VIEW_FILE;

        }

    }

    class FolderViewHolder extends BaseBindingViewHolder {

        LinearLayout folderItemLayout;

        RelativeLayout contentLayout;

        private RemoteFolderItemLayoutBinding binding;

        FolderViewHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);

            binding = (RemoteFolderItemLayoutBinding) viewDataBinding;

            contentLayout = binding.contentLayout;

            folderItemLayout = binding.remoteFolderItemLayout;

        }

        @Override
        public void refreshView(int position) {

            if (position == 0) {

                Util.setMargin(contentLayout, 0, Util.dip2px(activity, 8), Util.dip2px(activity, 16), 0);

            }

            final RemoteFolder abstractRemoteFile = (RemoteFolder) abstractRemoteFiles.get(position);

            FileItemViewModel fileItemViewModel = binding.getFileItemViewModel();

            if (fileItemViewModel == null)
                fileItemViewModel = new FileItemViewModel();

            fileItemViewModel.selectMode.set(selectMode);

            folderItemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (selectMode)
                        return;

                    currentFolderUUID = abstractRemoteFile.getUuid();

                    retrievedFolderUUIDList.add(currentFolderUUID);

                    currentFolderName = abstractRemoteFile.getName();

                    retrievedFolderNameList.add(currentFolderName);

                    getFile();

                    handleFileListOperateCallback.handleFileListOperate(currentFolderName);
                }
            });

            binding.setFileItemViewModel(fileItemViewModel);

        }
    }


    private class FileViewHolder extends BaseBindingViewHolder {

        LinearLayout remoteFileItemLayout;

        RelativeLayout contentLayout;

        ImageButton itemMenu;

        private RemoteFileItemLayoutBinding binding;

        FileViewHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);

            binding = (RemoteFileItemLayoutBinding) viewDataBinding;

            contentLayout = binding.contentLayout;
            remoteFileItemLayout = binding.remoteFileItemLayout;
            itemMenu = binding.itemMenu;

        }

        @Override
        public void refreshView(int position) {

            if (position == 0) {

                Util.setMargin(contentLayout, 0, Util.dip2px(activity, 8), 0, 0);

            }

            final RemoteFile abstractRemoteFile = (RemoteFile) abstractRemoteFiles.get(position);

            FileItemViewModel fileItemViewModel = binding.getFileItemViewModel();

            if (fileItemViewModel == null)
                fileItemViewModel = new FileItemViewModel();

            fileItemViewModel.selectMode.set(selectMode);

            if (selectMode) {

                toggleFileIconBgResource(fileItemViewModel, abstractRemoteFile);

                remoteFileItemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        toggleFileInSelectedFile(abstractRemoteFile);
                        toggleFileIconBgResource(binding.getFileItemViewModel(), abstractRemoteFile);

                        fileListSelectModeListener.onFileSelectItemClick(selectedFiles.size());

                    }
                });

                remoteFileItemLayout.setOnLongClickListener(null);

            } else {

                fileItemViewModel.showFileIcon.set(true);

                itemMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        List<BottomMenuItem> bottomMenuItems = new ArrayList<>();

                        BottomMenuItem menuItem;
                        if (fileDownloadManager.checkIsDownloaded(abstractRemoteFile.getUuid())) {

                            bottomMenuItems.add(new BottomMenuItem(R.drawable.download, activity.getString(R.string.re_download_the_item), new ReDownloadCommand(abstractRemoteFile)));

                            menuItem = new BottomMenuItem(R.drawable.file_icon, activity.getString(R.string.open_the_item), new OpenFileCommand(activity, abstractRemoteFile.getName()));

                        } else {
                            AbstractCommand macroCommand = new MacroCommand();
                            AbstractCommand downloadFileCommand = new DownloadFileCommand(fileDownloadManager, abstractRemoteFile, stationFileRepository, currentUserUUID, rootUUID);
                            macroCommand.addCommand(downloadFileCommand);
                            macroCommand.addCommand(new ChangeToDownloadPageCommand(changeToDownloadPageCallback));

                            menuItem = new BottomMenuItem(R.drawable.download, activity.getString(R.string.download_the_item), macroCommand);
                        }
                        bottomMenuItems.add(menuItem);

                        BottomMenuItem cancelMenuItem = new BottomMenuItem(R.drawable.close, activity.getString(R.string.cancel), nullCommand);
                        bottomMenuItems.add(cancelMenuItem);

                        getBottomSheetDialog(bottomMenuItems).show();
                    }
                });

                remoteFileItemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (fileDownloadManager.checkIsDownloaded(abstractRemoteFile.getUuid())) {

                            if (!abstractRemoteFile.openAbstractRemoteFile(activity, rootUUID)) {
                                Toast.makeText(activity, activity.getString(R.string.open_file_failed), Toast.LENGTH_SHORT).show();
                            }

                        } else {

                            selectedFiles.clear();
                            selectedFiles.add(abstractRemoteFile);

                            checkWriteExternalStoragePermission();
                        }


                    }
                });

                remoteFileItemLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        fileListSelectModeListener.onFileItemLongClick();

                        selectMode = true;
                        refreshSelectMode(selectMode, abstractRemoteFile);

                        return true;
                    }
                });

            }

            binding.setFileItemViewModel(fileItemViewModel);

        }

        private void toggleFileIconBgResource(FileItemViewModel fileItemViewModel, AbstractRemoteFile abstractRemoteFile) {
            if (selectedFiles.contains(abstractRemoteFile)) {

                fileItemViewModel.fileIconBg.set(R.drawable.check_circle_selected);
                fileItemViewModel.showFileIcon.set(false);


            } else {

                fileItemViewModel.fileIconBg.set(R.drawable.round_circle);
                fileItemViewModel.showFileIcon.set(true);

            }
        }

        private void toggleFileInSelectedFile(AbstractRemoteFile abstractRemoteFile) {
            if (selectedFiles.contains(abstractRemoteFile)) {
                selectedFiles.remove(abstractRemoteFile);
            } else {
                selectedFiles.add(abstractRemoteFile);
            }
        }

    }

}