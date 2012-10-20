package com.eucalyptus.troubleshooting.checker;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Eucalyptus;

public class PermGenMemoryCheck {
	private final static Logger LOG = Logger.getLogger(PermGenMemoryCheck.class);

	private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private static final int OUT_OF_PERM_GEN_MEMORY_FAULT_ID = 1005;
	private final static long DEFAULT_POLL_INTERVAL = 60 * 1000;
	private static final Class <? extends ComponentId> DEFAULT_COMPONENT_ID_CLASS = Eucalyptus.class;

	/**
	 * Marking the constructor private on purpose, so that no code can instantiate an object this class
	 */
	private PermGenMemoryCheck() {

	}

	public static ScheduledFuture<?> start(PermGenMemoryChecker checker) {
		return pool.scheduleWithFixedDelay(checker, 0, checker.pollInterval, TimeUnit.MILLISECONDS);
	}

	// Someone should be calling this, currently no one is. Might be a nice thing to say hello to in the service shutdown hooks. Although might complicate stuff
	// when multiple services using it
	public static void shutdown() {
		pool.shutdownNow();
	}

	public static class PermGenMemoryChecker implements Runnable {
		private double ratio;
		private long pollInterval;
		private Class <? extends ComponentId> componentIdClass;

		public PermGenMemoryChecker(double ratio) {
			this.ratio = ratio;
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentIdClass = DEFAULT_COMPONENT_ID_CLASS;
		}

		public PermGenMemoryChecker(double ratio, Class<? extends ComponentId> componentIdClass,
				long pollInterval) {
			this.ratio = ratio;
			this.pollInterval = pollInterval;
			this.componentIdClass = componentIdClass;
		}

		@Override
		public void run() {
			List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
			boolean noPermGenBeans = true;
			if (null != beans) {
				boolean alreadyFaulted = false;
				for (MemoryPoolMXBean bean : beans) {
					String name = bean.getName();
					if (!name.contains("Perm Gen")) continue;
					if (bean.getType() != MemoryType.NON_HEAP) continue;
					noPermGenBeans = false;
					
					double actualRatio = ((double) bean.getUsage().getUsed()) / ((double) bean.getUsage().getMax());
					LOG.debug("Perm-gen memory usage ratio = " + actualRatio);
					if (actualRatio > ratio) {
						if (!alreadyFaulted) {
							Faults.forComponent(Eucalyptus.class).havingId(OUT_OF_PERM_GEN_MEMORY_FAULT_ID).withVar("component", Eucalyptus.INSTANCE.getFaultLogPrefix()).log();
							alreadyFaulted = true;
						}
					}
				}
			} else {
				// nothing to check
			}
			if (noPermGenBeans) {
				LOG.warn("Unable to find any mxbeans for Perm-Gen usage");
			}
		}
	}
}
