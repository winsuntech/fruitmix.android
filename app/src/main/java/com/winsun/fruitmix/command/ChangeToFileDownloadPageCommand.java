package com.winsun.fruitmix.command;

import com.winsun.fruitmix.refactor.contract.FileMainFragmentContract;
import com.winsun.fruitmix.refactor.presenter.FileMainFragmentPresenterImpl;

/**
 * Created by Administrator on 2017/3/3.
 */

public class ChangeToFileDownloadPageCommand extends AbstractCommand {

    private FileMainFragmentContract.FileMainFragmentPresenter fileMainFragmentPresenter;

    public ChangeToFileDownloadPageCommand(FileMainFragmentContract.FileMainFragmentPresenter fileMainFragmentPresenter) {
        this.fileMainFragmentPresenter = fileMainFragmentPresenter;
    }

    @Override
    public void execute() {
        fileMainFragmentPresenter.setBottomNavigationItemChecked(FileMainFragmentPresenterImpl.PAGE_FILE_DOWNLOAD);
    }

    @Override
    public void unExecute() {

    }
}
