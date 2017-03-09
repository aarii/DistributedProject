package se.kth.id2203.kvstore;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by araxi on 2017-03-08.
 */
public class KVRequest implements KompicsEvent, Serializable {

    public String operation;
    public String key;
    public String value;
    public NetAddress client;
    public UUID id;
    public NetAddress groupmember;
    public int maxTimestamp;


    public KVRequest(String operation, String key, String value, UUID id, NetAddress groupmember, int maxTimestamp){
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.client = client;
        this.id = id;
        this.groupmember = groupmember;
        this.maxTimestamp = maxTimestamp;
    }

    public KVRequest(String operation, String key, UUID id, NetAddress groupmember, int maxTimestamp){
        this.operation = operation;
        this.key = key;
        this.client = client;
        this.id = id;
        this.groupmember = groupmember;
        this.maxTimestamp = maxTimestamp;
    }


}
