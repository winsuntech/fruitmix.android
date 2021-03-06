package com.winsun.fruitmix.group.data.source;

import com.winsun.fruitmix.file.data.model.AbstractFile;
import com.winsun.fruitmix.group.data.model.Pin;
import com.winsun.fruitmix.group.data.model.PrivateGroup;
import com.winsun.fruitmix.group.data.model.TextComment;
import com.winsun.fruitmix.group.data.model.UserComment;
import com.winsun.fruitmix.mediaModule.model.Media;
import com.winsun.fruitmix.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Administrator on 2017/7/20.
 */

public class FakeGroupDataSource implements GroupDataSource {

    private static FakeGroupDataSource instance;

    private List<PrivateGroup> mPrivateGroups;

    public static final String AIMI_UUID = "aimi_uuid";
    public static final String NAOMI_UUID = "naomi_uuid";
    public static final String MYSELF_UUID = "myself_uuid";

    private FakeGroupDataSource() {
        mPrivateGroups = new ArrayList<>();
    }

    public static GroupDataSource getInstance() {

        if (instance == null) {
            instance = new FakeGroupDataSource();
        }

        return instance;
    }

    public void setCurrentUser(User currentUser) {

        addTestData(currentUser);

    }

    private void addTestData(User myself) {

        if (!mPrivateGroups.isEmpty())
            return;

        List<User> users = new ArrayList<>();

        User aimi = new User();
        aimi.setUserName("Aimi");
        aimi.setUuid(AIMI_UUID);
        users.add(aimi);

        User naomi = new User();
        naomi.setUserName("Naomi");
        naomi.setUuid(NAOMI_UUID);
        users.add(naomi);

//        User myself = new User();
//        myself.setUserName("myself");
//        myself.setUuid(MYSELF_UUID);

        users.add(myself);

        List<UserComment> userComments = new ArrayList<>();

        UserComment userComment = new TextComment(aimi, 1494475200, "照片扔进毕业十年聚,请务必放进毕业十年聚，别忘啦");
        userComments.add(userComment);

        userComment = new TextComment(naomi, 1494820800, "务必放进毕业十年聚");
        userComments.add(userComment);

        userComment = new TextComment(myself, 1497067200, "同学们速度快点");
        userComments.add(userComment);

        userComment = new TextComment(aimi, 1500189121, "快点");
        userComments.add(userComment);

        userComment = new TextComment(naomi, 1500189301, "来了");
        userComments.add(userComment);

        userComment = new TextComment(aimi, 1500189361, "ok");
        userComments.add(userComment);

        userComment = new TextComment(myself, 1500189421, "come on");
        userComments.add(userComment);

        userComment = new TextComment(naomi, 1500189481, "coming");
        userComments.add(userComment);

        userComment = new TextComment(aimi, 1500189541, "waiting");
        userComments.add(userComment);

        userComment = new TextComment(naomi, 1500189601, "here");
        userComments.add(userComment);

        List<Pin> pins = new ArrayList<>();

        Pin pin1 = new Pin("1", "testPing1");

        pins.add(pin1);

        Pin pin2 = new Pin("2", "testPing2");

        pins.add(pin2);

        Pin pin3 = new Pin("3", "testPing3");

        pins.add(pin3);

        Pin pin4 = new Pin("4", "testPing4");

        pins.add(pin4);

        String groupName1 = "大学同学";

        PrivateGroup privateGroup1 = new PrivateGroup("1", groupName1, new ArrayList<>(users));

        privateGroup1.addPins(pins);

        privateGroup1.addUserComments(userComments);

        mPrivateGroups.add(privateGroup1);

        String groupName2 = "外卖小分队";

        PrivateGroup privateGroup2 = new PrivateGroup("2", groupName2, new ArrayList<>(users));
        privateGroup2.addUserComments(userComments);

        mPrivateGroups.add(privateGroup2);

        String groupName3 = "软件学院同学会";

        PrivateGroup privateGroup3 = new PrivateGroup("3", groupName3, new ArrayList<>(users));
        privateGroup3.addUserComments(userComments);

        mPrivateGroups.add(privateGroup3);

        String groupUuid4 = "4";
        String groupName4 = "吃货群";

        PrivateGroup privateGroup4 = new PrivateGroup(groupUuid4, groupName4, new ArrayList<>(users));
        privateGroup4.addUserComments(userComments);

        mPrivateGroups.add(privateGroup4);

        String groupName5 = "校广播站";

        PrivateGroup privateGroup5 = new PrivateGroup("5", groupName5, new ArrayList<>(users));
        privateGroup5.addUserComments(userComments);

        mPrivateGroups.add(privateGroup5);

        String groupName6 = "211宿舍派对";

        PrivateGroup privateGroup6 = new PrivateGroup("6", groupName6, new ArrayList<>(users));
        privateGroup6.addUserComments(userComments);

        mPrivateGroups.add(privateGroup6);


    }

