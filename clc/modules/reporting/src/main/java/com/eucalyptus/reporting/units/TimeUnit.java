package com.eucalyptus.reporting.units;

public enum TimeUnit
{
	MS(1, "ms"),
	SECS(1000, "secs"),
	MINS(60*1000, "mins"),
	HOURS(60*60*1000, "hrs"),
	DAYS(24*60*60*1000, "days"),
	YEARS(365*24*60*60*1000, "years");
	
	private final long factor;
	private final String str;
	private TimeUnit(long factor, String str)
	{
		this.factor = factor;
		this.str = str;
	}
	
	public long getFactor()
	{
		return this.factor;
	}
	
	@Override
	public String toString()
	{
		return this.str;
	}
}