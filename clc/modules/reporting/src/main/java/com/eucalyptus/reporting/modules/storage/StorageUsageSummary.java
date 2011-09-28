package com.eucalyptus.reporting.modules.storage;

/**
 * <p>StorageUsageSummary is a summary of storage usage for some entity over
 * time. It contains statistics of aggregate usage like megs-seconds and maximum
 * usage.
 * 
 * <p>StorageUsageSummary is not a bean. StorageUsageSummary <i>accumulates</i>
 * data through the <pre>update</pre> method and then returns resulting
 * statistics using the various <pre>get</pre> methods.
 * 
 * @author tom.werges
 */
public class StorageUsageSummary
{
	private long volumesMegsMax;
	private long volumesMegsSecs;
	private long snapshotsMegsMax;
	private long snapshotsMegsSecs;

	public StorageUsageSummary()
	{
		this.volumesMegsMax    = 0l;
		this.volumesMegsSecs   = 0l;
		this.snapshotsMegsMax  = 0l;
		this.snapshotsMegsSecs = 0l;
	}

	public long getVolumesMegsMax()
	{
		return volumesMegsMax;
	}

	public long getVolumesMegsSecs()
	{
		return volumesMegsSecs;
	}

	public long getSnapshotsMegsMax()
	{
		return snapshotsMegsMax;
	}

	public long getSnapshotsMegsSecs()
	{
		return snapshotsMegsSecs;
	}
	
	

	void setVolumesMegsMax(long volumesMegsMax)
	{
		this.volumesMegsMax = volumesMegsMax;
	}

	void setVolumesMegsSecs(long volumesMegsSecs)
	{
		this.volumesMegsSecs = volumesMegsSecs;
	}

	void setSnapshotsMegsMax(long snapshotsMegsMax)
	{
		this.snapshotsMegsMax = snapshotsMegsMax;
	}

	void setSnapshotsMegsSecs(long snapshotsMegsSecs)
	{
		this.snapshotsMegsSecs = snapshotsMegsSecs;
	}
	
	public void addVolumesMegsSecs(long volumesMegsSecs)
	{
		this.volumesMegsSecs += volumesMegsSecs;
	}

	public void addSnapshotsMegsSecs(long snapshotsMegsSecs)
	{
		this.snapshotsMegsSecs += snapshotsMegsSecs;
	}

	void addUsage(StorageUsageSummary summary)
	{
		this.volumesMegsMax   = Math.max(this.volumesMegsMax, summary.getVolumesMegsMax());
		this.snapshotsMegsMax   = Math.max(this.snapshotsMegsMax, summary.getSnapshotsMegsMax());
		
		this.volumesMegsSecs  += summary.getVolumesMegsSecs();
		this.snapshotsMegsSecs  += summary.getSnapshotsMegsSecs();
	}


	public void updateValues(long volumesMegs, long snapshotsMegs,
			long durationSecs)
	{
		this.volumesMegsMax   = Math.max(this.volumesMegsMax, volumesMegs);
		this.snapshotsMegsMax = Math.max(this.snapshotsMegsMax, snapshotsMegs);
		
		this.volumesMegsSecs   += volumesMegs   * durationSecs;
		this.snapshotsMegsSecs += snapshotsMegs * durationSecs;
	}
	
	@Override
	public String toString()
	{
		return String.format("[volsMegsSecs:%d,volsMegsMax:%d,snapsMegsSecs:%d,"
				+ "snapsMegsMax:%d]",
				volumesMegsSecs, volumesMegsMax, snapshotsMegsSecs,
				snapshotsMegsMax);
	}

}
