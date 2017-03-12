package se.kth.id2203;

import com.google.common.base.Optional;
import se.kth.id2203.bootstrapping.BootstrapClient;
import se.kth.id2203.bootstrapping.BootstrapServer;
import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.distributor.DistributionPort;
import se.kth.id2203.distributor.DistributorComponent;
import se.kth.id2203.kvstore.KVPort;
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class ParentComponent
        extends ComponentDefinition {

    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Children ******
    protected  Component overlay;
    protected final Component kv = create(KVService.class, Init.NONE);
    protected Component distributor;
    protected final Component boot;

    {
        Optional<NetAddress> serverO = config().readValue("id2203.project.bootstrap-address", NetAddress.class);
        if (serverO.isPresent()) { // start in client mode
            boot = create(BootstrapClient.class, Init.NONE);
            overlay = create(VSOverlayManager.class, Init.NONE);

            // Overlay
            connect(boot.getPositive(Bootstrapping.class), overlay.getNegative(Bootstrapping.class), Channel.TWO_WAY);
            connect(net, overlay.getNegative(Network.class), Channel.TWO_WAY);
            connect(timer, overlay.getNegative(Timer.class), Channel.TWO_WAY);

            // KV
            connect(overlay.getPositive(KVPort.class), kv.getNegative(KVPort.class), Channel.TWO_WAY);
            connect(overlay.getPositive(Routing.class), kv.getNegative(Routing.class), Channel.TWO_WAY);
            connect(net, kv.getNegative(Network.class), Channel.TWO_WAY);


        } else { // start in server mode

            boot = create(BootstrapServer.class, Init.NONE);
            distributor = create(DistributorComponent.class, Init.NONE);

            connect(boot.getPositive(DistributionPort.class), distributor.getNegative(DistributionPort.class), Channel.TWO_WAY);
            // Distributor
            // connect(distributor.getPositive(Network.class), overlay.getNegative(Network.class), Channel.TWO_WAY);
            connect(net, distributor.getNegative(Network.class), Channel.TWO_WAY);
            connect(timer, distributor.getNegative(Timer.class), Channel.TWO_WAY);


        }

        connect(timer, boot.getNegative(Timer.class), Channel.TWO_WAY);
        connect(net, boot.getNegative(Network.class), Channel.TWO_WAY);

    }
}
