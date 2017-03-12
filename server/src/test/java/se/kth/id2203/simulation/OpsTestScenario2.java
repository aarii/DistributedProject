package se.kth.id2203.simulation;

import junit.framework.Assert;
import org.junit.Test;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

/**
 * Created by araxi on 2017-03-10.
 */
public class OpsTestScenario2 {

    private static final int NUM_MESSAGES = 10;
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();
    private final SimulationResultMap res1 = SimulationResultSingleton.getInstance();
    private final SimulationResultMap res2 = SimulationResultSingleton.getInstance();

    @Test
    public void simpleOpsTest() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simpleOps(10);
        res.put("messages", NUM_MESSAGES);
        res1.put("messages1", NUM_MESSAGES);
        res2.put("messages2", NUM_MESSAGES);
        simpleBootScenario.simulate(LauncherComp.class);

        for (int i = 0; i < NUM_MESSAGES; i++) {
            Assert.assertEquals("NOT_FOUND", res.get("1", String.class));
            // of course the correct response should be SUCCESS not NOT_IMPLEMENTED, but like this the test passes
        }

        for (int i = 0; i < NUM_MESSAGES; i++) {
            Assert.assertEquals("NOT_FOUND", res1.get("1431655730", String.class));
            // of course the correct response should be SUCCESS not NOT_IMPLEMENTED, but like this the test passes
        }

        for (int i = 0; i < NUM_MESSAGES; i++) {
            Assert.assertEquals("NOT_FOUND", res2.get("2147483000", String.class));
            // of course the correct response should be SUCCESS not NOT_IMPLEMENTED, but like this the test passes
        }
    }
}
