package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by kishore on 3/5/16.
 */
public class TestKeys implements View.OnClickListener{

    private static final String TAG = TestKeys.class.getName();
    private static final int TEST_CNT = 25;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private final TextView mTextView;
    private final ContentResolver mContentResolver;
    private final Uri mUri;

    public TestKeys(TextView _tv, ContentResolver _cr){
        mTextView = _tv;
        mContentResolver = _cr;
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    }


    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public void onClick(View v) {
        testQuery();
    }

    /**
     * testQuery() uses ContentResolver.query() to retrieves values from your ContentProvider.
     * It simply queries one key at a time and verifies whether it matches any (key, value) pair
     * previously inserted by testInsert().
     *
     * Please pay extra attention to the Cursor object you return from your ContentProvider.
     * It should have two columns; the first column (KEY_FIELD) is for keys
     * and the second column (VALUE_FIELD) is values. In addition, it should include exactly
     * one row that contains a key and a value.
     *
     * @return
     */
    private void testQuery() {
        try {
            for (int i = 0; i < TEST_CNT; i++) {
                String key = Integer.toString(i);

                Cursor resultCursor = mContentResolver.query(mUri, null, key, null, null);
                if (resultCursor == null) {
                    Log.e(TAG, "Result null");
                    throw new Exception();
                }

                int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
                int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);

                resultCursor.moveToFirst();

                if (key.equals(resultCursor.getString(keyIndex))) {
                    String opMsg = "query key " + key + " value " + resultCursor.getString(valueIndex);
                    Log.v(TAG, opMsg);
                    mTextView.append(opMsg + "\n");
                } else {
                    Log.v(TAG, "Could not find message with key " + key);
                }
                resultCursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in testQuery");
            e.printStackTrace();
        }
    }
}
