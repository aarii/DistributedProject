package se.kth.id2203.distributor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.NodeAssignment;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;


/**
 * Created by Amir on 2017-02-22.
 */
public class DistributorComponent extends ComponentDefinition {
    final static Logger LOG = LoggerFactory.getLogger(DistributorComponent.class);

    protected final Positive<DistributionPort> distribution = requires(DistributionPort.class);
    protected final Negative<Network> net = provides(Network.class);

    protected final Handler<SendLookupTable> lookUpTableHandler = new Handler<SendLookupTable>() {


        @Override
        public void handle(SendLookupTable lookupTable) {

            LookupTable na = (LookupTable) lookupTable.lookupTable;
            LOG.debug("SendLookupTable är " + na);
            LOG.debug("na.getNodes är:" + na.getNodes());


            //for(NetAdress i : na.getNodes())
           // trigger(new LeaderNotification("You are the leader!"), net);

        }
    };


    {
    subscribe(lookUpTableHandler, distribution);
}
}
