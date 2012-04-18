package edu.ucsb.eucalyptus.cloud.ws;

public interface CallBack {
	void run();
	long getUpdateThreshold();
	void finish();
	void failed();
}

