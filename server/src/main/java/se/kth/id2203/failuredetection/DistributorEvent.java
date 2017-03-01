package se.kth.id2203.failuredetection;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by Amir on 2017-02-28.
 */
public class DistributorEvent implements KompicsEvent, Serializable {

    public String heartbeat;
    public DistributorEvent(String heartbeat){ this.heartbeat = heartbeat;}


}
