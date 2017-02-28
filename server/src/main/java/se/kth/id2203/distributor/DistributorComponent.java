package se.kth.id2203.distributor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.failuredetection.FDEvent;
import se.kth.id2203.failuredetection.FDTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;

import java.util.ArrayList;
import java.util.UUID;


/**
 * Created by Amir on 2017-02-22.
 */
public class DistributorComponent extends ComponentDefinition {
    final static Logger LOG = LoggerFactory.getLogger(DistributorComponent.class);
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    ArrayList<NetAddress> leaders = new ArrayList<>();
    protected final Positive<DistributionPort> distribution = requires(DistributionPort.class);
    protected final Positive<Network> net = requires(Network.class);


    protected final Handler<SendLookupTable> lookUpTableHandler = new Handler<SendLookupTable>() {

        @Override
        public void handle(SendLookupTable lookupTable) {
            LookupTable table = (LookupTable) lookupTable.lookupTable;
            LOG.debug("SendLookupTable är " + table);
            LOG.debug("na.getNodes är:" + table.getNodes());

            for(ArrayList<NetAddress> group : table.getNodes()){

                for(int i = 1; i<group.size(); i++){
                    trigger(new Message(self, group.get(i), new LeaderNotification("You are not the leader")), net);
                }
                NetAddress leader = group.get(0);
                leaders.add(leader);
                 trigger(new Message(self, leader, new LeaderNotification("You are the leader")), net);



            }
        }
    };


    protected final ClassMatchedHandler<FDEvent, Message> heartbeatHandler = new ClassMatchedHandler<FDEvent, Message>() {

        @Override
        public void handle(FDEvent fdEvent, Message message) {
            LOG.debug("I received " + fdEvent.heartbeat + " from " + message.getSource());
        }
    };

    {
    subscribe(heartbeatHandler, net);
    subscribe(lookUpTableHandler, distribution);

}
}
