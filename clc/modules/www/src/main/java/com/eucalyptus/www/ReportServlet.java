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

package com.eucalyptus.www;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.reporting.*;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.server.*;
import com.google.common.base.Objects;

@SuppressWarnings("serial")
public class ReportServlet
	extends HttpServlet
{
	private static final Logger LOG = Logger.getLogger( ReportServlet.class );


	/**
	 * <p>Expects the following servlet params:
	 * type,format,session,start,end
	 * 
	 * <p>Type and format are taken from the
	 * enums: ReportType, ReportFormat, and ReportingCriterion.
	 * GroupByCriterion can also have the value "None". Start and
	 * end are in milliseconds. Session is a session id string.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		User user = this.getUserFromSession(Param.session.getRaw(req));
		if ( user==null ) {
			throw new RuntimeException("User was null");
		}

		if (user.isSystemAdmin()) {
			throw new RuntimeException("Only admins can generate reports");
		}

		/* Parse all params
			*/
		final String reportType = Param.type.get(req);
		final ReportFormat format = ReportFormat.valueOf( Objects.firstNonNull(Param.format.get(req),"html"));
		final long start = Long.parseLong(Param.start.get(req));
		final long end = Long.parseLong(Param.end.get(req));
		final Period period = new Period(start, end);

		LOG.info(String.format("Params: type:%s format:%s period:%s", reportType, format, period));
		
		
		/* Set servlet response content type, etc, based upon report format.
		 */
		setContentTypeHeader(res, format, Param.type.get(req));
		
		/* Generate the report and send it thru the OutputStream
		 */
		try {
			String report = ReportGenerationFacade.generateReport( reportType, format.name(), period.getBeginningMs(), period.getEndingMs() );
			res.getWriter().print( report );
		} catch (ReportGenerationFacade.ReportGenerationException e) {
			LOG.error( e, e );
		}
	}

	
	private enum Param {
		type, format, session, start, end,
		criterion, groupByCriterion;

		public String getRaw(HttpServletRequest req)
			throws IllegalArgumentException
		{
			if (req.getParameter(this.name()) == null) {
				throw new IllegalArgumentException("'" + this.name()
						+ "' is a required argument.");
			} else {
				return req.getParameter(this.name());
			}
		}
		
		public String get(HttpServletRequest req)
			throws IllegalArgumentException
		{
			return getRaw(req).toUpperCase().replace(' ', '_');
		}
	}

	private void setContentTypeHeader(HttpServletResponse res,
			ReportFormat format, String filename)
	{
		switch (format) {
			case CSV:
				res.setContentType("text/plain");
				res.setHeader("Content-Disposition", "file; filename="
						+ filename + ".csv");
				break;
			case HTML:
				res.setContentType("text/html");
				break;
		}		
	}
	
	private User getUserFromSession( String sessionId )
	{
	    WebSession ws = WebSessionManager.getInstance( ).getSessionById( sessionId );
	    if ( ws == null ) {
	    	throw new RuntimeException("Session verification failed:" + sessionId);
	    }
	    User user;
		try {
			user = EuareWebBackend.getUser( ws.getUserId( ) );
		} catch ( EucalyptusServiceException ex ) {
			throw new RuntimeException("Session verification failed", ex);
		}
	    return user;
	}
	
}
