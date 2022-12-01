package songbook.viewer.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import songbook.viewer.data.SQLHelper;
import songbook.viewer.data.Song;
import songbook.viewer.data.Songbook;

public class SongbookService extends Service {

    public final static long PARAM_INVALID_SBID = Long.MIN_VALUE;

    private final static String TAG = SongbookService.class.getCanonicalName();

    private final IBinder mBinder = new SongbookBinder();
    private SQLHelper sqlHelper;

    private Locale locale = Locale.getDefault();

    public class SongbookBinder extends Binder {
        public SongbookService getService() {
            return SongbookService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {

        sqlHelper = new SQLHelper(getApplicationContext());

        Log.i(TAG, "onCreate");

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Received start id " + startId + ": " + intent);

        return START_STICKY;
    }

    public Cursor findSongsByKeyWord(long songBookId, String keyWord,
                                     boolean byArtist) {

        if (keyWord != null && keyWord.trim().length() > 0) {

            String searchParameter = keyWord.toUpperCase(locale);

            SQLiteDatabase db = sqlHelper.getReadableDatabase();

            String[] columns = {SQLHelper.tblSongs_ID,
                    SQLHelper.tblSongs_NUMCODE, SQLHelper.tblSongs_TITLE,
                    SQLHelper.tblSongs_ARTIST};

            String selection = MessageFormat.format("{0} = ? AND {1} LIKE ? ",
                    SQLHelper.tblSongs_SBID,
                    byArtist ? SQLHelper.tblSongs_ARTIST
                            : SQLHelper.tblSongs_TITLE);

            String orderBy = null;

            if (byArtist) {
                orderBy = MessageFormat.format("{0} ASC, {1} ASC",
                        SQLHelper.tblSongs_ARTIST, SQLHelper.tblSongs_TITLE);
            } else {
                orderBy = MessageFormat.format("{0} ASC",
                        SQLHelper.tblSongs_TITLE);
            }

            return db.query(
                    SQLHelper.tblSongs,
                    columns,
                    selection,
                    new String[]{
                            MessageFormat.format("{0,number,#}", songBookId),
                            "%" + searchParameter + "%"}, null, null, orderBy);

        } else {
            return null;
        }
    }

    public Cursor findSongsByIndex(long songBookId, String index,
                                   boolean byArtist) {

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        String[] columns = {SQLHelper.tblSongs_ID, SQLHelper.tblSongs_NUMCODE,
                SQLHelper.tblSongs_TITLE, SQLHelper.tblSongs_ARTIST};

        String orderBy = null;

        if (byArtist) {
            orderBy = MessageFormat.format("{0} ASC, {1} ASC",
                    SQLHelper.tblSongs_ARTIST, SQLHelper.tblSongs_TITLE);
        } else {
            orderBy = MessageFormat.format("{0} ASC", SQLHelper.tblSongs_TITLE);
        }

        if (index != null && index.trim().length() > 0) {

            String selection = MessageFormat.format("{0} = ? AND {1} LIKE ? ",
                    SQLHelper.tblSongs_SBID,
                    byArtist ? SQLHelper.tblSongs_ARTIST
                            : SQLHelper.tblSongs_TITLE);

            Log.i(TAG, selection);

            return db.query(
                    SQLHelper.tblSongs,
                    columns,
                    selection,
                    new String[]{
                            MessageFormat.format("{0,number,#}", songBookId),
                            index + "%"}, null, null, orderBy);

        } else {

            String selection = MessageFormat.format(
                    "{0} = ? AND length({1}) = 0", SQLHelper.tblSongs_SBID,
                    byArtist ? SQLHelper.tblSongs_ARTIST
                            : SQLHelper.tblSongs_TITLE);

            Log.i(TAG, selection);

            return db.query(SQLHelper.tblSongs, columns, selection,
                    new String[]{MessageFormat.format("{0,number,#}",
                            songBookId)}, null, null, orderBy);
        }

    }

    public String[] retrieveIndeces(long songBookId, boolean byArtist) {

        Set<String> indeces = new TreeSet<String>();

        if (songBookId == PARAM_INVALID_SBID) {
            throw new IllegalArgumentException();
        }

        Cursor cursor = null;

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        try {

            cursor = db
                    .rawQuery(
                            MessageFormat
                                    .format("SELECT DISTINCT substr({0},1,1) FROM {1} WHERE {2} = {3,number,#}",
                                            byArtist ? SQLHelper.tblSongs_ARTIST
                                                    : SQLHelper.tblSongs_TITLE,
                                            SQLHelper.tblSongs,
                                            SQLHelper.tblSongs_SBID, songBookId),
                            null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; cursor
                    .moveToNext()) {
                indeces.add(cursor.getString(0));
            }

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return indeces.toArray(new String[0]);
    }

    public boolean deleteSongbook(long songBookId) {

        boolean isSuccess = false;

        Log.i(TAG, "deleteSongbook id = " + songBookId);

        if (songBookId == PARAM_INVALID_SBID) {
            throw new IllegalArgumentException();
        }

        SQLiteDatabase db = sqlHelper.getWritableDatabase();

        try {

            db.beginTransaction();

            db.delete(SQLHelper.tblSongs, MessageFormat.format("{0}=?",
                    SQLHelper.tblSongs_SBID), new String[]{MessageFormat
                    .format("{0,number,#}", songBookId)});

            db.delete(SQLHelper.tblSongbook, MessageFormat.format("{0}=?",
                    SQLHelper.tblSongbook_ID), new String[]{MessageFormat
                    .format("{0,number,#}", songBookId)});

            db.setTransactionSuccessful();

            isSuccess = true;
        } finally {
            db.endTransaction();
        }

        return isSuccess;
    }

    public Songbook retrieveSongbookById(long id, boolean populateSongs) {

        Songbook songBook = null;

        SQLiteDatabase db = sqlHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String[] columns = {SQLHelper.tblSongbook_ID,
                    SQLHelper.tblSongbook_NAME, SQLHelper.tblSongbook_DESC,
                    SQLHelper.tblSongbook_DEFAULTFLAG};

            cursor = db.query(SQLHelper.tblSongbook, columns, MessageFormat
                            .format("{0}={1,number,#}", SQLHelper.tblSongbook_ID, id),
                    null, null, null, null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; ) {
                songBook = new Songbook(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getInt(3) == 1 ? true
                        : false);
                break;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (songBook != null && populateSongs) {
            populateSongs(songBook);
        }

        Log.i(TAG, "retrieveSongbookById");

        return songBook;
    }

    public boolean setAsDefault(long songBookId) {

        boolean isSuccess = false;

        Log.i(TAG, "setAsDefault id = " + songBookId);

        if (songBookId == PARAM_INVALID_SBID) {
            throw new IllegalArgumentException();
        }

        SQLiteDatabase db = sqlHelper.getWritableDatabase();

        try {

            db.beginTransaction();

            ContentValues args = new ContentValues();

            args.put(SQLHelper.tblSongbook_DEFAULTFLAG, 0);

            db.update(SQLHelper.tblSongbook, args, MessageFormat.format(
                    "{0}=1", SQLHelper.tblSongbook_DEFAULTFLAG), null);

            args.clear();

            args.put(SQLHelper.tblSongbook_DEFAULTFLAG, 1);

            db.update(SQLHelper.tblSongbook, args, MessageFormat.format(
                            "{0}={1,number,#}", SQLHelper.tblSongbook_ID, songBookId),
                    null);

            db.setTransactionSuccessful();

            isSuccess = true;
        } finally {
            db.endTransaction();
        }

        return isSuccess;
    }

    public boolean importSongbook(InputStream input, String name, String desc,
                                  boolean defaultFlag) {

        Log.i(TAG, "importSongbook");

        if (input == null) {
            throw new IllegalArgumentException("Invalid input");
        }

        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid name");
        }

        if (existSongbookByName(name) == true) {
            throw new IllegalArgumentException(
                    "Songbook with that name already exists");
        }

        Log.i(TAG, "Input: " + input);

        BufferedReader bReader = null;

        Songbook songBook = new Songbook(0L, name, desc, defaultFlag);

        SQLiteDatabase db = sqlHelper.getWritableDatabase();

        try {

            bReader = new BufferedReader(new InputStreamReader(input));

            String strLine = null;

            while ((strLine = bReader.readLine()) != null) {

                if (strLine.trim().length() > 0) {

                    String[] data = strLine.split("\\|");

                    if (data.length >= 2 && data.length <= 3) {
                        songBook.addSong(new Song(0L, 0L, data[1].trim(),
                                data.length == 3 ? data[2].trim() : "", Integer
                                .parseInt(data[0])));
                    } else {
                        throw new IllegalArgumentException("Invalid content");
                    }

                }
            }

            if (false == songBook.getSongs().isEmpty()) {

                db.beginTransaction();

                try {

                    ContentValues cv = new ContentValues();
                    cv.put(SQLHelper.tblSongbook_NAME, songBook.getName());
                    cv.put(SQLHelper.tblSongbook_DESC,
                            songBook.getDescription());
                    cv.put(SQLHelper.tblSongbook_DEFAULTFLAG,
                            songBook.isDefaultFlag() ? 1 : 0);

                    songBook.setId(db.insert(SQLHelper.tblSongbook, null, cv));

                    for (Song song : songBook.getSongs()) {

                        cv = new ContentValues();
                        cv.put(SQLHelper.tblSongs_SBID, songBook.getId());
                        cv.put(SQLHelper.tblSongs_ARTIST, song.getArtist());
                        cv.put(SQLHelper.tblSongs_TITLE, song.getTitle());
                        cv.put(SQLHelper.tblSongs_NUMCODE, song.getNumCode());

                        db.insert(SQLHelper.tblSongs, null, cv);

                    }

                    db.setTransactionSuccessful();

                } catch (Exception error) {
                    throw new IllegalStateException(error);
                } finally {
                    db.endTransaction();
                }

            }

        } catch (IOException error) {
            throw new IllegalStateException(error);
        } finally {

            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException error) {
                    throw new IllegalStateException(error);
                }
            }

            if (db != null) {
                db.close();
            }

        }

        return false;
    }

