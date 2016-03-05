package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by kishore on 3/4/16.
 */
public class Message implements Comparable<Message>{

    private static final String TAG = Message.class.getSimpleName();

    private static final String DELIM = "|";

    // message type message, proposal, agreement
    public enum Type {MSG, PROP, AGR};

    private String message;
    private String id; // message identifier
    private Type type;
    private long majorSeqNum;
    private long minorSeqNum; // for tie breaking
    private boolean deliverable;

    public Message(Type type, String message, long majorSeqNum, long minorSeqNum, String id){
        this.type = type;
        this.message = message;
        this.majorSeqNum = majorSeqNum;
        this.minorSeqNum = minorSeqNum;
        this.id = id;
    }

    public Message(String rcvMessage) {
        this.parseMessage(rcvMessage);
    }

    @Override
    public int compareTo(Message another) {
        // assuming only MSG type of messages will be compared
        if (this.majorSeqNum == another.majorSeqNum) {
            if (this.minorSeqNum < another.minorSeqNum) {
                return -1;
            } else {
                return 1;
            }
        } if (this.majorSeqNum < another.majorSeqNum) {
            return -1;
        }
        return 1;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getMajorSeqNum() {
        return majorSeqNum;
    }

    public void setMajorSeqNum(long majorSeqNum) {
        this.majorSeqNum = majorSeqNum;
    }

    public long getMinorSeqNum() {
        return minorSeqNum;
    }

    public void setMinorSeqNum(long minorSeqNum) {
        this.minorSeqNum = minorSeqNum;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public void setDeliverable(boolean isDeliverable) {
        this.deliverable = isDeliverable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.type.toString() + DELIM);
        sb.append(this.id + DELIM);
        sb.append(this.message + DELIM);
        sb.append(this.majorSeqNum + DELIM);
        sb.append(this.minorSeqNum);
        return sb.toString();
    }

    private void parseMessage(String rcvMessage) {
        String[] vals = rcvMessage.split("\\"+DELIM);
        this.type = Type.valueOf(vals[0]);
        this.id = vals[1];
        this.message = vals[2];
        this.majorSeqNum = Long.parseLong(vals[3]);
        this.minorSeqNum = Long.parseLong(vals[4]);
    }
}
