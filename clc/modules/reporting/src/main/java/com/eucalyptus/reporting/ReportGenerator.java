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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.units.Units;

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
	
	private static ReportGenerator instance;

	private ReportGenerator()
	{
	}
	
	public static ReportGenerator getInstance()
	{
		if (instance == null) {
			instance = new ReportGenerator();
		}
		return instance;
	}


	/**
	 * STEVE: This is the method to use.
	 * 
	 * @param displayUnits Can be null if you just want the default units.
	 * @param type The type of report, which is INSTANCE for the time being regardless of what you specify
	 * @param format The report format, which must be HTML at the moment.
	 * @param period The period for which you wish to generate a report
	 * @param out Where to send the generated report
	 * @param accountId The account to generate the report as, which is ignored at present.
	 * 
	 * @throws IOException If it cannot write to the stream you passed.
	 */
	public void generateReport(Period period, ReportFormat format, ReportType type,
			Units displayUnits, OutputStream out, String accountId)
		throws IOException
	{
		if (period==null) {
			throw new IllegalArgumentException("Period can't be null");
		}
		if (type==null) {
			throw new IllegalArgumentException("type can't be null");
		}
		if (out==null) {
			throw new IllegalArgumentException("out can't be null");
		}
		if (format==null) format=ReportFormat.HTML;
		
		ReportArtEntity report = new ReportArtEntity(period.getBeginningMs(), period.getEndingMs());
		if (displayUnits==null) displayUnits=Units.getDefaultDisplayUnits();
		type.getGenerator().generateReportArt(report);
		type.getRendererFactory().getRenderer(format).render(report, out, displayUnits);
		
		return;
	}
	
	/**
	 * Deprecated; do not use.
	 * 
	 * @deprecated
	 */
	public void generateReport(String reportType, ReportFormat format,
			Period period, ReportingCriterion criterion,
			ReportingCriterion groupByCriterion, Units displayUnits,
			OutputStream out, String accountId)
	{
		return;
	}
	
	
}
