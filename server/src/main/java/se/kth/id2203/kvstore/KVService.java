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
package se.kth.id2203.kvstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.Value;
import se.kth.id2203.kvstore.OpResponse.Code;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class KVService extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(KVService.class);
    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Positive<KVPort> kv = requires(KVPort.class);
    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    protected Map<Integer, Value> KVStore = new HashMap<>();

    //******* Handlers ******


    protected final Handler<KVEvent> clientRequestHandler = new Handler<KVEvent>()

    {

        @Override
        public void handle(KVEvent kvEvent) {

            String op = kvEvent.operation;
            int key = Integer.parseInt(kvEvent.key);
            int value = 0;
            int refValue = 0;
            UUID id = kvEvent.id;
            int timestamp = kvEvent.timestamp;

            NetAddress groupmember = kvEvent.groupmember;
            ArrayList<NetAddress> group = kvEvent.group;
            LOG.debug("op är " + op  + " groupmember är " + groupmember);
            if(kvEvent.value != null) {
                value = Integer.parseInt(kvEvent.value);
            }
            if(kvEvent.refValue != null) {
                refValue = Integer.parseInt(kvEvent.refValue);
            }

            if (op.equalsIgnoreCase("put")){
                Value v = new Value(groupmember, timestamp, value);

                if(KVStore.containsKey(key)) {
                    LOG.debug("VI KOMMER IN I PUT I KVSVERICE groupmember är: " + groupmember + " timestamp är: " + timestamp + " och value är: " + value );
                    Value v1 = KVStore.get(key);
                    if(v1.timestamp == timestamp){
                        for(int i = 0; i < group.size(); i++){

                            if(group.get(i).equals(groupmember)){
                                KVStore.put(key, v);
                                trigger(new Message(self, groupmember, new KVResponse("put", id)), net);
                                break;
                            }
                            if(group.get(i).equals(v1.groupmember)){
                                KVStore.put(key, v1);
                                trigger(new Message(self, groupmember, new KVResponse("put", id)), net);
                                break;
                            }
                        }
                    }else {
                        KVStore.put(key, v);
                        trigger(new Message(self, groupmember, new KVResponse("put", id)), net);
                    }
                }else{
                    LOG.debug("VI KOMMER IN I PUT Förförsta gången I KVSVERICE groupmember är: " + groupmember + " timestamp är: " + timestamp + " och value är: " + value );
                    KVStore.put(key, v);
                    trigger(new Message(self, groupmember, new KVResponse("put", id)), net);
                }


            }

            if(op.equalsIgnoreCase("get")){

                if(KVStore.containsKey(key)){
                    LOG.debug("groupmember är " + groupmember);
                    LOG.debug("VI ÄR I EN GET I KVSERVICE MED KEY " + key + " MED VALUE " + KVStore.get(key).value);
                    trigger(new Message(self, groupmember, new KVResponse("get", KVStore.get(key))), net);
                }else{
                    trigger(new Message(self, groupmember, new KVResponse("get", id, String.valueOf(key), "No value for that key")), net);

                }

            }
            if(op.equalsIgnoreCase("cas")){

            }

        }
    };

    {
        subscribe(clientRequestHandler, kv);
    }
}
