package com.eucalyptus.troubleshooting.resourcefaults;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

public class SimpleMemoryResourceCheck extends Thread {
	private final static Logger LOG = Logger.getLogger(SimpleMemoryResourceCheck.class);
	private final static long POLL_TIME = 1 * 1000; // 60 * 1000; // poll every minute (TODO: configure)
	private static final int OUT_OF_MEMORY_FAULT_ID = 1004;
	private long minimumFreeMemoryBytes;
	
	public SimpleMemoryResourceCheck(long minimumFreeMemoryBytes) {
		this.minimumFreeMemoryBytes = minimumFreeMemoryBytes;
	}
	
	@Override
	public void run() {
		while (true) {
			List<GarbageCollectorMXBean> mxBeans = ManagementFactory.getGarbageCollectorMXBeans();
			LOG.debug("num garbage collection beans = " + mxBeans.size());
			for (GarbageCollectorMXBean bean: mxBeans) {
				LOG.debug("garbage bean info = [name = " + bean.getName() + ", collectionCount = " + bean.getCollectionCount() + ", collectionTime = " + bean.getCollectionTime());
			}
			long availableMemory = Runtime.getRuntime().freeMemory();
			LOG.debug("availableMemory="+availableMemory);
			if (availableMemory < minimumFreeMemoryBytes) {
				//FaultSubsystem.forComponent(Eucalyptus.INSTANCE).havingId(OUT_OF_MEMORY_FAULT_ID).withVar("component", "eucalyptus").log();
				break; // no need to continue monitoring.  This fault can only be logged once.
			}
			try {
				Thread.sleep(POLL_TIME);
			} catch (InterruptedException ex) {
				LOG.warn("Polling thread interrupted");
			}
		}
	}

}
