package com.eucalyptus.www;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import com.eucalyptus.util.ExposedCommand;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.Session;
import com.eucalyptus.webui.server.EucalyptusServiceImpl;

/**
 * <p>CommandServlet invokes a single static method within a running Eucalyptus
 * system. It accepts the following params: className, methodName, methodArgs,
 * and sessionId. The <code>args</code> param may be omitted, but if it's present,
 * it must be a comma-delimited list of String arguments to be passed to the
 * invoked method. The <code>sessionId</code> param must contain a session id
 * of the administrator, gathered through the <code>LoginServlet</code>
 * 
 * <p>Every method invoked from this servlet must be static, must take only
 * String paramaters, and must have the <code>ExposedCommand</code> annotation.
 * 
 * <p>CommandServlet has two purposes: 1) to allow testing of internal
 * Eucalyptus functionality; 2) to allow infrequently-used administration
 * commands.
 * 
 * <p>CommandServlet is normally used by the <code>wget</code> shell command
 * with the appropriate URL. For example, <code>wget --no-check-certificate
 * 'https://localhost:8443/commandservlet?adminPw=admin&className=Foo&
 * methodName=Bar&args=one,two,three' </code>
 * 
 * <p>Any invoked command is presumed to be successful if it returns normally
 * without exceptions, in which case, an HTTP 200 is returned from this servlet.
 * If an exception is caught while invoking the specified command, then an
 * HTTP 500 is returned.
 * 
 * @author tom.werges
 */
public class CommandServlet
	extends HttpServlet
{
	private static Logger LOG = Logger.getLogger( CommandServlet.class );

	@SuppressWarnings("rawtypes")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		final String className  = req.getParameter("className");
		final String methodName = req.getParameter("methodName");
		final String methodArgs = req.getParameter("methodArgs");
		final String sessionId    = req.getParameter("sessionId");

		final String[] methodArgsArray;
		if (methodArgs == null) {
			methodArgsArray = new String[0];
		} else {
			methodArgsArray = methodArgs.split(",");
		}

		if (className == null || methodName == null || sessionId == null) {
			throw new ServletException(
					"className, methodName, sessionId params must be present");
		}

		LOG.info(String.format(
				"CommandServlet called, class:%s method:%s args:%s", className,
				methodName, methodArgs));


		/* Verify session id */
		try {
			if (EucalyptusServiceImpl.verifySession(new Session( sessionId) ) == null) {
				throw new ServletException("Invalid session id");
			}
		} catch (EucalyptusServiceException ex) {
			LOG.error(ex);
			throw new ServletException(ex);
		}

		// (Only get methods which have n String params corresponding to passed args)
		Class[] params = new Class[methodArgsArray.length];
		for (int j = 0; j < params.length; j++) {
			params[j] = String.class;
		}

		Method method = null;
		try {
			method = Class.forName(className)
					.getDeclaredMethod(methodName, params);
		} catch (Exception ex) {
			LOG.error(ex);
			throw new ServletException(ex);
		}


		if (! method.isAnnotationPresent(ExposedCommand.class)) {
			throw new ServletException("Method lacks ExposedCommand annotation");			
		}


		try {
			method.invoke(null, (Object[]) methodArgsArray);
		} catch (Exception ex) {
			LOG.error("Invocation failed of method" + method, ex);
			throw new ServletException(ex);
		}

		res.setStatus(HttpServletResponse.SC_OK);
	}
}
