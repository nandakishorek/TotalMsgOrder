package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;
import android.os.AsyncTask;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by kishore on 2/7/16.
 */
public class SendMsgClickListener  implements View.OnClickListener{

    static final String TAG = SendMsgClickListener.class.getSimpleName();

    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};

    private EditText editText;
    private TextView textView;
    private String mPort; // the port of this client for generating a unique id for a message

    private long mIdSuffix = 1L; // suffix for generating the id of a message

    // handle to node state
    private State mState;

    private ExecutorService execService = Executors.newSingleThreadExecutor();

    public SendMsgClickListener(EditText editText, TextView textView, State state, String myPort) {
        this.editText = editText;
        this.textView = textView;
        this.mState = state;
        this.mPort = myPort;
    }

    @Override
    public void onClick(View v) {
        // get the message from the editor
        final String message = editText.getText().toString();

        // clear the editor
        editText.getText().clear();

        /*// append the message to the text view
        textView.append(message + System.lineSeparator());*/

        Log.v(TAG, "sending " + message);
        //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
        execService.submit(new Runnable() {
            @Override
            public void run() {
                // construct a message to be sent to the other clients
                Message msg = new Message(Message.Type.MSG, message, 0, Long.parseLong(mPort), mPort + mIdSuffix);

                // increment the id suffix
                ++mIdSuffix;

                // multicast the message
                // get the proposed seq numbers back
                long maxMajorSeqNum = 0L;
                long minorSeqNum = 0L; // corresponsing minor num

                Socket[] peerSockets = new Socket[REMOTE_PORTS.length];
                BufferedReader[] in = new BufferedReader[REMOTE_PORTS.length];
                BufferedWriter[] out = new BufferedWriter[REMOTE_PORTS.length];

                for (int i =0; i < REMOTE_PORTS.length; ++i) {
                    try {

                        // TODO: socket timeout for handling failures
                        peerSockets[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORTS[i]));

                        //peerSockets[i].setSoTimeout(500);
                        peerSockets[i].setSoLinger(true, 0);

                        try {
                            out[i] = new BufferedWriter(new OutputStreamWriter(peerSockets[i].getOutputStream()));
                            in[i] = new BufferedReader(new InputStreamReader(peerSockets[i].getInputStream()));

                            out[i].write(msg.toString() + "\n");
                            out[i].flush();

                            try {
                                String line = in[i].readLine();
                                if (line != null && line.length() > 0) {
                                    // parse the received message into a message object
                                    Message propMsg = new Message(line);
                                    if (propMsg.getMajorSeqNum() >= maxMajorSeqNum) {
                                        maxMajorSeqNum = propMsg.getMajorSeqNum();
                                        minorSeqNum = propMsg.getMinorSeqNum();
                                    }

                                } else {
                                    // clean up
                                    mState.cleanUp(REMOTE_PORTS[i]);
                                }
                                Log.v(TAG, "Received proposal " + line);
                            } catch (IOException ioe) {
                                Log.e(TAG, "Error reading proposal");
                                ioe.printStackTrace();
                            }

                        } catch (IOException ioe) {
                            Log.e(TAG, "Error writing to socket");
                            ioe.printStackTrace();
                        }

                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException - " + e.getMessage());
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException - " + e.getMessage());
                    }
                }

                Log.v(TAG, "Multicast done");

                // set the agreed sequence number in this major
                msg.setMajorSeqNum(maxMajorSeqNum);
                msg.setMinorSeqNum(minorSeqNum);
                msg.setType(Message.Type.AGR);

                // multicast the agreed sequence number
                for (int i = 0; i < REMOTE_PORTS.length; ++i) {
                    try {
                        out[i].write(msg.toString() + "\n");
                        out[i].flush();

                        // close connections
                        out[i].close();
                        in[i].close();

                        Log.v(TAG, "Multicast port: " + REMOTE_PORTS[i] + " message" + msg);
                    } catch (IOException ioe) {
                        Log.e(TAG, "Error while sending the message");
                        ioe.printStackTrace();
                    }
                }

                Log.v(TAG, "Agreement done");
            }
        });
    }
}
