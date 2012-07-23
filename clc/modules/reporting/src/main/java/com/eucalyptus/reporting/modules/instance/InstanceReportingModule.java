/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.reporting.modules.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.ReportingModule;
import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class InstanceReportingModule
	implements ReportingModule<InstanceReportLine>
{
	private static Logger log = Logger.getLogger( InstanceReportingModule.class );

	public InstanceReportingModule()
	{
		
	}
	
	public List<InstanceReportLine> getReportLines(Period period,
			ReportingCriterion criterion, Units displayUnits,
			String accountId)
	{
		return getReportLines(period, null, criterion, displayUnits, accountId);
	}

	public List<InstanceReportLine> getReportLines(Period period, ReportingCriterion groupByCrit,
			ReportingCriterion crit, Units displayUnits, String accountId)
	{
		if (period==null || crit==null || displayUnits==null) {
			throw new IllegalArgumentException("Args can't be null");
		}
		
		Map<InstanceReportLineKey, InstanceReportLine> reportLineMap =
			new HashMap<InstanceReportLineKey, InstanceReportLine>();
		
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<InstanceSummaryKey, InstanceUsageSummary> usageMap = 
			usageLog.getUsageSummaryMap(period, accountId);
		for (InstanceSummaryKey key: usageMap.keySet()) {
			String critVal = getAttributeValue(crit, key);
			String groupVal = (groupByCrit==null) ? null : getAttributeValue(groupByCrit, key);
			InstanceReportLineKey lineKey = new InstanceReportLineKey(critVal, groupVal);
			if (!reportLineMap.containsKey(lineKey)) {
				reportLineMap.put(lineKey, new InstanceReportLine(lineKey,
						new InstanceUsageSummary(), displayUnits));
			}
			InstanceReportLine reportLine = reportLineMap.get(lineKey);
			InstanceUsageSummary summary = usageMap.get(key);
			reportLine.addUsage(summary);
		}

		final List<InstanceReportLine> results = new ArrayList<InstanceReportLine>();
		for (InstanceReportLineKey lineKey: reportLineMap.keySet()) {
			results.add(reportLineMap.get(lineKey));
		}
		
		Collections.sort(results);
		return results;
	}

	private static String getAttributeValue(ReportingCriterion criterion,
			InstanceSummaryKey key)
	{
		log.debug("owner id:" + key.getOwnerId() + " account id:" + key.getAccountId());

		switch (criterion) {
			case ACCOUNT:
				return ReportingAccountDao.getInstance().getAccountName(key.getAccountId());
			case USER:
				return ReportingUserDao.getInstance().getUserName(key.getOwnerId());
			case CLUSTER:
				return key.getClusterName();
			case AVAILABILITY_ZONE:
				return key.getAvailabilityZone();
			default:
				return key.getOwnerId();
		}
	}

	@Override
	public String getJrxmlFilename()
	{
		return "instance.jrxml";
	}

	@Override
	public String getNestedJrxmlFilename()
	{
		return "nested_instance.jrxml";
	}

}
