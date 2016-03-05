package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;

    private ServerTask mServerTask;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        // get the textview reference
        mTextView = (TextView) findViewById(R.id.textView1);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        // register click handler for content provider test button
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(mTextView, getContentResolver()));

        // initialize the node state
        State state = new State();

        // register click handler for sending the typed message
        EditText editText = (EditText)findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new SendMsgClickListener(editText, mTextView, state, myPort));

        // start the server thread
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            mServerTask = new ServerTask(new MessageStore(getContentResolver()), state, myPort);
            mServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket - " + e.getMessage());
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
