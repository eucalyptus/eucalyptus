package edu.ucsb.eucalyptus.cloud.ws;

public interface CallBack {
	void run();
	int getUpdateThreshold();
	void finish();
	void failed();
}

