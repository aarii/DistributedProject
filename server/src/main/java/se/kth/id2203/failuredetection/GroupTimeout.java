package se.kth.id2203.failuredetection;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created by araxi on 2017-03-07.
 */
public class GroupTimeout extends Timeout {

    public GroupTimeout(SchedulePeriodicTimeout spt) {
        super(spt);
    }
}
