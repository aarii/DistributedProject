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
import se.kth.id2203.kvstore.OpResponse.Code;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

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

    protected Map<Integer, Integer> KVStore = new HashMap<>();

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
            NetAddress client = kvEvent.client;
            if(kvEvent.value != null) {
                value = Integer.parseInt(kvEvent.value);
            }
            if(kvEvent.refValue != null) {
                refValue = Integer.parseInt(kvEvent.refValue);
            }

            if (op.equalsIgnoreCase("put")){
                LOG.debug("VI KOMMER IN I PUT I KVSVERICE");
                KVStore.put(key,value);
                trigger(new KVEvent("done", client, id), kv);

            }else if(op.equalsIgnoreCase("get")){
                KVStore.get(key);
            }else{
                int temp = KVStore.get(key);
                if(temp == refValue){
                    KVStore.put(key, value);
                }
            }

        }
    };

    {
        subscribe(clientRequestHandler, kv);
    }
}
