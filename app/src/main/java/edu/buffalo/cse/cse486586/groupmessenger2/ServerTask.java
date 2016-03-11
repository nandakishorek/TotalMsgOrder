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

    public ServerTask(State state, String myPort) {
        this.mState = state;
        this.mPort = myPort;
    }

    @Override
    protected Void doInBackground(ServerSocket... sockets) {

        Log.v(TAG, "ServerTask started");
        ServerSocket serverSocket = sockets[0];

        while (!isCancelled()) {
            try {

                final Socket clientSocket = serverSocket.accept();
                final BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                Log.v(TAG, "New Worker thread");

                // delegate work to worker thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //clientSocket.setSoTimeout(500);
                            clientSocket.setSoLinger(true, 0);

                            String listenPort = receiveMessage(clientSocket, br, bw, null); // the listen port of the client which has connected now

                            while(true) {
                                // receive message and send back proposal
                                if(receiveMessage(clientSocket, br, bw, listenPort) == null) {
                                    // quit
                                    Log.e(TAG, "client connection closed " + listenPort);
                                    break;
                                }

                                // receive the message with the agreed seq num
                                if(receiveMessage(clientSocket, br, bw, listenPort) == null)  {
                                    // quit
                                    Log.e(TAG, "client connection closed " + listenPort);
                                    break;
                                }
                            }
                            br.close();
                            bw.close();
                        } catch (IOException ioe) {
                            Log.e(TAG, "Error in server thread");
                            ioe.printStackTrace();
                        }
                    }
                }).start();
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

    /**
     * Method to process client message
     *
     * @param clientSocket
     * @param br
     * @param bw
     * @param clientPort client's listen port, incase of failure all message from this client will be removed
     * @return
     */
    String receiveMessage(Socket clientSocket, BufferedReader br, BufferedWriter bw, String clientPort) {
        String listenPort = "";
        try{
            String line = br.readLine();
            Log.v(TAG, "Received " + line);
            if(line != null && line.length() > 0) {
                // parse the received message into a message object
                Message msg = new Message(line);

                Log.v(TAG, "Received message " + msg.toString());
                switch (msg.getType()) {
                    case ID:
                        listenPort = msg.getMessage();
                        break;
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
                if (clientPort != null) {
                    // cleanup
                    mState.cleanUp(clientPort);
                    return null;
                }
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error reading message after accept");
            ioe.printStackTrace();
        }
        return listenPort;
    }

    /**
     * Method to send proposal back to sender
     *
     * @param bw
     * @param msg Incoming new message
     */
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
