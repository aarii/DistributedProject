package se.kth.id2203;

import se.kth.id2203.networking.NetAddress;

import java.io.Serializable;

/**
 * Created by araxi on 2017-03-08.
 */
public class Value implements Serializable{

    public NetAddress groupmember;
    public int timestamp;
    public int value;

    public Value(NetAddress owner, int timestamp, int value){
        this.groupmember = owner;
        this.timestamp = timestamp;
        this.value = value;
    }
}
