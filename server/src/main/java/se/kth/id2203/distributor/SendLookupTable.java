package se.kth.id2203.distributor;

import se.kth.id2203.bootstrapping.NodeAssignment;
import se.sics.kompics.KompicsEvent;

/**
 * Created by Amir on 2017-02-23.
 */
public class SendLookupTable implements KompicsEvent {

    public final NodeAssignment lookupTable;

    public SendLookupTable(final NodeAssignment assignment) {
        this.lookupTable = assignment;
    }
}
