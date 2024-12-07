package com.example.webbrowser.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.webbrowser.models.FavoriteItem;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class FavoriteItemDatabase implements Database<FavoriteItem>, Closeable {

    private static final String DATABASE_NAME = "webBrowserFavorites";
    private static final int DATABASE_ACCESS = Context.MODE_PRIVATE;

    private static final String
            TABLE_NAME = "favoriteItem",
            QUERY_STRUCT = "CREATE TABLE IF NOT EXISTS favoriteItem (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, url TEXT NOT NULL);",
            QUERY_SELECT_ALL = "SELECT * FROM favoriteItem ORDER BY itemIdentifier;",
            QUERY_CLEAR = "DROP TABLE IF EXISTS favoriteItem",
            COLLUMN_TITLE = "title",
            COLLUMN_URL = "url";

    private SQLiteDatabase database;
    private Context context;

    public FavoriteItemDatabase(Context context) {
        this.context = context;
        create();
    }

    @Override
    public void insert(FavoriteItem item) {
        ContentValues values = new ContentValues();

        values.put(COLLUMN_TITLE, item.getTitle());
        values.put(COLLUMN_URL, item.getUrl());

        database.insert(TABLE_NAME, null, values);
    }

    @Override
    public void remove(FavoriteItem item) {
        database.delete(TABLE_NAME, "title = ?", new String[]{String.valueOf(item.getTitle())});
    }

    @Override
    public List<FavoriteItem> getAll() {
        List<FavoriteItem> favoriteItemList = new ArrayList<>();
        try (Cursor cursor = database.rawQuery(QUERY_SELECT_ALL, null)) {
            if (cursor.moveToFirst())
                do {
                    favoriteItemList.add(getFavoriteItemFromCursor(cursor));
                } while(cursor.moveToNext());
        }
        return favoriteItemList;
    }

    @Override
    public void create() {
        database = context.openOrCreateDatabase(DATABASE_NAME, DATABASE_ACCESS, null);
        database.execSQL(QUERY_STRUCT);
    }

    @Override
    public void clear() {
        database.execSQL(QUERY_CLEAR);
    }

    @Override
    public void close() {
        database.close();
    }

    private FavoriteItem getFavoriteItemFromCursor(Cursor cursor) {
        return new FavoriteItem(
                cursor.getString(cursor.getColumnIndexOrThrow(COLLUMN_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLLUMN_URL))
        );
    }
}
