package se.kth.id2203.overlay;

import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by araxi on 2017-03-08.
 */
public class RequestTimestampEvent implements KompicsEvent,Serializable {

    public Operation operation;



    public RequestTimestampEvent(Operation operation){
        this.operation = operation;

    }


}
