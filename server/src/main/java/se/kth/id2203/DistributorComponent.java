package se.kth.id2203;

import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.distributor.Distribution;
import se.kth.id2203.distributor.SendLookupTable;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;

/**
 * Created by Amir on 2017-02-22.
 */
public class DistributorComponent extends ComponentDefinition {

    protected final Positive<Distribution> distribution = requires(Distribution.class);

    protected final Handler<SendLookupTable> lookUpTableHandler = new Handler<SendLookupTable>() {


        @Override
        public void handle(SendLookupTable sendLookupTable) {


        }
    };
    {
    subscribe(lookUpTableHandler, distribution);
}
}
