package edu.buffalo.cse.cse486586.groupmessenger2;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by kishore on 3/4/16.
 */
public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

    private static final String TAG = ServerTask.class.getSimpleName();
    private MessageStore mMsgStore;

    // handle to node state
    private State mState;

    private String mPort; // the port of this client for generating a unique id for a message

    // message buffer
    private PriorityQueue<Message> mQueue;

    public ServerTask(MessageStore msgStore, State state, String myPort) {
        this.mMsgStore = msgStore;
        this.mState = state;
        this.mPort = myPort;
        this.mQueue = new PriorityQueue<Message>();
    }

    @Override
    protected Void doInBackground(ServerSocket... sockets) {

        Log.v(TAG, "ServerTask started");
        ServerSocket serverSocket = sockets[0];

        while (!isCancelled()) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ) {
                clientSocket.setSoTimeout(500);

                StringBuilder message = new StringBuilder();
                try{
                    String line = br.readLine();
                    message.append(line);
                    Log.v(TAG, "Received " + line);
                } catch (IOException ioe) {
                    Log.e(TAG, "Error reading message after accept");
                    ioe.printStackTrace();
                }

                // parse the received message into a message object
                Message msg = new Message(message.toString());
                Log.v(TAG, "Received message " + msg.toString());
                switch(msg.getType()) {
                    case MSG:
                        sendBackProposal(clientSocket, msg);
                        break;
                    case AGR:
                        // set the max agreed seq num
                        mState.setAgreedSeqNum(msg.getMajorSeqNum());

                        try {
                            clientSocket.close();
                        } catch (IOException ioe) {
                            Log.e(TAG, "Error while closing the client connection");
                            ioe.printStackTrace();
                        }

                        // handle the agreement message
                        handleAgreement(msg);
                        break;
                    case PROP:
                    default:
                        Log.e(TAG, "invalid message type " + msg.getType());
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error while accepting the client connection");
            }
        }

        try {
            serverSocket.close();
        } catch (IOException ioe) {
            Log.e(TAG, "Could not close server socket");
        }

        return null;
    }


    void sendBackProposal(Socket clientSocket, Message msg) {

        Long nextSeqNum = mState.getNextSeqNum();

        Log.v(TAG, "nextSeqNum " + nextSeqNum);

        // create a new proposal message
        Message propMsg = new Message(Message.Type.PROP, msg.getMessage(), nextSeqNum, Long.parseLong(mPort), msg.getId());

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            bw.write(propMsg.toString() + "\n");
            bw.flush();

            mQueue.add(propMsg);
            Log.v(TAG, "sendBackProposal: Message queued " + propMsg.toString());
            clientSocket.close();
        } catch (IOException ioe) {
            Log.e(TAG, "sendBackProposal: IO error");
            ioe.printStackTrace();
        }

        Log.v(TAG, "sendBackProposal Done");
    }

    void handleAgreement(Message msg) {

        Log.v(TAG, "handleAgreement: " + msg.toString());

        Iterator<Message> iter = mQueue.iterator();
        while(iter.hasNext()) {
            Message message = iter.next();
            if (message.getId().equals(msg.getId())) {

                Log.v(TAG, "Found " + message.toString());
                // remove the old one
                iter.remove();

                // add the new one
                msg.setDeliverable(true);
                mQueue.add(msg);
                break;
            }
        }

        Log.v(TAG, "handleAgreement: queue " + mQueue.toString());

        Message head = mQueue.peek();
        while(head != null && head.isDeliverable()) {
            // there is a deliverable message at the head, deliver it
            head = mQueue.poll();
            mMsgStore.storeMessage(head.getMessage());
            head = mQueue.peek();
        }
    }
}