    @Override
    public void addGroup(Collection<PrivateGroup> groups) {
        mPrivateGroups.addAll(groups);
    }

    @Override
    public List<PrivateGroup> getAllGroups() {

        List<PrivateGroup> privateGroups = new ArrayList<>(mPrivateGroups.size());

        for (PrivateGroup privateGroup : mPrivateGroups) {
            privateGroups.add(privateGroup.cloneSelf());
        }

        return privateGroups;
    }

    @Override
    public void clearGroups() {
        mPrivateGroups.clear();
    }

    @Override
    public PrivateGroup getGroupByUUID(String groupUUID) {

        PrivateGroup originalPrivateGroup = null;

        for (PrivateGroup privateGroup : mPrivateGroups) {
            if (privateGroup.getUUID().equals(groupUUID))
                originalPrivateGroup = privateGroup;
        }

        if (originalPrivateGroup != null) {

            return originalPrivateGroup.cloneSelf();
        }

        return null;

    }

    private PrivateGroup getOriginalGroupByUUID(String groupUUID) {

        for (PrivateGroup privateGroup : mPrivateGroups) {
            if (privateGroup.getUUID().equals(groupUUID))
                return privateGroup;
        }

        return null;

    }


    @Override
    public UserComment insertUserComment(String groupUUID, UserComment userComment) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup != null)
            privateGroup.addUserComment(userComment);

        return userComment;
    }

    @Override
    public Pin insertPin(String groupUUID, Pin pin) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup != null)
            privateGroup.addPin(pin);

        return pin;
    }

    @Override
    public boolean modifyPin(String groupUUID, String pinName, String pinUUID) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup != null) {

            Pin originalPin = privateGroup.getPin(pinUUID);

            originalPin.setName(pinName);

            return true;
        }

        return false;
    }

    @Override
    public boolean deletePin(String groupUUID, String pinUUID) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup != null) {

            privateGroup.deletePin(pinUUID);
            return true;

        }

        return false;
    }

    @Override
    public boolean insertFileToPin(Collection<AbstractFile> files, String groupUUID, String pinUUID) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup != null) {

            Pin pin = privateGroup.getPin(pinUUID);

            pin.addFiles(files);

            return true;
        }

        return false;
    }

    @Override
    public boolean insertMediaToPin(Collection<Media> medias, String groupUUID, String pinUUID) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup != null) {

            Pin pin = privateGroup.getPin(pinUUID);

            pin.addMedias(medias);

            return true;
        }

        return false;
    }

    @Override
    public Pin getPinInGroup(String pinUUID, String groupUUID) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup == null)
            return null;

        Pin pin = privateGroup.getPin(pinUUID);

        if (pin == null)
            return null;
        else
            return pin.cloneSelf();


    }

    private Pin getOriginalPinInGroup(String pinUUID, String groupUUID) {

        PrivateGroup privateGroup = getOriginalGroupByUUID(groupUUID);

        if (privateGroup == null)
            return null;

        Pin pin = privateGroup.getPin(pinUUID);

        if (pin == null)
            return null;
        else
            return pin;

    }

    @Override
    public boolean updatePinInGroup(Pin pin, String groupUUID) {

        Pin originalPin = getOriginalPinInGroup(pin.getUuid(), groupUUID);

        if (originalPin == null)
            return false;

        List<Media> originalMedias = originalPin.getMedias();
        List<Media> modifiedMedias = pin.getMedias();

        Iterator<Media> mediaIterator = originalMedias.iterator();

        while (mediaIterator.hasNext()) {

            Media media = mediaIterator.next();

            if (!modifiedMedias.contains(media)) {
                mediaIterator.remove();
            }

        }

        for (Media media : modifiedMedias) {

            if (!originalMedias.contains(media)) {
                originalMedias.add(media);
            }

        }

        List<AbstractFile> originalFiles = originalPin.getFiles();
        List<AbstractFile> modifiedFiles = pin.getFiles();

        Iterator<AbstractFile> fileIterator = originalFiles.iterator();

        while (fileIterator.hasNext()) {
            AbstractFile file = fileIterator.next();

            if (!modifiedFiles.contains(file)) {
                fileIterator.remove();
            }

        }

        for (AbstractFile file : modifiedFiles) {

            if (!originalFiles.contains(file)) {
                originalFiles.add(file);
            }

        }

        return true;
    }


}
