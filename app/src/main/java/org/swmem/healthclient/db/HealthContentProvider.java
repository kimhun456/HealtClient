package org.swmem.healthclient.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.ArrayList;

public class HealthContentProvider extends ContentProvider {

    private HealthDbHelper mOpenHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final SQLiteQueryBuilder sInsulinQueryBuilder;

    static final int GLUCOSE = 100;


    static{
        sInsulinQueryBuilder = new SQLiteQueryBuilder();
        sInsulinQueryBuilder.setTables(
                HealthContract.GlucoseEntry.TABLE_NAME);
    }

    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = HealthContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, HealthContract.PATH_GLUCOSE, GLUCOSE);

        return matcher;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        // this makes delete all rows return the number of rows deleted

        if ( null == selection ) selection = "1";
        switch (match) {
            case GLUCOSE:
                rowsDeleted = db.delete(
                        HealthContract.GlucoseEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case GLUCOSE:
                return HealthContract.GlucoseEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {

            case GLUCOSE: {
                long _id = db.insert(HealthContract.GlucoseEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = HealthContract.GlucoseEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public boolean onCreate() {

        mOpenHelper = new HealthDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "GLUCOSE/*/*"
            case GLUCOSE:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        HealthContract.GlucoseEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );

                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case GLUCOSE:
                rowsUpdated = db.update(HealthContract.GlucoseEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;

    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        int numberInserted = 0;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {

            case GLUCOSE: {

                db.beginTransaction();

                for(ContentValues contentValues : values){

                    long _id = db.insert(HealthContract.GlucoseEntry.TABLE_NAME, null, contentValues);
                    if(_id <=0)
                        throw new android.database.SQLException("Failed to insert row into " + uri);

                }

                db.setTransactionSuccessful();
                break;

            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);


        db.endTransaction();
        return numberInserted;
    }


    public int bulkInsert(Uri uri, ArrayList<ContentValues> values) {

        int numberInserted = 0;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {

            case GLUCOSE: {

                db.beginTransaction();

                for(ContentValues contentValues : values){

                    long _id = db.insert(HealthContract.GlucoseEntry.TABLE_NAME, null, contentValues);
                    if(_id <=0)
                        throw new android.database.SQLException("Failed to insert row into " + uri);

                }

                db.setTransactionSuccessful();
                break;

            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);


        db.endTransaction();
        return numberInserted;
    }
}
