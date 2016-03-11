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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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
    private LinkedBlockingQueue<String> inputMsgQ = new LinkedBlockingQueue<>();

    // handle to node state
    private State mState;

    // flag set when all the connections have been opened
    private boolean mIsConReady;

    private Socket[] peerSockets = new Socket[REMOTE_PORTS.length];
    private BufferedReader[] in = new BufferedReader[REMOTE_PORTS.length];
    private BufferedWriter[] out = new BufferedWriter[REMOTE_PORTS.length];

    public SendMsgClickListener(EditText editText, TextView textView, State state, String myPort) {
        this.editText = editText;
        this.textView = textView;
        this.mState = state;
        this.mPort = myPort;
        new Thread(new MessageSender()).start();
    }

    @Override
    public void onClick(View v) {
        // get the message from the editor
        final String message = editText.getText().toString();

        // clear the editor
        editText.getText().clear();

        try {
            inputMsgQ.put(message);
            Log.v(TAG, "Queued " + message);
        } catch (InterruptedException e) {
            Log.e(TAG, "interrupted");
            e.printStackTrace();
        }
    }

    private void openConnections() {
        if (!mIsConReady) {
            // open connections to other peers
            for (int i = 0; i < REMOTE_PORTS.length; ++i) {
                try {
                    peerSockets[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORTS[i]));

                    //peerSockets[i].setSoTimeout(500);
                    peerSockets[i].setSoLinger(true, 0);

                    out[i] = new BufferedWriter(new OutputStreamWriter(peerSockets[i].getOutputStream()));
                    in[i] = new BufferedReader(new InputStreamReader(peerSockets[i].getInputStream()));

                    // send this AVD's listen port
                    Message msg = new Message(Message.Type.ID, mPort, 0, Long.parseLong(mPort), mPort + mIdSuffix);
                    out[i].write(msg.toString() + "\n");
                    out[i].flush();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "MessageSender UnknownHostException - " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "MessageSender socket IOException - " + e.getMessage());
                    e.printStackTrace();
                }
            }
            mIsConReady = true;
        }
    }

    private class MessageSender implements Runnable{

        @Override
        public void run() {
            // now keep dequeuing messages and sending them
            while(true) {
                try {
                    // get a message from the input buffer
                    String message = inputMsgQ.take();
                    Log.v(TAG, "Dequeued " + message);

                    // establish connections if haven't already
                    openConnections();

                    // construct a message to be sent to the other clients
                    Message msg = new Message(Message.Type.MSG, message, 0, Long.parseLong(mPort), mPort + mIdSuffix);

                    // increment the id suffix
                    ++mIdSuffix;

                    // multicast the message
                    // get the proposed seq numbers back
                    long maxMajorSeqNum = 0L;
                    long minorSeqNum = 0L; // corresponsing minor num

                    for (int i =0; i < REMOTE_PORTS.length; ++i) {
                        try {
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
                            Log.v(TAG, "Multicast port: " + REMOTE_PORTS[i] + " message" + msg);
                        } catch (IOException ioe) {
                            Log.e(TAG, "Error while sending the message");
                            ioe.printStackTrace();
                        }
                    }

                    Log.v(TAG, "Agreement done");
                } catch (InterruptedException e) {
                    Log.e(TAG, "interrupted in the loop");
                    e.printStackTrace();
                }
            }
        }
    }
}
