package com.eucalyptus.reporting.units;

public class Units
{
	public static final Units DEFAULT_DISPLAY_UNITS =
		new Units(TimeUnit.DAYS, SizeUnit.GB, TimeUnit.DAYS, SizeUnit.GB);
	
	private final TimeUnit timeUnit;
	private final SizeUnit sizeUnit;
	private final TimeUnit sizeTimeTimeUnit;
	private final SizeUnit sizeTimeSizeUnit;

	public Units(TimeUnit timeUnit, SizeUnit sizeUnit,
			TimeUnit sizeTimeTimeUnit, SizeUnit sizeTimeSizeUnit)
	{
		this.timeUnit = timeUnit;
		this.sizeUnit = sizeUnit;
		this.sizeTimeTimeUnit = sizeTimeTimeUnit;
		this.sizeTimeSizeUnit = sizeTimeSizeUnit;
	}

	public TimeUnit getTimeUnit()
	{
		return timeUnit;
	}

	public SizeUnit getSizeUnit()
	{
		return sizeUnit;
	}

	public TimeUnit getSizeTimeTimeUnit()
	{
		return sizeTimeTimeUnit;
	}

	public SizeUnit getSizeTimeSizeUnit()
	{
		return sizeTimeSizeUnit;
	}
	
	public String toString()
	{
		return String.format("[timeUnit:%s,sizeUnit:%s,sizeTimeTimeUnit:%s,"
				+ "sizeTimeSizeUnit:%s]", timeUnit, sizeUnit,sizeTimeTimeUnit,
				sizeTimeSizeUnit);
	}
	
}
