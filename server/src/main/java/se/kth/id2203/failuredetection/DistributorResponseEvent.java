package se.kth.id2203.failuredetection;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by Amir on 2017-03-01.
 */
public class DistributorResponseEvent implements KompicsEvent, Serializable {

    public String heartbeat;
    public DistributorResponseEvent(String heartbeat){ this.heartbeat = heartbeat;}
}
