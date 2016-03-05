package edu.buffalo.cse.cse486586.groupmessenger2;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

        Log.e(TAG, "ServerTask started");
        ServerSocket serverSocket = sockets[0];

            /*// set the socket timeout to 1s
            try {
                serverSocket.setSoTimeout(1000);
            } catch (SocketException e) {
                Log.e(TAG, "error setting the server socket timeout");
            }*/

        while (!isCancelled()) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ) {
                StringBuilder message = new StringBuilder();
                try{
                    String line = br.readLine();
                    message.append(line);
                    Log.e(TAG, "Received " + line);
                } catch (IOException ioe) {
                    Log.e(TAG, "Error reading message after accept");
                    ioe.printStackTrace();
                }

                // parse the received message into a message object
                Message msg = new Message(message.toString());
                Log.e(TAG, "Received message " + msg.toString());
                switch(msg.getType()) {
                    case MSG:
                        sendBackProposal(clientSocket, msg);
                        break;
                    case AGR:
                        try {
                            clientSocket.close();
                        } catch (IOException ioe) {
                            Log.e(TAG, "Error while closing the client connection");
                            ioe.printStackTrace();
                        }

                        // set the max agreed seq num
                        mState.setAgreedSeqNum(msg.getMajorSeqNum());

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

        Log.e(TAG, "nextSeqNum " + nextSeqNum);

        // create a new proposal message
        Message propMsg = new Message(Message.Type.PROP, msg.getMessage(), nextSeqNum, Long.parseLong(mPort), msg.getId());

        try (PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true)) {
            pw.println(propMsg.toString());
            if (pw.checkError()) {
                Log.e(TAG, "Error while sending the message");
            }

            // flush the buffers
            pw.flush();

            // set the propsed seq num
            msg.setMajorSeqNum(nextSeqNum);
            msg.setMinorSeqNum(Long.parseLong(mPort));
            msg.setDeliverable(false);
            mQueue.add(msg);
            Log.e(TAG, "sendBackProposal: Message queued " + msg.toString());
            clientSocket.close();
        } catch (IOException ioe) {
            Log.e(TAG, "sendBackProposal: IO error");
            ioe.printStackTrace();
        }

        Log.e(TAG, "sendBackProposal Done");
    }

    void handleAgreement(Message msg) {

        Log.e(TAG, "handleAgreement: " + msg.toString());

        Iterator<Message> iter = mQueue.iterator();
        while(iter.hasNext()) {
            Message message = iter.next();
            if (message.getId().equals(msg.getId())) {

                Log.e(TAG, "Found " + message.toString());
                // remove the old one
                iter.remove();

                // add the new one
                msg.setDeliverable(true);
                mQueue.add(msg);
                break;
            }
        }

        Log.e(TAG, "handleAgreement: queue " + mQueue.toString());

        Message head = mQueue.peek();
        while(head != null && head.isDeliverable()) {
            // there is a deliverable message at the head, deliver it
            head = mQueue.poll();
            mMsgStore.storeMessage(head.getMessage());
            head = mQueue.peek();
        }
    }
}
