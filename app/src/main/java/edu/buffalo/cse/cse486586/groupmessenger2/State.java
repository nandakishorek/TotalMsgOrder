package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by kishore on 3/4/16.
 *
 * To hold the state of the node
 * All methods are synchronized
 */
public class State {

    private static final String TAG = State.class.getSimpleName();

    private MessageStore mMsgStore;

    // message buffer
    private PriorityQueue<Message> mQueue;

    // maximum of proposed sequence numbers
    private long propSeqNum = 0;

    // maximum of agreed sequence numbers
    private long agreedSeqNum = 0;

    public State(MessageStore msgStore) {
        this.mMsgStore = msgStore;
        this.mQueue = new PriorityQueue<Message>();
    }

    public synchronized void setAgreedSeqNum(long agreedSeqNum) {
        if (agreedSeqNum > this.agreedSeqNum) {
            this.agreedSeqNum = agreedSeqNum;
        }
    }

    public synchronized long getNextSeqNum() {
        long ret = 0;

        // max of all proposals and agreed seq nums
        ret = (propSeqNum > agreedSeqNum) ? (propSeqNum + 1L) : (agreedSeqNum + 1L);

        // set the proposed seq num
        propSeqNum = ret;

        return ret;
    }

    public synchronized void add(Message msg) {
        mQueue.add(msg);
    }

    /**
     * Remove the undelivered messages from the crashed client
     * @param port
     */
    public synchronized void cleanUp(String port) {
        Log.v(TAG, "cleanup " + port);
        Iterator<Message> iter = mQueue.iterator();
        while(iter.hasNext()) {
            Message message = iter.next();
            if (port.equals(message.getId().substring(0, 5))) {
                // remove the message
                iter.remove();

                Log.v(TAG, "Removed " + message.toString());
            }
        }
    }

    public synchronized void handleAgreement(Message msg) {

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
