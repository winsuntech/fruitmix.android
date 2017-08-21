package com.winsun.fruitmix.logged.in.user;

import com.winsun.fruitmix.user.User;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Administrator on 2017/7/6.
 */

public class LoggedInUserRepository implements LoggedInUserDataSource {

    private static LoggedInUserRepository instance;

    private Collection<LoggedInUser> cacheLoggedInUsers;

    private LoggedInUser currentLoggedInUser;

    private LoggedInUserDataSource loggedInUserDBDataSource;

    private boolean loadedAllLoggedInUserFromDB = false;

    private String preQueryUserUUID;

    private LoggedInUserRepository(LoggedInUserDataSource loggedInUserDBDataSource) {
        this.loggedInUserDBDataSource = loggedInUserDBDataSource;

        cacheLoggedInUsers = new ArrayList<>();
    }

    public static LoggedInUserRepository getInstance(LoggedInUserDataSource loggedInUserDBDataSource) {

        if (instance == null)
            instance = new LoggedInUserRepository(loggedInUserDBDataSource);

        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }

    Collection<LoggedInUser> getCacheLoggedInUsers() {
        return cacheLoggedInUsers;
    }

    void setCacheLoggedInUsers(Collection<LoggedInUser> cacheLoggedInUsers) {
        this.cacheLoggedInUsers = cacheLoggedInUsers;
    }

    @Override
    public boolean insertLoggedInUsers(Collection<LoggedInUser> loggedInUsers) {

        cacheLoggedInUsers.addAll(loggedInUsers);

        return loggedInUserDBDataSource.insertLoggedInUsers(loggedInUsers);

    }

    @Override
    public boolean deleteLoggedInUsers(Collection<LoggedInUser> loggedInUsers) {

        cacheLoggedInUsers.removeAll(loggedInUsers);

        return loggedInUserDBDataSource.deleteLoggedInUsers(loggedInUsers);
    }

    @Override
    public boolean clear() {
        return false;
    }

    @Override
    public Collection<LoggedInUser> getAllLoggedInUsers() {

        if (!loadedAllLoggedInUserFromDB) {

            Collection<LoggedInUser> loggedInUsers = loggedInUserDBDataSource.getAllLoggedInUsers();

            cacheLoggedInUsers.addAll(loggedInUsers);

            loadedAllLoggedInUserFromDB = true;
        }

        return cacheLoggedInUsers;
    }

    @Override
    public LoggedInUser getLoggedInUserByUserUUID(String userUUID) {

        if (preQueryUserUUID == null || !preQueryUserUUID.equals(userUUID)) {

            currentLoggedInUser = loggedInUserDBDataSource.getLoggedInUserByUserUUID(userUUID);

            preQueryUserUUID = userUUID;

        }

        return currentLoggedInUser;
    }

}
