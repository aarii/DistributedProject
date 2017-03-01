package se.kth.id2203.failuredetection;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by Amir on 2017-02-28.
 */
public class DistributorRequestEvent implements KompicsEvent, Serializable {

    public String heartbeat;
    public DistributorRequestEvent(String heartbeat){ this.heartbeat = heartbeat;}


}
