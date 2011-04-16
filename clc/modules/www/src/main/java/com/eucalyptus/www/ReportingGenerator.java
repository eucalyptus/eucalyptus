package com.eucalyptus.www;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.*;

/**
 * <p>This servlet will generate or remove fake reporting data. It can be used
 * for testing. It accepts two HTTP params: <code>command</code> and
 * <code>target</code>. Command can be either "generate" or "remove", and target
 * can be either "instance" (which means generate/remove fake instance reporting
 * data) or "storage" (which means generate/remove fake storage reporting data).
 */
public class ReportingGenerator
	extends HttpServlet
{

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		String command = req.getParameter("command");
		String target = req.getParameter("target");

		if (command == null || target == null) {
			throw new ServletException(
					"Command and target params must both be present");
		}

		int rv = 0;
		if (command.equalsIgnoreCase("generate")
				&& target.equalsIgnoreCase("instance")) {

			rv = com.eucalyptus.reporting.instance.FalseDataGenerator
					.generateFalseData();
			res.setStatus((rv == 0)
					? HttpServletResponse.SC_ACCEPTED
					: HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} else if (command.equalsIgnoreCase("remove")
				&& target.equalsIgnoreCase("instance")) {

			rv = com.eucalyptus.reporting.instance.FalseDataGenerator
					.removeFalseData();
			res.setStatus((rv == 0)
					? HttpServletResponse.SC_ACCEPTED
					: HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} else if (command.equalsIgnoreCase("generate")
				&& target.equalsIgnoreCase("storage")) {

			rv = com.eucalyptus.reporting.storage.FalseDataGenerator
					.generateFalseData();
			res.setStatus((rv == 0)
					? HttpServletResponse.SC_ACCEPTED
					: HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} else if (command.equalsIgnoreCase("remove")
				&& target.equalsIgnoreCase("storage")) {

			rv = com.eucalyptus.reporting.storage.FalseDataGenerator
					.removeFalseData();
			res.setStatus((rv == 0)
					? HttpServletResponse.SC_ACCEPTED
					: HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} else {

			res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

		}

	}
}
