package com.eucalyptus.reporting.storage;

/**
 * <p>StorageUsageSummary is a summary of storage usage for some entity over
 * time. It contains statistics of aggregate usage like Megs-seconds and maximum
 * usage.
 * 
 * <p>StorageUsageSummary is not a bean. StorageUsageSummary <i>accumulates</i>
 * data through the <pre>update</pre> method and then returns results of
 * statistics using the various <pre>get</pre> methods.
 * 
 * @author tom.werges
 */
public class StorageUsageSummary
{
	private long volumesMegsMax;
	private long volumesMegsHrs;
	private long snapshotsMegsMax;
	private long snapshotsMegsHrs;
	private long objectsMegsMax;
	private long objectsMegsHrs;

	public StorageUsageSummary()
	{
		this.volumesMegsMax    = 0l;
		this.volumesMegsHrs    = 0l;
		this.snapshotsMegsMax  = 0l;
		this.snapshotsMegsHrs  = 0l;
		this.objectsMegsMax    = 0l;
		this.objectsMegsHrs    = 0l;
	}

	public long getVolumesMegsMax()
	{
		return volumesMegsMax;
	}

	public long getVolumesMegsHrs()
	{
		return volumesMegsHrs;
	}

	public long getSnapshotsMegsMax()
	{
		return snapshotsMegsMax;
	}

	public long getSnapshotsMegsHrs()
	{
		return snapshotsMegsHrs;
	}

	public long getObjectsMegsMax()
	{
		return objectsMegsMax;
	}

	public long getObjectsMegsHrs()
	{
		return objectsMegsHrs;
	}

	public void updateValues(long volumesMegs, long snapshotsMegs, long objectsMegs,
			long durationSecs)
	{
		this.volumesMegsMax   = Math.max(this.volumesMegsMax, volumesMegs);
		this.snapshotsMegsMax = Math.max(this.snapshotsMegsMax, snapshotsMegs);
		this.objectsMegsMax   = Math.max(this.objectsMegsMax, objectsMegs);
		
		final long durationHrs = durationSecs / 60 / 60;
		this.volumesMegsHrs   += volumesMegs   * durationHrs;
		this.snapshotsMegsHrs += snapshotsMegs * durationHrs;
		this.objectsMegsHrs   += objectsMegs   * durationHrs;
	}
	
	@Override
	public String toString()
	{
		return String.format("[volsMegsHrs:%d,volsMegsMax:%d,snapsMegsHrs:%d,"
				+ "snapsMegsMax:%d,objsMegsHrs:%d,objsMegsMax:%d]",
				volumesMegsHrs, volumesMegsMax, snapshotsMegsHrs,
				snapshotsMegsMax, objectsMegsHrs, objectsMegsMax);
	}

}
