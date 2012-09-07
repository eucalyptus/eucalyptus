package com.eucalyptus.troubleshooting.resourcefaults;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;

public class DiskResourceCheck extends Thread {
	private final static Logger LOG = Logger.getLogger(DiskResourceCheck.class);
	private final static long POLL_TIME = 60 * 1000; // poll every minute (TODO: configure)
	private static final int OUT_OF_DISK_SPACE_FAULT_ID = 1003;
	private boolean started = false;
	private Set<LocationInfo> alreadyFaulted = new HashSet<LocationInfo>();
	public class LocationInfo {
		private File file;
		private long minimumFreeSpace;
		public File getFile() {
			return file;
		}
		public long getMinimumFreeSpace() {
			return minimumFreeSpace;
		}
		public LocationInfo(File file, long minimumFreeSpace) {
			super();
			this.file = file;
			this.minimumFreeSpace = minimumFreeSpace;
		}
	}

	private List<LocationInfo> locations = new ArrayList<LocationInfo>();
	// TODO: consolidate locations
	public void addLocationInfo(File location, long minimumFreeSpace) {
		if (started) {
			throw new IllegalStateException("Can not add location info after thread has started");
		}
		locations.add(new LocationInfo(location, minimumFreeSpace));
	}
	@Override
	public void run() {
		this.started = true;
		while (true) {
			for (LocationInfo location: locations) {
				long usableSpace = location.getFile().getUsableSpace();
				LOG.debug("Checking disk space for " + location.getFile() + " usableSpace = " + usableSpace);
				if (!alreadyFaulted.contains(location) && location.getFile().getUsableSpace() < location.getMinimumFreeSpace()) {
					// TODO: what component? Eucalyptus for now
					FaultSubsystem.forComponent(Eucalyptus.INSTANCE).havingId(OUT_OF_DISK_SPACE_FAULT_ID).withVar("component", "eucalyptus").withVar("file",  location.getFile().getAbsolutePath()).log();
					alreadyFaulted.add(location);
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
