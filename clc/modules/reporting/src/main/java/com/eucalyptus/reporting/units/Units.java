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
