package com.eucalyptus.reporting;

import java.util.List;

import com.eucalyptus.reporting.units.Units;


public interface ReportingModule<T extends ReportLine>
{
	public List<T> getReportLines(Period period,
			ReportingCriterion criterion, Units displayUnits, String accountId);
	
	public List<T> getReportLines(Period period, ReportingCriterion groupByCrit,
			ReportingCriterion crit, Units displayUnits, String accountId);
	
	public String getJrxmlFilename();
	
	public String getNestedJrxmlFilename();

}
