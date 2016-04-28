package microbenchmarks.onesided;

import java.util.HashMap;

import microbenchmarks.TestBase;

public class TestRunner {

	public static void main(String[] args) {
		HashMap<String, TestBase> map = new HashMap<>();
		map.put("PutLatency", new PutLatencyTest());
		map.put("PutBandwidth", new PutBandwidthTest());
		map.put("PutBiBandwidth", new PutBiBandwidthTest());
		map.put("GetLatency", new GetLatencyTest());
		map.put("GetBandwidth", new GetBandwidthTest());
		map.put("GetAccumulateLatency", new GetAccumulateLatencyTest());
		map.put("CasLatency", new CasLatencyTest());
		map.put("FopLatency", new FopLatencyTest());
		
		TestBase test = map.get(System.getProperty("TestName"));
		test.setOsArgs(args);
		test.run();
	}

}
