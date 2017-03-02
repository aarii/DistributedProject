/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.overlay;

import com.larskroll.common.J6;

import java.util.ArrayList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.Booted;
import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.bootstrapping.GetInitialAssignments;
import se.kth.id2203.bootstrapping.InitialAssignments;
import se.kth.id2203.distributor.LeaderNotification;
import se.kth.id2203.failuredetection.*;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The V(ery)S(imple)OverlayManager.
 * <p>
 * Keeps all nodes in a single partition in one replication group.
 * <p>
 * Note: This implementation does not fulfill the project task. You have to
 * support multiple partitions!
 * <p>
 * @author Lars Kroll <lkroll@kth.se>
 */
public class VSOverlayManager extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(VSOverlayManager.class);
    //******* Ports ******
    protected final Negative<Routing> route = provides(Routing.class);
    protected final Positive<Bootstrapping> boot = requires(Bootstrapping.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Fields ******

    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    NetAddress distributor = null;
    private LookupTable lut = null;
    private ArrayList<NetAddress> group = null;
    public boolean leader = false;
    private UUID timeoutId;
    private ArrayList<NetAddress> aliveInside = new ArrayList<>();
    private ArrayList<NetAddress> suspectInside = new ArrayList<>();
    private ArrayList<NetAddress> monitoringInside = new ArrayList<>();
    private ArrayList<NetAddress> aliveOutside = new ArrayList<>();
    private ArrayList<NetAddress> suspectOutside = new ArrayList<>();
    private ArrayList<NetAddress> monitoringOutside = new ArrayList<>();
    private int incInside = 2;
    private int incOutside = 2;
    private int seqNrInside = 0;
    private int seqNrOutside = 0;



    //******* Handlers ******
    protected final Handler<GetInitialAssignments> initialAssignmentHandler = new Handler<GetInitialAssignments>() {

        @Override
        public void handle(GetInitialAssignments event) {
            LOG.info("Generating LookupTable...");
            LookupTable lut = LookupTable.generate(event.nodes);
            LOG.debug("Generated assignments:\n{}", lut);
            trigger(new InitialAssignments(lut), boot);
        }
    };
    protected final Handler<Booted> bootHandler = new Handler<Booted>() {

        @Override
        public void handle(Booted event) {
            if (event.assignment instanceof LookupTable) {
                LOG.info("Got NodeAssignment, overlay ready.");
                lut = (LookupTable) event.assignment;
                group = lut.get(0);
            } else {
                LOG.error("Got invalid NodeAssignment type. Expected: LookupTable; Got: {}", event.assignment.getClass());
            }
        }
    };
    protected final ClassMatchedHandler<RouteMsg, Message> routeHandler = new ClassMatchedHandler<RouteMsg, Message>() {

        @Override
        public void handle(RouteMsg content, Message context) {
            ArrayList<NetAddress> partition = lut.lookup(Integer.parseInt(content.key));
            NetAddress target = J6.randomElement(partition);
            LOG.info("Forwarding message for key {} to {}", content.key, target);
            trigger(new Message(context.getSource(), target, content.msg), net);
        }
    };
    protected final Handler<RouteMsg> localRouteHandler = new Handler<RouteMsg>() {

        @Override
        public void handle(RouteMsg event) {
            ArrayList<NetAddress> partition = lut.lookup(Integer.parseInt(event.key));
            NetAddress target = J6.randomElement(partition);
            LOG.info("Routing message for key {} to {}", event.key, target);
            trigger(new Message(self, target, event.msg), net);
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

    protected final ClassMatchedHandler<LeaderNotification, Message> leaderNotificationHandler = new ClassMatchedHandler<LeaderNotification, Message>() {

        @Override
        public void handle(LeaderNotification leaderNotification, Message message) {
            String notification = leaderNotification.notification;
            distributor = message.getSource();
            if(notification.equalsIgnoreCase("You are the leader")){
                leader = true;
                monitoringOutside.add(distributor);
                aliveInside.add(distributor);
                LOG.debug(self + " got: " + notification + " and leader parameter is now: " + leader);
                startTimerLeader(incOutside);

            }else {
                monitoringInside.add(group.get(0));
                aliveInside.add(group.get(0));
                LOG.debug(self + " got: " + notification + " and leader parameter is now: " + leader);
                startTimerReplica(incInside);
            }
        }
    };

    protected final Handler<DistributorTimeout> DHBHandler = new Handler<DistributorTimeout>() {
        @Override
        public void handle(DistributorTimeout distributorTimeout) {

            if(!aliveOutside.isEmpty() && !suspectOutside.isEmpty()){
                tearDown();
                startTimerLeader(incOutside + 1);
                seqNrOutside += 1;
            }

            for(int i = 0; i< monitoringOutside.size(); i++){
                if(!aliveOutside.contains(monitoringOutside.get(i)) && !suspectOutside.contains(monitoringOutside.get(i))){
                    suspectOutside.add(monitoringOutside.get(i));
                }else if(aliveOutside.contains(monitoringOutside.get(i)) && suspectOutside.contains(monitoringOutside.get(i))){
                    suspectOutside.remove(monitoringOutside.get(i));
                }
            }

            trigger(new Message(self, distributor, new DistributorRequestEvent("Are you alive distributor?")), net);
            aliveOutside.clear();

        }
    };

    protected final Handler<ReplicaTimeout> RHBHandler = new Handler<ReplicaTimeout>() {
        @Override
        public void handle(ReplicaTimeout replicaTimeout) {

            if(!aliveInside.isEmpty() && !suspectInside.isEmpty()){
                LOG.debug("I am:  " + self +   " and received a heartbeat too late and will increment my timer now");
                tearDown();
                startTimerReplica(incInside + 1);
                seqNrInside += 1;
                LOG.debug("seqNr has now been incremented to: " + seqNrInside);
            }
            for(int i = 0; i< monitoringInside.size(); i++){

                if(!aliveInside.contains(monitoringInside.get(i)) && !suspectInside.contains(monitoringInside.get(i))){
                    LOG.debug("I am: " + self + "  and I am suspecting the leader: " + monitoringInside.get(i) + " with seqNr: " + seqNrInside);
                    suspectInside.add(monitoringInside.get(i));
                }else if(aliveInside.contains(monitoringInside.get(i)) && suspectInside.contains(monitoringInside.get(i))){
                    LOG.debug("I am: " + self + "  and I stopped suspecting the leader: " + suspectInside.get(i) + " with seqNr " + seqNrInside);
                    suspectInside.remove(monitoringInside.get(i));
                }
            }
                trigger(new Message(self, group.get(0), new LeaderRequestEvent("Are you alive leader?")), net);
                aliveInside.clear();

        }
    };

    protected final ClassMatchedHandler<DistributorResponseEvent, Message> distributorResponseHandler = new ClassMatchedHandler<DistributorResponseEvent, Message>(){

        @Override
        public void handle(DistributorResponseEvent distributorResponse, Message message) {
            LOG.debug("I am the leader: " + self + " and I got an acknowledgement from the distributor: " + message.getSource() + " with message: " + distributorResponse.heartbeat);
        }
    };

    protected final ClassMatchedHandler<LeaderRequestEvent, Message> LHBHandler = new ClassMatchedHandler<LeaderRequestEvent, Message>() {
        @Override
        public void handle(LeaderRequestEvent leaderEvent, Message message) {
            LOG.debug("(LHBHandler) I am leader: " + self + " and I received " + leaderEvent.heartbeat + " from: " + message.getSource() + " with seqNr: " + seqNrInside);
            try {
                LOG.debug("(LHBHandler) I am leader and I will sleep now" );
                Thread.sleep(10000);
                LOG.debug("(LHBHandler) I am leader and I have slept now. I will trigger my heartbeat response back to my replicas");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            trigger(new Message(message.getDestination(), message.getSource(), new LeaderResponseEvent("I'm alive as a leader!")), net);
        }
    };

    protected final ClassMatchedHandler<LeaderResponseEvent, Message> leaderResponseHandler =  new ClassMatchedHandler<LeaderResponseEvent, Message>() {
        @Override
        public void handle(LeaderResponseEvent leaderResponseEvent, Message message) {
            LOG.debug("I am" + self + " and I got an acknowledgement from leader: " + message.getSource() + " that he is alive");
            aliveInside.add(message.getSource());
        }
    };

    @Override
    public void tearDown() {
        trigger(new CancelPeriodicTimeout(timeoutId), timer);
    }


    public void startTimerLeader(int x){

        long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * x);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new DistributorTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();

    }

    public void startTimerReplica(int x){
        long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * x);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new ReplicaTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();
    }

    {
        subscribe(initialAssignmentHandler, boot);
        subscribe(bootHandler, boot);
        subscribe(routeHandler, net);
        subscribe(localRouteHandler, route);
        subscribe(connectHandler, net);
        subscribe(leaderNotificationHandler, net);
        subscribe(DHBHandler, timer);
        subscribe(LHBHandler, net);
        subscribe(leaderResponseHandler, net);
        subscribe(distributorResponseHandler, net);
        subscribe(RHBHandler, timer);
    }
}