    public boolean existSongbookDefault() {

        boolean exists = false;

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        Cursor cursor = null;

        try {

            String[] columns = {SQLHelper.tblSongbook_ID,
                    SQLHelper.tblSongbook_NAME, SQLHelper.tblSongbook_DESC,
                    SQLHelper.tblSongbook_DEFAULTFLAG};

            String whereClause = MessageFormat.format("{0} = 1",
                    SQLHelper.tblSongbook_DEFAULTFLAG);

            cursor = db.query(SQLHelper.tblSongbook, columns, whereClause,
                    null, null, null, null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; ) {
                exists = true;
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return exists;
    }

    public boolean existSongbookByName(String name) {

        boolean exists = false;

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        Cursor cursor = null;

        try {

            String[] columns = {SQLHelper.tblSongbook_ID};

            String whereClause = MessageFormat.format("{0}=?'",
                    SQLHelper.tblSongbook_NAME);

            Log.i(TAG, whereClause);

            cursor = db.query(SQLHelper.tblSongbook, columns, whereClause,
                    new String[]{name.trim().toUpperCase(locale)}, null,
                    null, null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; ) {
                exists = true;
                break;
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return exists;
    }

    public void populateSongs(Songbook songBook) {

        SQLiteDatabase db = sqlHelper.getReadableDatabase();
        Cursor cursor = null;

        try {

            String[] columns = {SQLHelper.tblSongs_ID,
                    SQLHelper.tblSongs_SBID, SQLHelper.tblSongs_TITLE,
                    SQLHelper.tblSongs_ARTIST, SQLHelper.tblSongs_NUMCODE};

            cursor = db.query(SQLHelper.tblSongs, columns, MessageFormat
                    .format("{0} = {1,number,#}", SQLHelper.tblSongs_SBID,
                            songBook.getId()), null, null, null, null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; cursor
                    .moveToNext()) {
                songBook.addSong(new Song(cursor.getLong(0), cursor.getLong(1),
                        cursor.getString(2), cursor.getString(3), cursor
                        .getInt(4)));
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.i(TAG, "populateSongs");
    }

    public long retrieveDefaultSongbookId() {

        long songBookId = PARAM_INVALID_SBID;

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        Cursor cursor = null;

        try {
            String[] columns = {SQLHelper.tblSongbook_ID};

            String whereClause = MessageFormat.format("{0} = 1",
                    SQLHelper.tblSongbook_DEFAULTFLAG);

            cursor = db.query(SQLHelper.tblSongbook, columns, whereClause,
                    null, null, null, null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; ) {
                songBookId = cursor.getLong(0);
                break;
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return songBookId;

    }

    public Songbook retrieveDefaultSongbook() {

        Songbook songBook = null;
        SQLiteDatabase db = sqlHelper.getReadableDatabase();
        Cursor cursor = null;

        try {

            String[] columns = {SQLHelper.tblSongbook_ID,
                    SQLHelper.tblSongbook_NAME, SQLHelper.tblSongbook_DESC,
                    SQLHelper.tblSongbook_DEFAULTFLAG};

            String whereClause = MessageFormat.format("{0} = 1",
                    SQLHelper.tblSongbook_DEFAULTFLAG);

            cursor = db.query(SQLHelper.tblSongbook, columns, whereClause,
                    null, null, null, null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; ) {
                songBook = new Songbook(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getInt(3) == 1 ? true
                        : false);
                break;
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (songBook != null) {
            populateSongs(songBook);
        }

        return songBook;

    }

    public Cursor retrieveSongBooksByCursor() {

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        String[] columns = {SQLHelper.tblSongbook_ID,
                SQLHelper.tblSongbook_NAME, SQLHelper.tblSongbook_DESC,
                SQLHelper.tblSongbook_DEFAULTFLAG};

        return db.query(SQLHelper.tblSongbook, columns, null, null, null, null,
                null);

    }

    public List<Songbook> retrieveSongbooks() {

        List<Songbook> songBooks = new ArrayList<Songbook>();

        SQLiteDatabase db = sqlHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String[] columns = {SQLHelper.tblSongbook_ID,
                    SQLHelper.tblSongbook_NAME, SQLHelper.tblSongbook_DESC,
                    SQLHelper.tblSongbook_DEFAULTFLAG};

            cursor = db.query(SQLHelper.tblSongbook, columns, null, null, null,
                    null, null);

            for (cursor.moveToFirst(); cursor.isAfterLast() == false; cursor
                    .moveToNext()) {
                songBooks.add(new Songbook(cursor.getLong(0), cursor
                        .getString(1), cursor.getString(2),
                        cursor.getInt(3) == 1 ? true : false));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        for (Songbook sb : songBooks) {
            populateSongs(sb);
        }

        Log.i(TAG, "retrieveSongbooks");

        return songBooks;
    }
}
