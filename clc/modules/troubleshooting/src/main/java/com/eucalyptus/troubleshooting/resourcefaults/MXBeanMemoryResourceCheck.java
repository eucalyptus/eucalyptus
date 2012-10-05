package com.eucalyptus.troubleshooting.resourcefaults;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

public class MXBeanMemoryResourceCheck {
	private final static Logger LOG = Logger.getLogger(MXBeanMemoryResourceCheck.class);

	private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private static final int MXBEAN_OUT_OF_MEMORY_FAULT_ID = 1005;
	private final static long DEFAULT_POLL_INTERVAL = 60 * 1000;
	private static final Class <? extends ComponentId> DEFAULT_COMPONENT_ID_CLASS = Eucalyptus.class;

	/**
	 * Marking the constructor private on purpose, so that no code can instantiate an object this class
	 */
	private MXBeanMemoryResourceCheck() {

	}

	public static ScheduledFuture<?> start(MXBeanMemoryChecker checker) {
		return pool.scheduleWithFixedDelay(checker, 0, checker.pollInterval, TimeUnit.MILLISECONDS);
	}

	// Someone should be calling this, currently no one is. Might be a nice thing to say hello to in the service shutdown hooks. Although might complicate stuff
	// when multiple services using it
	public static void shutdown() {
		pool.shutdownNow();
	}

	public static class MXBeanInfo {
		private Long minimumFreeSpace;
		private Double percentFreeSpace;

		public Long getThreshold(MemoryPoolMXBean bean) {
			if (null != this.minimumFreeSpace) {
				return this.minimumFreeSpace;
			} else {
				return (long) (bean.getUsage().getMax() * this.percentFreeSpace / 100);
			}
		}

		public MXBeanInfo(Long minimumFreeSpace) {
			super();
			this.minimumFreeSpace = minimumFreeSpace;
		}

		public MXBeanInfo(Double percentFreeSpace) {
			super();
			this.percentFreeSpace = percentFreeSpace;
		}

	}

	public static class MXBeanMemoryChecker implements Runnable {
		private MXBeanInfo mxBeanInfo;
		private long pollInterval;
		private Class <? extends ComponentId> componentIdClass;

		private Set<String> alreadyFaulted = new HashSet<String>();

		public MXBeanMemoryChecker(MXBeanInfo mxBeanInfo) {
			this.mxBeanInfo = mxBeanInfo;
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentIdClass = DEFAULT_COMPONENT_ID_CLASS;
		}

		public MXBeanMemoryChecker(MXBeanInfo mxBeanInfo, Class<? extends ComponentId> componentIdClass,
				long pollInterval) {
			this.mxBeanInfo = mxBeanInfo;
			this.pollInterval = pollInterval;
			this.componentIdClass = componentIdClass;
		}

		@Override
		public void run() {
			List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
			if (null != beans) {
				for (MemoryPoolMXBean bean : beans) {
					String name = bean.getName();
					String identifier = bean.getName() + ":" + bean.getType().toString();
					LOG.debug("Polling memory pool " + identifier + " threshold = " + mxBeanInfo.getThreshold(bean));
					try {
						long free = bean.getUsage().getMax() - bean.getUsage().getUsed();
						if (free < mxBeanInfo.getThreshold(bean)) {
							if (!this.alreadyFaulted.contains(identifier)) {
								FaultSubsystem.forComponent(Eucalyptus.class).havingId(MXBEAN_OUT_OF_MEMORY_FAULT_ID).withVar("component", Eucalyptus.INSTANCE.getFaultLogPrefix()).withVar("name",  name).log();
								this.alreadyFaulted.add(identifier);
							} else {
								// fault has already been logged. do nothing
							}
						} else {
							// Remove this location from the already faulted set. If the location is not in the set, this call will simply return false. no harm
							// done. another if condition is just one unnecessary step
							this.alreadyFaulted.remove(identifier);
						}
					} catch (Exception ex) {
						// what to do when an exception is caught? should we remove the location off the list?
						LOG.error("Disk resource check failed for " + identifier, ex);
					}
				}
			} else {
				// nothing to check
			}
		}
	}
}
