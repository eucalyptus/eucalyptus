package com.eucalyptus.reporting.units;

/**
 * @author tom.werges
 */
public class UnitUtil
{
	public static Long convertSize(Long size, SizeUnit from, SizeUnit to)
	{
		return (size==null)
			? null
			: new Long((size.longValue() * from.getFactor()) / to.getFactor());
	}
	
	public static Long convertTime(Long time, TimeUnit from, TimeUnit to)
	{
		return (time==null)
			? null
			: new Long((time.longValue() * from.getFactor()) / to.getFactor());
	}
	
	public static Long convertSizeTime(Long sizeTime, SizeUnit sizeFrom,
			SizeUnit sizeTo, TimeUnit timeFrom, TimeUnit timeTo)
	{
		return convertTime(convertSize(sizeTime, sizeFrom, sizeTo), timeFrom, timeTo);
	}
}
