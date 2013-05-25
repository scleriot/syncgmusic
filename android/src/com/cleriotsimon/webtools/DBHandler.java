package com.cleriotsimon.webtools;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHandler extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 3;

	// Database Name
	private static final String DATABASE_NAME = "qqdroid";

	// Table name
	private static final String TABLE_REQUEST = "Requests";

	// Profiles Table Columns names
	private static final String REQUEST_KEY_ID = "id";
	private static final String REQUEST_KEY_URL = "url";
	private static final String REQUEST_KEY_CONTENT = "content";
	private static final String REQUEST_KEY_FIRST_TIME = "first";
	private static final String REQUEST_KEY_MAX_TIME = "max";

	public DBHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			String CREATE_PROFILES_TABLE = "CREATE TABLE " + TABLE_REQUEST
					+ "(" + REQUEST_KEY_ID + " INTEGER PRIMARY KEY,"
					+ REQUEST_KEY_URL + " VARCHAR(500)," + REQUEST_KEY_CONTENT
					+ " TEXT" + "," + REQUEST_KEY_FIRST_TIME + " INTEGER, "
					+ REQUEST_KEY_MAX_TIME + " TIMESTAMP)";
			db.execSQL(CREATE_PROFILES_TABLE);

			Log.e("Create Request Table", "Create" + db);
		} catch (Exception e) {
			Log.e("DataBase create", "DataBase" + e);
		}
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUEST);

		// Create tables again
		try {
			onCreate(db);
			Log.e("Upgrade Request Table  ", "Upgrade" + db);
		} catch (Exception e) {
			Log.e("DataBase Upgrade", "DataBase Upgrade" + e);
		}
	}

	public String getContent(String url) {
		SQLiteDatabase db = this.getReadableDatabase();

		String selectQuery = "SELECT " + REQUEST_KEY_ID + ", "
				+ REQUEST_KEY_MAX_TIME + ", " + REQUEST_KEY_CONTENT + " FROM "
				+ TABLE_REQUEST + " WHERE url=?";

		Cursor cursor = db.rawQuery(selectQuery, new String[] { url });
		if (cursor.getCount() == 0)
			return null;
		cursor.moveToFirst();

		if (cursor.getLong(1) > System.currentTimeMillis()) {
			db.close();
			return cursor.getString(2);
		} else {
			db.delete(TABLE_REQUEST, REQUEST_KEY_ID + "=?",
					new String[] { cursor.getString(0) });
			db.close();
			return null;
		}
	}

	public void addContent(String url, String content, long maxTime) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues v = new ContentValues();
		v.put(REQUEST_KEY_URL, url);
		v.put(REQUEST_KEY_CONTENT, content);
		v.put(REQUEST_KEY_MAX_TIME, (System.currentTimeMillis() + maxTime));
		db.insert(TABLE_REQUEST, null, v);

		db.close();
	}

	public void removeCache() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_REQUEST, null, null);
		db.close();
	}
}
