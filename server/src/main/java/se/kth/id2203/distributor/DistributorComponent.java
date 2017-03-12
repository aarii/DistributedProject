package se.kth.id2203.distributor;

import com.larskroll.common.J6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.kvstore.KVEvent;
import se.kth.id2203.kvstore.OpResponse;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.*;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.util.ArrayList;
import java.util.UUID;


/**
 * Created by Amir on 2017-02-22.
 */
public class DistributorComponent extends ComponentDefinition {
    final static Logger LOG = LoggerFactory.getLogger(DistributorComponent.class);
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    protected final Positive<DistributionPort> distribution = requires(DistributionPort.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    protected LookupTable lut = new LookupTable();
    protected int groupDivision = 0;
    protected int keyInterval = Integer.MAX_VALUE;


    protected  ArrayList<Integer> grpValHolder = null;

    protected final Handler<SendLookupTable> lookUpTableHandler = new Handler<SendLookupTable>() {

        @Override
        public void handle(SendLookupTable lookupTable) {
            LookupTable table = (LookupTable) lookupTable.lookupTable;
            lut = table;
            LOG.info("The received Lookuptable to the Distributor from BootstrapServer is: " + table);

            groupDivision = table.getNodes().size();
            grpValHolder = new ArrayList<Integer>(groupDivision);

            setKeyIntervals();

            for(ArrayList<NetAddress> group : table.getNodes()){
                for(int i = 0; i<group.size(); i++) {
                    trigger(new Message(self, group.get(i), new DistributorNotification()), net);
                }
            }
        }
    };

    private void setKeyIntervals() {

        for(int i = 0; i<groupDivision; i++){
            grpValHolder.add((i+1) * (keyInterval/groupDivision));
        }
        LOG.info("Setting key intervals for " + groupDivision + " groups");
        for(int i = 0; i<groupDivision; i++){
            LOG.info("Key interval for group" + (i+1) + " is: " + grpValHolder.get(i));
        }
    }

    protected final ClassMatchedHandler<RouteMsg, Message> routeHandler = new ClassMatchedHandler<RouteMsg, Message>() {

        @Override
        public void handle(RouteMsg content, Message context) {

            for(int i = 0; i<grpValHolder.size(); i++){
                if (Integer.parseInt(content.key) <= grpValHolder.get(i)){
                    ArrayList<NetAddress> group = lut.get(i);
                    NetAddress target = J6.randomElement(group);
                    if(content.value != null) {
                        LOG.info("Forwarding message with operation {} for key {} with value {} to {}", content.operation, content.key, content.value, target);
                    }else{
                        LOG.info("Forwarding message with operation {} for key {} to {}", content.operation, content.key, target);

                    }
                    trigger(new Message(context.getSource(), target, content.msg), net);
                    break;
                }
            }
        }
    };


    protected final ClassMatchedHandler<Connect, Message> connectHandler = new ClassMatchedHandler<Connect, Message>() {

        @Override
        public void handle(Connect content, Message context) {

            if (lut != null) {
                LOG.debug("Accepting connection request from {}", context.getSource());
                int size = lut.getNodes().size();
                trigger(new Message(self, context.getSource(), content.ack(size)), net);
            } else {
                LOG.info("Rejecting connection request from {}, as system is not ready, yet.", context.getSource());
            }
        }
    };

    {

        subscribe(connectHandler, net);
        subscribe(lookUpTableHandler, distribution);
        subscribe(routeHandler, net);

    }
}
