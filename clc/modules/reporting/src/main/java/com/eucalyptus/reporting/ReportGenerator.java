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

package com.eucalyptus.reporting;

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.system.SubDirectory;

/**
 * <p>ReportGenerator is the main class by which the reporting system is
 * accessed by outside modules. It acts as a facade for the various reporting
 * sub-packages (instance, storage, and s3 sub-packages).
 * 
 * <p>ReportGenerator contains all the jasper-related stuff.
 */
public class ReportGenerator
{
	private static Logger log = Logger.getLogger( ReportGenerator.class );

	private static final int DEFAULT_CACHE_SIZE = 5;
	private final Map<ReportKey, List<ReportLine>> lineListMap;
	private final Map<String, ReportingModule> modules;
	
	private static ReportGenerator instance;

	private ReportGenerator()
	{
		this.lineListMap = new HashMap<ReportKey, List<ReportLine>>();
		this.modules = new HashMap<String, ReportingModule>();
	}
	
	public static ReportGenerator getInstance()
	{
		if (instance == null) {
			instance = new ReportGenerator();
		}
		return instance;
	}
	
	/**
	 * <p>Generates a report and sends it to an OutputStream.
	 * 
	 * @param reportType The type of report. Can be "Instance" or "S3" or
	 *    any other valid module in the modules sub-package.
	 *    
	 * @param groupByCriterion Can be null if none selected
	 * 
	 * @throws IllegalArgumentException If reportType does not refer to a
	 *    valid reporting module.
	 */
	public void generateReport(String reportType, ReportFormat format,
			Period period, ReportingCriterion criterion,
			ReportingCriterion groupByCriterion, Units displayUnits,
			OutputStream out, String accountId)
	{
		if (reportType == null)
			throw new IllegalArgumentException("ReportType can't be null");
		if (criterion == null)
			throw new IllegalArgumentException("Criterion can't be null");
		if (displayUnits == null)
			displayUnits = Units.getDefaultDisplayUnits();

		if (! modules.containsKey(reportType)) {
			try {
				/* Dynamically load the ReportingModule class corresponding to
				 * this reportType.
				 */
				final String moduleClassName = 
					String.format("com.eucalyptus.reporting.modules.%s.%sReportingModule",
							reportType.toLowerCase(),
							capitalizeFirstChar(reportType));
				ReportingModule mod =
					(ReportingModule) Class.forName(moduleClassName).getConstructors()[0].newInstance();
				modules.put(reportType, mod);
				log.info("Loaded reporting module:" + reportType);
			} catch (Exception ex) {
				throw new IllegalArgumentException("No such reporting module:" + reportType, ex);
			}
		}
		

		
		final ReportingModule module = modules.get(reportType);

		final String jrxmlFilename = (groupByCriterion==null)
			? module.getJrxmlFilename()
			: module.getNestedJrxmlFilename();
		final File jrxmlFile = 	new File(SubDirectory.REPORTS.toString()
					+ File.separator + jrxmlFilename);


		final Map<String, String> params = new HashMap<String, String>();
		params.put("criterion", criterion.toString());
		params.put("timeUnit", displayUnits.getTimeUnit().toString());
		params.put("sizeUnit", displayUnits.getSizeUnit().toString());
		params.put("sizeTimeTimeUnit",
				displayUnits.getSizeTimeTimeUnit().toString());
		params.put("sizeTimeSizeUnit",
				displayUnits.getSizeTimeSizeUnit().toString());
		if (groupByCriterion != null) {
			params.put("groupByCriterion", groupByCriterion.toString());			
		}


		/* We maintain a small cache here of very recently-viewed reports. If
		 * not found in cache then get report lines from {s3,storage,instance}
		 * API
		 */
		List<ReportLine> reportLines = null;
		ReportKey key = new ReportKey(reportType, period, criterion,
				groupByCriterion, displayUnits, System.currentTimeMillis());
		if (lineListMap.containsKey(key)) {  //TODO: this isnt working all of the sudden even though hashcode and equals do not use timestampMs
			log.info("Gathered report data from cache:" + key);
			reportLines = lineListMap.get(key);
		} else {
			while (lineListMap.size() >= DEFAULT_CACHE_SIZE) {
				ReportKey oldestKey = null;
				for (ReportKey oldKey: lineListMap.keySet()) {
					if (oldestKey==null || oldestKey.getTimestampMs() > oldKey.getTimestampMs()) {
						oldestKey = oldKey;
					}
				}
				if (oldestKey != null) {
					lineListMap.remove(oldestKey);
					log.info("Removed report data from cache:" + oldestKey);
				}
			}
			reportLines = module.getReportLines(period, groupByCriterion,
					criterion, displayUnits, accountId);
			log.info("Generated report data from db:" + key);
			lineListMap.put(key, reportLines);
			
		}
		JRDataSource dataSource = new JRBeanCollectionDataSource(reportLines);

		
		try {
			JasperReport report = JasperCompileManager.compileReport(jrxmlFile
					.getAbsolutePath());
			JasperPrint jasperPrint = JasperFillManager.fillReport(report,
					params, dataSource);

			JRExporter exporter = format.getExporter();
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, out);
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
			exporter.exportReport();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private class ReportKey
	{
		private final String type;
		private final Period period;
		private final ReportingCriterion groupByCriterion;
		private final ReportingCriterion criterion;
		private final Units units;
		private final long timestampMs;

		public ReportKey(String type, Period period,
				ReportingCriterion criterion,
				ReportingCriterion groupByCriterion, Units units,
				long timestampMs)
		{
			super();
			this.type = type;
			this.period = period;
			this.groupByCriterion = groupByCriterion;
			this.criterion = criterion;
			this.units = units;
			this.timestampMs = timestampMs;
		}

		public long getTimestampMs() {
			return timestampMs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((criterion == null) ? 0 : criterion.hashCode());
			result = prime
					* result
					+ ((groupByCriterion == null) ? 0 : groupByCriterion
							.hashCode());
			result = prime * result
					+ ((period == null) ? 0 : period.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((units == null) ? 0 : units.hashCode());
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
			ReportKey other = (ReportKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (criterion != other.criterion)
				return false;
			if (groupByCriterion != other.groupByCriterion)
				return false;
			if (period == null) {
				if (other.period != null)
					return false;
			} else if (!period.equals(other.period))
				return false;
			if (type != other.type)
				return false;
			if (units == null) {
				if (other.units != null)
					return false;
			} else if (!units.equals(other.units))
				return false;
			return true;
		}

		private ReportGenerator getOuterType() {
			return ReportGenerator.this;
		}

		public String toString()
		{
			return String.format("[type:%s period:%s crit:%s groupBy:%s timestamp:%d]",
					type, period, criterion, groupByCriterion, timestampMs);
		}
		
	}
	
	private final String capitalizeFirstChar(String str)
	{
		return (str.length()==0) ? str : str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}
}
