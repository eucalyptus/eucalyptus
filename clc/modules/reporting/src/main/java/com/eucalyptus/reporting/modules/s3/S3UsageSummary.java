package com.eucalyptus.reporting.modules.s3;

/**
 * <p>S3UsageSummary is a summary of storage usage for some entity over
 * time. It contains statistics of aggregate usage like megs-seconds and maximum
 * usage.
 * 
 * @author tom.werges
 */
public class S3UsageSummary
{
	private long bucketsNumMax;
	private long objectsMegsMax;
	private long objectsMegsSecs;

	public S3UsageSummary()
	{
		this.bucketsNumMax     = 0l;
		this.objectsMegsMax    = 0l;
		this.objectsMegsSecs   = 0l;
	}

	public long getBucketsNumMax()
	{
		return bucketsNumMax;
	}

	public long getObjectsMegsMax()
	{
		return objectsMegsMax;
	}

	public long getObjectsMegsSecs()
	{
		return objectsMegsSecs;
	}
	
	public void setBucketsNumMax(long bucketsNumMax)
	{
		this.bucketsNumMax = bucketsNumMax;
	}

	public void setObjectsMegsMax(long objectsMegsMax)
	{
		this.objectsMegsMax = objectsMegsMax;
	}
	
	public void addObjectsMegsSecs(long objectsMegsSecs)
	{
		this.objectsMegsSecs += objectsMegsSecs;
	}

	public void setObjectsMegsSecs(long objectsMegsSecs)
	{
		this.objectsMegsSecs = objectsMegsSecs;
	}

	public void updateValues(long objectsMegs, long bucketsNum, long durationSecs)
	{
		this.objectsMegsMax   = Math.max(this.objectsMegsMax, objectsMegs);
		this.bucketsNumMax    = Math.max(this.bucketsNumMax, bucketsNum);
		
		this.objectsMegsSecs   += objectsMegs   * durationSecs;
	}
	
	void addUsage(S3UsageSummary summary)
	{
		this.objectsMegsMax   = Math.max(this.objectsMegsMax, summary.getObjectsMegsMax());
		this.bucketsNumMax    = Math.max(this.bucketsNumMax, summary.getBucketsNumMax());
		
		this.objectsMegsSecs  += summary.getObjectsMegsSecs();		
	}
	
	@Override
	public String toString()
	{
		return String.format("[bucketsNumMax:%d,objsMegsSecs:%d,objsMegsMax:%d]",
				bucketsNumMax, objectsMegsSecs, objectsMegsMax);
	}

}
