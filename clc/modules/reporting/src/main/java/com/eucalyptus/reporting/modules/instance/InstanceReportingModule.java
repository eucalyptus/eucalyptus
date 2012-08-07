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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
import com.eucalyptus.reporting.domain.ReportingAccountDao;
import com.eucalyptus.reporting.domain.ReportingUserDao;

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
				return ReportingAccountDao.getInstance().getReportingAccount(key.getAccountId()).getName();
			case USER:
				return ReportingUserDao.getInstance().getReportingUser(key.getOwnerId()).getName();
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
