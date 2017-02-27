package se.kth.id2203.distributor;

import se.sics.kompics.KompicsEvent;

/**
 * Created by Amir on 2017-02-24.
 */
public class LeaderNotification implements KompicsEvent {

    public String notification;

    public LeaderNotification(String s){ this.notification = s; }
}
