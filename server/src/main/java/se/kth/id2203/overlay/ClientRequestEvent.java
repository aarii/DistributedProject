package se.kth.id2203.overlay;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by Amir on 2017-03-03.
 */
public class ClientRequestEvent implements KompicsEvent, Serializable {
    public String operation;
    public String key;
    public String value;

    public ClientRequestEvent(String operation, String key, String value){
        this.operation = operation;
        this.key = key;
        this.value = value;
    }
}
