package se.kth.id2203.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.kvstore.OpResponse;
import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.RouteMsg;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by araxi on 2017-03-10.
 */
public class ScenarioClientGet extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(ScenarioClientPut.class);
    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Fields ******
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final NetAddress server = config().getValue("id2203.project.bootstrap-address", NetAddress.class);
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();
    private final SimulationResultMap res1 = SimulationResultSingleton.getInstance();
    private final SimulationResultMap res2 = SimulationResultSingleton.getInstance();
    private final Map<UUID, String> pending = new TreeMap<>();
    private final Map<UUID, String> pending1 = new TreeMap<>();
    private final Map<UUID, String> pending2 = new TreeMap<>();
    //******* Handlers ******
    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            int messages = res.get("messages", Integer.class);
            for (int i = 0; i < messages; i++) {
                Operation op = new Operation("get", "1");
                RouteMsg rm = new RouteMsg(op.operation,op.key, op.value, op); // don't know which partition is responsible, so ask the bootstrap server to forward it
                trigger(new Message(self, server, rm), net);
                pending.put(op.id, op.key);
                LOG.info("Sending {}", op);
                res.put(op.key, "SENT");
            }

            int messages1 = res1.get("messages1", Integer.class);
            for (int i = 0; i < messages1; i++) {
                Operation op1 = new Operation("get", "1431655730");
                RouteMsg rm1 = new RouteMsg(op1.operation,op1.key, op1.value, op1); // don't know which partition is responsible, so ask the bootstrap server to forward it
                trigger(new Message(self, server, rm1), net);
                pending1.put(op1.id, op1.key);
                LOG.info("Sending {}", op1);
                res1.put(op1.key, "SENT");
            }


            int messages2 = res2.get("messages2", Integer.class);
            for (int i = 0; i < messages2; i++) {
                Operation op2 = new Operation("get", "2147483000");
                RouteMsg rm1 = new RouteMsg(op2.operation,op2.key, op2.value, op2); // don't know which partition is responsible, so ask the bootstrap server to forward it
                trigger(new Message(self, server, rm1), net);
                pending2.put(op2.id, op2.key);
                LOG.info("Sending {}", op2);
                res2.put(op2.key, "SENT");
            }
        }
    };
    protected final ClassMatchedHandler<OpResponse, Message> responseHandler = new ClassMatchedHandler<OpResponse, Message>() {

        @Override
        public void handle(OpResponse content, Message context) {
            LOG.debug("Got OpResponse: {}", content);
            //LOG.debug("Got OpResponse with id: {}  status: {}  value: ", content.id, content.status, content.value);

            String key = pending.remove(content.id);
            if (key != null) {
                res.put(key, content.status.toString());
            } else {
                LOG.warn("ID {} was not pending! Ignoring response.", content.id);
            }

            String key1 = pending1.remove(content.id);
            if (key1 != null) {
                res1.put(key1, content.status.toString());
            } else {
                LOG.warn("ID {} was not pending! Ignoring response.", content.id);
            }

            String key2 = pending2.remove(content.id);
            if (key2 != null) {
                res2.put(key2, content.status.toString());
            } else {
                LOG.warn("ID {} was not pending! Ignoring response.", content.id);
            }
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(responseHandler, net);
    }
}
