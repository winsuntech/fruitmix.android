package com.winsun.fruitmix.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2016/7/8.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String TAG = DBHelper.class.getSimpleName();

    public static final String USER_KEY_ID = "id";
    public static final String USER_KEY_USERNAME = "user_name";
    public static final String USER_KEY_UUID = "user_uuid";
    public static final String USER_KEY_AVATAR = "user_avatar";
    public static final String USER_KEY_EMAIL = "user_email";
    public static final String USER_KEY_DEFAULT_AVATAR = "user_default_avatar";
    public static final String USER_KEY_DEFAULT_AVATAR_BG_COLOR = "user_default_avatar_bg_color";
    public static final String USER_KEY_HOME = "home";
    public static final String USER_KEY_LIBRARY = "library";
    public static final String USER_KEY_IS_ADMIN = "is_admin";

    public static final String MEDIA_KEY_ID = "id";
    public static final String MEDIA_KEY_UUID = "media_uuid";
    public static final String MEDIA_KEY_TIME = "media_time";
    public static final String MEDIA_KEY_WIDTH = "media_width";
    public static final String MEDIA_KEY_HEIGHT = "media_height";
    public static final String MEDIA_KEY_THUMB = "media_thumb";
    public static final String MEDIA_KEY_LOCAL = "media_local";
    public static final String MEDIA_KEY_UPLOADED_USER_UUID = "media_key_uploaded_device_id";
    public static final String MEDIA_KEY_SHARING = "media_key_sharing";
    public static final String MEDIA_KEY_ORIENTATION_NUMBER = "media_key_orientation_number";
    public static final String MEDIA_KEY_TYPE = "media_key_type";
    public static final String MEDIA_KEY_MINI_THUMB = "media_key_mini_thumb";
    public static final String MEDIA_KEY_ORIGINAL_PHOTO_PATH = "media_key_original_photo_path";
    public static final String MEDIA_KEY_LATITUDE = "media_key_latitude";
    public static final String MEDIA_KEY_LONGITUDE = "media_key_longitude";

    public static final String FILE_KEY_ID = "id";
    public static final String FILE_KEY_NAME = "file_name";
    public static final String FILE_KEY_UUID = "file_uuid";
    public static final String FILE_KEY_TIME = "file_time";
    public static final String FILE_KEY_SIZE = "file_size";
    public static final String FILE_KEY_CREATOR_UUID = "file_creator_uuid";

    public static final String LOGGED_IN_USER_GATEWAY = "logged_in_user_gateway";
    public static final String LOGGED_IN_USER_EQUIPMENT_NAME = "logged_in_user_equipment_name";
    public static final String LOGGED_IN_USER_TOKEN = "logged_in_user_token";
    public static final String LOGGED_IN_USER_DEVICE_ID = "logged_in_user_device_id";

    private static final String DB_NAME = "fruitmix";
    private static final String REMOTE_COMMENT_TABLE_NAME = "remote_comment";
    private static final String LOCAL_COMMENT_TABLE_NAME = "local_comment";
    private static final String LOCAL_SHARE_TABLE_NAME = "local_share";
    private static final String REMOTE_SHARE_TABLE_NAME = "remote_share";
    static final String REMOTE_USER_TABLE_NAME = "remote_user";
    static final String REMOTE_MEDIA_TABLE_NAME = "remote_media";
    static final String LOCAL_MEDIA_TABLE_NAME = "local_media";
    private static final String REMOTE_MEDIA_SHARE_CONTENT_TABLE_NAME = "remote_media_share_content";
    private static final String LOCAL_MEDIA_SHARE_CONTENT_TABLE_NAME = "local_media_share_content";
    static final String DOWNLOADED_FILE_TABLE_NAME = "downloaded_file";
    static final String LOGGED_IN_USER_TABLE_NAME = "logged_in_user";

    private static final int DB_VERSION = 28;

    private static final String CREATE_TABLE = "create table ";

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

    private static final String DATABASE_MEDIA_CREATE = " (" + MEDIA_KEY_ID + " integer primary key autoincrement,"
            + MEDIA_KEY_UUID + " text not null," + MEDIA_KEY_TIME + " text not null," + MEDIA_KEY_WIDTH + " text not null,"
            + MEDIA_KEY_HEIGHT + " text not null," + MEDIA_KEY_THUMB + " text," + MEDIA_KEY_LOCAL + " integer not null,"
            + MEDIA_KEY_UPLOADED_USER_UUID + " text," + MEDIA_KEY_SHARING + " integer not null,"
            + MEDIA_KEY_ORIENTATION_NUMBER + " integer," + MEDIA_KEY_TYPE + " text,"
            + MEDIA_KEY_MINI_THUMB + " text," + MEDIA_KEY_ORIGINAL_PHOTO_PATH + " text,"
            + MEDIA_KEY_LONGITUDE + " text," + MEDIA_KEY_LATITUDE + " text)";

    private static final String DATABASE_REMOTE_MEDIA_CREATE = CREATE_TABLE + REMOTE_MEDIA_TABLE_NAME + DATABASE_MEDIA_CREATE;

    private static final String DATABASE_LOCAL_MEDIA_CREATE = CREATE_TABLE + LOCAL_MEDIA_TABLE_NAME + DATABASE_MEDIA_CREATE;

    private static final String USER_FIELD_CREATE = " ("
            + USER_KEY_ID + " integer primary key autoincrement," + USER_KEY_UUID + " text not null,"
            + USER_KEY_USERNAME + " text not null," + USER_KEY_AVATAR + " text not null,"
            + USER_KEY_EMAIL + " text," + USER_KEY_DEFAULT_AVATAR + " text not null," + USER_KEY_DEFAULT_AVATAR_BG_COLOR + " integer not null,"
            + USER_KEY_HOME + " text not null," + USER_KEY_LIBRARY + " text not null," + USER_KEY_IS_ADMIN + " integer not null";

    private static final String DATABASE_REMOTE_USER_CREATE = CREATE_TABLE + REMOTE_USER_TABLE_NAME + USER_FIELD_CREATE + ")";

    private static final String DATABASE_DOWNLOADED_FILE_CREATE = CREATE_TABLE + DOWNLOADED_FILE_TABLE_NAME + " (" + FILE_KEY_ID + " integer primary key autoincrement,"
            + FILE_KEY_NAME + " text," + FILE_KEY_UUID + " text not null," + FILE_KEY_TIME + " text," + FILE_KEY_SIZE + " text," + FILE_KEY_CREATOR_UUID + " text not null)";

    private static final String DATABASE_LOGGED_IN_USER_CREATE = CREATE_TABLE + LOGGED_IN_USER_TABLE_NAME + USER_FIELD_CREATE + ","
            + LOGGED_IN_USER_GATEWAY + " text not null," + LOGGED_IN_USER_EQUIPMENT_NAME + " text not null," + LOGGED_IN_USER_TOKEN + " text not null,"
            + LOGGED_IN_USER_DEVICE_ID + " text not null)";

    DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(DATABASE_REMOTE_MEDIA_CREATE);
        db.execSQL(DATABASE_LOCAL_MEDIA_CREATE);
        db.execSQL(DATABASE_REMOTE_USER_CREATE);

        db.execSQL(DATABASE_DOWNLOADED_FILE_CREATE);
        db.execSQL(DATABASE_LOGGED_IN_USER_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

//        Log.i(TAG, "Upgrading database from version " + oldVersion + "to " +
//                newVersion + ", which will destroy all old data");

//        db.execSQL(DROP_TABLE + REMOTE_MEDIA_TABLE_NAME);
//        db.execSQL(DROP_TABLE + LOCAL_MEDIA_TABLE_NAME);
//        db.execSQL(DROP_TABLE + REMOTE_USER_TABLE_NAME);

//        db.execSQL(DROP_TABLE + DOWNLOADED_FILE_TABLE_NAME);
//        db.execSQL(DROP_TABLE + LOGGED_IN_USER_TABLE_NAME);
//
//        onCreate(db);

//        db.execSQL(DROP_TABLE + REMOTE_COMMENT_TABLE_NAME);
//        db.execSQL(DROP_TABLE + LOCAL_COMMENT_TABLE_NAME);
//        db.execSQL(DROP_TABLE + LOCAL_SHARE_TABLE_NAME);
//        db.execSQL(DROP_TABLE + REMOTE_SHARE_TABLE_NAME);
//        db.execSQL(DROP_TABLE + REMOTE_MEDIA_SHARE_CONTENT_TABLE_NAME);
//        db.execSQL(DROP_TABLE + LOCAL_MEDIA_SHARE_CONTENT_TABLE_NAME);

        db.execSQL(DROP_TABLE + LOCAL_MEDIA_TABLE_NAME);

    }
}
