package com.example.offlinedailyjournal;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "offline_journal.db";
    private static final int    DATABASE_VERSION = 2;  // bumped to version 2

    // Table names
    public static final String TABLE_FOLDERS  = "folders";
    public static final String TABLE_JOURNALS = "journals";

    // Folders table columns
    public static final String COLUMN_FOLDER_ID   = "id";
    public static final String COLUMN_FOLDER_NAME = "name";
    public static final String COLUMN_FOLDER_ICON = "icon";

    // Journals table columns
    public static final String COLUMN_JOURNAL_ID            = "id";
    public static final String COLUMN_JOURNAL_TITLE         = "title";
    public static final String COLUMN_JOURNAL_CONTENT       = "content";
    public static final String COLUMN_JOURNAL_FOLDER_ID     = "folder_id";
    public static final String COLUMN_JOURNAL_DATE_ADDED    = "date_added";
    public static final String COLUMN_JOURNAL_DATE_MODIFIED = "date_modified";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create folders table
        String CREATE_FOLDERS_TABLE = ""
                + "CREATE TABLE IF NOT EXISTS " + TABLE_FOLDERS + " ("
                +     COLUMN_FOLDER_ID   + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                +     COLUMN_FOLDER_NAME + " TEXT NOT NULL, "
                +     COLUMN_FOLDER_ICON + " TEXT"
                + ");";
        db.execSQL(CREATE_FOLDERS_TABLE);

        // Create journals table with both date_added and date_modified
        String CREATE_JOURNALS_TABLE = ""
                + "CREATE TABLE IF NOT EXISTS " + TABLE_JOURNALS + " ("
                +     COLUMN_JOURNAL_ID            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                +     COLUMN_JOURNAL_TITLE         + " TEXT NOT NULL, "
                +     COLUMN_JOURNAL_CONTENT       + " TEXT NOT NULL, "
                +     COLUMN_JOURNAL_FOLDER_ID     + " INTEGER, "
                +     COLUMN_JOURNAL_DATE_ADDED    + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                +     COLUMN_JOURNAL_DATE_MODIFIED + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                +     "FOREIGN KEY(" + COLUMN_JOURNAL_FOLDER_ID + ") "
                +         "REFERENCES " + TABLE_FOLDERS + "(" + COLUMN_FOLDER_ID + ")"
                + ");";
        db.execSQL(CREATE_JOURNALS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Rename the old journals table
            db.execSQL("ALTER TABLE " + TABLE_JOURNALS + " RENAME TO journals_old;");

            // Recreate the new journals table with the updated schema
            String CREATE_JOURNALS_TABLE = ""
                    + "CREATE TABLE " + TABLE_JOURNALS + " ("
                    +     COLUMN_JOURNAL_ID            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    +     COLUMN_JOURNAL_TITLE         + " TEXT NOT NULL, "
                    +     COLUMN_JOURNAL_CONTENT       + " TEXT NOT NULL, "
                    +     COLUMN_JOURNAL_FOLDER_ID     + " INTEGER, "
                    +     COLUMN_JOURNAL_DATE_ADDED    + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    +     COLUMN_JOURNAL_DATE_MODIFIED + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    +     "FOREIGN KEY(" + COLUMN_JOURNAL_FOLDER_ID + ") "
                    +         "REFERENCES " + TABLE_FOLDERS + "(" + COLUMN_FOLDER_ID + ")"
                    + ");";
            db.execSQL(CREATE_JOURNALS_TABLE);

            // Migrate data from the old table, using the old "timestamp" column for both added & modified
            db.execSQL(
                    "INSERT INTO " + TABLE_JOURNALS + " ("
                            + COLUMN_JOURNAL_ID + ", "
                            + COLUMN_JOURNAL_TITLE + ", "
                            + COLUMN_JOURNAL_CONTENT + ", "
                            + COLUMN_JOURNAL_FOLDER_ID + ", "
                            + COLUMN_JOURNAL_DATE_ADDED + ", "
                            + COLUMN_JOURNAL_DATE_MODIFIED
                            + ") SELECT "
                            + COLUMN_JOURNAL_ID + ", "
                            + COLUMN_JOURNAL_TITLE + ", "
                            + COLUMN_JOURNAL_CONTENT + ", "
                            + COLUMN_JOURNAL_FOLDER_ID + ", "
                            + "timestamp" + ", "  // old column name
                            + "timestamp"
                            + " FROM journals_old;"
            );

            // Drop the old table
            db.execSQL("DROP TABLE IF EXISTS journals_old;");
        }
        // future schema upgrades go here
    }

    /** Insert a new folder */
    public long insertFolder(String name, String icon) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FOLDER_NAME, name);
        values.put(COLUMN_FOLDER_ICON, icon);
        return db.insert(TABLE_FOLDERS, null, values);
    }

    /** Insert a new journal entry, setting both date_added & date_modified to now */
    public long insertJournal(String title, String content, long folderId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_JOURNAL_TITLE, title);
        values.put(COLUMN_JOURNAL_CONTENT, content);
        values.put(COLUMN_JOURNAL_FOLDER_ID, folderId);

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        values.put(COLUMN_JOURNAL_DATE_ADDED, now);
        values.put(COLUMN_JOURNAL_DATE_MODIFIED, now);

        return db.insert(TABLE_JOURNALS, null, values);
    }
}
