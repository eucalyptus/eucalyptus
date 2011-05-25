package com.eucalyptus.www;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.export.*;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.units.Units;
import com.google.gwt.user.client.rpc.SerializableException;

import edu.ucsb.eucalyptus.admin.server.EucalyptusWebBackendImpl;
import edu.ucsb.eucalyptus.admin.server.SessionInfo;

public class ReportServlet
	extends HttpServlet
{
	private static final Logger LOG = Logger.getLogger( ReportServlet.class );


	/**
	 * <p>Expects the following servlet params:
	 * type,format,session,start,end,criterion,groupByCriterion
	 * 
	 * <p>Type, format, criterion, and groupByCriterion are taken from the
	 * enums: ReportType, ReportFormat, and ReportingCriterion.
	 * GroupByCriterion can also have the value "None". Start and
	 * end are in milliseconds. Session is a session id string.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		this.verifySession(Param.session.get(req));
		
	
		/* Parse all params
		 */
		final ReportType reportType = ReportType.valueOf(Param.type.get(req));
		final ReportFormat format = ReportFormat.valueOf(Param.format.get(req));
		final long start = Long.parseLong(Param.start.get(req));
		final long end = Long.parseLong(Param.end.get(req));
		final Period period = new Period(start, end);
		final ReportingCriterion criterion =
			ReportingCriterion.valueOf(Param.criterion.get(req));
		// TODO: configurable
		final Units displayUnits = Units.DEFAULT_DISPLAY_UNITS;
		
		ReportingCriterion groupByCriterion = null;
		//GroupByCriterion can optionally have value "None"; check for it
		if (req.getParameter(Param.groupByCriterion.name()).equalsIgnoreCase("None")) {
			groupByCriterion =
				ReportingCriterion.valueOf(Param.groupByCriterion.get(req));
		}
		LOG.info(String.format("Params: type:%s format:%s period:%s"
				+ "criterion:%s groupBy:%s", reportType, format, period,
				criterion, groupByCriterion));

		
		
		/* Set servlet response content type, etc, based upon report format.
		 */
		setContentTypeHeader(res, format, Param.type.get(req));
		
		
		/* Generate the report and send it thru the OutputStream
		 */
		ReportGenerator.generateReport(reportType, format, period, criterion,
				groupByCriterion, displayUnits,	res.getOutputStream());
	  
	}

	
	private enum Param {
		type, format, session, start, end,
		criterion, groupByCriterion;

		public String get(HttpServletRequest req)
				throws IllegalArgumentException
		{
			if (req.getParameter(this.name()) == null) {
				throw new IllegalArgumentException("'" + this.name()
						+ "' is a required argument.");
			} else {
				return req.getParameter(this.name());
			}
		}
	}

	private void setContentTypeHeader(HttpServletResponse res,
			ReportFormat format, String filename)
	{
		switch (format) {
			case csv:
				res.setContentType("text/plain");
				res.setHeader("Content-Disposition", "file; filename="
						+ filename + ".csv");
				break;
			case html:
				res.setContentType("text/html");
				break;
			case pdf:
				res.setContentType("application/pdf");
				res.setHeader("Content-Disposition", "file; filename="
						+ filename + ".pdf");
				break;
			case xls:
				res.setContentType("application/vnd.ms-excel");
				res.setHeader("Content-Disposition", "file; filename="
						+ filename + ".xls");
				break;
		}		
	}
	
	private void verifySession(String sessionId)
	{
		SessionInfo session;
		try {
			session = EucalyptusWebBackendImpl.verifySession(sessionId);
			User user = null;
			try {
				user = Accounts.lookupUserByName(session.getUserId());
			} catch (Exception e) {
				throw new RuntimeException("User does not exist");
			}
			if (!user.isSystemAdmin()) {
				throw new RuntimeException(
						"Only administrators can view reports.");
			}
		} catch (SerializableException e1) {
			throw new RuntimeException("Error obtaining session info.");
		}
		session.setLastAccessed(System.currentTimeMillis());
	}
	
}
