package com.eucalyptus.troubleshooting.resourcefaults;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

public class GarbageCollectionCountResourceCheck {
	private final static Logger LOG = Logger.getLogger(GarbageCollectionCountResourceCheck.class);

	private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private static final int GARBAGE_COLLECTION_COUNT_FAULT_ID = 1007;
	private final static long DEFAULT_POLL_INTERVAL = 1 * 1000;
	private static final Class <? extends ComponentId> DEFAULT_COMPONENT_ID_CLASS = Eucalyptus.class;

	private GarbageCollectionCountResourceCheck() {

	}

	public static ScheduledFuture<?> start(GarbageCollectionChecker checker) {
		return pool.scheduleWithFixedDelay(checker, 0, checker.pollInterval, TimeUnit.MILLISECONDS);
	}

	// Someone should be calling this, currently no one is. Might be a nice thing to say hello to in the service shutdown hooks. Although might complicate stuff
	// when multiple services using it
	public static void shutdown() {
		pool.shutdownNow();
	}

	public static class GarbageCollectorInfo {
		private String name;
		private Long threshold;
		private Long lastCollectionCount = 0L;

		public void setLastCollectionCount(Long lastCollectionCount) {
			this.lastCollectionCount = lastCollectionCount;
		}

		public Long getLastCollectionCount() {
			return lastCollectionCount;
		}
		public String getName() {
			return name;
		}

		public Long getThreshold() {
			return threshold;
		}

		public GarbageCollectorInfo(String name, Long threshold) {
			super();
			this.name = name;
			this.threshold = threshold;
		}


		// Added hashCode() and equals() since we do Set related operations
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GarbageCollectorInfo other = (GarbageCollectorInfo) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}

	public static class GarbageCollectionChecker implements Runnable {

		private Map<String, GarbageCollectorInfo> garbageCollectorMap = new HashMap<String, GarbageCollectorInfo>();
		private long pollInterval;
		private Class <? extends ComponentId> componentIdClass;

		private Set<GarbageCollectorInfo> alreadyFaulted = new HashSet<GarbageCollectorInfo>();

		public GarbageCollectionChecker(GarbageCollectorInfo garbageCollectorInfo) {
			if (garbageCollectorInfo == null) throw new IllegalArgumentException("Garbage collector can not be null");
			this.garbageCollectorMap.put(garbageCollectorInfo.getName(), garbageCollectorInfo);
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentIdClass = DEFAULT_COMPONENT_ID_CLASS;
		}

		public GarbageCollectionChecker(GarbageCollectorInfo garbageCollectorInfo, Class <? extends ComponentId> componentIdClass, long pollTime) {
			if (garbageCollectorInfo == null) throw new IllegalArgumentException("Garbage collector can not be null");
			this.garbageCollectorMap.put(garbageCollectorInfo.getName(), garbageCollectorInfo);
			this.componentIdClass = componentIdClass;
			this.pollInterval = pollTime;
		}

		public GarbageCollectionChecker(Map<String, GarbageCollectorInfo> garbageCollectorInfos, Class <? extends ComponentId> componentIdClass, long pollTime) {
			this.garbageCollectorMap.putAll(garbageCollectorInfos);
			this.componentIdClass = componentIdClass;
			this.pollInterval = pollTime;
		}

		@Override
		public void run() {
			List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
			if (null != gcMXBeans) {
				for (GarbageCollectorMXBean gcMXBean: gcMXBeans) {
					if (!garbageCollectorMap.containsKey(gcMXBean.getName())) return;
					GarbageCollectorInfo info = garbageCollectorMap.get(gcMXBean.getName()); 
					LOG.debug("Polling gc " + info.getName() + ",polllInterval=" + pollInterval + " threshold = " + info.getThreshold());
					// Enclose everything between try catch because nothing should throw an exception to the executor upstream or it may halt subsequent tasks
					try {
						long currentGCCount = gcMXBean.getCollectionCount();
						if (currentGCCount == -1) continue; // unable to check this one...
						if (currentGCCount - info.getLastCollectionCount() > info.getThreshold()) {
							if (!this.alreadyFaulted.contains(info)) {
								FaultSubsystem.forComponent(this.componentIdClass).havingId(GARBAGE_COLLECTION_COUNT_FAULT_ID)
										.withVar("component", ComponentIds.lookup(this.componentIdClass).getFaultLogPrefix()).withVar("name", info.getName()).log();
								this.alreadyFaulted.add(info);
							} else {
								// fault has already been logged. do nothing
							}
						} else {
							// Remove this location from the already faulted set. If the location is not in the set, this call will simply return false. no harm
							// done. another if condition is just one unnecessary step
							this.alreadyFaulted.remove(info);
						}
						info.setLastCollectionCount(currentGCCount);
					} catch (Exception ex) {
						// what to do when an exception is caught? should we remove the location off the list?
						LOG.error("GC check failed for " + info.getName(), ex);
					}
				}
			} else {
				// nothing to check
			}
		}
	}
}
