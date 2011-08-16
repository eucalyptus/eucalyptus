package com.eucalyptus.reporting;

/**
 * <p>ReportLine represents a line on a visual report. It contains all the
 * printable values for each line on a visual report. Each class which
 * implements ReportLine has numeric properties which have already undergone
 * unit conversion and are suitable for display.
 * 
 * @author tom.werges
 */
public interface ReportLine
{
	public String getLabel();
	public String getGroupBy();
}
