package com.eucalyptus.reporting.storage;

/**
 * <p>StorageUsageSummary is a summary of storage usage for some entity over
 * time. It contains statistics of aggregate usage like GB-seconds and maximum
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
	private long volumesGbMax;
	private long volumesGbSecs;
	private long snapshotsGbMax;
	private long snapshotsGbSecs;
	private long objectsGbMax;
	private long objectsGbSecs;

	public StorageUsageSummary()
	{
		this.volumesGbMax    = 0l;
		this.volumesGbSecs   = 0l;
		this.snapshotsGbMax  = 0l;
		this.snapshotsGbSecs = 0l;
		this.objectsGbMax    = 0l;
		this.objectsGbSecs   = 0l;
	}

	public long getVolumesGbMax()
	{
		return volumesGbMax;
	}

	public long getVolumesGbSecs()
	{
		return volumesGbSecs;
	}

	public long getSnapshotsGbMax()
	{
		return snapshotsGbMax;
	}

	public long getSnapshotsGbSecs()
	{
		return snapshotsGbSecs;
	}

	public long getObjectsGbMax()
	{
		return objectsGbMax;
	}

	public long getObjectsGbSecs()
	{
		return objectsGbSecs;
	}

	public void updateValues(long volumesGb, long snapshotsGb, long objectsGb,
			long durationSecs)
	{
		this.volumesGbMax   = Math.max(this.volumesGbMax, volumesGb);
		this.snapshotsGbMax = Math.max(this.snapshotsGbMax, snapshotsGb);
		this.objectsGbMax   = Math.max(this.objectsGbMax, objectsGb);
		
		this.volumesGbSecs   += volumesGb * durationSecs;
		this.snapshotsGbSecs += snapshotsGb * durationSecs;
		this.objectsGbSecs   += objectsGb * durationSecs;
	}
	
	@Override
	public String toString()
	{
		return String.format("[volsGbSecs:%d,volsGbMax:%d,snapsGbSecs:%d,"
				+ "snapsGbMax:%d,objsGbSecs:%d,objsGbMax:%d]",
				volumesGbSecs, volumesGbMax, snapshotsGbSecs,
				snapshotsGbMax, objectsGbSecs, objectsGbMax);
	}

}
