package se.kth.id2203.failuredetection;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created by Amir on 2017-03-01.
 */
public class ReplicaTimeout extends Timeout{
    public ReplicaTimeout(SchedulePeriodicTimeout spt) {
        super(spt);
    }

}
