package se.kth.id2203.failuredetection;

import se.sics.kompics.KompicsEvent;

/**
 * Created by Amir on 2017-02-28.
 */
public class FDEvent implements KompicsEvent {

    public String heartbeat;
    public FDEvent(String heartbeat){ this.heartbeat = heartbeat;}

}
