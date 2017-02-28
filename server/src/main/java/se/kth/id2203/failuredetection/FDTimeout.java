package se.kth.id2203.failuredetection;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created by araxi on 2017-02-28.
 */
public class FDTimeout extends Timeout {

    public FDTimeout(SchedulePeriodicTimeout spt) {
        super(spt);
    }
}
