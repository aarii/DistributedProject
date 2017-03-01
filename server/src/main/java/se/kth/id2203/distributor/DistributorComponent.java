package se.kth.id2203.distributor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.failuredetection.DistributorEvent;
import se.kth.id2203.failuredetection.DistributorTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

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
    protected final Positive<Timer> timer = requires(Timer.class);
    private UUID timeoutId;



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

            long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 2);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new DistributorTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };

    protected final Handler<DistributorTimeout> heartBeatHandler = new Handler<DistributorTimeout>() {
        @Override
        public void handle(DistributorTimeout distributorTimeout) {

            for(int i = 0; i<leaders.size(); i++) {
                LOG.debug("VI är i heartbeat handler och distributor är " + self + " och leader är " + leaders.get(i));
                trigger(new Message(self, leaders.get(i), new DistributorEvent("I'm alive")), net);
            }
        }
    };




    {
    subscribe(heartBeatHandler, timer);
    subscribe(lookUpTableHandler, distribution);

}
}
