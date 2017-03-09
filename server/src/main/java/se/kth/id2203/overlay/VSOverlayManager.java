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

import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.Value;
import se.kth.id2203.bootstrapping.Booted;
import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.bootstrapping.GetInitialAssignments;
import se.kth.id2203.bootstrapping.InitialAssignments;
import se.kth.id2203.distributor.DistributorNotification;
import se.kth.id2203.failuredetection.*;
import se.kth.id2203.kvstore.*;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.MessageNotify;
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
    protected final Negative<KVPort> clientRequest = provides(KVPort.class);
    //******* Fields ******

    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    NetAddress distributor = null;
    private LookupTable lut = null;
    private ArrayList<NetAddress> group = null;
    private UUID timeoutId;
    private ArrayList<NetAddress> alive = new ArrayList<>();
    private ArrayList<NetAddress> suspect = new ArrayList<>();
    private ArrayList<NetAddress> monitoring = new ArrayList<>();
    private int delta = 2;
    int seqNr = 0;
    NetAddress client = null;
    boolean sent = false;

    protected int majorityCounter = 0;
    protected int epsilon = 2;
    protected ArrayList<Value> groupValues = new ArrayList<>();
    protected int timestamp = 0;
    protected ArrayList<Integer> timestamps = new ArrayList<>();




    protected final ClassMatchedHandler<GetInitialAssignments, Message> initialAssignmentHandler = new ClassMatchedHandler<GetInitialAssignments, Message>() {
        @Override
        public void handle(GetInitialAssignments getInitialAssignments, Message message) {
            LOG.info("Generating LookupTable...");
            LookupTable lut = LookupTable.generate(getInitialAssignments.nodes);
            LOG.debug("Generated assignments:\n{}", lut);
            trigger(new Message(self, message.getSource(), new InitialAssignments(lut)), net);
        }
    };

    protected final Handler<Booted> bootHandler = new Handler<Booted>() {

        @Override
        public void handle(Booted event) {
            if (event.assignment instanceof LookupTable) {
                LOG.info("Got NodeAssignment, overlay ready.");
                lut = (LookupTable) event.assignment;
                group = lut.get(0);
                initFailureDetector();
            } else {
                LOG.error("Got invalid NodeAssignment type. Expected: LookupTable; Got: {}", event.assignment.getClass());
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

    protected final ClassMatchedHandler<ClientRequestEvent, Message> clientRequestHandler = new ClassMatchedHandler<ClientRequestEvent, Message>() {
        @Override
        public void handle(ClientRequestEvent clientRequestEvent, Message message) {

            LOG.info("I am leader {} and I received a route msg with operation {} and key {} value {} ", self,
                    clientRequestEvent.operation, clientRequestEvent.key, clientRequestEvent.value);
            LOG.debug("I am leader " + self + " and I received a route msg with operation " + clientRequestEvent.operation+
                    " and key " + clientRequestEvent.key + " value " + clientRequestEvent.value);

        }
    };

    protected final ClassMatchedHandler<Operation, Message> opHandler = new ClassMatchedHandler<Operation, Message>() {
        @Override
        public void handle(Operation operation, Message message) {
            client = message.getSource();
            if(operation.operation.equalsIgnoreCase("put")){
                for(int i = 0; i<group.size(); i++) {
                    trigger(new Message(self, group.get(i), new RequestTimestampEvent(operation)), net);
                    LOG.debug("I am " + self + " and I am sending a requesttimestampevent to " + group.get(i));
                }
            }

            if(operation.operation.equalsIgnoreCase("get")) {
                for (int i = 0; i < group.size(); i++) {
                    trigger(new Message(self, group.get(i), new RequestGetValuesEvent(operation)), net);
                    LOG.debug("I am " + self + " and I am sending a requestgetvaluesevent to " + group.get(i));

                }
            }

        }
    };


    protected final ClassMatchedHandler<RequestGetValuesEvent, Message> requestGetValueHandler = new ClassMatchedHandler<RequestGetValuesEvent, Message>() {
        @Override
        public void handle(RequestGetValuesEvent requestGetValuesEvent, Message message) {
            Operation op = requestGetValuesEvent.operation;
            LOG.debug("I am " + self + " and I received a requestGetvalue and will now do a kvevent");
            trigger(new KVEvent(op.operation, op.key,op.id, message.getSource()), clientRequest);

        }
    };

    protected final ClassMatchedHandler<RequestTimestampEvent, Message> requestTimestampHandler = new ClassMatchedHandler<RequestTimestampEvent, Message>() {
        @Override
        public void handle(RequestTimestampEvent requestTimestampEvent, Message message) {
            LOG.debug("I am " + self + " and I received a requesttimestampevent and will now response ");
            trigger(new Message(self, message.getSource(), new ResponseTimestampEvent(requestTimestampEvent.operation, timestamp)), net);

        }
    };

    protected final ClassMatchedHandler<ResponseTimestampEvent, Message> responseTimestampHandler = new ClassMatchedHandler<ResponseTimestampEvent, Message>() {
        @Override
        public void handle(ResponseTimestampEvent responseTimestampEvent, Message message) {
            Operation operation = responseTimestampEvent.operation;
            timestamps.add(responseTimestampEvent.timestamp);
            if(timestamps.size() == epsilon){
                int maxTimestamp = 0;
                for(int i = 0; i<timestamps.size(); i++){
                    if(maxTimestamp < timestamps.get(i)){
                        maxTimestamp = timestamps.get(i);
                    }
                }
                        timestamps.clear();
                if(operation.operation.equalsIgnoreCase("put")){
                    for(int i = 0; i<group.size(); i++) {
                        LOG.debug("I am " + self + " and send a kvrequest to " + group.get(i));
                        trigger(new Message(self,group.get(i),new KVRequest(operation.operation, operation.key, operation.value, operation.id, self, maxTimestamp +1)), net);
                    }
                }
            }
        }
    };

    protected final ClassMatchedHandler<KVRequest, Message> KVRequestHandler = new ClassMatchedHandler<KVRequest, Message>() {
        @Override
        public void handle(KVRequest kvRequest, Message message) {
            if(kvRequest.operation.equalsIgnoreCase("put")){
                if(timestamp <= kvRequest.maxTimestamp) {
                    timestamp = kvRequest.maxTimestamp;
                    LOG.debug("I am " + self + " and I will do a kvevent now");
                    trigger(new KVEvent(kvRequest.operation, kvRequest.key, kvRequest.value, kvRequest.id, kvRequest.groupmember, timestamp, group), clientRequest);
                }
            }
        }
    };

    protected final ClassMatchedHandler<KVResponse, Message> KVResponseHandler = new ClassMatchedHandler<KVResponse, Message>() {
        @Override
        public void handle(KVResponse kvResponse, Message message) {
            LOG.debug("I am "+ self + " and I received an operation: " + kvResponse.operation);
            if(kvResponse.operation.equalsIgnoreCase("put")) {
                majorityCounter++;
                LOG.debug("I am " + self + " and majorityCounter is now in put  " + majorityCounter);


                if(sent == true){
                    majorityCounter = 0;
                    LOG.debug("I am " + self + " and majorityCounter is now in put  " + majorityCounter);
                    sent = false;
                }

                if (majorityCounter == epsilon) {
                    LOG.debug("ÄR I IF I MAJORITYCOUNTER");
                    trigger(new Message(self, client, new OpResponse(kvResponse.operation, kvResponse.id, OpResponse.Code.OK)), net);
                    sent = true;
                    majorityCounter = 0;
                }

            }else if(kvResponse.operation.equalsIgnoreCase("get")){

                LOG.debug("Vi kommer in i KVResponseHandler get ");
                groupValues.add(kvResponse.v);
                LOG.debug("I am " + self + " and majoritycounter is in get: " + majorityCounter);

                if(sent == true){
                    groupValues.clear();
                    LOG.debug("I am " + self + " and majorityCounter is now in get  " + majorityCounter);
                    sent = false;
                }

                    if(groupValues.size() == epsilon){

                        for(int i = 0; i<groupValues.size(); i++){
                            if(kvResponse.value != null){
                                if(kvResponse.value.equalsIgnoreCase("No value for that key")) {
                                    trigger(new Message(self, client, new OpResponse(kvResponse.operation, kvResponse.id, kvResponse.value, OpResponse.Code.NOT_FOUND)), net);
                                    sent = true;
                                    groupValues.clear();
                                    break;
                                }
                            }
                            LOG.debug("groupValues size är " + groupValues.size() + " groupValues get i är " + groupValues.get(i).value + " " +
                                    "groupValues i + 1 är " + groupValues.get(i+1).value);
                            if(groupValues.get(i).timestamp < groupValues.get(i+1).timestamp){
                                LOG.debug("LALALALALALALALALA VÄRDET ÄR " + groupValues.get(i+1).value);
                                trigger(new Message(self, client, new OpResponse(kvResponse.operation, kvResponse.id,  String.valueOf(groupValues.get(i + 1).value), OpResponse.Code.OK)), net);
                                sent = true;
                                groupValues.clear();
                                break;

                            }else{

                                LOG.debug("LALALALALALALALALA VÄRDET ÄR " + groupValues.get(i).value);
                                trigger(new Message(self, client, new OpResponse(kvResponse.operation, kvResponse.id, String.valueOf(groupValues.get(i).value), OpResponse.Code.OK)), net);
                                sent = true;
                                groupValues.clear();
                                break;
                            }

                        }
                    }
            }
        }
    };




    /* FAILURE DETECTOR FUNCTIONALITY */

    protected final ClassMatchedHandler<DistributorNotification, Message> distributorNotificationHandler = new ClassMatchedHandler<DistributorNotification, Message>() {

        @Override
        public void handle(DistributorNotification distributorNotification, Message message) {

            distributor = message.getSource();
            LOG.debug("I am " + self + " and I received a message from my distributor with address: " + distributor);
        }
    };

    protected final Handler<GroupTimeout> heartBeatHandler = new Handler<GroupTimeout>() {
        @Override
        public void handle(GroupTimeout groupTimeout) {

            if(!alive.isEmpty() && !suspect.isEmpty()){
                LOG.debug("I am:  " + self +   " and received a heartbeat too late from a group member and will increment my timer now");
                tearDown();
                delta +=1;
                startTimer(delta);
                seqNr += 1;
                LOG.debug("seqNr has now been incremented to: " + seqNr);
            }

            for(int i = 0; i< monitoring.size(); i++){
                if(!alive.contains(monitoring.get(i)) && !suspect.contains(monitoring.get(i))){
                    LOG.debug("I am: " + self + " and I am suspecting group member: " + monitoring.get(i) + " with seqNr" + seqNr);
                    LOG.warn("");
                    suspect.add(monitoring.get(i));
                    if(suspect.size() == epsilon){
                        LOG.warn("WARNING TOO FEW MEMBERS IN GROUP!!!!!!!!!!!!");
                    }
                }else if(alive.contains(monitoring.get(i)) && suspect.contains(monitoring.get(i))){
                    LOG.debug("I am: " + self + " and I am unsuspecting group member: " + monitoring.get(i) +  " with seqNr" + seqNr);
                    suspect.remove(monitoring.get(i));
                }

                trigger(new Message(self, monitoring.get(i) , new HeartbeatRequestEvent("Are you alive group member?")), net);
            }
            alive.clear();
        }
    };


    protected final ClassMatchedHandler<HeartbeatRequestEvent, Message> HBRequestHandler = new ClassMatchedHandler<HeartbeatRequestEvent, Message>() {
        @Override
        public void handle(HeartbeatRequestEvent heartbeatRequest, Message message) {
            trigger(new Message(self, message.getSource(), new HeartbeatResponseEvent("I am alive as a replica")), net);

        }
    };

    protected final ClassMatchedHandler<HeartbeatResponseEvent, Message> HBResponseHandler = new ClassMatchedHandler<HeartbeatResponseEvent, Message>(){

        @Override
        public void handle(HeartbeatResponseEvent heartbeatResponse, Message message) {
            LOG.debug("I am: " + self + " and I got an acknowledgement from group member: " + message.getSource() + " that he is alive");
            alive.add(message.getSource());
        }
    };



    @Override
    public void tearDown() { trigger(new CancelPeriodicTimeout(timeoutId), timer); }


    public void startTimer(int delta){

        long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * delta);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new GroupTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();

    }

    public void initFailureDetector(){

        for(int i = 0; i<group.size(); i++) {
            if(!group.get(i).equals(self)) {
                monitoring.add(group.get(i));
                alive.add(group.get(i));
            }
        }
        startTimer(delta);
    }

    {
        subscribe(initialAssignmentHandler, net);
        subscribe(bootHandler, boot);
        subscribe(opHandler, net);
        subscribe(clientRequestHandler,net);
        subscribe(connectHandler, net);
        subscribe(distributorNotificationHandler, net);
        subscribe(heartBeatHandler, timer);
        //subscribe(HBRequestHandler, net);
        //subscribe(HBResponseHandler, net);
        subscribe(KVResponseHandler, net);
        subscribe(KVRequestHandler, net);
        subscribe(requestTimestampHandler, net);
        subscribe(responseTimestampHandler, net);
        subscribe(requestGetValueHandler, net);
    }
}
