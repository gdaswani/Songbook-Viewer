package songbook.viewer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.MessageFormat;

public class SQLHelper extends SQLiteOpenHelper {

	private final static String TAG = SQLHelper.class.getCanonicalName();

	private final static String dbName = "SongBook.db";

	public final static String tblSongbook = "tblSongbook";
	public final static String tblSongbook_ID = "_id";
	public final static String tblSongbook_NAME = "NAME";
	public final static String tblSongbook_DESC = "DESC";
	public final static String tblSongbook_DEFAULTFLAG = "DEFAULT_FLAG";

	public final static String tblSongs = "tblSongs";
	public final static String tblSongs_ID = "_id";
	public final static String tblSongs_SBID = "SB_ID";
	public final static String tblSongs_ARTIST = "ARTIST";
	public final static String tblSongs_TITLE = "TITLE";
	public final static String tblSongs_NUMCODE = "NUMCODE";

	private final static int VERSION = 14;

	public SQLHelper(Context context) {
		super(context, SQLHelper.dbName, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		if (db == null) {
			throw new IllegalArgumentException("invalid database handle",
					new NullPointerException());
		}

		String createCatalog = MessageFormat
				.format("CREATE TABLE {0} ( {1} INTEGER PRIMARY KEY AUTOINCREMENT, {2} TEXT NOT NULL UNIQUE, {3} TEXT NULL, {4} BOOLEAN NOT NULL)",
						SQLHelper.tblSongbook, tblSongbook_ID,
						tblSongbook_NAME, tblSongbook_DESC,
						tblSongbook_DEFAULTFLAG);

		db.execSQL(createCatalog);

		Log.i(TAG, createCatalog);

		String createSongs = MessageFormat
				.format("CREATE TABLE {0} ( {1} INTEGER PRIMARY KEY AUTOINCREMENT, {2} INTEGER NOT NULL, {3} TEXT NULL, {4} TEXT NULL, {5} INTEGER NOT NULL )",
						SQLHelper.tblSongs, tblSongs_ID, tblSongs_SBID,
						tblSongs_ARTIST, tblSongs_TITLE, tblSongs_NUMCODE);

		db.execSQL(createSongs);

		Log.i(TAG, createSongs);

		initializeDB(db);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		Log.i(TAG, "onUpgrade");

		db.execSQL(MessageFormat.format("DROP TABLE IF EXISTS {0}",
				SQLHelper.tblSongbook));

		db.execSQL(MessageFormat.format("DROP TABLE IF EXISTS {0}",
				SQLHelper.tblSongs));

		onCreate(db);

	}

	private void initializeDB(SQLiteDatabase db) {

	}
}
