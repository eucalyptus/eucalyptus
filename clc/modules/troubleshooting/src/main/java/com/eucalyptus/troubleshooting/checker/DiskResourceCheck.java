package com.eucalyptus.troubleshooting.checker;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Eucalyptus;

/**
 * <p>
 * DiskResourceCheck can be used by any eucalyptus component (walrus, SC, NC etc...) to perform periodic checks on disk space and warn the user when the system
 * runs low on space. This class provides a static method to {@link #start(Checker) start} the disk resource check for a particular location at a specified
 * interval.
 * </p>
 * <p>
 * {@link ScheduledExecutorService} is used for scheduling the disk space checks at configurable intervals. The thread pool size is limited to 1
 * </p>
 * <p>
 * If the system is running low on disk space a fault is recorded in the log file for the specified component. Subsequent faults for the same location are not
 * logged until the state is reset for that location. A state reset occurs when the file location has enough free space
 * </p>
 */
public class DiskResourceCheck {
	private final static Logger LOG = Logger.getLogger(DiskResourceCheck.class);

	private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
	private static final int OUT_OF_DISK_SPACE_FAULT_ID = 1003;
	private final static long DEFAULT_POLL_INTERVAL = 5 * 1000;
	private static final Class <? extends ComponentId> DEFAULT_COMPONENT_ID_CLASS = Eucalyptus.class;

	/**
	 * Marking the constructor private on purpose, so that no code can instantiate an object this class
	 */
	private DiskResourceCheck() {

	}

	/**
	 * <p>
	 * Kicks off an infinite series of disk resource checks with a delay in between consecutive checks. {@link ScheduledExecutorService#scheduleWithFixedDelay
	 * Executor service framework} is used for scheduling the worker thread, {@link Checker checker}, at regular intervals. The time delay, file location, logic
	 * for disk space check and other configuration is provided by checker
	 * </p>
	 * 
	 * <p>
	 * This method returns a {@link ScheduledFuture} object that can be used by the caller to cancel the execution. Thread execution can also be cancelled by
	 * shutting down the executor service
	 * </p>
	 * 
	 * @param checker
	 * @return ScheduledFuture
	 */
	public static ScheduledFuture<?> start(Checker checker) {
		return pool.scheduleWithFixedDelay(checker, 0, checker.pollInterval, TimeUnit.MILLISECONDS);
	}

	// Someone should be calling this, currently no one is. Might be a nice thing to say hello to in the service shutdown hooks. Although might complicate stuff
	// when multiple services using it
	public static void shutdown() {
		pool.shutdownNow();
	}

	public static class LocationInfo {
		private File file;
		private Long minimumFreeSpace;
		private Double percentFreeSpace;

		public File getFile() {
			return file;
		}

		public Long getThreshold() {
			if (null != this.minimumFreeSpace) {
				return this.minimumFreeSpace;
			} else {
				return (long) (this.file.getTotalSpace() * this.percentFreeSpace / 100);
			}
		}

		/**
		 * Constructor to be used when free space is an absolute quantity in bytes
		 * 
		 * @param file
		 * @param minimumFreeSpace
		 */
		public LocationInfo(File file, Long minimumFreeSpace) {
			super();
			this.file = file;
			this.minimumFreeSpace = minimumFreeSpace;
		}

		/**
		 * Constructor to be used when free space is a percentage of the total space available
		 * 
		 * @param file
		 * @param percentFreeSpace
		 */
		public LocationInfo(File file, Double percentFreeSpace) {
			super();
			this.file = file;
			this.percentFreeSpace = percentFreeSpace;
		}

		// Added hashCode() and equals() since we do Set related operations
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((file == null) ? 0 : file.hashCode());
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
			LocationInfo other = (LocationInfo) obj;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			return true;
		}
	}

	/**
	 * Worker thread that holds the logic for disk space checks and all the relevant information required. An instance of this class is fed to
	 * {@link ScheduledExecutorService#scheduleWithFixedDelay} method
	 * 
	 */
	public static class Checker implements Runnable {

		private Set<LocationInfo> locations = new HashSet<LocationInfo>();
		private long pollInterval;
		private Class <? extends ComponentId> componentIdClass;

		private Set<LocationInfo> alreadyFaulted = new HashSet<LocationInfo>();

		public Checker(LocationInfo locationInfo) {
			this.locations.add(locationInfo);
			this.pollInterval = DEFAULT_POLL_INTERVAL;
			this.componentIdClass = DEFAULT_COMPONENT_ID_CLASS;
		}

		public Checker(LocationInfo locationInfo, Class <? extends ComponentId> componentIdClass, long pollTime) {
			this.locations.add(locationInfo);
			this.componentIdClass = componentIdClass;
			this.pollInterval = pollTime;
		}

		public Checker(List<LocationInfo> locations, Class <? extends ComponentId> componentIdClass, long pollTime) {
			this.locations.addAll(locations);
			this.componentIdClass = componentIdClass;
			this.pollInterval = pollTime;
		}

		@Override
		public void run() {
			if (null != locations) {
				for (LocationInfo location : this.locations) {
					LOG.debug("Polling disk " + location.getFile() + ", pollInterval=" + pollInterval + ", threshold = " + location.getThreshold());
					// Enclose everything between try catch because nothing should throw an exception to the executor upstream or it may halt subsequent tasks
					try {
						long usableSpace = location.getFile().getUsableSpace();
						if (usableSpace < location.getThreshold()) {
							if (!this.alreadyFaulted.contains(location)) {
								Faults.forComponent(this.componentIdClass).havingId(OUT_OF_DISK_SPACE_FAULT_ID)
										.withVar("component", ComponentIds.lookup(this.componentIdClass).getFaultLogPrefix()).withVar("file", location.getFile().getAbsolutePath()).log();
								this.alreadyFaulted.add(location);
							} else {
								// fault has already been logged. do nothing
							}
						} else {
							// Remove this location from the already faulted set. If the location is not in the set, this call will simply return false. no harm
							// done. another if condition is just one unnecessary step
							this.alreadyFaulted.remove(location);
						}
					} catch (Exception ex) {
						// what to do when an exception is caught? should we remove the location off the list?
						LOG.error("Disk resource check failed for " + location.getFile().getAbsolutePath(), ex);
					}
				}
			} else {
				// nothing to check
			}
		}
	}
}
