package com.winsun.fruitmix.logged.in.user.androidtest;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.winsun.fruitmix.db.DBUtils;
import com.winsun.fruitmix.logged.in.user.LoggedInUserDBDataSource;
import com.winsun.fruitmix.logged.in.user.LoggedInUserDBSource;
import com.winsun.fruitmix.logged.in.user.LoggedInUserDataSource;
import com.winsun.fruitmix.logged.in.user.LoggedInUser;
import com.winsun.fruitmix.user.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Created by Administrator on 2017/7/4.
 */

@RunWith(AndroidJUnit4.class)
public class LoggedInUserDBDataSourceTest {

    private LoggedInUserDBSource loggedInUserDataSource;

    @Before
    public void init() {

        loggedInUserDataSource = LoggedInUserDBDataSource.getInstance(DBUtils.getInstance(InstrumentationRegistry.getTargetContext()));

    }

    @After
    public void cleanUp() {
        loggedInUserDataSource.clear();
    }

    private Collection<LoggedInUser> createLoggedInUsers() {

        Collection<LoggedInUser> loggedInUsers = new ArrayList<>();

        loggedInUsers.add(new LoggedInUser());

        return loggedInUsers;

    }

    @Test
    public void testClear() {

        loggedInUserDataSource.clear();

        assertEquals(0, loggedInUserDataSource.getAllLoggedInUsers().size());

    }

    @Test
    public void testInsertLoggedInUser() {

        int size = loggedInUserDataSource.getAllLoggedInUsers().size();

        loggedInUserDataSource.insertLoggedInUsers(createLoggedInUsers());

        size++;

        assertEquals(size, loggedInUserDataSource.getAllLoggedInUsers().size());

    }

    @Test
    public void testDeleteLoggedInUser() {

        loggedInUserDataSource.insertLoggedInUsers(createLoggedInUsers());

        boolean result = loggedInUserDataSource.deleteLoggedInUsers(Collections.singletonList(new LoggedInUser()));

        assertEquals(true, result);

    }

    @Test
    public void testGetLoggedInUserByUserUUID() {

        User user = new User();

        String testUserUUID = "testUserUUID";

        user.setUuid(testUserUUID);

        String testToken = "testToken";
        String testGateway = "testGateway";

        LoggedInUser loggedInUser = new LoggedInUser("", testToken, testGateway, "", user);

        loggedInUserDataSource.insertLoggedInUsers(Collections.singletonList(loggedInUser));

        LoggedInUser currentLoggedInUser = loggedInUserDataSource.getLoggedInUserByUserUUID(testUserUUID);

        assertEquals(testToken, currentLoggedInUser.getToken());
        assertEquals(testGateway, currentLoggedInUser.getGateway());
    }

}
