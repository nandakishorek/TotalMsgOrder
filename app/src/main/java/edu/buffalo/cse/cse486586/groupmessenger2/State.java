package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by kishore on 3/4/16.
 *
 * To hold the state of the node
 * All methods are synchronized
 */
public class State {
    // maximum of proposed sequence numbers
    private long propSeqNum = 0;

    // maximum of agreed sequence numbers
    private long agreedSeqNum = 0;

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
}
