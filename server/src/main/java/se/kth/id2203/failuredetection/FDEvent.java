package se.kth.id2203.failuredetection;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by Amir on 2017-02-28.
 */
public class FDEvent implements KompicsEvent, Serializable {

    public final static FDEvent event = new FDEvent();
    public String heartbeat;
    public FDEvent(String heartbeat){ this.heartbeat = heartbeat;}

    public FDEvent() {

    }
}
