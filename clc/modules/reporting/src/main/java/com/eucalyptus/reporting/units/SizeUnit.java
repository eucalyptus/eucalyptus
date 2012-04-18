package com.eucalyptus.reporting.units;

public enum SizeUnit
{
	BYTES(1),
	KB(1<<10),
	MB(1<<20),
	GB(1<<30),
	TB(1<<40),
	PB(1<<50);
	
	private final long factor;
	private SizeUnit(long factor)
	{
		this.factor = factor;
	}

	public long getFactor()
	{
		return this.factor;
	}
}