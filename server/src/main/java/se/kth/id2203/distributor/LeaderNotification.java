package se.kth.id2203.distributor;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by Amir on 2017-02-24.
 */
public class LeaderNotification implements KompicsEvent, Serializable {

    public String notification;
    public LeaderNotification(String s){ this.notification = s; }
}
