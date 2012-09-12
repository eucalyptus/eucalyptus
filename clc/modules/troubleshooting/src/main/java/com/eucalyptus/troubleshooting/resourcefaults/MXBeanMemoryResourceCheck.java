package com.eucalyptus.troubleshooting.resourcefaults;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

public class MXBeanMemoryResourceCheck extends Thread {
	private final static Logger LOG = Logger.getLogger(DiskResourceCheck.class);
	private final static long POLL_TIME = 60 * 1000; // poll every minute (TODO: configure)
	private static final int MXBEAN_OUT_OF_MEMORY_FAULT_ID = 1005;
	private static final long THRESHOLD = 1;//512 * 1024; // 512K, TODO: make this customizable
	private Set<String> alreadyFaulted = new HashSet<String>();

	@Override
	public void run() {
		while (true) {
			Iterator iter = ManagementFactory.getMemoryPoolMXBeans().iterator();
			while (iter.hasNext()) {
			    MemoryPoolMXBean item = (MemoryPoolMXBean) iter.next();
			    String name = item.getName();
			    MemoryType type = item.getType();
			    MemoryUsage usage = item.getUsage();
			    LOG.debug("Checking memory pool " + name + " of type" + type);
			    if (usage.getMax() - usage.getUsed() < THRESHOLD) {
			    	if (!alreadyFaulted.contains(name + ":" + type.toString())) {
						FaultSubsystem.forComponent(Eucalyptus.INSTANCE).havingId(MXBEAN_OUT_OF_MEMORY_FAULT_ID).withVar("component", "eucalyptus").withVar("name",  name).log();
						alreadyFaulted.add(name + ":" + type.toString());
			    	}
			    }
			}
			try {
				Thread.sleep(POLL_TIME);
			} catch (InterruptedException ex) {
				LOG.warn("Polling thread interrupted");
			}
		}
	}
}
