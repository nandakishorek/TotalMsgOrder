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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by kishore on 3/4/16.
 */
public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

    private static final String TAG = ServerTask.class.getSimpleName();

    // handle to node state
    private State mState;

    private String mPort; // the port of this client for generating a unique id for a message

    private String mListenPort; // the listen port of the client which has connected now

    public ServerTask(State state, String myPort) {
        this.mState = state;
        this.mPort = myPort;
    }

    @Override
    protected Void doInBackground(ServerSocket... sockets) {

        Log.v(TAG, "ServerTask started");
        ServerSocket serverSocket = sockets[0];

        while (!isCancelled()) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            ) {
                //clientSocket.setSoTimeout(500);
                clientSocket.setSoLinger(true, 0);

                // receive message and send back proposal
                receiveMessage(clientSocket, br, bw);

                // receive the message with the agreed seq num
                receiveMessage(clientSocket, br, bw);
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

    void receiveMessage(Socket clientSocket, BufferedReader br, BufferedWriter bw) {
        try{
            String line = br.readLine();
            Log.v(TAG, "Received " + line);
            if(line != null && line.length() > 0) {
                // parse the received message into a message object
                Message msg = new Message(line);

                // add the port to listen port mapping
                mListenPort = msg.getId().substring(0, 5);

                Log.v(TAG, "Received message " + msg.toString());
                switch (msg.getType()) {
                    case MSG:
                        sendBackProposal(bw, msg);
                        break;
                    case AGR:
                        // set the max agreed seq num
                        mState.setAgreedSeqNum(msg.getMajorSeqNum());

                        // handle the agreement message
                        mState.handleAgreement(msg);
                        break;
                    case PROP:
                    default:
                        Log.e(TAG, "invalid message type " + msg.getType());
                        break;
                }
            } else {
                if (mListenPort != null) {
                    mState.cleanUp(mListenPort);
                    mListenPort = null;
                }
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error reading message after accept");
            ioe.printStackTrace();
        }
    }

    void sendBackProposal(BufferedWriter bw, Message msg) {

        Long nextSeqNum = mState.getNextSeqNum();

        Log.v(TAG, "nextSeqNum " + nextSeqNum);

        // create a new proposal message
        Message propMsg = new Message(Message.Type.PROP, msg.getMessage(), nextSeqNum, Long.parseLong(mPort), msg.getId());

        try {
            bw.write(propMsg.toString() + "\n");
            bw.flush();

            mState.add(propMsg);
            Log.v(TAG, "sendBackProposal: Message queued " + propMsg.toString());
        } catch (IOException ioe) {
            Log.e(TAG, "sendBackProposal: IO error");
            ioe.printStackTrace();
        }

        Log.v(TAG, "sendBackProposal Done");
    }


}
