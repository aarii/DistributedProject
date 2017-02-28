package se.kth.id2203.failuredetection;

import se.sics.kompics.PortType;

/**
 * Created by Amir on 2017-02-28.
 */
public class FDPort extends PortType {

    {
        indication(FDEvent.class);
        request(FDEvent.class);
    }

}
