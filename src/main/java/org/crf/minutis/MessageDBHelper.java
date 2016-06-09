package org.crf.minutis;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.lang.StringBuilder;

public class MessageDBHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "minutis.db";

	public MessageDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder builder = new StringBuilder("CREATE TABLE ");
		builder.append("messages")
		    .append("(")
		    .append("_id INTEGER PRIMARY KEY,")
		    .append("type INTEGER,")
		    .append("date LONG,")
		    .append("content TEXT,")
		    .append("address TEXT DEFAULT ''")
		    .append(");");
		db.execSQL(builder.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS messages");
		onCreate(db);
	}
}
