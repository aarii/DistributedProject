package se.kth.id2203.kvstore;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.UUID;

/**
 * Created by Amir on 2017-03-03.
 */
public class KVEvent implements KompicsEvent {

    public UUID id;
    public NetAddress client;
    public String done;
    public String key;
    public String value;
    public String refValue;
    public String operation;

    public KVEvent(String done, NetAddress client,UUID id){
        this.id = id;
        this.client = client;
        this.done = done;
    }


    public KVEvent(String operation, String key, String value, NetAddress client, UUID id){
        this.id = id;
        this.client = client;
        this.key = key;
        this.value = value;
        this.operation = operation;
    }

    public KVEvent(String operation, String key, NetAddress client,UUID id){
        this.id = id;
        this.client = client;
        this.key = key;
        this.operation = operation;
    }

    public KVEvent(String operation, String key, String refValue, String value, NetAddress client, UUID id){
        this.client = client;
        this.id = id;
        this.operation = operation;
        this.key = key;
        this.refValue = refValue;
        this.value = value;
    }
}
