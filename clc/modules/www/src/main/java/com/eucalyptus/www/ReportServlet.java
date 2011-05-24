package com.eucalyptus.www;

import java.io.IOException;
import java.io.PrintWriter;

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
	private static Logger LOG = Logger.getLogger( ReportServlet.class );

	
	private void exportReport(HttpServletRequest req, HttpServletResponse res)
		throws IOException
	{
		//TODO: res stuff from enum
		
		final ReportType reportType = ReportType.valueOf(Param.type.get(req));
		final ReportFormat format = ReportFormat.valueOf(Param.format.get(req));

		final long start = Long.parseLong(Param.start.get(req));
		final long end = Long.parseLong(Param.end.get(req));
		final Period period = new Period(start, end);
		final int criterionId = Integer.parseInt(Param.criterionId.get(req));
		final int groupById = Integer.parseInt(Param.groupById.get(req));
		// TODO: explain magic num
		final ReportingCriterion criterion = ReportingCriterion.values()[criterionId + 1];
		// TODO: configurable
		final Units displayUnits = Units.DEFAULT_DISPLAY_UNITS;

		// TODO: explain magic num
		final ReportingCriterion groupByCriterion =
			(groupById > 0)
			? ReportingCriterion.values()[groupById - 1]
			: null;
					
		ReportGenerator.generateReport(reportType, format, period, criterion,
			groupByCriterion, displayUnits,	res.getOutputStream());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		for (Param p : Param.values()) {
			try {
				p.get(req);
			} catch (IllegalArgumentException e) {
				if (p.isRequired()) {
					LOG.debug(e, e);
					throw new RuntimeException(e);
				}
			}
		}

		try {
			this.verifySession(Param.session.get());
		} catch (NoSuchFieldException e) {
			LOG.debug(e, e);
			throw new RuntimeException(e);
		}
		for (Param p : Param.values()) {
			try {
				LOG.debug(String.format("REPORT: %10.10s=%s", p.name(), p.get()));
			} catch (NoSuchFieldException e1) {
				LOG.debug(String.format("REPORT: %10.10s=%s", p.name(),
						e1.getMessage()));
			}
		}
		this.exportReport(req, res);
	  
	}


	enum Param {
		type, format, session, start, end,
		criterionId, groupById;

		private String value = null;
		private Boolean required = Boolean.TRUE;

		public Boolean isRequired()
		{
			return this.required;
		}

		private Param(String value, Boolean required)
		{
			this.value = value;
			this.required = required;
		}

		private Param()
		{
		}

		private Param(Boolean required)
		{
			this.required = required;
		}

		public String get() throws NoSuchFieldException
		{
			if (this.value == null) {
				throw new NoSuchFieldException();
			} else {
				return this.value;
			}
		}

		public String get(HttpServletRequest req)
				throws IllegalArgumentException
		{
			if (req.getParameter(this.name()) == null) {
				throw new IllegalArgumentException("'" + this.name()
						+ "' is a required argument.");
			} else {
				this.value = req.getParameter(this.name());
				LOG.debug("Found parameter: " + this.name() + "=" + this.value);
				return this.value;
			}
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
	
	enum Type
	{
		pdf {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				res.setContentType("application/pdf");
				res.setHeader("Content-Disposition", "file; filename=" + name
						+ ".pdf");
				JRExporter exporter = new JRPdfExporter();
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM,
						res.getOutputStream());
				return exporter;
			}
		},
		csv {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				res.setContentType("text/plain");
				res.setHeader("Content-Disposition", "file; filename=" + name
						+ ".csv");
				JRExporter exporter = new JRCsvExporter();
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM,
						res.getOutputStream());
				return exporter;
			}
		},
		html {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				PrintWriter out = res.getWriter();
				res.setContentType("text/html");
				JRExporter exporter = new JRHtmlExporter();
				exporter.setParameter(new JRExporterParameter("EUCA_WWW_DIR")
				{
				}, "/");
				exporter.setParameter(JRExporterParameter.OUTPUT_WRITER,
						res.getWriter());
				exporter.setParameter(
						JRHtmlExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS,
						Boolean.TRUE);
				exporter.setParameter(
						JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN,
						Boolean.FALSE);
				exporter.setParameter(
						JRHtmlExporterParameter.IGNORE_PAGE_MARGINS,
						Boolean.TRUE);
				return exporter;
			}

			@Override
			public void close(HttpServletResponse res) throws IOException
			{
				res.getWriter().close();
			}
		},
		xls {
			@Override
			public JRExporter getReportExporter(HttpServletRequest request,
					HttpServletResponse res, String name) throws IOException
			{
				res.setContentType("application/vnd.ms-excel");
				res.setHeader("Content-Disposition", "file; filename=" + name
						+ ".xls");
				JRExporter exporter = new JRXlsExporter();
				exporter.setParameter(JRExporterParameter.OUTPUT_STREAM,
						res.getOutputStream());
				return exporter;
			}
		};
		public abstract JRExporter getReportExporter(HttpServletRequest request,
				HttpServletResponse res, String name) throws IOException;

		public void close(HttpServletResponse res) throws IOException
		{
			res.getOutputStream().close();
		}
	}
	
}
