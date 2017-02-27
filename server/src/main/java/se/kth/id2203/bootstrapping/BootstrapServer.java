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
package se.kth.id2203.bootstrapping;

import com.google.common.collect.ImmutableSet;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.distributor.DistributionPort;
import se.kth.id2203.distributor.SendLookupTable;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

public class BootstrapServer extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(BootstrapServer.class);
    //******* Ports ******
    protected final Negative<Bootstrapping> boot = provides(Bootstrapping.class);
    protected final Negative<DistributionPort> distributionPort = provides(DistributionPort.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    final int bootThreshold = config().getValue("id2203.project.bootThreshold", Integer.class);
    private State state = State.COLLECTING;
    private UUID timeoutId;
    private LookupTable groups = new LookupTable();
    private  ArrayList<NetAddress> active = new ArrayList<>();
    private final Set<NetAddress> ready = new HashSet<>();
    private NodeAssignment initialAssignment = null;
    private int groupCount = 0;
    private ArrayList<NetAddress> done = new ArrayList<>();
    //******* Handlers ******
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            LOG.info("Starting bootstrap server on {}, waiting for {} nodes...", self, bootThreshold);
            long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 2);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new BSTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
            //active.add(self);
        }
    };
    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {
        @Override
        public void handle(BSTimeout e) {
            if (state == State.COLLECTING) {
                LOG.info("{} hosts in active set.", active.size());

                if (active.size() >= bootThreshold) {
                    ArrayList<NetAddress> group = new ArrayList<>(active.subList(0, 3));
                        // LOG.info("group innehalller " + group);
                        bootUp(group);
                        for(int i =0; i<group.size(); i++){
                            done.add(group.get(i));
                        }

                        groups.put(groupCount, group);
                        LOG.info("Lookup table: " + groups.toString());
                        active.clear();

                    groupCount++;
                        if(groupCount == bootThreshold) {
                            state = State.SEEDING;
                        }
                }
            } else if (state == State.SEEDING) {
                LOG.info("{} hosts in ready set.", ready.size());
                if (ready.size() >= bootThreshold) {
                    LOG.info("Finished seeding. Bootstrapping complete.");
                    //LOG.info("Initial assignment är: " + initialAssignment);
                    //trigger(new Booted(initialAssignment), boot);
                    if(groupCount == bootThreshold){
                        LOG.debug("I am about to send the look up table to the Distributor ");
                        trigger(new SendLookupTable(groups), distributionPort);

                        //starta en ny komponent som heter distributor och skicka alla viktiga grejer till den
                        state = State.DONE;
                    }
                }
            } else if (state == State.DONE) {
                suicide();
            }
        }
    };
    protected final Handler<InitialAssignments> assignmentHandler = new Handler<InitialAssignments>() {
        @Override
        public void handle(InitialAssignments e) {
            LOG.info("Seeding assignments...");
            initialAssignment = e.assignment;
           // LOG.info("GROUPCOUNT ÄR " + groupCount + " MED INITIALASSIGNMENT " + initialAssignment);
            for (NetAddress node : done) {
                trigger(new Message(self, node, new Boot(initialAssignment)), net);
            }

            //ready.add(self);
        }
    };
    protected final ClassMatchedHandler<CheckIn, Message> checkinHandler = new ClassMatchedHandler<CheckIn, Message>() {

        @Override
        public void handle(CheckIn content, Message context) {
            if(!done.contains(context.getSource())) {
                active.add(context.getSource());
            }
        }
    };
    protected final ClassMatchedHandler<Ready, Message> readyHandler = new ClassMatchedHandler<Ready, Message>() {
        @Override
        public void handle(Ready content, Message context) {
            ready.add(context.getSource());
        }
    };



    {
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(assignmentHandler, boot);
        subscribe(checkinHandler, net);
        subscribe(readyHandler, net);
    }

    @Override
    public void tearDown() {
        trigger(new CancelPeriodicTimeout(timeoutId), timer);
    }

    private void bootUp(ArrayList active) {
        LOG.info("Threshold reached. Generating assignments...");
        trigger(new GetInitialAssignments(active), boot);
    }

    static enum State {

        COLLECTING,
        SEEDING,
        DONE;
    }
}
