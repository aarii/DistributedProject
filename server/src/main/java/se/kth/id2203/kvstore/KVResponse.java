package se.kth.id2203.kvstore;

import se.kth.id2203.Value;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by araxi on 2017-03-08.
 */
public class KVResponse implements KompicsEvent, Serializable {
    public String operation;
    public UUID id;
    public String key;
    public String value;
    public Value v;

    public KVResponse(String operation, UUID id, String key, String value){
        this.operation = operation;
        this.id = id;
        this.key = key;
        this.value = value;
    }
    public KVResponse(String operation, UUID id, Value v){
        this.operation = operation;
        this.v = v;
        this.id = id;
    }

    public KVResponse(String operation, UUID id){
        this.operation = operation;
        this.id = id;
    }

}
