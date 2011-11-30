package com.eucalyptus.reporting.units;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;

@ConfigurableClass(root = "reporting", description = "Parameters controlling reporting units")
public class Units
{
	@ConfigurableField(initial = "DAYS", description = "Default time unit")
	public static String DEFAULT_TIME_UNIT = "DAYS";
	@ConfigurableField(initial = "GB", description = "Default size unit")
	public static String DEFAULT_SIZE_UNIT = "GB";
	@ConfigurableField(initial = "DAYS", description = "Default size-time time unit (GB-days, etc)")
	public static String DEFAULT_SIZE_TIME_TIME_UNIT = "DAYS";
	@ConfigurableField(initial = "GB", description = "Default size-time size unit (GB-days, etc)")
	public static String DEFAULT_SIZE_TIME_SIZE_UNIT = "GB";

	public static Units getDefaultDisplayUnits()
	{
		return new Units(TimeUnit.valueOf(DEFAULT_TIME_UNIT),
						SizeUnit.valueOf(DEFAULT_SIZE_UNIT),
						TimeUnit.valueOf(DEFAULT_SIZE_TIME_TIME_UNIT),
						SizeUnit.valueOf(DEFAULT_SIZE_TIME_SIZE_UNIT));
	}
	
	private final TimeUnit timeUnit;
	private final SizeUnit sizeUnit;
	private final TimeUnit sizeTimeTimeUnit;
	private final SizeUnit sizeTimeSizeUnit;

	/**
	 * Default no-arg ctor is required for euca to set dynamic properties
	 * above. Please don't use this; it may go away.
	 */
	public Units()
	{
		this(TimeUnit.valueOf(DEFAULT_TIME_UNIT),
			SizeUnit.valueOf(DEFAULT_SIZE_UNIT),
			TimeUnit.valueOf(DEFAULT_SIZE_TIME_TIME_UNIT),
			SizeUnit.valueOf(DEFAULT_SIZE_TIME_SIZE_UNIT));
	}
	
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((sizeTimeSizeUnit == null) ? 0 : sizeTimeSizeUnit.hashCode());
		result = prime
				* result
				+ ((sizeTimeTimeUnit == null) ? 0 : sizeTimeTimeUnit.hashCode());
		result = prime * result
				+ ((sizeUnit == null) ? 0 : sizeUnit.hashCode());
		result = prime * result
				+ ((timeUnit == null) ? 0 : timeUnit.hashCode());
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
		Units other = (Units) obj;
		if (sizeTimeSizeUnit != other.sizeTimeSizeUnit)
			return false;
		if (sizeTimeTimeUnit != other.sizeTimeTimeUnit)
			return false;
		if (sizeUnit != other.sizeUnit)
			return false;
		if (timeUnit != other.timeUnit)
			return false;
		return true;
	}


}
