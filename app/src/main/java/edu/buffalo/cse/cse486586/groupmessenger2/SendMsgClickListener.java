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

    public SendMsgClickListener(EditText editText, TextView textView, State state, String myPort) {
        this.editText = editText;
        this.textView = textView;
        this.mState = state;
        this.mPort = myPort;
    }

    @Override
    public void onClick(View v) {
        // get the message from the editor
        String message = editText.getText().toString();

        // clear the editor
        editText.getText().clear();

        /*// append the message to the text view
        textView.append(message + System.lineSeparator());*/

        Log.v(TAG, "sending " + message);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String msgToSend = msgs[0];

            // construct a message to be sent to the other clients
            Message msg = new Message(Message.Type.MSG, msgs[0], 0, Long.parseLong(mPort), mPort + mIdSuffix);

            // increment the id suffix
            ++mIdSuffix;

            // multicast the message
            // get the proposed seq numbers back
            long maxMajorSeqNum = 0L;
            long minorSeqNum = 0L; // corresponsing minor num

            for (String port : REMOTE_PORTS) {
                try {

                    // TODO: socket timeout for handling failures
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));

                    socket.setSoTimeout(500);

                    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                         BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    ) {
                        bw.write(msg.toString() + "\n");
                        bw.flush();

                        // read the proposal
                        StringBuilder msgBuilder = new StringBuilder();
                        try {
                            String line = br.readLine();
                            if (line != null) {
                                msgBuilder.append(line);
                            }
                            Log.v(TAG, "Received proposal " + line);
                        } catch (IOException ioe) {
                            Log.e(TAG, "Error reading proposal");
                            ioe.printStackTrace();
                        }

                        // parse the received message into a message object
                        String message = msgBuilder.toString();
                        if (message.length() > 0) {
                            Message propMsg = new Message(message);
                            if (propMsg.getMajorSeqNum() >= maxMajorSeqNum) {
                                maxMajorSeqNum = propMsg.getMajorSeqNum();
                                minorSeqNum = propMsg.getMinorSeqNum();
                            }
                        }
                    } catch (IOException ioe) {
                        Log.e(TAG, "Error writing to socket");
                        ioe.printStackTrace();
                    }
                    socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException - " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException - " + e.getMessage());
                }
            }

            Log.v(TAG, "Multicast done");

            // set the agreed sequence number in the global state
            mState.setAgreedSeqNum(maxMajorSeqNum);

            // set the agreed sequence number in this major
            msg.setMajorSeqNum(maxMajorSeqNum);
            msg.setMinorSeqNum(minorSeqNum);
            msg.setType(Message.Type.AGR);

            // multicast the agreed sequence number
            for (String port : REMOTE_PORTS) {
                try {

                    // TODO: socket timeout for handling failures
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));

                    socket.setSoTimeout(500);

                    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                        bw.write(msg.toString() + "\n");
                        bw.flush();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Error while sending the message");
                        ioe.printStackTrace();
                    }
                    socket.close();

                    Log.v(TAG, "Multicast port: " + port + " message" + msg);
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException - " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException - " + e.getMessage());
                }
            }

            Log.v(TAG, "Agreement done");

            return null;
        }
    }
}
