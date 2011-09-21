package com.eucalyptus.reporting.modules.s3;

import javax.persistence.*;

@Embeddable
public class S3UsageData
{
	@Column(name="buckets_num", nullable=false)
	protected Long bucketsNum;
	@Column(name="objects_num", nullable=false)
	protected Long objectsNum;
	@Column(name="objects_megs", nullable=false)
	protected Long objectsMegs;
	
	public S3UsageData()
	{
		this.bucketsNum     = new Long(0);
		this.objectsNum     = new Long(0);
		this.objectsMegs    = new Long(0);
	}

	/**
	 * Copy constructor to avoid Hibernate badness.
	 */
	public S3UsageData(S3UsageData usageData)
	{
		this(new Long(usageData.getBucketsNum()), new Long(usageData.getObjectsNum()),
				new Long(usageData.getObjectsMegs()));
	}
	
	public S3UsageData(Long bucketsNum, Long objectsNum, Long objectsMegs)
	{
		if (bucketsNum == null || objectsNum == null || objectsMegs == null)
		{
			throw new IllegalArgumentException("args can't be null");
		}
		this.bucketsNum     = bucketsNum;
		this.objectsNum     = objectsNum;
		this.objectsMegs    = objectsMegs;
	}

	public Long getBucketsNum()
	{
		return bucketsNum;
	}
	
	public Long getObjectsNum()
	{
		return objectsNum;
	}
	
	public Long getObjectsMegs()
	{
		return objectsMegs;
	}

	public void setBucketsNum(Long bucketsNum)
	{
		if (bucketsNum==null) throw new IllegalArgumentException("arg can't be null");
		this.bucketsNum = bucketsNum;
	}

	public void setObjectsNum(Long objectsNum)
	{
		if (objectsNum==null) throw new IllegalArgumentException("arg can't be null");
		this.objectsNum = objectsNum;
	}

	public void setObjectsMegs(Long objectsMegs)
	{
		if (objectsMegs==null) throw new IllegalArgumentException("arg can't be null");
		this.objectsMegs = objectsMegs;
	}

	private static Long sumLongs(Long a, Long b)
	{
		return new Long(a.longValue() + b.longValue());
	}

	public S3UsageData sumFrom(S3UsageData other)
	{
		if (other == null) return null;
		return new S3UsageData(
				sumLongs(this.bucketsNum, other.bucketsNum),
				sumLongs(this.objectsNum, other.objectsNum),
				sumLongs(this.objectsMegs, other.objectsMegs)
				);
	}

	public String toString()
	{
		return String.format("[bucketsNum:%d,,objs:%d,"
				+ "objsMegs:%d]", bucketsNum, objectsNum, objectsMegs);
	}


}
