package com.winsun.fruitmix.logged.in.user;

import com.winsun.fruitmix.mock.MockThreadManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Administrator on 2017/7/6.
 */

public class LoggedInUserRepositoryTest {

    @Mock
    private LoggedInUserDBSource loggedInUserDataSource;

    @Mock
    private LoggedInUserRemoteDataSource mLoggedInUserRemoteDataSource;

    @Mock
    private Collection<LoggedInUser> cacheLoggedInUsers;

    private LoggedInUserRepository loggedInUserRepository;

    private Collection<LoggedInUser> createLoggedInUsers() {

        Collection<LoggedInUser> loggedInUsers = new ArrayList<>();

        loggedInUsers.add(new LoggedInUser());

        return loggedInUsers;

    }

    @Before
    public void init() {

        MockitoAnnotations.initMocks(this);

        loggedInUserRepository = LoggedInUserRepository.getInstance(new MockThreadManager(),loggedInUserDataSource,mLoggedInUserRemoteDataSource);

    }

    @After
    public void clean(){
        LoggedInUserRepository.destroyInstance();
    }

    @Test
    public void insertLoggedInUserTest() {

        Collection<LoggedInUser> loggedInUsers = createLoggedInUsers();

        loggedInUserRepository.insertLoggedInUsers(loggedInUsers);

        verify(loggedInUserDataSource).insertLoggedInUsers(ArgumentMatchers.<LoggedInUser>anyCollection());

        assertThat(loggedInUserRepository.getCacheLoggedInUsers().size(), is(loggedInUsers.size()));
    }

    @Test
    public void deleteLoggedInUserTest() {

        Collection<LoggedInUser> loggedInUsers = createLoggedInUsers();

        loggedInUserRepository.insertLoggedInUsers(loggedInUsers);

        loggedInUserRepository.deleteLoggedInUsers(loggedInUsers);

        verify(loggedInUserDataSource).deleteLoggedInUsers(ArgumentMatchers.<LoggedInUser>anyCollection());

        assertTrue(loggedInUserRepository.getCacheLoggedInUsers().isEmpty());

    }

    @Test
    public void getLoggedInUserFirstCallTest() {

        loggedInUserRepository.setCacheLoggedInUsers(cacheLoggedInUsers);

        loggedInUserRepository.getAllLoggedInUsers();

        verify(loggedInUserDataSource).getAllLoggedInUsers();

        verify(cacheLoggedInUsers).addAll(ArgumentMatchers.<LoggedInUser>anyCollection());

    }

    @Test
    public void getLoggedInUserSecondCallTest() {

        loggedInUserRepository.getAllLoggedInUsers();

        loggedInUserRepository.setCacheLoggedInUsers(cacheLoggedInUsers);

        loggedInUserRepository.getAllLoggedInUsers();

        verify(cacheLoggedInUsers, never()).addAll(ArgumentMatchers.<LoggedInUser>anyCollection());

    }


}
