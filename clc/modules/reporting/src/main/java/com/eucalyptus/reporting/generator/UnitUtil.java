package com.eucalyptus.reporting.generator;

/**
 * @author tom.werges
 */
public class UnitUtil
{
	/**
	 * <p>Translates a size in MB, to a String with units; for example, 2048
	 * would become "2 GB"
	 */
	public static String getSizeString(long megs)
	{
		String[] units = new String[] {"MB","GB","TB","PB","EB"}; //megabytes to exabytes
		int i;
		for (i=0; i<units.length && megs>1024; i++) {
			megs>>=10;
		}
		return String.format("%d%s", megs, units[i]);                           
	}

	/**
	 * <p>Translates an amount, to a String with units; for example, 4,000,000
	 * would become "4M"
	 */
	public static String getAmountString(long amt)
	{
		String[] units = new String[] {"","k","M","B","Tril","Quadril"};
		int i;
		for (i=0; i<units.length && amt>1000; i++) {
			amt>>=1000;
		}
		return String.format("%d%s", amt, units[i]);                           
		
	}

	/**
	 * <p>Translates a time in secs, to a String with units; for example, 120
	 * would become "2hrs" 
	 */
	public static String getTimeString(long timeSecs)
	{
		String[] units = new String[] {"secs","mins","hrs","days"};
		int[] divs = new int[] {60,60,24};
		int i;
		for (i=0; i<divs.length && timeSecs>divs[i]; i++) {
			timeSecs/=divs[i];
		}
		return String.format("%d%s", timeSecs, units[i]);
	}
}
