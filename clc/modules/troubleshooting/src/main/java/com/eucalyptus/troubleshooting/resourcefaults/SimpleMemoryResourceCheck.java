package com.eucalyptus.troubleshooting.resourcefaults;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

public class SimpleMemoryResourceCheck {
	private final static Logger LOG = Logger.getLogger(SimpleMemoryResourceCheck.class);

	private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private static final int OUT_OF_MEMORY_FAULT_ID = 1004;
	private final static long DEFAULT_POLL_INTERVAL = 60 * 1000;
	private static final Class <? extends ComponentId> DEFAULT_COMPONENT_ID_CLASS = Eucalyptus.class;

	/**
	 * Marking the constructor private on purpose, so that no code can instantiate an object this class
	 */
	private SimpleMemoryResourceCheck() {

	}

	public static ScheduledFuture<?> start(MemoryChecker checker) {
		return pool.scheduleWithFixedDelay(checker, 0, checker.pollInterval, TimeUnit.MILLISECONDS);
	}

	// Someone should be calling this, currently no one is. Might be a nice thing to say hello to in the service shutdown hooks. Although might complicate stuff
	// when multiple services using it
	public static void shutdown() {
		pool.shutdownNow();
	}

	public static class MemoryInfo {
		private Long minimumFreeSpace;
		private Double percentFreeSpace;

		public Long getThreshold() {
			if (null != this.minimumFreeSpace) {
				return this.minimumFreeSpace;
			} else {
				return (long) (Runtime.getRuntime().maxMemory() * this.percentFreeSpace / 100);
			}
		}

		public MemoryInfo(Long minimumFreeSpace) {
			super();
			this.minimumFreeSpace = minimumFreeSpace;
		}

		public MemoryInfo(Double percentFreeSpace) {
			super();
			this.percentFreeSpace = percentFreeSpace;
		}

	}

	public static class MemoryChecker implements Runnable {
		private MemoryInfo memoryInfo;
		private boolean hasFaultedRecently = false;
		private long pollInterval;
		private Class <? extends ComponentId> componentIdClass;

		public MemoryChecker(MemoryInfo memoryInfo) {
			this.memoryInfo = memoryInfo;
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentIdClass = DEFAULT_COMPONENT_ID_CLASS;
		}

		public MemoryChecker(MemoryInfo info, Class<? extends ComponentId> componentIdClass,
				long pollInterval) {
			this.memoryInfo = memoryInfo;
			this.pollInterval = pollInterval;
			this.componentIdClass = componentIdClass;
		}

		@Override
		public void run() {
			LOG.debug("Polling memory " + pollInterval + " threshold = " + memoryInfo.getThreshold());
			// Enclose everything between try catch because nothing should throw an exception to the executor upstream or it may halt subsequent tasks
			try {
				long usableSpace = Runtime.getRuntime().freeMemory();
				if (usableSpace < memoryInfo.getThreshold()) {
					if (!hasFaultedRecently) {
						FaultSubsystem.forComponent(this.componentIdClass).havingId(OUT_OF_MEMORY_FAULT_ID)
								.withVar("component", ComponentIds.lookup(this.componentIdClass).getFaultLogPrefix()).log();
						this.hasFaultedRecently = true;
					} else {
						// fault has already been logged. do nothing
					}
				} else {
					// Remove this location from the already faulted set. If the location is not in the set, this call will simply return false. no harm
					// done. another if condition is just one unnecessary step
					this.hasFaultedRecently = false;
				}
			} catch (Exception ex) {
				// what to do when an exception is caught? should we remove the location off the list?
				LOG.error("Simple memory resource check failed", ex);
			}
		}
	}
}
