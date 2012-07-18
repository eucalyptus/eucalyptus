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

package com.eucalyptus.www;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.server.EucalyptusServiceImpl;

/**
 * <p>Logs into eucalyptus and returns an administrator session id. To be used
 * with the CommandServlet which expects such an ID. This expects one param:
 * <code>adminPw</code>.
 * 
 * <p>NOTE: This servlet is <b>NOT</b> used for the regular eucalyptus login!
 * It's only used for the <code>CommandServlet</code>.
 */
public class LoginServlet
	extends HttpServlet
{
	private static Logger LOG = Logger.getLogger( LoginServlet.class );

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		final String adminPw = req.getParameter("adminPw");
		
		if (adminPw == null) {
			throw new ServletException("Required param adminPw was not provided");
		}
		
		/* Verify admin pw */
		String sessionId;
		try {
			sessionId = (new EucalyptusServiceImpl()).login(Account.SYSTEM_ACCOUNT, User.ACCOUNT_ADMIN, adminPw).getId();
		} catch (EucalyptusServiceException e) {
			LOG.error(e);
			throw new ServletException("Incorrect admin password");
		}

		Writer writer = null;
		try {
			writer = new OutputStreamWriter(res.getOutputStream());
			writer.write(sessionId);
		} finally {
			if (writer!= null) writer.close();
		}
	}
	
}
