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
 * 
 * @author tom.werges
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
