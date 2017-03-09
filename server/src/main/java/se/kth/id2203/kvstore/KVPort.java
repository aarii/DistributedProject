package se.kth.id2203.kvstore;

import se.sics.kompics.PortType;

/**
 * Created by Amir on 2017-03-03.
 */
public class KVPort extends PortType {
    {
        indication(KVEvent.class);
        //request(KVEvent.class);
    }
}
