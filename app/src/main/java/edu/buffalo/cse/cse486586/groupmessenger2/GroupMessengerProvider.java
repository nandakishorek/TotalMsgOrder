package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String TAG = GroupMessengerProvider.class.getSimpleName();

    // column names
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String[] COLUMN_NAMES = {KEY_FIELD, VALUE_FIELD};

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String key = (String) values.get(KEY_FIELD);
        String val = (String) values.get(VALUE_FIELD);
        try (FileOutputStream fos = getContext().openFileOutput(key, Context.MODE_PRIVATE)) {
            fos.write(val.getBytes());
            fos.flush();
        } catch (FileNotFoundException fnf) {
            Log.e(TAG, "File not found - " + key);
        } catch (IOException ioe) {
            Log.e(TAG, "Error writing to file - " + key);
        }
        Log.v(TAG, "insert " + values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getContext().openFileInput(selection)))) {
            String value = br.readLine();
            cursor.addRow(new String[]{selection, value});
            Log.v(TAG, "query key - " + selection + "value - " + value);
        } catch (IOException ioe) {
            Log.e(TAG, "Error while opening file " + selection);
        }
        return cursor;
    }
}
