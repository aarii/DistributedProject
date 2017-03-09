package se.kth.id2203.overlay;

import se.kth.id2203.kvstore.Operation;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by araxi on 2017-03-08.
 */
public class ResponseTimestampEvent implements KompicsEvent,Serializable {

    public Operation operation;
    public int timestamp;



    public ResponseTimestampEvent(Operation operation, int timestamp){
        this.operation = operation;
        this.timestamp = timestamp;

    }
}
