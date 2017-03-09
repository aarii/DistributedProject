package se.kth.id2203.kvstore;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;
import sun.nio.ch.Net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Amir on 2017-03-03.
 */
public class KVEvent implements KompicsEvent, Serializable {

    public UUID id;
    public NetAddress client;
    public String done;
    public String key;
    public String value;
    public String refValue;
    public String operation;
    public Operation op;
    public NetAddress distributor;
    public NetAddress groupmember;
    public int timestamp;
    public ArrayList<NetAddress> group;


    public KVEvent(String operation, String key, String value, UUID id,
                   NetAddress groupmember, int timestamp, ArrayList<NetAddress> group){
        this.id = id;
        this.client = client;
        this.key = key;
        this.value = value;
        this.operation = operation;
        this.groupmember = groupmember;
        this.timestamp = timestamp;
        this.group = group;
    }

    public KVEvent(String operation, String key,UUID id,
                   NetAddress groupmember){
        this.id = id;
        this.key = key;
        this.operation = operation;
        this.groupmember = groupmember;
    }

    public KVEvent (Operation operation){
        this.op = operation;
    }

}
