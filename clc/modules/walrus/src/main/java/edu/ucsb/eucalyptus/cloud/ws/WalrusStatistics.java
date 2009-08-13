package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

public class WalrusStatistics {
	private Logger LOG = Logger.getLogger( WalrusStatistics.class );
	private long totalBytesIn;
	private long totalBytesOut;

	public WalrusStatistics() {
		totalBytesIn = 0;
		totalBytesOut = 0;
	}

	public void updateBytesIn(long bytes) {
		totalBytesIn += bytes;
	}

	public void updateBytesOut(long bytes) {
		totalBytesOut += bytes;
	}

	public void resetBytesIn() {
		totalBytesIn = 0;
	}

	public void resetBytesOut() {
		totalBytesOut = 0;
	}

	public void dumpBytesIn() {
		LOG.info("Bytes IN so far: " + totalBytesIn);
	}

	public void dumpBytesOut() {
		LOG.info("Bytes OUT so far: " + totalBytesOut);
	}
}
