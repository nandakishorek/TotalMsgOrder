package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

/**
 * Created by kishore on 2/7/16.
 */
public class MessageStore {

    private static final String TAG = MessageStore.class.getSimpleName();
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private final ContentResolver mContentResolver;
    private final Uri mUri;
    private long seqNum;

    public MessageStore(ContentResolver cr) {
        mContentResolver = cr;
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        seqNum = 0L;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    public void storeMessage(String message) {
        ContentValues values = new ContentValues();
        values.put(KEY_FIELD, Long.toString(seqNum));
        values.put(VALUE_FIELD, message);
        mContentResolver.insert(mUri, values);
        ++seqNum;
        Log.e(TAG, "Message " + message + " Seq num " + seqNum);
    }
}
