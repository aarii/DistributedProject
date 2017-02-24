package se.kth.id2203;

import javafx.geometry.Pos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.BootstrapServer;
import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.bootstrapping.InitialAssignments;
import se.kth.id2203.bootstrapping.NodeAssignment;
import se.kth.id2203.distributor.Distribution;
import se.kth.id2203.distributor.SendLookupTable;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;


/**
 * Created by Amir on 2017-02-22.
 */
public class DistributorComponent extends ComponentDefinition {
    final static Logger LOG = LoggerFactory.getLogger(DistributorComponent.class);

    protected final Positive<Distribution> distributionPort = requires(Distribution.class);

    protected final Handler<SendLookupTable> lookUpTableHandler = new Handler<SendLookupTable>() {


        @Override
        public void handle(SendLookupTable lookupTable) {

            NodeAssignment na = lookupTable.lookupTable;

            LOG.debug("SendLookupTable är " +  (LookupTable) na);

        }
    };
    {
    subscribe(lookUpTableHandler, distributionPort);
}
}
