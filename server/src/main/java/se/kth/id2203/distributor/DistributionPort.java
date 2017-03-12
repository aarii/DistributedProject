package se.kth.id2203.distributor;

import se.sics.kompics.PortType;

/**
 * Created by Amir on 2017-02-23.
 */
public class DistributionPort extends PortType {

    {
        indication(SendLookupTable.class);
    }
}
