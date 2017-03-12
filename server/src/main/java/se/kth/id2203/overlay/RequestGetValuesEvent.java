package se.kth.id2203.overlay;

import se.kth.id2203.kvstore.Operation;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by araxi on 2017-03-09.
 */
public class RequestGetValuesEvent implements KompicsEvent, Serializable {

    public Operation operation;
    public UUID id;

    public RequestGetValuesEvent(Operation operation, UUID id){

        this.operation = operation;
        this.id = id;

    }

}
